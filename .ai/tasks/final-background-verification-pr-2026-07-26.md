# Task: Финальный PR — runtime верификация фона, интеграционный тест, устойчивость

## Контекст

PR #56–60 добавили персональный фон главного экрана. После 5 итераций:
- #56: feat — добавлен HrmMainScreen с SVG-фонами
- #57: fix — ResourceReference в AfterShow (вместо BeforeShow)
- #58: fix — регистрация hrmMainScreen в web-screens.xml
- #59: fix — удалён конфликт legacy-регистрации
- #60: fix — inline CSS вместо Page.getStyles().add() + поправлен legacy web-app.properties

Фон всё ещё **не проходит полную верификацию** на уровне runtime.

---

## План работ

### P1: Runtime-верификация уже слитого SHA

HEAD: `f1e95162e7a2` (сейчас `30e37542` после добавления task-файла)

Проверить на fresh deploy + новый браузерный сеанс:

1. **Какой контроллер реально загружен** — `HrmMainScreen` или старый `ExtMainScreen`?
2. **Эффективный `cuba.web.mainScreenId`** — убедиться что `hrmMainScreen`, а не `extMainScreen`
3. **DOM-маркеры** — есть ли data-атрибут или CSS-класс фона на root-контейнере
4. **Inline `background-image`** — применяется ли CSS-свойство к элементу
5. **HTTP 200 connector-ресурса** — URL из `background-image` должен возвращать 200
6. **Системный фон** — без пользовательского файла отображается SVG-композиция
7. **Пользовательский фон** — после загрузки PNG/JPG в SettingsWindow отображается выбранный файл

### P1: Интеграционный smoke-тест вместо source-contract

`MainScreenBackgroundContractTest` проверяет только `contains()` на Java-файлах. Он не доказывает что фон реально доходит до браузера.

Нужен новый тест (screen-level или browser-level):

1. Выполнить login (CUBA TestContainer)
2. Подтвердить класс root screen — `HrmMainScreen`
3. Найти DOM-элемент по маркеру `data-hrm-main-background="applied"`
4. Прочитать computed style `background-image`
5. Извлечь URL из CSS
6. Проверить HTTP 200 и MIME-type ресурса
7. Сделать screenshot assertion, если возможно

### P2: Один выделенный background layer

Сейчас фон назначается одновременно `mainVBox` и `mainDashboard`:
```java
vaadinLayout.addStyleName(...)
vaadinDashboard.addStyleName(...)
```

Это может давать:
- двойную отрисовку изображения
- разное кадрирование `background-size: cover`
- несовпадение позиции при разных размерах контейнеров
- лишнюю перерисовку

Устойчивый вариант:
```
mainVBox
├── backgroundLayer  ← только сюда фон
└── mainDashboard    ← transparent, z-index выше
```

Фон назначается только `backgroundLayer`, dashboard получает прозрачный фон и локальный `z-index`.

### P2: Мгновенное обновление фона после SettingsWindow

Сейчас фон применяется только в `AfterShowEvent` главного экрана. После сохранения изображения в SettingsWindow пользователь должен выйти и войти заново.

Добавить локальное application event:
```java
MainScreenBackgroundChangedEvent
```

`HrmMainScreen` подписывается на событие и вызывает `resolveForUser()` + `applyBackground()` повторно. Это даст:
- пользовательский ↔ системный фон без перезахода
- замена одного изображения на другое

### P3: Валидация и оптимизация загружаемых изображений

Текущая проверка — только по расширению файла. 15 МБ лимит, но:
- содержимое не валидируется (magic bytes / MIME)
- размеры изображения не проверяются (можно загрузить 20000×20000)
- нет автоматического уменьшения
- нет очистки EXIF
- нет защиты от decompression bomb

Добавить:
- проверку magic bytes
- декодирование перед сохранением
- запрет чрезмерных размеров
- автоматический ресайз до разумного разрешения
- удаление EXIF
- ограничение итогового файла ~2–4 МБ

### P3: Исключить немедленное повторение случайного SVG-варианта

Сейчас `ThreadLocalRandom.current().nextInt(VARIANT_COUNT)` может выдать тот же номер подряд. Сохранять последний вариант в сессии пользователя и исключать его из выбора.

---

## Приоритет

| Приоритет | Задача |
|-----------|--------|
| **P1** | Runtime-верификация фона на fresh deploy |
| **P1** | Интеграционный browser/integration тест |
| **P2** | Один выделенный background layer |
| **P2** | Мгновенное обновление через событие |
| **P3** | Валидация и оптимизация загружаемых изображений |
| **P3** | Исключить повтор случайного SVG-варианта |

## Файлы для изменения

- `modules/web/src/com/company/hunttech/web/screens/mainscreen/HrmMainScreen.java`
- `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java`
- `modules/core/test/com/company/hunttech/core/MainScreenBackgroundContractTest.java` (новый integration-тест)
- `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-main-background.xml`
- docs: `docs/ui/HrmMainScreen_Spec.md`, `docs/ui/ExtSettingsWindowMainBackground_Spec.md`

## Проверка

```bash
cd /Users/alekseyananyev/StudioProjects/hunttech_recruiting
export JAVA_HOME=$(/usr/libexec/java_home -v 11)

# Компиляция
./gradlew :app-web:compileJava :app-core:compileTestJava --no-daemon --stacktrace

# Тесты
./gradlew :app-core:test --tests 'com.company.hunttech.core.MainScreenBackgroundContractTest' --no-daemon --stacktrace

# SCSS + сборка
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace

# Deploy + перезапуск
APP_CONTEXT=hrm ./scripts/rebuild-widgetset-and-start.sh
```

После deploy — новый браузерный сеанс, `http://localhost:8080/hrm/?restartApplication`.

# HrmMainScreen — тематический фон главного экрана

> Экран HRM HuntTech: `hrmMainScreen`.  
> Контроллер: `com.company.hunttech.web.screens.mainscreen.HrmMainScreen`.  
> Сервис выбора ресурса: `MainScreenBackgroundService`.  
> Базовый экран: `ExtMainScreen`.  
> XML: `hrm-main-screen.xml`, наследует `ext-main-screen.xml`.

## Назначение и бизнес-смысл (What & Why)

Главный экран является основной рабочей точкой dashboard HRM HuntTech. Фон персонализирует рабочую область, но не должен влиять на меню, widgets, уведомления, favicon, резервы и загрузку бизнес-данных.

Стандартные фоны являются частью визуальной темы и поставляются как статические JPG-файлы. Это отделяет дизайн темы от Java-кода, исключает runtime-генерацию SVG и позволяет проверять полноту каталога на этапе сборки. Пользовательский фон остаётся отдельным персональным ресурсом в FileStorage и имеет приоритет над тематическим каталогом.

Недоступность физического файла при сохранённом `FileDescriptor` не должна блокировать вход пользователя. В этом случае экран временно возвращается к системному фону, не удаляя metadata: после восстановления FileStorage персональный фон снова применяется автоматически.

## UI Context & Navigation

Экран создаётся через Screens API CUBA Platform 7.3:

- `cuba.web.mainScreenId=hrmMainScreen`;
- `@UiController("hrmMainScreen")`;
- `@UiDescriptor("hrm-main-screen.xml")`;
- legacy-регистрация `hrmMainScreen` в `web-screens.xml` отсутствует.

Персональный файл задаётся в `SettingsWindow` → «Интерфейс» → «Фон главного экрана». После успешного «ОК» текущая браузерная вкладка получает `MainScreenBackgroundChangedEvent` и обновляет фон без повторного входа.

## Behavior Summary

- вход → создаётся фактический `HrmMainScreen` → наследуемая бизнес-логика `ExtMainScreen` выполняется без изменений;
- `AfterShowEvent` → Vaadin UI готов → `MainScreenBackgroundService` разрешает ресурс;
- `UserSettings.fileImageFace` содержит файл с префиксом `hrm-main-background-` → `FileLoader.openStream()` проверяет физическую доступность FileStorage;
- пользовательский файл доступен → создаётся `ExternalResource` с прямым URL `/{context}/dispatch/download?f={uuid}`;
- пользовательский файл отсутствует или FileStorage возвращает ошибку → исключение перехватывается → descriptor сохраняется в БД → выбирается системный фон активной темы;
- пользовательского descriptor нет → выбирается случайный `1.jpg … 10.jpg` из `backgrounds/` активной темы;
- предыдущий индекс темы известен в `UserSession` → немедленное повторение исключается;
- ресурс разрешён → динамический локальный CSS назначает фон только `mainVBox`;
- CSS применён → изображение растягивается до `100% × 100%` рабочей области без пустых полос и обрезки;
- dashboard получает прозрачный локальный стиль → фон остаётся видимым под рабочими widgets;
- SettingsWindow успешно сохранён → публикуется UI-scoped event → фон обновляется без перезахода;
- ошибка декоративного фона → записывается warning/error → открытие главного экрана не блокируется.

## Каталог тематических фонов

Для каждой поддерживаемой темы репозиторий содержит ровно десять JPG-файлов размером `1920 × 1080`:

```text
modules/web/themes/{theme}/backgrounds/
├── 1.jpg
├── 2.jpg
├── ...
└── 10.jpg
```

Поддерживаемые темы:

```text
halo
havana
helium
hover
hunttech-modern
hunttech-modern-light
hunttech-modern-dark
```

Изображения нейтральные, без текста и логотипов, с палитрой соответствующей темы. В Java-коде отсутствуют SVG-шаблоны, палитры и генераторы геометрии.

## Разрешение theme-ресурса

`MainScreenBackgroundService` хранит индекс варианта в диапазоне `0..9`, а имя файла формирует как `variant + 1`. Для стандартного режима возвращается:

```java
new ThemeResource("backgrounds/" + (variant + 1) + ".jpg")
```

`ThemeResource` разрешает путь относительно активной Vaadin-темы:

```text
VAADIN/themes/{activeTheme}/backgrounds/{1..10}.jpg
```

Allowlist поддерживаемых тем используется для нормализации имени и отдельного session-ключа. Неизвестное или отсутствующее имя темы нормализуется к `hover`.

## Пользовательский фон

Пользовательская цепочка сохранения не меняется:

```text
Upload → MainScreenBackgroundImageProcessor → FileStorage
→ UserSettings.fileImageFace
```

Контракт пользовательского файла:

- имя начинается с `hrm-main-background-`;
- файл хранится в FileStorage;
- файл имеет приоритет над тематическим ресурсом;
- legacy-фотография в `fileImageFace` без указанного префикса не считается фоном;
- при Cancel временно созданные файлы очищаются по существующему контракту SettingsWindow;
- перед выдачей URL сервис открывает поток через `FileLoader.openStream()` и сразу закрывает его;
- `FileStorageException` или `IOException` преобразуются в системный fallback;
- недоступный `FileDescriptor` не удаляется из БД;
- browser URL формируется через существующий helper:

```text
/{cuba.webContextName}/dispatch/download?f={fileDescriptor.uuid}
```

Прямой dispatch URL не зависит от Vaadin connector resource key и остаётся одинаковым после обновления UI или перезапуска Tomcat. Фактическая выдача файла по-прежнему зависит от доступности FileStorage и штатной web-аутентификации CUBA.

## Применение ресурса в HrmMainScreen

Архитектура контроллера:

```text
mainVBox (динамический CSS-класс фона)
└── mainDashboard (локальный прозрачный стиль)
```

Скрытый Vaadin `Image`, `ResourceReference` и connector URL для пользовательского фона не используются.

Контроллер разрешает только два штатных типа:

1. `ThemeResource` → `VAADIN/themes/{theme}/backgrounds/{n}.jpg`;
2. `ExternalResource` → `/{context}/dispatch/download?f={uuid}`.

После разрешения URL контроллер:

1. создаёт уникальный локальный CSS-класс;
2. назначает `background-image`, `background-position`, `background-repeat` и `background-size` на `mainVBox`;
3. устанавливает `background-size: 100% 100%`, чтобы изображение совпадало с шириной и высотой рабочей области;
4. оставляет dashboard прозрачным.

### Масштабирование на дисплеях

Фон растягивается независимо по ширине и высоте контейнера `mainVBox`. Контракт сознательно использует `100% 100%`, а не `cover`:

- вся рабочая область главного экрана заполнена;
- пустые полосы отсутствуют;
- изображение не обрезается по краям;
- при несовпадении соотношения сторон файла и дисплея допускается изменение визуальных пропорций изображения;
- логика выбора тематического или пользовательского ресурса не зависит от разрешения дисплея.

## Runtime-маркеры

| Элемент | Маркер | Назначение |
|---|---|---|
| `mainVBox` | `data-hrm-main-background="applied"` | подтверждает выполнение background lifecycle |
| `mainVBox` | `data-hrm-main-controller="HrmMainScreen"` | подтверждает фактический root controller |

Назначенные через `HtmlAttributes` значения проверяются integration-тестом. Реальный computed style, HTTP-статус theme/dispatch-ресурса и screenshot проверяются Hermes после clean deploy.

## Выбор системного варианта

Для каждой из семи тем сохраняется последний индекс `0..9` в `UserSession`. Следующий индекс выбирается из девяти остальных вариантов. Состояние относится только к текущей пользовательской сессии и не записывается в БД.

## Событие обновления

`MainScreenBackgroundChangedEvent` реализует `UiEvent`. Событие:

- публикуется только после успешной commit-цепочки SettingsWindow;
- доставляется синхронно в текущую браузерную вкладку;
- не обновляет чужие UI-сеансы;
- не публикуется при Cancel или неуспешной загрузке.

## Автоматические проверки

`MainScreenBackgroundContractTest` проверяет:

1. отсутствие `createSvg`, `createPalettes`, `Palette` и MIME `image/svg+xml`;
2. использование `ThemeResource` с путём `backgrounds/{1..10}.jpg`;
3. наличие allowlist всех семи тем;
4. наличие ровно десяти файлов в каждом `theme/backgrounds`;
5. имена `1.jpg … 10.jpg`;
6. декодирование каждого файла через `ImageIO`;
7. размер каждого изображения `1920 × 1080`;
8. сохранение приоритета пользовательского фона и session-антиповтора;
9. применение `background-size: 100% 100%` и отсутствие `background-size: cover`;
10. перехват ошибок `FileLoader.openStream()` и системный fallback;
11. прямой dispatch URL через `ExternalResource`;
12. отсутствие `ResourceReference`, скрытого `Image` и `app://APP`;
13. неизменность SettingsWindow/FileStorage-цепочки.

`HrmMainScreenIntegrationTest` проверяет фактический root screen, `mainVBox`, dashboard, DOM-маркеры, отсутствие удалённого `mainScreenBackgroundLayer` и обновление системного URL по `MainScreenBackgroundChangedEvent`.

## Проверки Hermes

Hermes проверяет точный HEAD PR:

- `git diff --check`;
- compile web/core tests;
- `MainScreenBackgroundContractTest`;
- `HrmMainScreenIntegrationTest`;
- `ScreenViewIntegrityTest 8/8`;
- сборку SCSS семи тем;
- `clean assemble`;
- clean local deploy;
- `/hrm/` = HTTP 200;
- системный фон для каждой из семи тем;
- пользовательский URL имеет вид `/hrm/dispatch/download?f={uuid}` и не содержит `connector` или `app://APP`;
- dispatch-ресурс отвечает HTTP 200 и `image/jpeg` до и после перезапуска Tomcat;
- отсутствующий физический файл при сохранённом descriptor не блокирует вход и включает системный fallback;
- descriptor не удаляется при временной недоступности FileStorage;
- computed `background-size: 100% 100%` на `1366 × 768`, `1920 × 1080` и `2560 × 1440`;
- отсутствие пустых полос и обрезки изображения на указанных разрешениях;
- отсутствие немедленного повтора после обновления/перезахода;
- отсутствие Tomcat critical errors, P1 и P2.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Пользовательский фон переведён с Vaadin connector на прямой FileStorage dispatch URL; ошибка чтения файла включает системный fallback без удаления descriptor |
| 2026-07-26 | Фон главного экрана переведён с `cover` на точное растягивание `100% × 100%` для заполнения рабочей области на любом дисплее |
| 2026-07-26 | Стандартные фоны вынесены из Java runtime-SVG в каталоги семи тем: 7 × 10 JPG; контрактный тест проверяет состав и формат файлов |
| 2026-07-26 | Добавлен единый background layer, UI-scoped refresh, исключение повтора SVG и screen-level integration test |
| 2026-07-26 | Синхронизированы оба `mainScreenId`; динамический фон переведён на inline CSS через `HtmlAttributes` |
| 2026-07-26 | Удалена несовместимая legacy-регистрация `hrmMainScreen` |
| 2026-07-26 | Добавлен каталог 7 × 10 и приоритет персонального фона |

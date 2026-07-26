# Task: Исправить загрузку фона в SettingsWindow — вкладка «Интерфейс»

## Контекст

PR #56–60 добавили настройку фона главного экрана в SettingsWindow → вкладка «Интерфейс». 
UI Spec: `docs/ui/ExtSettingsWindowMainBackground_Spec.md`
Контроллер: `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindowMainBackground.java`
Дескриптор: `modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-main-background.xml`

## Проблема

Пользователь сообщает:

1. **Кнопка «Загрузить» не работает** — нажатие не открывает диалог выбора файла
2. **Нет отображения имени файла** после загрузки
3. **Нет кнопки «Очистить»** рядом, которая очищает выбор и переводит главный экран на системные фоны
4. **Label статуса не обновляется** — должно быть:
   - `"Используется случайный фон активной темы."` — когда персонального фона нет
   - `"Используется пользовательский фон."` — когда загружен свой фон

## Диагноз

### Проблема 1: `<upload>` без databinding и `uploadButton`

В `ext-settings-window-main-background.xml`:
```xml
<upload id="mainScreenBackgroundUpload"
        fileStoragePutMode="IMMEDIATE"
        caption="Выбрать изображение"
        width="100%"
        showFileName="false"
        showClearButton="false"/>
```

- Нет `datasource` и `property` — файл может не привязываться к сущности
- `showFileName="false"` — имя файла не показывается
- Может отсутствовать внутренняя кнопка upload'а

### Проблема 2: Статус-лейбл не обновляется после commit/cancel

В контроллере `onMainScreenBackgroundUploaded()` (строка 142-170):
- После upload файл коммитится через `dataManager.commit(uploaded)`
- `userSettingsDs.getItem().setFileImageFace(committedDescriptor)` — напрямую меняет datasource
- После сохранения (commit) формы статус может сброситься

### Проблема 3: Нет визуальной обратной связи

- После выбора файла пользователь не видит имени файла
- Нет индикации что файл загружается/загружен

## Что требуется сделать

1. **Починить `<upload>`** — проверить что `FileUploadField` корректно открывает диалог и загружает файл. Возможно нужно добавить вложенный `<uploadButton>` или изменить параметры.
2. **Показывать имя файла** после загрузки — либо через `showFileName="true"`, либо через label рядом.
3. **Добавить отдельную кнопку «Очистить»** — сейчас есть `clearMainScreenBackgroundBtn` (строка 70-74), проверить что она корректно работает.
4. **Статус-лейбл** — `mainScreenBackgroundStatusLabel` должен обновляться при загрузке, очистке и открытии формы.

## Проверка

```bash
cd /Users/alekseyananyev/StudioProjects/hunttech_recruiting
./gradlew compileJava deploy -x test --no-daemon --stacktrace
```

Перезапустить Tomcat. Открыть SettingsWindow → вкладка «Интерфейс». Проверить:
- Клик по кнопке загрузки открывает диалог выбора файла
- После выбора PNG/JPG появляется имя файла
- Статус-лейбл меняется на «персональное изображение»
- Кнопка «Очистить» сбрасывает на системные фоны
- Cancel не сохраняет изменения
- После выхода/входа фон сохраняется

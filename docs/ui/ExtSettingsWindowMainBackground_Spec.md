# ExtSettingsWindowMainBackground — настройка фона главного экрана

> Фактический screen ID: `settings`.  
> Контроллер: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindowMainBackground`.  
> Базовый controller chain: `ExtSettingsWindowInterfaceLayout` → `ExtSettingsWindowEmailNavigation` → `ExtSettingsWindow`.  
> XML: `ext-settings-window-main-background.xml`.

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Расширение даёт пользователю HRM HuntTech выбор между персональным изображением главного экрана и нейтральным тематическим каталогом. Настройка добавляется в существующую вкладку «Интерфейс» и не меняет оформление, расположение или бизнес-логику других вкладок.

### UI Context & Navigation

Окно открывается стандартным действием CUBA `settings`. В `web-screens.xml` существующий screen ID перенаправлен на extension descriptor, который наследует фактическую текущую компоновку `ext-settings-window-email-navigation.xml`.

Новая карточка `mainScreenBackgroundCard` вставляется после действующей карточки рабочего пространства. Она использует те же локальные классы `settings-section-card interface-settings-card`; отдельный SCSS не вводится.

### Behavior Summary

- открытие настроек → загружается существующий `UserSettings.fileImageFace` → маркированный файл отображается как текущий персональный фон;
- загрузка PNG/JPG/JPEG/WEBP → файл получает маркер `hrm-main-background-` → ссылка сохраняется вместе с `UserSettings`;
- нажатие «Очистить фоновое изображение» → маркированная ссылка очищается → после сохранения включается тематический случайный каталог;
- нажатие Cancel → прежняя сохранённая ссылка остаётся активной;
- успешный commit → заменённые маркированные файлы удаляются из fileStorage, если больше не являются активными;
- legacy-файл без маркера → не считается фоном и не очищается либо не удаляется новой логикой.

## 1. Технический контекст

| Компонент | Назначение |
|---|---|
| `mainScreenBackgroundUpload` | независимая загрузка файла с ручной записью ссылки в `userSettingsDs.fileImageFace` |
| `mainScreenBackgroundStatusLabel` | показывает персональный или тематический режим |
| `clearMainScreenBackgroundBtn` | очищает только маркированный фоновый файл |
| `MainScreenBackgroundService.CUSTOM_BACKGROUND_PREFIX` | отделяет фон от legacy-файла |

## 2. Связь с моделью данных

Изменения entity и БД отсутствуют. Используется существующий datasource:

| Datasource | Entity | View | Поле |
|---|---|---|---|
| `userSettingsDs` | `hunttech_UserSettings` | `userSettings-view` | `fileImageFace` |

`userSettings-view` уже включает `fileImageFace`, поэтому Data View Integrity не требует изменения `views.xml`. Upload намеренно не получает XML binding: контроллер устанавливает ссылку после проверки формата и маркировки файла.

## 3. Формат и ограничения файла

- PNG, JPG, JPEG, WEBP;
- максимальный размер 15 МБ;
- хранение в штатном CUBA fileStorage;
- рекомендуемое соотношение сторон 16:9;
- изображение масштабируется на главном экране через `background-size: cover`.

## 4. Изоляция изменений

Не изменяются:

- `ExtSettingsWindow.java`;
- `ExtSettingsWindowEmailNavigation.java`;
- `ExtSettingsWindowInterfaceLayout.java`;
- базовый `ext-settings-window.xml` и его существующая компоновка;
- вкладки «Обо мне», email и AI;
- AI, email, avatar, validation и commit-контракты;
- entity, таблицы, Liquibase, JPQL и production.

## 5. Проверки Hermes

Проверить загрузку, замену, очистку, Cancel и OK; повторный вход; все семь тем; неподдерживаемый файл; отсутствие удаления legacy-файла; сохранность email/AI/аватара и штатных interface-настроек.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Добавлена изолированная карточка загрузки и очистки персонального фона без изменения entity, БД и существующего оформления SettingsWindow |

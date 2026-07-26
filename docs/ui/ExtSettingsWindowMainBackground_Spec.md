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

Карточка `mainScreenBackgroundCard` вставляется после действующей карточки рабочего пространства. Изменение ограничено собственным namespace `main-screen-background-*`; соседние блоки вкладки и остальные вкладки не перестраиваются.

### Behavior Summary

- открытие настроек → загружается существующий `UserSettings.fileImageFace` → маркированный файл отображается как текущий персональный фон;
- загрузка PNG/JPG/JPEG/WEBP → файл получает маркер `hrm-main-background-` → ссылка сохраняется вместе с `UserSettings`;
- нажатие «Использовать системные фоны» → маркированная ссылка очищается → после сохранения включается тематический случайный каталог;
- нажатие Cancel → прежняя сохранённая ссылка остаётся активной;
- успешный commit → заменённые маркированные файлы удаляются из fileStorage, если больше не являются активными;
- legacy-файл без маркера → не считается фоном и не очищается либо не удаляется новой логикой.

## 1. Компоновка карточки

```text
mainScreenBackgroundCard
├─ header
│  ├─ заголовок и краткое пояснение
│  └─ status pill
├─ options
│  ├─ «Персональное изображение»
│  │  ├─ требования к файлу
│  │  └─ «Выбрать изображение»
│  └─ «Системные фоны»
│     ├─ пояснение 7 × 10
│     └─ «Использовать системные фоны»
└─ компактное примечание о приоритете
```

Красная destructive-кнопка исключена: очистка здесь является переключением режима отображения, а не удалением бизнес-данных. Действующий `invoke="clearMainScreenBackground"` и component ID сохранены.

## 2. Технический контекст

| Компонент | Назначение |
|---|---|
| `mainScreenBackgroundUpload` | независимая загрузка файла с ручной записью ссылки в `userSettingsDs.fileImageFace` |
| `mainScreenBackgroundStatusLabel` | компактно показывает персональный или тематический режим |
| `clearMainScreenBackgroundBtn` | переключает на системный каталог и очищает только маркированный фоновый файл |
| `MainScreenBackgroundService.CUSTOM_BACKGROUND_PREFIX` | отделяет фон от legacy-файла |
| `main-screen-background-settings.scss` | локальная геометрия карточки во всех семи темах |

## 3. Связь с моделью данных

Изменения entity и БД отсутствуют. Используется существующий datasource:

| Datasource | Entity | View | Поле |
|---|---|---|---|
| `userSettingsDs` | `hunttech_UserSettings` | `userSettings-view` | `fileImageFace` |

`userSettings-view` уже включает `fileImageFace`, поэтому Data View Integrity не требует изменения `views.xml`. Upload намеренно не получает XML binding: контроллер устанавливает ссылку после проверки формата и маркировки файла.

## 4. Формат и ограничения файла

- PNG, JPG, JPEG, WEBP;
- максимальный размер 15 МБ;
- хранение в штатном CUBA fileStorage;
- рекомендуемое соотношение сторон 16:9;
- изображение масштабируется на главном экране через `background-size: cover`.

## 5. Локальный SCSS-контракт

Файл `main-screen-background-settings.scss` подключён в:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

Все селекторы находятся внутри `.ext-settings-window .main-screen-background-card`. Глобальные `.v-button`, `.v-label` и другие Vaadin-селекторы не вводятся. Стили задают status pill, две равные визуальные опции, спокойную theme-aware поверхность и компактное примечание.

## 6. Изоляция изменений

Не изменяются:

- `ExtSettingsWindow.java`;
- `ExtSettingsWindowEmailNavigation.java`;
- `ExtSettingsWindowInterfaceLayout.java`;
- базовый `ext-settings-window.xml` и компоновка соседних блоков;
- вкладки «Обо мне», email и AI;
- AI, email, avatar, validation и commit-контракты;
- entity, таблицы, Liquibase, JPQL и production.

## 7. Проверки Hermes

Проверить загрузку, замену, очистку, Cancel и OK; повторный вход; все семь тем; неподдерживаемый файл; отсутствие удаления legacy-файла; визуальную изоляцию карточки; сохранность email/AI/аватара и штатных interface-настроек.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Карточка разделена на персональный и системный режимы, добавлены status pill и локальный SCSS для семи тем без изменения соседних блоков |
| 2026-07-26 | Добавлена изолированная карточка загрузки и очистки персонального фона без изменения entity, БД и существующего оформления SettingsWindow |

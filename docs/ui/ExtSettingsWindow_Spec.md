# ExtSettingsWindow — настройки пользователя

> Экран HRM HuntTech: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> XML: `ext-settings-window.xml`.  
> Связанные документы: [UserSettings](../entities/user-settings/UserSettings.md), [UserAiProfile](../entities/UserAiProfile.md), [UserAiContextService](../services/UserAiContextService.md).  
> Визуальный эталон: [SettingsWindow AI — Halo](renders/SettingsWindow_AI_Halo.svg).

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран объединяет персональные настройки пользователя. Вкладка «Обо мне» формирует профессиональный ИИ-профиль, чтобы сервисы HRM HuntTech адаптировали язык, глубину и структуру ответа к рабочей роли пользователя. Вкладка AI управляет персональными подключениями к провайдерам и хранит предпочтение пользователя по выбору личных либо административных настроек API.

Checkbox `preferPersonalAiApiSettingsField` только фиксирует предпочтение. Алгоритм маршрутизации вызовов, fallback на административные настройки и ограничения функций при отсутствии личного API будут реализованы отдельной задачей.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экран открывается из стандартного меню настроек CUBA. Содержит вкладки `msgMyInfo`, `msgInterface`, `mailAccessTab` и `aiAccessTab`.

- «Обо мне» работает с `ExtUser` и `hunttech_UserAiProfile`;
- интерфейсные параметры сохраняет базовый `SettingsWindow`;
- почтовые параметры и предпочтение источника API хранятся в `UserSettings`;
- вкладка AI показывает `UserAiConfiguration`, ограниченные текущим пользователем.

Вкладки «Обо мне» и AI используют единый визуальный язык утверждённых Edit-форм: фиксированная левая панель 270 px, вертикальная навигация, единый верхний toolbar и карточки в правой рабочей области.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- открытие → до инициализации контроллера web-блок получает `userAiProfile-view` из своего `ViewRepository`;
- открытие → загружаются `ExtUser`, `UserSettings`, `UserAiProfile` и персональные `UserAiConfiguration`;
- `preferPersonalAiApiSettings` отсутствует или равен `null` → применяется безопасное `false`;
- пользователь изменяет checkbox во вкладке AI → значение меняется в `userSettingsDs`;
- сохранение → `ExtUser`, `UserSettings` и `UserAiProfile` фиксируются единым `CommitContext`;
- значение checkbox сохраняется, но текущий алгоритм вызовов API не изменяется;
- выбор AI-конфигурации → становятся доступны редактирование, удаление и тест подключения;
- создание, редактирование, удаление и тестирование → выполняются прежними методами контроллера;
- визуальный рефакторинг → не меняет datasource, JPQL, component ID, `invoke`, enable-состояния и сервисы;
- загрузка аватара → изображение нормализуется существующим `ImageProcessingService`;
- профиль отсутствует → создаётся несохранённый экземпляр;
- открытие «Обо мне» → раскрыт только «Профессиональный профиль», остальные секции свёрнуты;
- предпросмотр → локально формируется очищенный контекст и раскрывается секция предпросмотра;
- включение профиля без согласия → сохранение блокируется;
- очистка → сбрасывается только `UserAiProfile`.

## 1. Invocation & Context

| Параметр | Значение |
|---|---|
| Контроллер | `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow` |
| Базовый класс | `SettingsWindow` |
| XML schema | legacy `window.xsd` |
| Data API | legacy `dsContext` |
| Messages pack | `com.company.hunttech.web.screens.extsettingswindow` |

Существующие component ID сохранены для совместимости: `settingsTabSheet`, `msgMyInfo`, `msgInterface`, `mailAccessTab`, `aiAccessTab`, `okBtn`, `cancelBtn`, `userAvatarUpload`, `userPic`, `defaultPic`, `personalAiApiPreferenceBox`, `preferPersonalAiApiSettingsField`, `aiConfigsButtonsPanel`, `aiConfigsCreateBtn`, `aiConfigsEditBtn`, `aiConfigsRemoveBtn`, `aiConfigsTestBtn`, `aiConfigsTable`.

Новые ID относятся только к визуальным контейнерам вкладки AI: `aiSettingsMainBox`, `aiSettingsSidebar`, `aiSettingsSidebarIdentity`, `aiSettingsNavigation`, `aiSettingsSecurityBox`, `aiSettingsContent`, `aiSettingsToolbar`, `aiSettingsTitleBox`, `aiConnectionsCard`, `aiConnectionsHeader`.

## 2. Data & Entity Binding

| Datasource | Entity | View | Назначение |
|---|---|---|---|
| `extUserDs` | `ExtUser` | `extUser-view` | аватар и данные пользователя |
| `userSettingsDs` | `UserSettings` | `userSettings-view` | почтовые параметры и `preferPersonalAiApiSettings` |
| `userAiProfileDs` | `hunttech_UserAiProfile` | `userAiProfile-view` | профессиональный профиль и узкая связь с владельцем |
| `userAiConfigsDs` | `hunttech_UserAiConfiguration` | `userAiConfiguration-view` | персональные подключения пользователя к AI-провайдерам |

`userAiProfile-view` объявлен в `modules/global/src/com/company/hunttech/user-ai-profile-views.xml` и зарегистрирован в рабочих свойствах core и web:

- `modules/core/src/com/company/hunttech/app.properties`;
- `modules/web/src/com/company/hunttech/web-app.properties`.

Регистрация только в `app-component.xml` недостаточна для standalone-запуска HRM HuntTech: web `ViewRepository` не получает именованный view, и `DsBuilder` завершает открытие окна с `ViewNotFoundException` до вызова `ExtSettingsWindow.init()`.

Пользовательские настройки загружаются запросом:

```jpql
select e from hunttech_UserSettings e where e.user = :currentUser
```

Профиль загружается запросом:

```jpql
select e from hunttech_UserAiProfile e where e.user = :currentUser
```

Конфигурации AI загружаются запросом:

```jpql
select e from hunttech_UserAiConfiguration e where e.user = :ds$extUserDs
```

Отсутствующие `UserSettings` и `UserAiProfile` создаются только в памяти и сохраняются после нажатия `okBtn`.

## 3. Form Hierarchy

```text
settingsTabSheet
├── msgMyInfo
│   └── userAiProfileMainBox
│       ├── userAiProfileSidebar
│       │   ├── dropZone / picVBox / userPic / defaultPic
│       │   ├── userAiProfileIdentity
│       │   ├── userAiProfileSummary
│       │   ├── userAiProfileSectionNavigation
│       │   └── userAiProfileSensitiveWarningBox
│       └── userAiProfileContentScrollBox
│           └── userAiProfileContent
│               ├── userAiProfileToolbar
│               │   └── previewAiContextBtn / clearAiProfileBtn
│               ├── professionalProfileGroup
│               ├── recruitingProfileGroup
│               ├── responsePreferencesGroup
│               ├── goalsGroup
│               ├── privacyGroup
│               └── previewGroup
├── msgInterface
├── mailAccessTab
└── aiAccessTab
    └── aiSettingsMainBox
        ├── aiSettingsSidebar
        │   ├── aiSettingsSidebarIdentity
        │   ├── aiSettingsNavigation
        │   └── aiSettingsSecurityBox
        └── aiSettingsContent
            ├── aiSettingsToolbar
            │   └── aiConfigsButtonsPanel
            │       ├── aiConfigsCreateBtn
            │       ├── aiConfigsEditBtn
            │       ├── aiConfigsRemoveBtn
            │       └── aiConfigsTestBtn
            ├── personalAiApiPreferenceBox
            │   └── preferPersonalAiApiSettingsField
            └── aiConnectionsCard
                └── aiConfigsTable
```

## 4. Business Behavior

### Безопасные значения по умолчанию

- предпочтение личных настроек API — выключено;
- профиль выключен;
- внешняя обработка запрещена;
- язык — автоматически;
- детализация — сбалансированно;
- стиль — нейтральный;
- терминология — профессиональная;
- структура — автоматически.

### Предпочтение источника API

`preferPersonalAiApiSettingsField` связан с `UserSettings.preferPersonalAiApiSettings`. Значение `false` сохраняет действующее поведение и не требует наличия личного API-ключа. Значение `true` только сохраняет намерение пользователя предпочитать персональные настройки.

Контроллер не выбирает провайдера, не проверяет наличие личных ключей и не меняет маршрутизацию. Сервисы `HrmAiService` и другие API-интеграции этой задачей не изменяются.

### Согласие

`profileEnabled=true` разрешено только при `externalProcessingAllowed=true`. При первом согласии сохраняются `consentVersion=2026-07-22-v1` и `consentAcceptedAt`. При отзыве согласия персонализация выключается.

### Предпросмотр

`previewAiContext()` вызывает `UserAiContextService.buildContextPreview()` и раскрывает `previewGroup` через совместимый с CUBA 7.3 вызов `setExpanded(true)`. HTTP к LLM не выполняется.

### AI-подключения

Создание и редактирование выполняются через `UserAiConfigurationEdit`. Владелец новой конфигурации принудительно задаётся из текущей пользовательской сессии. Тест подключения делегируется `HrmAiService`; контроллер не содержит endpoint и HTTP-логику провайдера.

## 5. Actions & Methods

| Метод | Назначение |
|---|---|
| `loadOrCreateUserSettings()` | загружает `UserSettings`, применяет default `false` и устанавливает `userSettingsDs` |
| `loadOrCreateUserAiProfile()` | загружает профиль или создаёт несохранённый |
| `initAiProfileOptions()` | задаёт локализованные enum options |
| `refreshProfileSummary()` | обновляет левую карточку профиля |
| `previewAiContext()` | показывает очищенный контекст |
| `clearAiProfile()` | запрашивает подтверждение очистки |
| `validateAiProfile()` | проверяет согласие и диапазон опыта |
| `prepareProfileConsent()` | фиксирует или отзывает согласие |
| `refreshAiConfigs()` | обновляет персональные AI-подключения |
| `refreshAiActionState()` | синхронизирует доступность действий с выбранной строкой |
| `onAiConfigsCreateBtnClick()` | создаёт персональное подключение |
| `onAiConfigsEditBtnClick()` | редактирует выбранное подключение |
| `onAiConfigsRemoveBtnClick()` | удаляет выбранное подключение |
| `onAiConfigsTestBtnClick()` | тестирует выбранное подключение через `HrmAiService` |
| `commit()` | сохраняет основные настройки единым `CommitContext` |

## 6. Layout & Components

### Вкладка «Обо мне»

Вкладка следует направлению дизайна `JobCandidateEdit`: фиксированная левая панель 270 px и правая рабочая область с вертикальными секциями. Левая панель разделена на аватар, идентичность, статусную карточку, индекс разделов и предупреждение о чувствительных данных.

Кнопки `previewAiContextBtn` и `clearAiProfileBtn` собраны в верхнем toolbar. Секции оформлены как локальные карточки `showAsPanel="true"`; при открытии формы раскрыта только `professionalProfileGroup`, остальные секции имеют `collapsed="true"`.

### Вкладка AI

Вкладка AI приведена к той же концепции Edit-форм:

- слева фиксированная панель шириной 270 px;
- в панели — маркер AI, назначение вкладки, вертикальный индекс «Источник API / Подключения» и предупреждение о защите ключей;
- справа — единый toolbar с заголовком и существующими действиями;
- настройка источника API оформлена отдельной акцентной карточкой;
- таблица персональных подключений находится в самостоятельной рабочей карточке;
- `aiConfigsButtonsPanel` визуально перенесён в toolbar, но component ID, кнопки, `invoke` и состояния `enable=false` сохранены;
- `preferPersonalAiApiSettingsField` сохраняет binding `userSettingsDs.preferPersonalAiApiSettings`;
- `aiConfigsTable` сохраняет datasource `userAiConfigsDs` и прежние колонки;
- Java-контроллер, JPQL, сервисы, entity, views и БД не изменены.

Стили ограничены корнями `.user-ai-profile-editor` и `.ai-settings-editor`. Глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet` не изменяются. Вложенные Vaadin-селекторы допускаются только внутри `.ai-settings-editor .ai-settings-table`.

При ширине окна 1200 px горизонтальная прокрутка не требуется. Левая панель сохраняет ширину, а правая область занимает остаток окна.

### Синхронизация тем

Изменение визуального контракта экрана в одной теме требует одновременной адаптации всех поддерживаемых тем в той же задаче и в том же PR. Частичное обновление только одной темы считается незавершённым.

Локальный контур `.ai-settings-editor` добавлен в `com.company.hunttech/user-ai-profile.scss` каждой поддерживаемой темы.

| Тема | SCSS-контур | Адаптация вкладки AI |
|---|---|---|
| `halo` | `modules/web/themes/halo/` | светлые карточки, синий акцент, мягкие тени |
| `havana` | `modules/web/themes/havana/` | светлые карточки, синий акцент, мягкие тени |
| `helium` | `modules/web/themes/helium/` | светлые карточки, синий акцент, мягкие тени |
| `hover` | `modules/web/themes/hover/` | светлые карточки, синий акцент, мягкие тени |
| `hunttech-modern` | `modules/web/themes/hunttech-modern/` | базовая современная светлая палитра |
| `hunttech-modern-light` | `modules/web/themes/hunttech-modern-light/` | светлая современная палитра |
| `hunttech-modern-dark` | `modules/web/themes/hunttech-modern-dark/` | отдельные тёмные поверхности, светлые границы, акцент `#64a8ff` |

Структура, размеры, отступы, component ID и состояния одинаковы во всех темах. Различаются только theme-aware цвета, прозрачность поверхностей, тени и контраст текста.

## 7. База данных

Колонка `HUNTTECH_USER_SETTINGS.PREFER_PERSONAL_AI_API_SETTINGS BOOLEAN NOT NULL DEFAULT FALSE` была добавлена предыдущей задачей.

Текущий визуальный рефакторинг:

- не меняет entity и поля;
- не добавляет SQL или Liquibase;
- не изменяет `HUNTTECH_USER_AI_PROFILE` и `HUNTTECH_USER_AI_CONFIGURATION`;
- не изменяет API-ключи и административные настройки;
- не требует изменения данных.

PROD в данный PR не входит.

## 8. Проверки

Обязательны:

- `git diff --check`;
- компиляция global/core/web;
- `UserSettingsAiApiPreferenceTest` — 6/6;
- `ScreenViewIntegrityTest` — 8/8;
- Data View Integrity для `ExtSettingsWindow`;
- `./gradlew :app-web:buildScssThemes --no-daemon --stacktrace`;
- `./gradlew clean assemble --no-daemon --stacktrace`;
- визуальная проверка вкладки AI во всех семи темах при ширине окна 1200 px;
- отсутствие горизонтальной прокрутки, обрезки toolbar и нечитаемых состояний таблицы.

`UserSettingsAiApiPreferenceTest` проверяет:

1. metadata-поле и default `false`;
2. binding checkbox;
3. двухпанельный layout и sidebar 270 px;
4. сохранность четырёх `invoke`-контрактов действий;
5. сохранность datasource таблицы;
6. наличие локального `.ai-settings-editor` во всех семи темах;
7. миграции ранее добавленного поля.

Тест `test5_userAiProfile_view_registered` продолжает проверять runtime-регистрацию `userAiProfile-view`.

Merge и deploy выполняются отдельно и не входят в данный PR.

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-23 | Вкладка AI приведена к двухпанельной концепции Edit-форм: sidebar 270 px, единый toolbar, карточки источника API и подключений; SCSS синхронизирован во всех семи темах; бизнес-логика, datasource, component ID и `invoke` сохранены |
| 2026-07-23 | Добавлен checkbox «Предпочитать использовать в запросах мои настройки API», поле `UserSettings.preferPersonalAiApiSettings`, миграции и `UserSettingsAiApiPreferenceTest`; алгоритм маршрутизации API намеренно не изменён |
| 2026-07-23 | Исправлена повторная `ViewNotFoundException`: `user-ai-profile-views.xml` зарегистрирован в рабочих конфигурациях core/web; тест 5 переведён на `HunttechTestContainer` и проверяет runtime `ViewRepository` и оба `cuba.viewsConfig` |
| 2026-07-23 | Дополнительный `user-ai-profile-views.xml` был зарегистрирован только через `app-component.xml`; решение признано недостаточным для standalone-запуска приложения |
| 2026-07-23 | Введено обязательное правило синхронизации тем; SettingsWindow адаптирован для `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light` и `hunttech-modern-dark` |
| 2026-07-23 | После merge-регрессии восстановлены двухпанельная вкладка «Обо мне» и контроллер `UserAiProfile`; функциональность объединена с действующей вкладкой персональных AI-подключений без изменения БД |
| 2026-07-23 | XML приведён к утверждённой двухпанельной структуре: добавлены профильная навигация, единый toolbar, карточки-аккордеоны и начальное сворачивание вторичных секций; обновлён локальный Halo SCSS |
| 2026-07-22 | Контроллер, datasource ИИ-профиля и JPQL перенесены в namespace `hunttech`; legacy-контракты `ExtUser`, `UserSettings` и messages pack сохранены |
| 2026-07-22 | Исправлено программное раскрытие секции предпросмотра ИИ-контекста: используется совместимый с CUBA 7.3 метод `setExpanded(true)` |
| 2026-07-22 | Вкладка «Обо мне» переработана в двухпанельный профессиональный ИИ-профиль; добавлены согласие, предпросмотр и атомарное сохранение |

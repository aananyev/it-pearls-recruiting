# ExtSettingsWindow — настройки пользователя

> Экран HRM HuntTech: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> XML: `ext-settings-window.xml`.  
> Связанные документы: [UserSettings](../entities/user-settings/UserSettings.md), [UserAiProfile](../entities/UserAiProfile.md), [UserAiContextService](../services/UserAiContextService.md).

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран объединяет персональные настройки пользователя. Вкладка «Обо мне» формирует профессиональный ИИ-профиль, чтобы сервисы HRM HuntTech адаптировали ответы к роли и опыту пользователя без изменения объективных данных рекрутмента. Отдельная вкладка AI управляет персональными подключениями к провайдерам и хранит предпочтение пользователя по выбору личных либо административных настроек API.

Новый checkbox только фиксирует предпочтение. Алгоритм маршрутизации вызовов, fallback на административные настройки и ограничения функций при отсутствии личного API будут спроектированы и реализованы отдельной задачей.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из стандартного меню настроек CUBA. Содержит вкладки `msgMyInfo`, `msgInterface`, `mailAccessTab` и `aiAccessTab`. «Обо мне» работает с `ExtUser` и `hunttech_UserAiProfile`; интерфейсные параметры сохраняет базовый `SettingsWindow`; почтовые параметры и предпочтение источника API — `UserSettings`; вкладка AI — коллекция `UserAiConfiguration`, ограниченная текущим пользователем.

Внутри вкладки «Обо мне» левая профильная панель показывает аватар, имя, должность, статус и заполненность профиля, а также вертикальный индекс разделов. Справа разделы представлены карточками-аккордеонами в том же порядке, что и индекс слева.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- открытие → до инициализации контроллера web-блок получает `userAiProfile-view` из своего `ViewRepository`;
- открытие → загружаются `ExtUser`, `UserSettings`, `UserAiProfile` и персональные `UserAiConfiguration`;
- `preferPersonalAiApiSettings` отсутствует или равен `null` → применяется безопасное `false`;
- пользователь изменяет checkbox во вкладке AI → значение меняется в `userSettingsDs`;
- сохранение → `ExtUser`, `UserSettings` и `UserAiProfile` фиксируются единым `CommitContext`;
- значение checkbox сохраняется, но текущий алгоритм вызовов API не изменяется;
- загрузка аватара → изображение нормализуется существующим `ImageProcessingService`;
- профиль отсутствует → создаётся несохранённый экземпляр;
- открытие вкладки → раскрыт только «Профессиональный профиль», остальные секции свёрнуты;
- предпросмотр → локально формируется очищенный контекст и раскрывается секция предпросмотра;
- включение без согласия → сохранение блокируется;
- очистка → сбрасывается только `UserAiProfile`;
- выбор AI-конфигурации → становятся доступны редактирование, удаление и тест подключения.

## 1. Invocation & Context

| Параметр | Значение |
|---|---|
| Контроллер | `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow` |
| Базовый класс | `SettingsWindow` |
| XML schema | legacy `window.xsd` |
| Data API | legacy `dsContext` |
| Messages pack | `com.company.hunttech.web.screens.extsettingswindow` |

Существующие component ID сохранены для совместимости: `settingsTabSheet`, `msgMyInfo`, `msgInterface`, `mailAccessTab`, `aiAccessTab`, `okBtn`, `cancelBtn`, `userAvatarUpload`, `userPic`, `defaultPic`. Новый component ID: `preferPersonalAiApiSettingsField`.

## 2. Data & Entity Binding

| Datasource | Entity | View | Назначение |
|---|---|---|---|
| `extUserDs` | `ExtUser` | `extUser-view` | аватар и данные пользователя |
| `userSettingsDs` | `UserSettings` | `userSettings-view` | почтовые параметры и `preferPersonalAiApiSettings` |
| `userAiProfileDs` | `hunttech_UserAiProfile` | `userAiProfile-view` | профессиональный профиль и узкая связь с владельцем |
| `userAiConfigsDs` | `hunttech_UserAiConfiguration` | `userAiConfiguration-view` | персональные подключения пользователя к AI-провайдерам |

`userAiProfile-view` объявлен в `modules/global/src/com/company/hunttech/user-ai-profile-views.xml`. Для реально запускаемого приложения файл обязан быть перечислен не только в `app-component.xml`, но и в рабочих свойствах обоих блоков:

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
    ├── personalAiApiPreferenceBox
    │   └── preferPersonalAiApiSettingsField
    ├── aiConfigsButtonsPanel
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
| `refreshProfileSummary()` | обновляет левую карточку |
| `previewAiContext()` | показывает очищенный контекст |
| `clearAiProfile()` | запрашивает подтверждение очистки |
| `validateAiProfile()` | проверяет согласие и диапазон опыта |
| `prepareProfileConsent()` | фиксирует или отзывает согласие |
| `refreshAiConfigs()` | обновляет персональные AI-подключения |
| `refreshAiActionState()` | синхронизирует доступность действий с выбранной строкой |
| `commit()` | сохраняет основные настройки единым `CommitContext` |

## 6. Layout & Components

Вкладка следует направлению дизайна `JobCandidateEdit`: фиксированная левая панель 270 px и правая рабочая область с вертикальными секциями. Левая панель разделена на аватар, идентичность, статусную карточку, индекс разделов и предупреждение о чувствительных данных.

Кнопки `previewAiContextBtn` и `clearAiProfileBtn` собраны в верхнем toolbar. Секции оформлены как локальные карточки `showAsPanel="true"`; при открытии формы раскрыта только `professionalProfileGroup`, остальные секции имеют `collapsed="true"`.

На вкладке AI новый checkbox расположен после пояснения вкладки и перед панелью действий персональных конфигураций. Новые глобальные стили не добавляются; используются стандартные компоненты CUBA и существующие отступы вкладки.

Стили вкладки «Обо мне» подключены через локальный mixin `user-ai-profile` и ограничены корнем `.user-ai-profile-editor`. Глобальные `.v-*` правила не добавляются. При ширине окна 1200 px горизонтальная прокрутка не требуется; содержимое справа прокручивается вертикально. Вкладки «Интерфейс», «Почта» и AI сохраняют действующие component ID и функциональные контракты.

### Синхронизация тем

Изменение визуального контракта экрана в одной теме требует одновременной адаптации всех поддерживаемых тем в той же задаче и в том же PR. Частичное обновление только одной темы считается незавершённым.

Новый checkbox использует стандартный theme-aware компонент CUBA и не требует локального SCSS. Его положение и читаемость должны проверяться во всех поддерживаемых темах.

| Тема | SCSS-контур | Адаптация SettingsWindow |
|---|---|---|
| `halo` | `modules/web/themes/halo/` | светлая палитра, локальный `user-ai-profile` |
| `havana` | `modules/web/themes/havana/` | светлая палитра, локальный `user-ai-profile` |
| `helium` | `modules/web/themes/helium/` | светлая палитра, локальный `user-ai-profile` |
| `hover` | `modules/web/themes/hover/` | светлая палитра, локальный `user-ai-profile` |
| `hunttech-modern` | `modules/web/themes/hunttech-modern/` | базовая современная палитра |
| `hunttech-modern-light` | `modules/web/themes/hunttech-modern-light/` | светлая современная палитра |
| `hunttech-modern-dark` | `modules/web/themes/hunttech-modern-dark/` | отдельные тёмные поверхности, границы и акцентные цвета |

Структура, размеры, отступы и состояния аккордеонов одинаковы во всех темах. Различаться могут только theme-aware цвета, прозрачность поверхностей и контраст текста.

## 7. База данных

Добавлена колонка `HUNTTECH_USER_SETTINGS.PREFER_PERSONAL_AI_API_SETTINGS BOOLEAN NOT NULL DEFAULT FALSE`.

Миграции:

- `modules/core/db/update/postgres/26/260723-1-addPreferPersonalAiApiSettings.sql`;
- `modules/core/db/changelog/260723-1-addPreferPersonalAiApiSettings.xml`;
- Liquibase master обновлён.

Entity, таблица `HUNTTECH_USER_AI_PROFILE`, `UserAiConfiguration`, API-ключи и административные настройки не изменяются. PROD в данный PR не входит.

## 8. Проверки

Обязательны:

- `git diff --check`;
- компиляция global/core/web;
- `UserSettingsAiApiPreferenceTest` — 4/4;
- `ScreenViewIntegrityTest` — 8/8;
- Data View Integrity для `ExtSettingsWindow`;
- `clean assemble`;
- визуальная проверка checkbox во всех поддерживаемых темах.

`UserSettingsAiApiPreferenceTest` проверяет метаданные поля, default `false`, binding XML и обе миграции. Тест `test5_userAiProfile_view_registered` продолжает проверять runtime-регистрацию `userAiProfile-view`.

Merge и deploy выполняются отдельно и не входят в данный PR.

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-23 | Добавлен checkbox «Предпочитать использовать в запросах мои настройки API», поле `UserSettings.preferPersonalAiApiSettings`, миграции и `UserSettingsAiApiPreferenceTest`; алгоритм маршрутизации API намеренно не изменён |
| 2026-07-23 | Исправлена повторная `ViewNotFoundException`: `user-ai-profile-views.xml` зарегистрирован в рабочих конфигурациях core/web; тест 5 переведён на `HunttechTestContainer` и проверяет runtime `ViewRepository` и оба `cuba.viewsConfig` |
| 2026-07-23 | Дополнительный `user-ai-profile-views.xml` был зарегистрирован только через `app-component.xml`; решение признано недостаточным для standalone-запуска приложения |
| 2026-07-23 | Введено обязательное правило синхронизации тем; SettingsWindow адаптирован для `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light` и `hunttech-modern-dark` |
| 2026-07-23 | После merge-регрессии восстановлены двухпанельная вкладка «Обо мне» и контроллер `UserAiProfile`; функциональность объединена с действующей вкладкой персональных AI-подключений без изменения БД |
| 2026-07-23 | XML приведён к утверждённой двухпанельной структуре: добавлены профильная навигация, единый toolbar, карточки-аккордеоны и начальное сворачивание вторичных секций; обновлён локальный Halo SCSS |
| 2026-07-22 | Контроллер, datasource ИИ-профиля и JPQL перенесены в namespace `hunttech`; legacy-контракты `ExtUser`, `UserSettings` и messages pack сохранены |
| 2026-07-22 | Исправлено программное раскрытие секции предпросмотра ИИ-контекста: используется совместимый с CUBA 7.3 метод `setExpanded(true)` |
| 2026-07-22 | Вкладка «Обо мне» переработана в двухпанельный профессиональный ИИ-профиль; добавлены согласие, предпросмотр и атомарное сохранение |

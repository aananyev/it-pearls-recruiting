# ExtSettingsWindow — настройки пользователя

> Экран HRM HuntTech: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> XML: `ext-settings-window.xml`.  
> Связанные документы: [UserAiProfile](../entities/UserAiProfile.md), [UserAiContextService](../services/UserAiContextService.md).

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Экран объединяет персональные настройки пользователя. Вкладка «Обо мне» формирует профессиональный ИИ-профиль, чтобы сервисы HRM HuntTech адаптировали ответы к роли и опыту пользователя без изменения объективных данных рекрутмента. Отдельная вкладка AI управляет персональными подключениями к провайдерам и не смешивает API-ключи с профессиональным профилем.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из стандартного меню настроек CUBA. Содержит вкладки `msgMyInfo`, `msgInterface`, `mailAccessTab` и `aiAccessTab`. «Обо мне» работает с `ExtUser` и `hunttech_UserAiProfile`; интерфейсные параметры сохраняет базовый `SettingsWindow`; почтовые параметры — существующая сущность `UserSettings`; вкладка AI — коллекция `UserAiConfiguration`, ограниченная текущим пользователем.

Внутри вкладки «Обо мне» левая профильная панель показывает аватар, имя, должность, статус и заполненность профиля, а также вертикальный индекс разделов. Справа разделы представлены карточками-аккордеонами в том же порядке, что и индекс слева.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- открытие → загружаются `ExtUser`, `UserSettings`, `UserAiProfile` и персональные `UserAiConfiguration`;
- загрузка аватара → изображение нормализуется существующим `ImageProcessingService`;
- профиль отсутствует → создаётся несохранённый объект;
- открытие вкладки → раскрыт только «Профессиональный профиль», остальные секции свёрнуты;
- предпросмотр → локально формируется очищенный контекст и раскрывается секция предпросмотра;
- включение без согласия → сохранение блокируется;
- сохранение → `ExtUser`, `UserSettings` и `UserAiProfile` фиксируются единым `CommitContext`;
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

Существующие component ID сохранены для совместимости: `settingsTabSheet`, `msgMyInfo`, `msgInterface`, `mailAccessTab`, `aiAccessTab`, `okBtn`, `cancelBtn`, `userAvatarUpload`, `userPic`, `defaultPic`.

## 2. Data & Entity Binding

| Datasource | Entity | View | Назначение |
|---|---|---|---|
| `extUserDs` | `ExtUser` | `extUser-view` | аватар и данные пользователя |
| `userSettingsDs` | `UserSettings` | `userSettings-view` | существующий контракт почтовых настроек |
| `userAiProfileDs` | `hunttech_UserAiProfile` | `userAiProfile-view` | профессиональный профиль и узкая связь с владельцем |
| `userAiConfigsDs` | `hunttech_UserAiConfiguration` | `userAiConfiguration-view` | персональные подключения пользователя к AI-провайдерам |

Профиль загружается запросом:

```jpql
select e from hunttech_UserAiProfile e where e.user = :currentUser
```

Конфигурации AI загружаются запросом:

```jpql
select e from hunttech_UserAiConfiguration e where e.user = :ds$extUserDs
```

Отсутствующий профиль создаётся только в памяти и сохраняется после нажатия `okBtn`.

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
    ├── aiConfigsButtonsPanel
    └── aiConfigsTable
```

## 4. Business Behavior

### Безопасные значения по умолчанию

- профиль выключен;
- внешняя обработка запрещена;
- язык — автоматически;
- детализация — сбалансированно;
- стиль — нейтральный;
- терминология — профессиональная;
- структура — автоматически.

### Согласие

`profileEnabled=true` разрешено только при `externalProcessingAllowed=true`. При первом согласии сохраняются `consentVersion=2026-07-22-v1` и `consentAcceptedAt`. При отзыве согласия персонализация выключается.

### Предпросмотр

`previewAiContext()` вызывает `UserAiContextService.buildContextPreview()` и раскрывает `previewGroup` через совместимый с CUBA 7.3 вызов `setExpanded(true)`. HTTP к LLM не выполняется.

### AI-подключения

Создание и редактирование выполняются через `UserAiConfigurationEdit`. Владелец новой конфигурации принудительно задаётся из текущей пользовательской сессии. Тест подключения делегируется `HrmAiService`; контроллер не содержит endpoint и HTTP-логику провайдера.

## 5. Actions & Methods

| Метод | Назначение |
|---|---|
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

Стили подключены через локальный mixin `user-ai-profile` и ограничены корнем `.user-ai-profile-editor`. Глобальные `.v-*` правила не добавляются. При ширине окна 1200 px горизонтальная прокрутка не требуется; содержимое справа прокручивается вертикально. Вкладки «Интерфейс», «Почта» и AI сохраняют действующие component ID и функциональные контракты.

### Синхронизация тем

Изменение визуального контракта экрана в одной теме требует одновременной адаптации всех поддерживаемых тем в той же задаче и в том же PR. Частичное обновление только одной темы считается незавершённым.

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

Рефакторинг не создаёт новых entity, полей или changeSet. Используется существующая таблица `HUNTTECH_USER_AI_PROFILE` и существующая схема `UserAiConfiguration`. Hermes выполняет только read-only проверку наличия таблицы в локальной БД. Любое изменение локальной схемы требует отдельного согласования; PROD запрещён.

## 8. Проверки

Обязательны `git diff --check`, компиляция web, `ScreenViewIntegrityTest` (8/8), Data View Integrity, сборка всех SCSS-тем, `clean assemble`, локальный deploy и HTTP 200. Функциональный smoke выполняется для вкладок «Обо мне», «Интерфейс», «Почта» и AI. В каждой поддерживаемой теме отдельно проверяются открытие SettingsWindow, двухпанельная компоновка, читаемость, отсутствие горизонтальной прокрутки при ширине 1200 px и отсутствие SCSS/runtime-ошибок. Production-проверки и любые действия на PROD запрещены.

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-23 | Введено обязательное правило синхронизации тем; SettingsWindow адаптирован для `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light` и `hunttech-modern-dark` |
| 2026-07-23 | После merge-регрессии восстановлены двухпанельная вкладка «Обо мне» и контроллер `UserAiProfile`; функциональность объединена с действующей вкладкой персональных AI-подключений без изменения БД |
| 2026-07-23 | XML приведён к утверждённой двухпанельной структуре: добавлены профильная навигация, единый toolbar, карточки-аккордеоны и начальное сворачивание вторичных секций; обновлён локальный Halo SCSS |
| 2026-07-22 | Контроллер, datasource ИИ-профиля и JPQL перенесены в namespace `hunttech`; legacy-контракты `ExtUser`, `UserSettings` и messages pack сохранены |
| 2026-07-22 | Исправлено программное раскрытие секции предпросмотра ИИ-контекста: используется совместимый с CUBA 7.3 метод `setExpanded(true)` |
| 2026-07-22 | Вкладка «Обо мне» переработана в двухпанельный профессиональный ИИ-профиль; добавлены согласие, предпросмотр и атомарное сохранение |

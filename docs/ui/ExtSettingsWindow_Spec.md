# ExtSettingsWindow — настройки пользователя

> Экран HRM HuntTech: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.
> XML: `ext-settings-window.xml`.
> Базовый класс: `com.haulmont.cuba.web.app.ui.core.settings.SettingsWindow`.
> Связанные документы: [UI/UX-концепция HRM HuntTech](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md), [UserSettings](../entities/user-settings/UserSettings.md), [UserAiProfile](../entities/UserAiProfile.md), [UserAiContextService](../services/UserAiContextService.md).
> Визуальный эталон существующих вкладок: [SettingsWindow AI — Halo](renders/SettingsWindow_AI_Halo.svg).

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`ExtSettingsWindow` объединяет персональные настройки рабочего места пользователя HRM HuntTech:

- вкладка «Обо мне» формирует профессиональный ИИ-профиль;
- вкладка «Интерфейс» определяет режим главного окна, тему, язык, часовой пояс и стартовый экран;
- вкладка «Настройка email» хранит персональные параметры SMTP, POP3 и IMAP;
- вкладка AI управляет персональными подключениями к провайдерам и предпочтением источника API.

Редизайн вкладок «Интерфейс» и «Настройка email» устраняет визуальный разрыв внутри одного окна и приводит их к общей концепции Edit-форм: контекстная боковая панель, понятная навигация, заголовок рабочей области и функциональные карточки. Изменение не затрагивает правила сохранения, сервисы, сущности, БД и поведение базового `SettingsWindow`.

Визуальный слой экрана дополнительно унифицирован с `JobCandidateEdit`: применены theme-aware фон рабочей области, компактные карточки, умеренные рамки и тени, единая иерархия заголовков, локальная стилизация вкладок, полей и кнопок. Это не новый редизайн: существующая структура и компоновка `ExtSettingsWindow` сохранены.

### Связи в интерфейсе и навигация (UI Context & Navigation)

Экран открывается из стандартного меню настроек CUBA Platform и содержит четыре вкладки:

| Вкладка | ID | Источник данных и ответственность |
|---|---|---|
| «Обо мне» | `msgMyInfo` | `ExtUser`, `UserAiProfile`, предпросмотр ИИ-контекста |
| «Интерфейс» | `msgInterface` | компоненты и методы базового `SettingsWindow` |
| «Настройка email» | `mailAccessTab` | поля `UserSettings`, заполняемые и собираемые контроллером вручную |
| AI | `aiAccessTab` | `UserSettings.preferPersonalAiApiSettings`, `UserAiConfiguration` |

XML-компоновка и функциональные контракты вкладок `msgMyInfo` и `aiAccessTab` не изменяются. Общий корневой визуальный слой `.ext-settings-window` применяется ко всем четырём вкладкам только для унификации фона, вкладок, полей, карточек, кнопок и состояний.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- открытие окна → базовый `SettingsWindow` находит интерфейсные компоненты по legacy component ID → отображаются текущие настройки;
- изменение режима, темы, языка, часового пояса или стартового экрана → меняется состояние стандартных компонентов → значения применяются штатной логикой CUBA;
- нажатие `changePasswordBtn` → вызывается прежнее действие базового окна → открывается смена пароля;
- нажатие `resetScreenSettingsBtn` → вызывается прежнее действие базового окна → сохранённые настройки экранов сбрасываются после подтверждения;
- открытие вкладки email → `setEmailSettings()` заполняет существующие поля из `UserSettings` с fallback на `ExtUser`;
- сохранение окна → `collectEmailSettings()` читает те же `TextField` и `CheckBox` по ID → значения записываются в `UserSettings`;
- визуальная унификация → корневой класс `.ext-settings-window` ограничивает локальный SCSS → datasource, component ID, captions, типы компонентов, валидаторы и Java-методы остаются прежними;
- hover, focus, disabled или read-only → применяется локальное presentation-состояние → доступность, required и правила валидации не меняются;
- открытие «Обо мне» или AI → выполняется существующее поведение → XML-компоновка, bindings, actions и `invoke` остаются прежними.

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|---|---|
| Контроллер | `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow` |
| Базовый класс | `SettingsWindow` |
| XML schema | legacy `window.xsd` |
| Data API | legacy `dsContext` |
| Корневой визуальный namespace | `.ext-settings-window` на корневом `<layout>` |
| Messages pack | `com.company.hunttech.web.screens.extsettingswindow` |
| Поддерживаемые темы | `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark` |

Экран остаётся legacy-экраном CUBA Platform. Корневые секции `<window>`, `<dsContext>` и `<layout>` сохранены. На существующий `<layout>` добавлен только `stylename="ext-settings-window"`, а `settingsTabSheet` получил дополнительный локальный класс `ext-settings-tabs` при сохранении `framed`. Наследуемые компоненты базового `SettingsWindow` не переименовываются, поскольку контроллер платформы связывает их по ID.

## 2. Связь с моделью данных (Data & Entity Binding)

| Datasource | Entity | View | Назначение |
|---|---|---|---|
| `extUserDs` | `ExtUser` | `extUser-view` | пользователь, аватар и fallback почтовых значений |
| `userSettingsDs` | `UserSettings` | `userSettings-view` | почтовые параметры и предпочтение личного API |
| `userAiProfileDs` | `UserAiProfile` | `userAiProfile-view` | профессиональный ИИ-профиль |
| `userAiConfigsDs` | `UserAiConfiguration` | `userAiConfiguration-view` | персональные AI-подключения |

### Регистрация view и запросы существующих вкладок

`userAiProfile-view` объявлен в `modules/global/src/com/company/hunttech/user-ai-profile-views.xml` и зарегистрирован в рабочих свойствах core и web:

- `modules/core/src/com/company/hunttech/app.properties`;
- `modules/web/src/com/company/hunttech/web-app.properties`.

Регистрация только в `app-component.xml` недостаточна для standalone-запуска HRM HuntTech: web `ViewRepository` не получает именованный view, и `DsBuilder` завершает открытие окна с `ViewNotFoundException` до вызова `ExtSettingsWindow.init()`.

Пользовательские настройки загружаются запросом:

```jpql
select e from hunttech_UserSettings e where e.user = :currentUser
```

Профессиональный ИИ-профиль загружается запросом:

```jpql
select e from hunttech_UserAiProfile e where e.user = :currentUser
```

Персональные AI-конфигурации загружаются запросом:

```jpql
select e from hunttech_UserAiConfiguration e where e.user = :ds$extUserDs
```

Отсутствующие `UserSettings` и `UserAiProfile` создаются только в памяти и сохраняются после нажатия `okBtn`. Эти запросы, view и порядок загрузки задачей редизайна не изменены.

### Интерфейсные параметры

Компоненты `modeOptions`, `appThemeField`, `appLangField`, `timeZoneLookup`, `timeZoneAutoField` и `defaultScreenField` не получают новых binding. Инициализация и сохранение остаются ответственностью базового `SettingsWindow`.

### Почтовые параметры

Почтовые поля намеренно не связываются с datasource в XML. `ExtSettingsWindow` использует инъекцию по ID:

- `smtpServer`, `smtpPort`, `smtpPasswordRequired`, `smtpPassword`;
- `pop3Server`, `pop3Port`, `pop3PasswordRequired`, `pop3Password`;
- `imapServer`, `imapPort`, `imapPasswordRequired`, `imapPassword`.

Методы `setEmailSettings()` и `collectEmailSettings()` не изменены. Поля портов сохраняют `datatype="int"` и `IntegerValidator`.

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

```text
ext-settings-window
└── settingsTabSheet
    ├── msgMyInfo
    │   └── userAiProfileMainBox                         [без изменений]
    ├── msgInterface
    │   └── interfaceSettingsMainBox
    │       ├── interfaceSettingsSidebar (270 px)
    │       │   ├── interfaceSettingsSidebarIdentity
    │       │   ├── interfaceSettingsNavigation
    │       │   └── interfaceSettingsHintBox
    │       └── interfaceSettingsContentScrollBox
    │           └── interfaceSettingsContent
    │               ├── interfaceSettingsToolbar
    │               │   ├── changePasswordBtn
    │               │   └── resetScreenSettingsBtn
    │               └── interfaceAppearanceCard
    │                   └── grid
    │                       ├── modeOptions
    │                       ├── appThemeField
    │                       ├── appLangField
    │                       ├── timeZoneLookup / timeZoneAutoField
    │                       └── defaultScreenField
    ├── mailAccessTab
    │   └── emailSettingsMainBox
    │       ├── emailSettingsSidebar (270 px)
    │       │   ├── emailSettingsSidebarIdentity
    │       │   ├── emailSettingsNavigation
    │       │   └── emailSettingsSecurityBox
    │       └── emailSettingsContentScrollBox
    │           └── emailSettingsContent
    │               ├── emailSettingsToolbar
    │               └── emailSettingsGrid
    │                   ├── smtpSettingsCard
    │                   ├── pop3SettingsCard
    │                   └── imapSettingsCard
    └── aiAccessTab
        └── aiSettingsMainBox                            [без изменений]
```

## 4. Модель поведения и интерактивность (Behavior Model)

### Существующее поведение вкладок «Обо мне» и AI

Безопасные значения по умолчанию сохранены:

- предпочтение личных настроек API — выключено;
- профиль выключен;
- внешняя обработка запрещена;
- язык — автоматически;
- детализация — сбалансированно;
- стиль — нейтральный;
- терминология — профессиональная;
- структура — автоматически.

`preferPersonalAiApiSettingsField` связан с `UserSettings.preferPersonalAiApiSettings`. Значение `false` сохраняет действующее поведение и не требует личного API-ключа. Значение `true` только фиксирует намерение пользователя; контроллер не выбирает провайдера, не проверяет наличие ключей и не меняет маршрутизацию.

`profileEnabled=true` разрешено только при `externalProcessingAllowed=true`. При первом согласии сохраняются `consentVersion=2026-07-22-v1` и `consentAcceptedAt`; при отзыве согласия персонализация выключается.

`previewAiContext()` вызывает `UserAiContextService.buildContextPreview()` и раскрывает `previewGroup` через совместимый с CUBA 7.3 вызов `setExpanded(true)`. HTTP к LLM не выполняется.

Создание и редактирование AI-подключений выполняются через `UserAiConfigurationEdit`. Владелец новой конфигурации задаётся из текущей сессии. Тест подключения делегируется `HrmAiService`; endpoint и HTTP-логика провайдера в контроллер не переносятся.

### Вкладка «Интерфейс»

Вкладка разделена на фиксированную боковую панель шириной 270 px и прокручиваемую рабочую область. Боковая панель объясняет назначение разделов и предупреждает, что часть изменений применяется при следующем входе. Правая карточка сохраняет прежнюю последовательность полей.

`changePasswordBtn` и `resetScreenSettingsBtn` визуально перенесены в toolbar, но их ID, иконки, стиль `danger` и обработчики базового класса не изменены.

### Вкладка «Настройка email»

SMTP, POP3 и IMAP представлены тремя равноправными карточками. Внутри каждой карточки сохранён порядок:

```text
сервер → порт → требование пароля → пароль
```

Это только визуальная группировка. Почтовые пароли не передаются в ИИ-контекст и продолжают сохраняться существующим кодом `ExtSettingsWindow`.

### Неизменяемые вкладки

`msgMyInfo` и `aiAccessTab` не перерабатывались: их XML-блоки, component ID, datasource, `invoke` и порядок компонентов оставлены без функциональных изменений. Корневой `.ext-settings-window` только согласует их общие presentation-состояния с остальными вкладками.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Компонент / метод | Контракт |
|---|---|
| `changePasswordBtn` | существующая смена пароля базового `SettingsWindow` |
| `resetScreenSettingsBtn` | существующий сброс пользовательских настроек экранов |
| `okBtn` | вызывает существующую последовательность валидации и сохранения |
| `cancelBtn` | закрывает окно без согласованного сохранения |
| `setEmailSettings()` | заполняет почтовые поля при инициализации |
| `collectEmailSettings()` | переносит значения почтовых полей в `UserSettings` перед commit |
| `commit()` | сохраняет `UserSettings`, `ExtUser`, `UserAiProfile` существующим `CommitContext` |
| `loadOrCreateUserSettings()` | загружает `UserSettings`, применяет default `false` и устанавливает `userSettingsDs` |
| `loadOrCreateUserAiProfile()` | загружает профиль или создаёт несохранённый экземпляр |
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
| `onAiConfigsTestBtnClick()` | тестирует подключение через `HrmAiService` |

Java-контроллер, entity, views, JPQL, сервисы, `@Subscribe`, `@Install`, actions и API-контракты этой задачей не изменяются.

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### Общий паттерн

Обе переработанные вкладки используют одинаковый визуальный контракт:

- фиксированная левая панель `270px`;
- короткий маркер назначения (`UI` или `@`);
- заголовок и пояснение;
- вертикальный индекс разделов;
- нижняя информационная либо предупреждающая карточка;
- справа — toolbar и функциональные карточки;
- вертикальная прокрутка правой области;
- отсутствие глобальных Vaadin-селекторов.

Основной локальный корень:

```scss
.ext-settings-window
```

Внутри него используются существующие структурные классы `.interface-settings-editor`, `.email-settings-editor`, `.user-ai-profile-editor`, `.ai-settings-editor` и локальные Vaadin-селекторы. Селекторы `.v-button`, `.v-label`, `.v-tabsheet`, `.v-textfield` и аналогичные вне `.ext-settings-window` отсутствуют. Namespace `.job-candidate-editor` не подключается и не переиспользуется.

### Таблица соответствия визуальных решений

| Элемент `JobCandidateEdit` | Применимость к `ExtSettingsWindow` | Способ адаптации |
|---|---|---|
| Фон рабочей области | Полностью | `mix($v-app-background-color, $v-panel-background-color, 72%)` внутри `.ext-settings-window` |
| Боковая панель | Частично | сохранена ширина 270 px и текущий контент; применены компактные отступы, фон панели, локальная рамка и умеренная тень |
| Контейнеры-карточки | Полностью | фон `$v-panel-background-color`, рамка через `$v-font-color`, `$v-border-radius`, тень `0 2px 8px` |
| Заголовки секций | Полностью | размер 13 px, насыщенность 600, единые интервалы и контраст темы |
| Поясняющий текст | Полностью | размер 12 px, `line-height: 1.4`, вторичный цвет через `rgba($v-font-color, 0.66)` |
| TextField / TextArea | Полностью | локальная рамка, фон, скругление, focus, read-only и disabled без изменения binding и validators |
| LookupField / ComboBox | Полностью | стили `.v-filterselect` и `.v-filterselect-focus` только внутри корня |
| CheckBox / OptionsGroup | Частично | согласованы интервалы, текст и disabled; типы компонентов и значения не меняются |
| Кнопки | Полностью | скругление темы, hover/focus, сохранение стандартных `primary` и `danger` |
| Панели действий | Полностью | toolbar оформлен как компактная панель с рамкой, фоном и высотой не менее 38 px |
| TabSheet | Полностью | локальные active/hover-состояния и разделитель через `.ext-settings-tabs` |
| Разделители и интервалы | Полностью | ритм 4/6/8/10/12/14/18 px без изменения порядка компонентов |
| Тени и скругления | Полностью | исключены большие декоративные тени и радиусы 14 px; используются `$v-border-radius` и умеренные тени |
| Disabled / read-only | Полностью | presentation-состояния задаются локально; условия доступности и редактирования не меняются |
| Ограниченная ширина | Частично | добавлены `min-width: 0` и фиксированный layout таблицы email; структурный reflow не вводится |
| Halo и другие темы | Полностью | единый theme-aware SCSS использует переменные CUBA/Valo без прямой зависимости от стилей кандидата |

### Синхронизация тем

| Тема | Файл | Адаптация |
|---|---|---|
| `halo` | `com.company.hunttech/settings-window-sections.scss` | эталонная проверка на стандартных переменных Halo |
| `havana` | тот же локальный путь темы | тот же theme-aware контракт |
| `helium` | тот же локальный путь темы | тот же theme-aware контракт |
| `hover` | тот же локальный путь темы | тот же theme-aware контракт |
| `hunttech-modern` | тот же локальный путь темы | тот же theme-aware контракт |
| `hunttech-modern-light` | тот же локальный путь темы | тот же theme-aware контракт |
| `hunttech-modern-dark` | тот же локальный путь темы | автоматическая адаптация через переменные тёмной темы |

Каждый `styles.scss` продолжает импортировать `com.company.hunttech/settings-window-sections` и включать `@include settings-window-sections;`. Один и тот же SCSS-контракт используется во всех семи темах; цвета, фон, focus и контраст вычисляются из `$v-app-background-color`, `$v-panel-background-color`, `$v-font-color` и `$v-selection-color`.

Запрещены и не добавлены глобальные правила `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet`.

## 7. Соответствие CUBA Platform 7.3

Реализация сохраняет платформенные контракты legacy UI:

1. экран остаётся описан XML-дескриптором `window.xsd`;
2. вкладки остаются дочерними компонентами `TabSheet`;
3. компоновка построена стандартными `HBox`, `VBox`, `ScrollBox`, `Grid`, `ButtonsPanel`;
4. extension-контракты базового `SettingsWindow` сохраняются через прежние component ID;
5. стили подключаются через theme extension и стандартный XML-атрибут `stylename`;
6. `HBox`, `VBox`, `ScrollBox`, `Grid`, `ButtonsPanel` и `TabSheet` остаются штатными компонентами CUBA 7.3;
7. валидаторы портов остаются вложенными в соответствующие `TextField`;
8. data-binding и Java-инъекция не заменяются визуальным кодом.

## 8. База данных и ограничения задачи

Редизайн:

- не меняет entity и поля;
- не добавляет Liquibase или SQL;
- не меняет таблицы `HUNTTECH_USER_SETTINGS`, `HUNTTECH_USER_AI_PROFILE`, `HUNTTECH_USER_AI_CONFIGURATION`;
- не меняет формат хранения паролей;
- не меняет маршрутизацию AI API;
- не затрагивает production.

## 9. Обязательные проверки

Hermes проверяет точный HEAD ветки и PR без изменения кода:

- `git diff --check`;
- компиляция web и test-кода;
- `UserSettingsAiApiPreferenceTest` — ожидается **10/10**;
- `ScreenViewIntegrityTest` — ожидается **8/8**;
- Data View Integrity для `ExtSettingsWindow`;
- `./gradlew :app-web:buildScssThemes --no-daemon --stacktrace`;
- `./gradlew clean assemble --no-daemon --stacktrace`;
- локальный deploy с предварительным `updateDb`;
- HTTP `http://localhost:8080/hrm/` = 200;
- smoke вкладок «Интерфейс» и «Настройка email» во всех семи темах при ширине окна 1200 px;
- проверка отсутствия горизонтальной прокрутки, обрезки полей и toolbar;
- изменение и сохранение интерфейсных параметров;
- изменение, сохранение и повторное открытие почтовых параметров;
- смена пароля и сброс экранных настроек;
- регрессионное открытие вкладок «Обо мне» и AI без визуальных и runtime-ошибок;
- отсутствие критических ошибок в Tomcat logs.

`UserSettingsAiApiPreferenceTest` содержит десять сценариев:

1. metadata-поле и default `false`;
2. binding checkbox;
3. двухпанельный layout вкладки AI;
4. сохранность четырёх `invoke`-контрактов AI-действий;
5. двухпанельный layout вкладок «Интерфейс» и «Настройка email»;
6. сохранность legacy component ID и трёх `IntegerValidator`;
7. наличие локальных AI-стилей во всех семи темах;
8. наличие и подключение локальных стилей двух новых вкладок во всех семи темах;
9. локальный `.ext-settings-window`, theme-aware токены и отсутствие зависимости от `.job-candidate-editor`;
10. миграции ранее добавленного поля и порядок локального запуска.

Тест `test5_userAiProfile_view_registered` продолжает проверять runtime-регистрацию `userAiProfile-view`.

До отчёта Hermes по точному SHA статус задачи — `WAITING_FOR_HERMES`.

## 10. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-24 | Визуальное оформление `ExtSettingsWindow` адаптировано к дизайн-языку `JobCandidateEdit` без изменения структуры и бизнес-логики; добавлен локальный namespace `.ext-settings-window`, theme-aware состояния и синхронизация семи тем |
| 2026-07-24 | Вкладки «Интерфейс» и «Настройка email» приведены к двухпанельной концепции Edit-форм; сохранены legacy component ID, типы полей, валидаторы и бизнес-логика; локальный SCSS синхронизирован для семи тем |
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

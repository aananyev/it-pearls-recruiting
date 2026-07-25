# ExtSettingsWindow — настройки пользователя

> Экран HRM HuntTech: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> Фактическое расширение screen ID `settings`: `ExtSettingsWindowEmailNavigation`.  
> XML: `ext-settings-window.xml`, расширение `ext-settings-window-email-navigation.xml`.  
> Базовый класс: `com.haulmont.cuba.web.app.ui.core.settings.SettingsWindow`.  
> Локальный визуальный namespace: `.ext-settings-window`.  
> Связанные документы: [UI/UX-концепция HRM HuntTech](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md), [UserSettings](../entities/user-settings/UserSettings.md), [UserAiProfile](../entities/UserAiProfile.md), [UserAiContextService](../services/UserAiContextService.md), [ImageProcessingService](../services/file-storage/ImageProcessingService.md), [навигация и preview фактического экрана](ExtSettingsWindowEmailNavigation_Spec.md), [аватар ExtSettingsWindow](ExtSettingsWindowAvatar_Spec.md).

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`ExtSettingsWindow` объединяет персональные настройки рабочего места пользователя HRM HuntTech:

- вкладка «Обо мне» формирует профессиональный ИИ-профиль;
- вкладка «Интерфейс» определяет режим главного окна, тему, язык, часовой пояс и стартовый экран;
- вкладка «Настройка email» хранит персональные параметры SMTP, POP3 и IMAP;
- вкладка AI управляет персональными подключениями к провайдерам и сохраняемыми предпочтениями личного API и личных промптов.

По умолчанию пользователь предпочитает собственные API-подключения и промпты. Это делает персональный контур основным при наличии подходящих настроек, но не переносит в экран алгоритмы маршрутизации, fallback или разрешения конфликтов промптов.

Визуальный слой должен быть явно узнаваемой частью общего дизайн-языка HRM HuntTech. Для этого форма использует подтверждённую композицию `JobCandidateEdit`: тёмная контекстная панель слева, светлая рабочая область справа, выраженные вкладки, toolbar, карточки и поля высотой 38 px. SMTP, POP3 и IMAP располагаются вертикально в полноширинных раскрываемых секциях, чтобы фиксированная sidebar не сжимала почтовые поля. AI-предпочтения, аватар `OvaFallbackImage` и локальный SCSS семи тем сохраняются.

### UI Context & Navigation

Экран открывается из стандартного меню настроек CUBA Platform. В `web-screens.xml` screen ID `settings` зарегистрирован на расширяющий шаблон `/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-email-navigation.xml`, который наследует базовый `ext-settings-window.xml`.

| Вкладка | ID | Источник данных и ответственность |
|---|---|---|
| «Обо мне» | `msgMyInfo` | `ExtUser`, `UserAiProfile`, локальный предпросмотр текущего ИИ-контекста |
| «Интерфейс» | `msgInterface` | компоненты и методы базового `SettingsWindow` |
| «Настройка email» | `mailAccessTab` | поля `UserSettings`, заполняемые и собираемые контроллером вручную |
| AI | `aiAccessTab` | два предпочтения `UserSettings`, коллекция `UserAiConfiguration` |

Все четыре вкладки используют общий корневой визуальный слой `.ext-settings-window`. Внутри каждой вкладки сохраняется существующая двухпанельная структура: контекст и индекс разделов слева, рабочая область с toolbar и карточками либо аккордеонами справа.

На вкладке AI карточка `personalAiApiPreferenceBox` содержит:

- `preferPersonalAiApiSettingsField` — «Предпочитать использовать в запросах мои настройки API»;
- `preferPersonalPromptsField` — «Предпочитать мои промпты»;
- пояснения о семантике каждого выбора;
- оба checkbox связаны с `userSettingsDs`.

### Behavior Summary

- открытие окна → базовый `SettingsWindow` находит legacy-компоненты по ID → отображаются текущие настройки;
- начало `init()` → `ImageProcessingService` и `UserAiContextService` разрешаются через `AppBeans.get(<Service>.NAME)` → web-контекст получает именованные CUBA middleware proxy без прямого доступа к core Spring context;
- завершение базовой инициализации → `ExtSettingsWindowEmailNavigation` подключает кликабельную email- и AI-навигацию → данные и commit не меняются;
- загрузка `UserSettings` → отсутствующие или legacy-null AI-предпочтения нормализуются в `true` → оба checkbox включены;
- пользователь меняет AI-предпочтение → binding изменяет соответствующее поле `UserSettings` → отдельный обработчик не требуется;
- сохранение окна → `userSettings` добавляется в общий `CommitContext` → оба значения сохраняются атомарно;
- повторное открытие → значения загружаются из `HUNTTECH_USER_SETTINGS` → checkbox восстанавливают сохранённое состояние;
- изменение режима, темы, языка, часового пояса или стартового экрана → работает штатная логика CUBA → значения применяются без нового контроллерного кода;
- открытие вкладки email → `setEmailSettings()` заполняет прежние поля → SMTP отображается раскрытым, а POP3 и IMAP доступны в следующих вертикальных секциях;
- раскрытие или сворачивание почтовой секции → меняется только видимость её содержимого → значения полей и порядок сохранения не меняются;
- сохранение окна → `collectEmailSettings()` читает те же `TextField` и `CheckBox` → значения записываются в `UserSettings`;
- нажатие AI-действий → выполняются существующие `invoke`-методы → визуальный слой не инициирует запросы и не меняет маршрутизацию;
- нажатие «Показать передаваемые данные» → фактический controller берёт текущий `userAiProfileDs.item` → `UserAiContextBuilder` локально формирует очищенный preview → секция раскрывается и форма прокручивается к результату;
- профиль выключен или согласие отсутствует → preview явно сообщает, что контекст не передаётся → окно остаётся открытым;
- ошибка построения предпросмотра → stack trace записывается в журнал приложения → пользователю показывается warning без закрытия формы;
- загрузка или очистка аватара → обработчик вызывает `ImageProcessingService` через именованный middleware proxy → изображение обрабатывается в core без cross-context зависимости web-контроллера;
- hover, focus, disabled и read-only → меняется только presentation → required, validators, permissions и editable-состояния остаются прежними;
- смена темы → используется одинаковый локальный SCSS-контракт семи тем → component ID и поведение не меняются.

## 1. Технический контекст

| Параметр | Значение |
|---|---|
| Базовый контроллер | `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow` |
| Фактический контроллер | `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindowEmailNavigation` |
| Базовый класс | `SettingsWindow` |
| XML schema | legacy `window.xsd` |
| Data API | legacy `dsContext` |
| Screen ID | `settings` |
| Корневой visual style | `ext-settings-window` |
| TabSheet style | `framed ext-settings-tabs` |
| Footer style | `ext-settings-footer` |
| Поддерживаемые темы | `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark` |

Экран остаётся legacy-экраном CUBA Platform 7.3. Корневые секции `<window>`, `<dsContext>` и `<layout>` сохранены. Актуальный экран одновременно содержит AI-предпочтения, null-safe локальный предпросмотр, аватар `OvaFallbackImage`, кликабельную навигацию и визуальный email-аккордеон. `ImageProcessingService` и `UserAiContextService` остаются middleware Service API и разрешаются по стабильным CUBA service name; фактическая кнопка preview не передаёт редактируемую entity в middleware и использует общий builder модуля `global`.

## 2. Связь с моделью данных

| Datasource | Entity | View | Назначение |
|---|---|---|---|
| `extUserDs` | `ExtUser` | `extUser-view` | пользователь, аватар и fallback почтовых значений |
| `userSettingsDs` | `UserSettings` | `userSettings-view` | почтовые параметры, предпочтение личного API и личных промптов |
| `userAiProfileDs` | `UserAiProfile` | `userAiProfile-view` | профессиональный ИИ-профиль и текущий источник UI-preview |
| `userAiConfigsDs` | `UserAiConfiguration` | `userAiConfiguration-view` | персональные AI-подключения |

Используемые запросы:

```jpql
select e from hunttech_UserSettings e where e.user = :currentUser
```

```jpql
select e from hunttech_UserAiProfile e where e.user = :currentUser
```

```jpql
select e from hunttech_UserAiConfiguration e where e.user = :ds$extUserDs
```

`userSettings-view` расширяет `_local`, поэтому оба Boolean-поля доступны экрану как локальные скалярные атрибуты без дополнительного графа загрузки.

### 2.1. AI-предпочтения

| Компонент | Binding | Default |
|---|---|---|
| `preferPersonalAiApiSettingsField` | `userSettingsDs.preferPersonalAiApiSettings` | `true` |
| `preferPersonalPromptsField` | `userSettingsDs.preferPersonalPrompts` | `true` |

Новая запись создаётся методом `createNewUserSetting()`. Для загруженной legacy-записи `loadOrCreateUserSettings()` заменяет `null` на `true` до установки сущности в datasource.

При сохранении выполняется:

```java
CommitContext context = new CommitContext();
context.addInstanceToCommit(userSettings);
```

Следовательно, оба checkbox сохраняются в `UserSettings` без ручного копирования значений из UI-компонентов.

### 2.2. Почтовые параметры

Почтовые поля намеренно не получают datasource-binding в XML. Контроллер продолжает находить их по ID:

- `smtpServer`, `smtpPort`, `smtpPasswordRequired`, `smtpPassword`;
- `pop3Server`, `pop3Port`, `pop3PasswordRequired`, `pop3Password`;
- `imapServer`, `imapPort`, `imapPasswordRequired`, `imapPassword`.

Поля портов сохраняют `datatype="int"` и `IntegerValidator`. Перенос полей в `GroupBoxLayout` не меняет инъекции, загрузку `setEmailSettings()` и сбор значений `collectEmailSettings()`.

### 2.3. Middleware-сервисы и локальный builder

`ImageProcessingService` и `UserAiContextService` объявлены обычными полями базового контроллера без `@Inject`. Их интерфейсы и DTO находятся в `global`, а реализации зарегистрированы в `core` через `@Service(<Interface>.NAME)`.

В начале `init()` базовый web-контроллер получает именованные CUBA service proxy:

```java
imageProcessingService = (ImageProcessingService) AppBeans.get(ImageProcessingService.NAME);
userAiContextService = (UserAiContextService) AppBeans.get(UserAiContextService.NAME);
```

Получение выполняется до определения текущего пользователя, загрузки datasource и регистрации UI-listener. Lookup по стабильному CUBA service name возвращает web-side proxy и исключает попытку найти core-реализацию в локальном Spring-контексте webapp.

Class-based lookup запрещён:

```java
AppBeans.get(ImageProcessingService.class);
AppBeans.get(UserAiContextService.class);
```

`ImageProcessingService.process(...)` возвращает `ProcessedImage`, размещённый в `global` и реализующий `Serializable`; это обеспечивает удалённый контракт передачи байтов, имени, расширения и признака обработки между web и middleware.

Кнопка предпросмотра использует иной безопасный путь:

```java
UserAiProfile profile = userAiProfileDs.getItem();
String preview = UserAiContextBuilder.buildPreview(profile);
```

`UserAiContextBuilder` размещён в `global`, не зависит от Spring и применяется также `UserAiContextServiceBean`. UI получает единые sanitization и лимиты, но использует текущее состояние datasource, включая несохранённые изменения. `userAiContextService.buildContextPreview(profile)` фактическим screen controller не вызывается.

## 3. База данных

Таблица: `HUNTTECH_USER_SETTINGS`.

| Колонка | Тип | Ограничения | Назначение |
|---|---|---|---|
| `PREFER_PERSONAL_AI_API_SETTINGS` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | приоритет персональных API-настроек |
| `PREFER_PERSONAL_PROMPTS` | `BOOLEAN` | `NOT NULL DEFAULT TRUE` | приоритет личных промптов |

Историческая миграция `260723-1-addPreferPersonalAiApiSettings` не редактируется. Новое поведение добавляется отдельными артефактами:

- `modules/core/db/update/postgres/26/260724-1-enablePersonalAiPreferences.sql`;
- `modules/core/db/changelog/260724-1-enablePersonalAiPreferences.xml`;
- include в `db.changelog-master.xml`.

Миграция:

1. меняет default `PREFER_PERSONAL_AI_API_SETTINGS` на `TRUE`;
2. переводит существующие записи в `TRUE` согласно утверждённому новому поведению;
3. добавляет `PREFER_PERSONAL_PROMPTS BOOLEAN NOT NULL DEFAULT TRUE`;
4. остаётся идемпотентной для CUBA SQL-обновления и Liquibase.

`updateDb` должен выполняться до deploy, иначе entity-модель будет несовместима со схемой PostgreSQL.

## 4. Иерархия формы

```text
settings
└── ext-settings-window-email-navigation.xml
    └── extends ext-settings-window.xml
        └── settingsTabSheet [ext-settings-tabs]
            ├── msgMyInfo
            │   └── userAiProfileMainBox
            │       ├── userAiProfileSidebar (270 px)
            │       │   └── userPic [OvaFallbackImage, 176×176]
            │       └── userAiProfileContentScrollBox
            │           ├── previewAiContextBtn [invoke=previewAiContext]
            │           └── previewGroup
            │               └── aiContextPreviewArea
            ├── msgInterface
            │   └── interfaceSettingsMainBox
            │       ├── interfaceSettingsSidebar (270 px)
            │       └── interfaceSettingsContentScrollBox
            ├── mailAccessTab
            │   └── emailSettingsMainBox
            │       ├── emailSettingsSidebar (270 px)
            │       └── emailSettingsContentScrollBox
            │           └── emailSettingsContent
            │               ├── emailSettingsToolbar
            │               └── emailSettingsAccordion
            │                   ├── smtpSettingsSection — раскрыт
            │                   ├── pop3SettingsSection — свёрнут
            │                   └── imapSettingsSection — свёрнут
            └── aiAccessTab
                └── aiSettingsMainBox
                    ├── aiSettingsSidebar (270 px)
                    └── aiSettingsContent
                        ├── aiSettingsToolbar
                        ├── personalAiApiPreferenceBox
                        │   ├── preferPersonalAiApiSettingsField
                        │   └── preferPersonalPromptsField
                        └── aiConnectionsCard
```

Ширина боковых панелей задана существующим XML и не меняется. При ширине viewport до 1366 px локальный SCSS уменьшает фактическую ширину панели до 250 px, не перестраивая XML и не перенося компоненты.

## 5. Функциональные контракты

### 5.1. Вкладка «Интерфейс»

Сохраняются component ID:

- `grid`, `mainWindowLabel`, `modeOptions`;
- `visualThemeLabel`, `appThemeField`;
- `languageLabel`, `appLangField`;
- `timeZoneLabel`, `timeZoneBox`, `timeZoneLookup`, `timeZoneAutoField`;
- `defaultScreenLabel`, `defaultScreenField`;
- `changePasswordBtn`, `resetScreenSettingsBtn`.

`appThemeField` остаётся обязательным. Смена пароля и сброс экранных настроек используют прежние действия базового `SettingsWindow`.

### 5.2. Вкладка «Настройка email»

SMTP, POP3 и IMAP представлены тремя полноширинными секциями `GroupBoxLayout`, расположенными вертикально в контейнере `emailSettingsAccordion`.

Начальное состояние:

- `smtpSettingsSection` — раскрыта;
- `pop3SettingsSection` — свёрнута;
- `imapSettingsSection` — свёрнута.

Порядок внутри каждой секции сохраняется:

```text
сервер → порт → требование пароля → пароль
```

Сохраняются внутренние контейнеры `smtpSettingsCard`, `pop3SettingsCard`, `imapSettingsCard`, все 12 component ID почтовых полей, типы полей и `IntegerValidator`. Раскрытие секций не требует изменения загрузки или сохранения. Пароли не передаются в ИИ-контекст.

### 5.3. Вкладка «Обо мне» и предпросмотр

Сохраняются datasource, consent-логика, валидация опыта, аватар `OvaFallbackImage` и атомарное сохранение. `previewAiContext()` не отправляет HTTP-запрос к LLM и не сохраняет профиль.

Фактический controller `ExtSettingsWindowEmailNavigation` переопределяет публичный invoke-метод. Предпросмотр выполняется последовательно:

1. безопасно извлекается текущий `UserAiProfile` из `userAiProfileDs`;
2. проверяется инъекция `aiContextPreviewArea` и `previewGroup`;
3. вызывается локальный `UserAiContextBuilder.buildPreview(profile)`;
4. текст устанавливается в `aiContextPreviewArea`;
5. секция раскрывается через `setExpanded(true)`;
6. `focus()` прокручивает длинную форму к результату;
7. `RuntimeException` журналируется со stack trace и преобразуется в warning.

Такой порядок обеспечивает preview несохранённых значений и исключает remote-передачу редактируемой CUBA entity. Если профиль выключен или согласие отсутствует, builder возвращает понятное сообщение вместо пустой области.

### 5.4. Вкладка AI

Сохраняются четыре действия:

- `onAiConfigsCreateBtnClick`;
- `onAiConfigsEditBtnClick`;
- `onAiConfigsRemoveBtnClick`;
- `onAiConfigsTestBtnClick`.

Таблица продолжает использовать `userAiConfigsDs`; endpoint и HTTP-логика провайдера не переносятся в экран.

Оба новых значения являются предпочтениями. Экран не выбирает конкретный API, не проверяет наличие ключа и не определяет, какой текст считать личным промптом.

### 5.5. Аватар и обработка изображения

Компонент `userPic` и fallback `defaultPic` сохраняют утверждённый контракт `OvaFallbackImage`. Обработка нового файла выполняется прежним `AvatarImageUploadHelper.processUploadedImage(...)`; изменён только способ получения `ImageProcessingService`.

Цепочка выполнения:

```text
FileLoader.openStream
→ AvatarImageUploadHelper
→ ImageProcessingService proxy
→ ImageProcessingServiceBean в middleware
→ ProcessedImage (Serializable)
→ FileStorageService / DataManager
```

Core-сервис должен быть доступен после `AppBeans.get(ImageProcessingService.NAME)` до срабатывания upload-listener. AWT, `ImageIO`, Thumbnailator и серверная конфигурация не переносятся в web-контроллер.

## 6. Визуальный контракт

### 6.1. Корневой слой

Все правила ограничены:

```scss
.ext-settings-window { ... }
```

Запрещено подключать `.job-candidate-editor` как зависимость, изменять глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet`, `.v-textfield` и влиять на другие экраны.

### 6.2. Тёмная контекстная панель

Боковые панели всех четырёх вкладок используют тот же визуальный язык, что и `JobCandidateEdit`:

- фон `#172638`;
- градиент `#172638 → #132130 → #0f1b28`;
- текст `#f8fafc`;
- акцент заголовка и активной навигации `#ffb11b`;
- правая граница `rgba(15, 23, 42, 0.78)`;
- тень `5px 0 20px rgba(15, 23, 42, 0.18)`;
- ширина 270 px, при viewport до 1366 px — 250 px;
- подсказки и служебные блоки оформляются полупрозрачными карточками внутри тёмной панели.

### 6.3. Вкладки

`TabSheet` сохраняет штатное поведение CUBA, но получает геометрию `JobCandidateEdit`:

- высота строки вкладок — 48 px;
- размер подписи — 15 px;
- активная вкладка — цвет `$v-selection-color` и нижняя граница 3 px;
- рабочая область начинается после разделителя и имеет самостоятельный фон;
- hover не меняет размеры элементов.

### 6.4. Рабочая область, toolbar, карточки и аккордеоны

- фон рабочей области вычисляется из `$v-app-background-color` и `$v-panel-background-color`;
- toolbar имеет минимальную высоту 58 px, внутренние отступы и локальную тень;
- карточки имеют радиус 8 px, рамку, внутренний отступ 20–22 px и тень `0 2px 8px`;
- заголовки карточек — 18 px, насыщенность 700;
- заголовки toolbar — 20 px, насыщенность 700;
- email-секции используют локальный стиль `user-ai-profile-section` и занимают 100% рабочей ширины;
- прежний трёхколоночный `emailSettingsGrid` удалён из XML, поэтому фиксированная sidebar больше не сжимает три протокола в одну строку;
- второй AI-checkbox использует существующий класс `ai-settings-preference-checkbox`;
- preview сохраняет существующий `previewGroup` и read-only `aiContextPreviewArea`; после действия меняются только expanded/focus state.

### 6.5. Поля и состояния

- `TextField`, `LookupField`, `DateField` — минимальная высота 38 px;
- основной размер текста полей — 15 px;
- `TextArea` сохраняет заданную XML-высоту и получает line-height 1.45;
- локальный focus использует `$v-selection-color` и контур 2 px;
- read-only отличается фоном, disabled — непрозрачностью 0.55;
- captions — 13 px, насыщенность 600;
- кнопки — минимальная высота 38 px, радиус 5 px;
- footer — отдельная панель высотой не менее 62 px.

### 6.6. Темы

Одинаковый файл `com.company.hunttech/settings-window-sections.scss` синхронизирован в семи темах:

| Тема | Требование |
|---|---|
| `halo` | обязательный эталонный smoke |
| `havana` | тот же структурный контракт |
| `helium` | тот же структурный контракт |
| `hover` | активная тема приложения по умолчанию |
| `hunttech-modern` | тот же структурный контракт |
| `hunttech-modern-light` | тот же структурный контракт |
| `hunttech-modern-dark` | тёмная sidebar сохраняется, рабочие поверхности используют переменные темы |

Каждый `styles.scss` импортирует `com.company.hunttech/settings-window-sections` и вызывает `@include settings-window-sections;`.

## 7. Соответствие CUBA Platform 7.3

1. Базовый экран остаётся XML-экраном `window.xsd`.
2. Фактический screen ID `settings` использует штатное legacy XML inheritance.
3. Вкладки остаются дочерними компонентами `TabSheet`.
4. Data API остаётся `dsContext`.
5. Legacy component ID не переименовываются.
6. Существующие `datasource`, `property`, `required`, validators, actions и `invoke` сохраняются.
7. `previewAiContextBtn` сохраняет `invoke="previewAiContext"`, а Java override фактического controller вызывается полиморфно.
8. AI-checkbox используют штатный legacy datasource-binding.
9. `UserSettings` сохраняется через существующий `CommitContext`.
10. Почтовый аккордеон использует штатные атрибуты `GroupBoxLayout`: `collapsable`, `collapsed`, `showAsPanel`.
11. Middleware-сервисы разрешаются через `AppBeans.get(<Service>.NAME)`; `@Inject` и class-based lookup для этих полей не используются.
12. UI-preview использует stateless `UserAiContextBuilder` из `global`, не передавая редактируемую entity через remoting.
13. DTO удалённых Service API размещаются в `global` и реализуют `Serializable`.
14. Визуальный слой подключается только через `stylename` и theme extension.
15. SCSS не инициирует загрузку данных и не вмешивается в lifecycle.
16. Глобальные Vaadin-селекторы не добавляются.

## 8. Обязательные проверки

Hermes проверяет точный HEAD ветки без изменения кода:

```bash
git diff --check

./gradlew :app-global:compileJava \
          :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.service.UserAiContextServiceBeanTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowCoreBeanLookupTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowEmailNavigationTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowAiNavigationTest' \
          --tests 'com.company.hunttech.core.UserSettingsAiApiPreferenceTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowAvatarComponentTest' \
          --tests 'com.company.hunttech.app.ImageProcessingServiceBeanTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидается:

- `UserAiContextServiceBeanTest` — 8/8 PASS;
- `ExtSettingsWindowCoreBeanLookupTest` — 4/4 PASS;
- `ExtSettingsWindowEmailNavigationTest` — 3/3 PASS;
- `ExtSettingsWindowAiNavigationTest` — 3/3 PASS;
- `UserSettingsAiApiPreferenceTest` — 11/11 PASS;
- `ExtSettingsWindowAvatarComponentTest` — 2/2 PASS;
- `ImageProcessingServiceBeanTest` — PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS;
- SQL и Liquibase — N/A по diff;
- SCSS — PASS/N/A по diff, контрольная сборка всех тем — PASS;
- `BUILD SUCCESSFUL`;
- локальный deploy того же HEAD;
- HTTP `/hrm/` = 200;
- отсутствие новых критических ошибок в Tomcat logs.

### 8.1. Функциональный smoke

Проверить:

- окно фактически открывается без `NoSuchBeanDefinitionException`, ошибки создания контроллера и cross-context ошибки;
- `ImageProcessingService` и `UserAiContextService` доступны после `init()` как CUBA middleware proxy;
- class-based lookup `AppBeans.get(ImageProcessingService.class)` и `AppBeans.get(UserAiContextService.class)` отсутствует;
- оба checkbox включены у новой записи пользователя;
- существующая запись после миграции получает оба значения `true`;
- пользователь может выключить каждый checkbox независимо;
- `okBtn` сохраняет оба значения;
- после повторного открытия состояния восстановлены;
- Cancel не фиксирует несохранённые изменения;
- изменить `currentPosition` и `aboutMe` без Save, нажать «Показать передаваемые данные» → новые значения присутствуют в preview;
- `previewGroup` раскрывается, фокус/прокрутка переводят пользователя к `aiContextPreviewArea`;
- при выключенном профиле либо согласии preview сообщает, что контекст не передаётся;
- preview не содержит SMTP/POP3/IMAP-пароли, API-ключи и конфигурации подключения;
- runtime-ошибка не закрывает форму, журнал содержит stack trace, пользователю показывается warning;
- значения SMTP, POP3 и IMAP загружаются и сохраняются прежними методами;
- раскрытие и сворачивание почтовых секций не изменяет введённые значения;
- загрузка изображения меньше лимита сохраняет оригинал;
- загрузка изображения больше `targetImageSize` вызывает обработку в middleware и успешное повторное отображение;
- аватар `OvaFallbackImage`, upload, очистка и fallback работают без регрессии;
- в логах отсутствуют новые serialization errors, `ClassCastException`, `NoSuchBeanDefinitionException`, `NullPointerException` и критические ошибки.

### 8.2. Визуальный smoke

Во всех семи темах проверить:

- наличие тёмной sidebar на каждой вкладке;
- акцент `#ffb11b` для заголовка и активного пункта;
- высоту вкладок 48 px;
- toolbar 58 px и карточки с выраженной границей;
- поля высотой 38 px;
- оба AI-checkbox полностью видимы и не перекрываются;
- SMTP, POP3 и IMAP расположены вертикально и занимают всю ширину рабочей области;
- SMTP раскрыт при первом открытии, POP3 и IMAP свёрнуты;
- каждая почтовая секция раскрывается и сворачивается без потери введённых значений;
- previewGroup раскрывается, текст читаем и не обрезан;
- отсутствие горизонтальной прокрутки при ширине 1200 px;
- focus, hover, read-only, disabled и validation states;
- круглый аватар отображается через `OvaFallbackImage` без искажения;
- отсутствие влияния на `JobCandidateEdit` и другие формы;
- наличие `.ext-settings-window` и `#172638` в развёрнутом `VAADIN/themes/*/styles.css`;
- hard reload браузера с отключённым cache перед итоговым сравнением.

До отчёта Hermes по точному SHA статус задачи — `WAITING_FOR_HERMES`.

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Восстановлена кнопка «Показать передаваемые данные»: фактический controller формирует preview локально через общий `UserAiContextBuilder`, отражает несохранённые значения, раскрывает секцию и фокусирует результат без remote-передачи `UserAiProfile` |
| 2026-07-25 | Исправлен cross-context доступ: `ImageProcessingService` и `UserAiContextService` разрешаются по стабильному CUBA service name и вызываются через middleware proxy; class-based `AppBeans` lookup удалён |
| 2026-07-25 | `ImageProcessingService` и `UserAiContextService` переведены с `@Inject` на `AppBeans.get(...)` в начале `init()`; добавлена регрессионная проверка доступности core-бинов в legacy web-контроллере |
| 2026-07-25 | PR #17 перебазирован на актуальный `master`; документация email-аккордеона объединена с AI-предпочтениями и `OvaFallbackImage` без потери функциональных контрактов |
| 2026-07-24 | SMTP, POP3 и IMAP вкладки «Настройка email» перестроены из трёхколоночной сетки в вертикальный аккордеон; component ID, валидаторы, загрузка и сохранение значений не изменены |
| 2026-07-24 | Оба персональных AI-предпочтения включены по умолчанию; добавлен checkbox «Предпочитать мои промпты», сохранение в `UserSettings`, миграции и null-safe исправление `previewAiContext()` |
| 2026-07-24 | Исправлена недостаточно заметная визуальная адаптация: все вкладки получили тёмную контекстную панель, акцент `#ffb11b`, вкладки 48 px, toolbar 58 px, поля 38 px и выраженные карточки в дизайн-языке `JobCandidateEdit`; XML и бизнес-логика не изменены |
| 2026-07-24 | Визуальное оформление `ExtSettingsWindow` адаптировано к дизайн-языку `JobCandidateEdit` через локальный namespace и theme-aware состояния; реализация признана визуально недостаточной после локального deploy |
| 2026-07-24 | Вкладки «Интерфейс» и «Настройка email» приведены к двухпанельной концепции Edit-форм; сохранены legacy component ID, типы полей, валидаторы и бизнес-логика |
| 2026-07-23 | Вкладка AI приведена к двухпанельной концепции Edit-форм: sidebar 270 px, единый toolbar, карточки источника API и подключений |
| 2026-07-23 | Добавлен checkbox предпочтения персональных настроек API, поле `UserSettings.preferPersonalAiApiSettings`, миграции и тесты |
| 2026-07-23 | `user-ai-profile-views.xml` зарегистрирован в рабочих конфигурациях core/web |
| 2026-07-23 | Введено обязательное правило синхронизации семи тем |
| 2026-07-23 | Восстановлены двухпанельная вкладка «Обо мне» и контроллер `UserAiProfile` после merge-регрессии |
| 2026-07-22 | Контроллер, datasource ИИ-профиля и JPQL перенесены в namespace `hunttech` с сохранением legacy-контрактов |
| 2026-07-22 | Исправлено программное раскрытие предпросмотра через `setExpanded(true)` |
| 2026-07-22 | Вкладка «Обо мне» переработана в двухпанельный профессиональный ИИ-профиль |

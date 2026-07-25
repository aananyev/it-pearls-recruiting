# ExtSettingsWindowEmailNavigation — навигация по настройкам email и AI

> Проект: **HRM HuntTech**.  
> Экран: `settings`.  
> Базовый контроллер: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> Расширяющий контроллер: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindowEmailNavigation`.  
> Базовый XML: `ext-settings-window.xml`.  
> Расширяющий XML: `ext-settings-window-email-navigation.xml`.  
> Связанная спецификация: [ExtSettingsWindow_Spec.md](ExtSettingsWindow_Spec.md).

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Левые панели вкладок «Настройка email» и AI содержат навигационные индексы. Изначально элементы SMTP, POP3, IMAP, «Источник API» и «Подключения» были обычными `Label`: они выглядели как элементы выбора, но не управляли соответствующими рабочими блоками справа.

Расширение делает эти индексы функциональными без перестройки формы и без переноса бизнес-логики. Пользователь выбирает пункт слева и получает соответствующий UI-контекст справа:

- для email раскрывается нужный аккордеон и сворачиваются остальные;
- для AI фокус переводится в карточку персональных предпочтений либо в таблицу подключений;
- активный пункт левой панели синхронизируется с выбранным блоком.

### UI Context & Navigation

Экран открывается через screen ID `settings`. `web-screens.xml` направляет этот ID на `ext-settings-window-email-navigation.xml`, который наследует действующий `ext-settings-window.xml` средствами legacy XML inheritance CUBA Platform.

Пользовательские пути:

```text
Настройки → вкладка «Настройка email»
           → SMTP / POP3 / IMAP слева
           → соответствующий аккордеон справа
           → первое поле выбранного протокола
```

```text
Настройки → вкладка AI
           → Источник API / Подключения слева
           → соответствующая карточка справа
           → первое интерактивное содержимое карточки
```

Базовая двухпанельная компоновка, размеры, captions, sidebar, toolbar, карточки, аккордеоны и поля ввода не перестраиваются.

### Behavior Summary

- открытие экрана → выполняется полный `ExtSettingsWindow.init()` → данные и значения загружаются прежним способом;
- завершение базовой инициализации → расширение заменяет только некликабельные пункты навигации на borderless-кнопки CUBA → геометрия sidebar не меняется;
- нажатие SMTP → раскрывается `smtpSettingsSection`, POP3 и IMAP сворачиваются → фокус переходит в `smtpServer`;
- нажатие POP3 → раскрывается `pop3SettingsSection`, SMTP и IMAP сворачиваются → фокус переходит в `pop3Server`;
- нажатие IMAP → раскрывается `imapSettingsSection`, SMTP и POP3 сворачиваются → фокус переходит в `imapServer`;
- нажатие «Источник API» → активируется соответствующий пункт слева → фокус переходит в `preferPersonalAiApiSettingsField` без изменения его значения;
- нажатие «Подключения» → активируется соответствующий пункт слева → фокус переходит в `aiConfigsTable` без изменения выбранной строки;
- переход по AI-навигации → операции создания, редактирования, удаления и проверки подключения не вызываются;
- сохранение окна → выполняются прежние методы базового контроллера → бизнес-логика и данные не меняются.

## 1. Точка вызова и контекст

| Параметр | Значение |
|---|---|
| Screen ID | `settings` |
| Регистрация | `modules/web/src/com/company/hunttech/web-screens.xml` |
| Базовый дескриптор | `/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml` |
| Расширяющий дескриптор | `/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-email-navigation.xml` |
| Базовый контроллер | `ExtSettingsWindow` |
| Расширяющий контроллер | `ExtSettingsWindowEmailNavigation` |
| Email-вкладка | `mailAccessTab` |
| Email-навигация | `emailSettingsNavigation` |
| AI-вкладка | `aiAccessTab` |
| AI-навигация | `aiSettingsNavigation` |

Расширяющий XML содержит только ссылку `extends`, controller class и пустой наследуемый `<layout/>`. Все визуальные компоненты загружаются из базового дескриптора.

## 2. Связь с моделью данных

Изменение не затрагивает модель данных.

Сохраняются:

- entity `UserSettings` и `UserAiConfiguration`;
- datasource `userSettingsDs` и `userAiConfigsDs`;
- поля SMTP, POP3 и IMAP;
- оба AI-checkbox;
- таблица `aiConfigsTable`;
- `setEmailSettings()` и `collectEmailSettings()`;
- действия `onAiConfigsCreateBtnClick`, `onAiConfigsEditBtnClick`, `onAiConfigsRemoveBtnClick`, `onAiConfigsTestBtnClick`;
- общий `CommitContext`;
- БД, Liquibase, views, JPQL и сервисы.

Расширяющий контроллер не инжектирует `DataManager`, datasource или сервисы и не записывает значения в сущности.

## 3. Иерархия и взаимосвязь форм

```text
settings
└── ext-settings-window-email-navigation.xml
    └── extends ext-settings-window.xml
        ├── mailAccessTab
        │   ├── emailSettingsNavigation
        │   │   ├── emailSettingsSmtpNav [Button]
        │   │   ├── emailSettingsPop3Nav [Button]
        │   │   └── emailSettingsImapNav [Button]
        │   └── emailSettingsAccordion
        │       ├── smtpSettingsSection
        │       ├── pop3SettingsSection
        │       └── imapSettingsSection
        └── aiAccessTab
            ├── aiSettingsNavigation
            │   ├── aiSettingsSourceNav [Button]
            │   └── aiSettingsConnectionsNav [Button]
            └── aiSettingsContent
                ├── personalAiApiPreferenceBox
                │   └── preferPersonalAiApiSettingsField
                └── aiConnectionsCard
                    └── aiConfigsTable
```

## 4. Модель поведения и интерактивность

### 4.1. Email

| Действие | Раскрывается | Сворачиваются | Фокус |
|---|---|---|---|
| SMTP | `smtpSettingsSection` | POP3, IMAP | `smtpServer` |
| POP3 | `pop3SettingsSection` | SMTP, IMAP | `pop3Server` |
| IMAP | `imapSettingsSection` | SMTP, POP3 | `imapServer` |

При открытии экрана активен SMTP. Введённые значения не сбрасываются при сворачивании секции: `GroupBoxLayout.setExpanded()` меняет только presentation state.

### 4.2. AI

| Действие | Правый блок | Фокус | Не изменяется |
|---|---|---|---|
| Источник API | `personalAiApiPreferenceBox` | `preferPersonalAiApiSettingsField` | значения обоих checkbox |
| Подключения | `aiConnectionsCard` | `aiConfigsTable` | выбранная строка и состояния AI-действий |

Карточки AI не скрываются и не преобразуются в аккордеоны. Выбор слева является навигационным и не изменяет данные.

## 5. Логика управляющих элементов

Кнопки создаются через штатный `UiComponents`.

### 5.1. Email

- `emailSettingsSmtpNav`;
- `emailSettingsPop3Nav`;
- `emailSettingsImapNav`.

Обычный стиль:

```text
borderless settings-section-nav-item
```

Активный стиль:

```text
borderless settings-section-nav-item settings-section-nav-item-active
```

### 5.2. AI

- `aiSettingsSourceNav`;
- `aiSettingsConnectionsNav`.

Обычный стиль:

```text
borderless ai-settings-nav-item
```

Активный стиль:

```text
borderless ai-settings-nav-item ai-settings-nav-item-active
```

Используются существующие message keys; новые ключи локализации не требуются.

## 6. Визуальная компоновка элементов

Форма визуально не перестраивается:

- сохраняются `emailSettingsNavigation` и `aiSettingsNavigation`;
- сохраняется исходный порядок пунктов;
- кнопки имеют ширину `100%`;
- используются существующие локальные SCSS-классы;
- новый SCSS не добавляется;
- семь тем не получают отдельных изменений;
- глобальные `.v-button`, `.v-label`, `.v-table`, `.v-tabsheet` и другие Vaadin-селекторы не изменяются.

## 7. Соответствие CUBA Platform 7.3

1. Legacy-экран расширяется через атрибут `extends` корневого `<window>`.
2. Screen ID `settings` сохраняется.
3. Контроллер наследуется от `ExtSettingsWindow`.
4. UI-компоненты создаются через `UiComponents`.
5. Кнопки используют штатные click listeners.
6. Email-аккордеоны управляются через `GroupBoxLayout.setExpanded()`.
7. Фокус email устанавливается через `TextField.focus()`.
8. `Table<UserAiConfiguration>` реализует `Component.Focusable`, поэтому AI-таблица получает фокус штатным `focus()`.
9. Data API, lifecycle загрузки и commit базового контроллера не переопределяются.

## 8. Обязательные проверки

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.ExtSettingsWindowEmailNavigationTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowAiNavigationTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидается:

- `ExtSettingsWindowEmailNavigationTest` — 3/3 PASS;
- `ExtSettingsWindowAiNavigationTest` — 3/3 PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS/N/A, поскольку getters, views и datasource не меняются;
- SCSS — N/A по diff;
- `BUILD SUCCESSFUL`;
- local deploy того же HEAD;
- HTTP `/hrm/` = 200;
- critical Tomcat errors — NONE.

### Smoke Hermes

1. Открыть `settings` и проверить отсутствие визуальной регрессии всех вкладок.
2. Во вкладке email повторить smoke SMTP, POP3 и IMAP из предыдущего изменения.
3. Открыть вкладку AI: активен пункт «Источник API».
4. Нажать «Источник API»: фокус находится на первом checkbox, его значение не изменилось.
5. Изменить значение checkbox вручную, затем нажать «Подключения».
6. Проверить активную подсветку «Подключения» и фокус таблицы.
7. Убедиться, что выбранная строка таблицы не изменилась автоматически.
8. Вернуться к «Источнику API»: ранее введённое значение checkbox сохранено до Save/Cancel.
9. Проверить Create/Edit/Remove/Test — действия работают прежним способом.
10. Проверить Save, Cancel и повторное открытие окна.
11. Проверить отсутствие новых ошибок в Tomcat logs.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Добавлена кликабельная AI-навигация: «Источник API» фокусирует персональные предпочтения, «Подключения» — таблицу конфигураций; значения, выбор таблицы и AI-действия не изменяются |
| 2026-07-25 | Добавлена кликабельная навигация SMTP, POP3 и IMAP: выбранный пункт раскрывает соответствующий аккордеон, сворачивает остальные и фокусирует первое поле без изменения формы и бизнес-логики |

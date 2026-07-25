# ExtSettingsWindowEmailNavigation — навигация всех вкладок настроек

> Проект: **HRM HuntTech**.  
> Экран: `settings`.  
> Базовый контроллер: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> Расширяющий контроллер: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindowEmailNavigation`.  
> Базовый XML: `ext-settings-window.xml`.  
> Расширяющий XML: `ext-settings-window-email-navigation.xml`.  
> Связанная спецификация: [ExtSettingsWindow_Spec.md](ExtSettingsWindow_Spec.md).

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Все четыре вкладки `ExtSettingsWindow` используют двухпанельную компоновку HRM HuntTech: контекст и индекс разделов слева, рабочие блоки справа. Изначально пункты этих индексов были обычными `Label`: они выглядели как элементы навигации, но не выполняли переход.

Расширяющий контроллер делает навигацию функциональной без перестройки формы и без переноса бизнес-логики:

- «Обо мне» — выбирает один из шести аккордеонов профиля;
- «Интерфейс» — переводит фокус к соответствующей группе штатных настроек CUBA;
- «Настройка email» — выбирает аккордеон SMTP, POP3 или IMAP;
- AI — переводит фокус к персональным предпочтениям или таблице подключений;
- активный пункт левой панели синхронизируется с выбранным UI-контекстом.

### UI Context & Navigation

Экран открывается через screen ID `settings`. `web-screens.xml` направляет этот ID на `ext-settings-window-email-navigation.xml`, который наследует `ext-settings-window.xml` средствами legacy XML inheritance CUBA Platform.

Пользовательские пути:

```text
Настройки → «Обо мне»
           → Профессиональный профиль / Профиль рекрутера /
             Предпочтения ответов / Цели и интересы /
             Конфиденциальность / Предпросмотр контекста
           → соответствующий аккордеон справа
```

```text
Настройки → «Интерфейс»
           → Окно / Оформление / Региональные параметры / Стартовый экран
           → первое штатное поле соответствующей группы
```

```text
Настройки → «Настройка email»
           → SMTP / POP3 / IMAP
           → соответствующий аккордеон
```

```text
Настройки → AI
           → Источник API / Подключения
           → соответствующая карточка
```

Базовые размеры, captions, component ID, sidebar, toolbar, карточки, аккордеоны, datasource и поля не меняются.

### Behavior Summary

- открытие экрана → выполняется полный `ExtSettingsWindow.init()` → данные загружаются прежним способом;
- завершение базовой инициализации → расширение заменяет только навигационные `Label` на borderless-кнопки CUBA → геометрия вкладок не меняется;
- при начальной инициализации скрытые вкладки получают только активный стиль → принудительный фокус не устанавливается;
- выбор раздела «Обо мне» → раскрывается выбранный `GroupBoxLayout`, остальные пять сворачиваются → фокус переходит в первое поле выбранного раздела;
- выбор раздела «Интерфейс» → карточка и legacy-компоненты остаются на месте → фокус переходит к соответствующему полю;
- выбор SMTP, POP3 или IMAP → раскрывается выбранный почтовый аккордеон, остальные сворачиваются;
- выбор AI-раздела → фокус переходит в checkbox предпочтений или таблицу подключений;
- сохранение и Cancel → выполняются прежними методами базового контроллера → значения и бизнес-логика не меняются.

## 1. Точка вызова и контекст

| Параметр | Значение |
|---|---|
| Screen ID | `settings` |
| Регистрация | `modules/web/src/com/company/hunttech/web-screens.xml` |
| Базовый дескриптор | `/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml` |
| Расширяющий дескриптор | `/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-email-navigation.xml` |
| Базовый контроллер | `ExtSettingsWindow` |
| Расширяющий контроллер | `ExtSettingsWindowEmailNavigation` |
| «Обо мне» | `msgMyInfo` / `userAiProfileSectionNavigation` |
| «Интерфейс» | `msgInterface` / `interfaceSettingsNavigation` |
| Email | `mailAccessTab` / `emailSettingsNavigation` |
| AI | `aiAccessTab` / `aiSettingsNavigation` |

Расширяющий XML содержит только `extends`, controller class и пустой наследуемый `<layout/>`. Визуальные компоненты загружаются из базового дескриптора.

## 2. Связь с моделью данных

Изменение не затрагивает модель данных.

Сохраняются:

- `ExtUser`, `UserAiProfile`, `UserSettings`, `UserAiConfiguration`;
- `extUserDs`, `userAiProfileDs`, `userSettingsDs`, `userAiConfigsDs`;
- все datasource-binding, properties, validators и required-состояния;
- почтовые поля и методы `setEmailSettings()` / `collectEmailSettings()`;
- AI-действия Create/Edit/Remove/Test;
- `previewAiContext()` и `clearAiProfile()`;
- штатные методы базового `SettingsWindow`;
- общий `CommitContext`;
- БД, Liquibase, views, JPQL и сервисы.

Расширяющий контроллер не инжектирует `DataManager`, datasource или сервисы, не вызывает `setValue()` и не инициирует бизнес-действия.

## 3. Иерархия и взаимосвязь форм

```text
settings
└── ext-settings-window-email-navigation.xml
    └── extends ext-settings-window.xml
        ├── msgMyInfo
        │   ├── userAiProfileSectionNavigation
        │   │   ├── userAiProfileProfessionalNav
        │   │   ├── userAiProfileRecruitingNav
        │   │   ├── userAiProfileResponseNav
        │   │   ├── userAiProfileGoalsNav
        │   │   ├── userAiProfilePrivacyNav
        │   │   └── userAiProfilePreviewNav
        │   └── professionalProfileGroup / recruitingProfileGroup /
        │       responsePreferencesGroup / goalsGroup / privacyGroup / previewGroup
        ├── msgInterface
        │   ├── interfaceSettingsNavigation
        │   │   ├── interfaceSettingsWindowNav
        │   │   ├── interfaceSettingsAppearanceNav
        │   │   ├── interfaceSettingsRegionalNav
        │   │   └── interfaceSettingsStartupNav
        │   └── grid
        ├── mailAccessTab
        │   ├── emailSettingsNavigation
        │   └── emailSettingsAccordion
        └── aiAccessTab
            ├── aiSettingsNavigation
            └── personalAiApiPreferenceBox / aiConnectionsCard
```

## 4. Модель поведения и интерактивность

### 4.1. Вкладка «Обо мне»

| Пункт слева | Раскрывается | Фокус |
|---|---|---|
| Профессиональный профиль | `professionalProfileGroup` | `currentPositionField` |
| Профиль рекрутера | `recruitingProfileGroup` | `recruitingSpecializationsField` |
| Предпочтения ответов | `responsePreferencesGroup` | `preferredLanguageField` |
| Цели и интересы | `goalsGroup` | `professionalGoalsField` |
| Конфиденциальность | `privacyGroup` | `profileEnabledField` |
| Предпросмотр контекста | `previewGroup` | `aiContextPreviewArea` |

Выбор является взаимоисключающим: раскрывается один раздел, остальные пять сворачиваются. `setExpanded()` меняет только presentation state; значения `UserAiProfile`, согласие и preview-текст не изменяются.

Пункт «Предпросмотр контекста» только раскрывает существующий блок. Метод `previewAiContext()` автоматически не вызывается.

### 4.2. Вкладка «Интерфейс»

| Пункт слева | Целевой контекст | Фокус |
|---|---|---|
| Окно приложения | режим главного окна | `modeOptions` |
| Оформление | тема | `appThemeField` |
| Региональные параметры | язык и часовой пояс | `appLangField` |
| Стартовый экран | стартовый экран | `defaultScreenField` |

Вкладка сохраняет одну существующую карточку `interfaceAppearanceCard`. Переход не скрывает строки grid и не меняет значения базового `SettingsWindow`.

### 4.3. Email

| Пункт | Раскрывается | Сворачиваются | Фокус |
|---|---|---|---|
| SMTP | `smtpSettingsSection` | POP3, IMAP | `smtpServer` |
| POP3 | `pop3SettingsSection` | SMTP, IMAP | `pop3Server` |
| IMAP | `imapSettingsSection` | SMTP, POP3 | `imapServer` |

### 4.4. AI

| Пункт | Правый блок | Фокус | Не изменяется |
|---|---|---|---|
| Источник API | `personalAiApiPreferenceBox` | `preferPersonalAiApiSettingsField` | значения checkbox |
| Подключения | `aiConnectionsCard` | `aiConfigsTable` | selected row и состояния действий |

## 5. Логика управляющих элементов

Кнопки создаются через штатный `UiComponents`, получают прежние captions через существующие message keys и имеют ширину `100%`.

### 5.1. «Обо мне»

Обычный стиль:

```text
borderless user-ai-profile-nav-item
```

Активный стиль:

```text
borderless user-ai-profile-nav-item user-ai-profile-nav-item-active
```

### 5.2. «Интерфейс» и email

Обычный стиль:

```text
borderless settings-section-nav-item
```

Активный стиль:

```text
borderless settings-section-nav-item settings-section-nav-item-active
```

### 5.3. AI

Обычный стиль:

```text
borderless ai-settings-nav-item
```

Активный стиль:

```text
borderless ai-settings-nav-item ai-settings-nav-item-active
```

Новые message keys и новый SCSS не требуются.

## 6. Визуальная компоновка элементов

Форма визуально не перестраивается:

- сохраняются все четыре sidebar и рабочие контейнеры;
- сохраняется исходный порядок пунктов;
- кнопки используют существующие локальные классы;
- ширина sidebar, отступы, toolbar и карточки не меняются;
- семь тем не получают отдельных SCSS-изменений;
- глобальные `.v-button`, `.v-label`, `.v-table`, `.v-tabsheet` и другие Vaadin-селекторы не изменяются.

## 7. Соответствие CUBA Platform 7.3

1. Legacy-экран расширяется через `extends` корневого `<window>`.
2. Screen ID `settings` сохраняется.
3. Контроллер наследуется от `ExtSettingsWindow`.
4. Компоненты создаются через `UiComponents`.
5. Кнопки используют штатные click listeners.
6. Аккордеоны управляются через `GroupBoxLayout.setExpanded()`.
7. `TextField`, `TextArea`, `LookupField`, `CheckBox`, `OptionsGroup` и `Table` получают фокус штатным `focus()`.
8. Datasource-binding, lifecycle загрузки и commit базового контроллера не переопределяются.
9. Навигация не вызывает business actions и не записывает значения.

## 8. Обязательные проверки

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.ExtSettingsWindowEmailNavigationTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowAiNavigationTest' \
          --tests 'com.company.hunttech.core.ExtSettingsWindowRemainingNavigationTest' \
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
- `ExtSettingsWindowRemainingNavigationTest` — 4/4 PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS/N/A, getters, views и datasource не менялись;
- SCSS — N/A по diff;
- `BUILD SUCCESSFUL`;
- local deploy того же HEAD;
- HTTP `/hrm/` = 200;
- critical Tomcat errors — NONE.

### Smoke Hermes

1. Открыть `settings`, подтвердить отсутствие визуальной регрессии.
2. «Обо мне»: последовательно выбрать все 6 пунктов.
3. Для каждого пункта проверить: раскрыт только соответствующий аккордеон, активная подсветка слева синхронизирована, фокус находится в первом поле.
4. Изменить несохранённое значение, перейти в другой раздел и вернуться — значение не должно потеряться.
5. Выбрать «Предпросмотр контекста» — блок раскрывается, но `previewAiContext()` сам не запускается.
6. «Интерфейс»: проверить 4 пункта и фокус `modeOptions`, `appThemeField`, `appLangField`, `defaultScreenField`.
7. Убедиться, что навигация не меняет режим, тему, язык, часовой пояс или стартовый экран.
8. Проверить Save, Cancel, смену пароля и сброс экранных настроек.
9. Повторить smoke SMTP/POP3/IMAP.
10. Повторить smoke «Источник API»/«Подключения».
11. Проверить вкладки во всех поддерживаемых темах; новый SCSS отсутствует, но существующие состояния кнопок должны сохраниться.
12. Проверить Tomcat logs: critical errors — NONE.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Добавлена кликабельная навигация оставшихся вкладок: шесть пунктов «Обо мне» выбирают соответствующие аккордеоны, четыре пункта «Интерфейс» переводят фокус к штатным полям без изменения данных и бизнес-логики |
| 2026-07-25 | Добавлена кликабельная AI-навигация: «Источник API» фокусирует персональные предпочтения, «Подключения» — таблицу конфигураций; значения, выбор таблицы и AI-действия не изменяются |
| 2026-07-25 | Добавлена кликабельная навигация SMTP, POP3 и IMAP: выбранный пункт раскрывает соответствующий аккордеон, сворачивает остальные и фокусирует первое поле без изменения формы и бизнес-логики |

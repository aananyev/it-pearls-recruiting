# ExtSettingsWindowEmailNavigation — навигация всех вкладок и preview контекста

> Проект: **HRM HuntTech**.  
> Экран: `settings`.  
> Базовый контроллер: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> Фактический контроллер: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindowEmailNavigation`.  
> Базовый XML: `ext-settings-window.xml`.  
> Расширяющий XML: `ext-settings-window-email-navigation.xml`.  
> Связанные спецификации: [ExtSettingsWindow_Spec.md](ExtSettingsWindow_Spec.md), [UserAiContextService](../services/UserAiContextService.md).

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Все четыре вкладки `ExtSettingsWindow` используют двухпанельную компоновку HRM HuntTech: контекст и индекс разделов слева, рабочие блоки справа. Изначально пункты этих индексов были обычными `Label`: они выглядели как элементы навигации, но не выполняли переход.

Фактический контроллер делает навигацию функциональной без перестройки формы:

- «Обо мне» — выбирает один из шести аккордеонов профиля;
- «Интерфейс» — переводит фокус к соответствующей группе штатных настроек CUBA;
- «Настройка email» — выбирает аккордеон SMTP, POP3 или IMAP;
- AI — переводит фокус к персональным предпочтениям или таблице подключений;
- активный пункт левой панели синхронизируется с выбранным UI-контекстом.

Этот же фактический контроллер обеспечивает работу кнопки «Показать передаваемые данные». Предпросмотр должен отражать текущие значения `UserAiProfile`, включая изменения до Save, поэтому он формируется локально через общий `UserAiContextBuilder`, а редактируемая CUBA entity не передаётся в middleware.

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
Настройки → «Обо мне»
           → изменить профиль без сохранения
           → «Показать передаваемые данные»
           → previewGroup / aiContextPreviewArea
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
- нажатие «Показать передаваемые данные» → читается текущий `userAiProfileDs.item` → `UserAiContextBuilder` формирует очищенный preview → раскрывается `previewGroup`, активируется пункт preview и фокус переводится в результат;
- профиль выключен или согласие отсутствует → preview сообщает, что контекст не передаётся → окно остаётся открытым;
- runtime-ошибка preview → stack trace пишется в журнал → пользователю показывается warning без закрытия формы;
- сохранение и Cancel → выполняются прежними методами базового контроллера → значения и commit-логика не меняются.

## 1. Точка вызова и контекст

| Параметр | Значение |
|---|---|
| Screen ID | `settings` |
| Регистрация | `modules/web/src/com/company/hunttech/web-screens.xml` |
| Базовый дескриптор | `/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml` |
| Расширяющий дескриптор | `/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-email-navigation.xml` |
| Базовый контроллер | `ExtSettingsWindow` |
| Фактический контроллер | `ExtSettingsWindowEmailNavigation` |
| «Обо мне» | `msgMyInfo` / `userAiProfileSectionNavigation` |
| Preview action | `previewAiContextBtn`, `invoke="previewAiContext"` |
| Preview data | `userAiProfileDs` |
| Preview result | `previewGroup` / `aiContextPreviewArea` |
| Shared builder | `com.company.hunttech.service.UserAiContextBuilder` |
| «Интерфейс» | `msgInterface` / `interfaceSettingsNavigation` |
| Email | `mailAccessTab` / `emailSettingsNavigation` |
| AI | `aiAccessTab` / `aiSettingsNavigation` |

Расширяющий XML содержит только `extends`, controller class и пустой наследуемый `<layout/>`. Визуальные компоненты загружаются из базового дескриптора. Java dynamic dispatch вызывает override `previewAiContext()` фактического контроллера.

## 2. Связь с моделью данных

Изменение не затрагивает модель данных, БД, Liquibase, views или JPQL.

Сохраняются:

- `ExtUser`, `UserAiProfile`, `UserSettings`, `UserAiConfiguration`;
- `extUserDs`, `userAiProfileDs`, `userSettingsDs`, `userAiConfigsDs`;
- все datasource-binding, properties, validators и required-состояния;
- почтовые поля и методы `setEmailSettings()` / `collectEmailSettings()`;
- AI-действия Create/Edit/Remove/Test;
- `clearAiProfile()` и commit базового контроллера;
- штатные методы базового `SettingsWindow`;
- общий `CommitContext`.

Фактический контроллер инжектирует `Datasource<UserAiProfile>` только для чтения текущего item при preview. Он не инжектирует `DataManager`, не выполняет commit, не вызывает `setValue()` у полей профиля и не инициирует внешний AI-вызов.

## 3. Иерархия и взаимосвязь форм

```text
settings
└── ext-settings-window-email-navigation.xml
    └── ExtSettingsWindowEmailNavigation
        └── extends ExtSettingsWindow
            ├── msgMyInfo
            │   ├── userAiProfileSectionNavigation
            │   │   ├── userAiProfileProfessionalNav
            │   │   ├── userAiProfileRecruitingNav
            │   │   ├── userAiProfileResponseNav
            │   │   ├── userAiProfileGoalsNav
            │   │   ├── userAiProfilePrivacyNav
            │   │   └── userAiProfilePreviewNav
            │   ├── previewAiContextBtn [invoke=previewAiContext]
            │   └── professionalProfileGroup / recruitingProfileGroup /
            │       responsePreferencesGroup / goalsGroup / privacyGroup / previewGroup
            │       └── aiContextPreviewArea
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

#### Кнопка «Показать передаваемые данные»

Кнопка сохраняет XML-контракт `invoke="previewAiContext"`. Фактический controller переопределяет этот публичный метод:

```java
UserAiProfile profile = userAiProfileDs.getItem();
aiContextPreviewArea.setValue(UserAiContextBuilder.buildPreview(profile));
previewGroup.setExpanded(true);
updateUserAiProfileNavigationStyles(userAiProfilePreviewNav);
aiContextPreviewArea.focus();
```

Полный метод проверяет datasource и UI-компоненты, журналирует `RuntimeException` и показывает warning. Он не вызывает `userAiContextService.buildContextPreview(profile)`, не передаёт редактируемую entity через remoting и не сохраняет профиль.

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

После построения preview активируется `userAiProfilePreviewNav`, чтобы sidebar отражала фактический открытый результат.

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
- `previewGroup` и read-only `aiContextPreviewArea` сохраняют размеры и стили;
- семь тем не получают отдельных SCSS-изменений;
- глобальные `.v-button`, `.v-label`, `.v-table`, `.v-tabsheet` и другие Vaadin-селекторы не изменяются.

## 7. Соответствие CUBA Platform 7.3

1. Legacy-экран расширяется через `extends` корневого `<window>`.
2. Screen ID `settings` сохраняется.
3. Контроллер наследуется от `ExtSettingsWindow`.
4. Компоненты создаются через `UiComponents`.
5. Кнопки используют штатные click listeners.
6. XML action сохраняет `invoke="previewAiContext"`, Java override вызывается полиморфно.
7. Аккордеоны управляются через `GroupBoxLayout.setExpanded()`.
8. `TextField`, `TextArea`, `LookupField`, `CheckBox`, `OptionsGroup` и `Table` получают фокус штатным `focus()`.
9. Preview читает legacy `Datasource<UserAiProfile>` без commit.
10. `UserAiContextBuilder` размещён в `global` и не зависит от Spring/remoting.
11. Datasource-binding, lifecycle загрузки и commit базового контроллера не переопределяются.
12. Навигация не вызывает business actions и не записывает значения.

## 8. Обязательные проверки

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
          --tests 'com.company.hunttech.core.ExtSettingsWindowRemainingNavigationTest' \
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
- `ExtSettingsWindowRemainingNavigationTest` — 4/4 PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS/N/A, getters, views и datasource не менялись;
- SCSS — PASS/N/A по diff, контрольная сборка тем обязательна;
- `BUILD SUCCESSFUL`;
- local deploy того же HEAD;
- HTTP `/hrm/` = 200;
- critical Tomcat errors — NONE.

### Smoke Hermes

1. Открыть `settings`, подтвердить отсутствие визуальной регрессии.
2. «Обо мне»: последовательно выбрать все 6 пунктов.
3. Для каждого пункта проверить: раскрыт только соответствующий аккордеон, активная подсветка слева синхронизирована, фокус находится в первом поле.
4. Изменить несохранённое значение, перейти в другой раздел и вернуться — значение не должно потеряться.
5. Выбрать «Предпросмотр контекста» — блок раскрывается, но preview автоматически не строится.
6. Включить профиль и согласие, изменить `currentPosition` и `aboutMe` без Save.
7. Нажать «Показать передаваемые данные» — previewGroup раскрыт, пункт preview активен, форма прокручена к результату.
8. Подтвердить наличие несохранённых значений и отсутствие SMTP/POP3/IMAP-паролей и API-ключей.
9. Выключить профиль либо согласие и повторить — preview сообщает, что контекст не передаётся.
10. «Интерфейс»: проверить 4 пункта и фокус `modeOptions`, `appThemeField`, `appLangField`, `defaultScreenField`.
11. Убедиться, что навигация не меняет режим, тему, язык, часовой пояс или стартовый экран.
12. Проверить Save, Cancel, смену пароля и сброс экранных настроек.
13. Повторить smoke SMTP/POP3/IMAP.
14. Повторить smoke «Источник API»/«Подключения».
15. Проверить вкладки во всех поддерживаемых темах.
16. Проверить Tomcat logs: отсутствуют новые remoting/serialization errors, `NoSuchBeanDefinitionException`, `NullPointerException` и critical errors.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Восстановлена кнопка «Показать передаваемые данные»: preview строится локально из текущего `userAiProfileDs` через общий `UserAiContextBuilder`, раскрывает секцию, активирует пункт навигации и фокусирует результат без remote-передачи entity |
| 2026-07-25 | Добавлена кликабельная навигация оставшихся вкладок: шесть пунктов «Обо мне» выбирают соответствующие аккордеоны, четыре пункта «Интерфейс» переводят фокус к штатным полям без изменения данных и бизнес-логики |
| 2026-07-25 | Добавлена кликабельная AI-навигация: «Источник API» фокусирует персональные предпочтения, «Подключения» — таблицу конфигураций; значения, выбор таблицы и AI-действия не изменяются |
| 2026-07-25 | Добавлена кликабельная навигация SMTP, POP3 и IMAP: выбранный пункт раскрывает соответствующий аккордеон, сворачивает остальные и фокусирует первое поле без изменения формы и бизнес-логики |

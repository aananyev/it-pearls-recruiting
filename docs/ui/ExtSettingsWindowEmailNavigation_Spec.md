# ExtSettingsWindowEmailNavigation — навигация по настройкам email

> Проект: **HRM HuntTech**.  
> Экран: `settings`.  
> Базовый контроллер: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> Расширяющий контроллер: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindowEmailNavigation`.  
> Базовый XML: `ext-settings-window.xml`.  
> Расширяющий XML: `ext-settings-window-email-navigation.xml`.  
> Связанная спецификация: [ExtSettingsWindow_Spec.md](ExtSettingsWindow_Spec.md).

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Вкладка «Настройка email» содержит три независимых набора параметров: SMTP, POP3 и IMAP. В левой контекстной панели эти протоколы уже представлены как навигационный индекс, однако до изменения пункты были обычными `Label` и не управляли соответствующими секциями справа.

Расширение делает левую навигацию функциональной: пользователь выбирает протокол слева и сразу получает раскрытый блок ввода нужного протокола справа. Это сокращает ручной поиск в длинной форме и связывает навигационный индекс с аккордеоном, не меняя загрузку, редактирование и сохранение почтовых настроек.

### UI Context & Navigation

Экран открывается через screen ID `settings`. `web-screens.xml` направляет этот ID на `ext-settings-window-email-navigation.xml`, который наследует действующий `ext-settings-window.xml` средствами legacy XML inheritance CUBA Platform.

Пользовательский путь:

```text
Настройки → вкладка «Настройка email»
           → SMTP / POP3 / IMAP слева
           → соответствующая секция справа
           → первое поле выбранного протокола
```

Базовая двухпанельная компоновка, размеры, captions, sidebar, toolbar, ScrollBox, аккордеоны и поля ввода не перестраиваются.

### Behavior Summary

- открытие экрана → выполняется полный `ExtSettingsWindow.init()` → данные и значения email загружаются прежним способом;
- завершение базовой инициализации → расширение сохраняет заголовок левой навигации и заменяет только три некликабельных пункта протоколов на borderless-кнопки → геометрия sidebar не меняется;
- нажатие SMTP → раскрывается `smtpSettingsSection`, POP3 и IMAP сворачиваются → фокус переходит в `smtpServer`;
- нажатие POP3 → раскрывается `pop3SettingsSection`, SMTP и IMAP сворачиваются → фокус переходит в `pop3Server`;
- нажатие IMAP → раскрывается `imapSettingsSection`, SMTP и POP3 сворачиваются → фокус переходит в `imapServer`;
- изменение выбранного протокола → активный пункт слева получает `settings-section-nav-item-active` → остальные пункты возвращаются в обычное состояние;
- перевод фокуса → вертикальный `ScrollBox` прокручивает выбранное поле в видимую область → дополнительный JavaScript не требуется;
- сохранение окна → выполняются прежние `collectEmailSettings()` и `commit()` базового контроллера → значения и бизнес-логика не меняются.

## 1. Точка вызова и контекст

| Параметр | Значение |
|---|---|
| Screen ID | `settings` |
| Регистрация | `modules/web/src/com/company/hunttech/web-screens.xml` |
| Базовый дескриптор | `/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml` |
| Расширяющий дескриптор | `/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window-email-navigation.xml` |
| Базовый контроллер | `ExtSettingsWindow` |
| Расширяющий контроллер | `ExtSettingsWindowEmailNavigation` |
| Вкладка | `mailAccessTab` |
| Левая навигация | `emailSettingsNavigation` |
| Правый контейнер | `emailSettingsContentScrollBox` / `emailSettingsAccordion` |

Расширяющий XML содержит только ссылку `extends`, новый controller class и пустой наследуемый `<layout/>`. Все визуальные компоненты загружаются из базового дескриптора.

## 2. Связь с моделью данных

Изменение не затрагивает модель данных.

Сохраняются:

- entity `UserSettings`;
- datasource `userSettingsDs`;
- поля SMTP, POP3 и IMAP;
- `setEmailSettings()`;
- `collectEmailSettings()`;
- общий `CommitContext`;
- БД, Liquibase, views и JPQL.

Расширяющий контроллер не инжектирует `DataManager`, `UserSettings`, datasource или сервисы.

## 3. Иерархия и взаимосвязь форм

```text
settings
└── ext-settings-window-email-navigation.xml
    └── extends ext-settings-window.xml
        └── mailAccessTab
            └── emailSettingsMainBox
                ├── emailSettingsSidebar
                │   └── emailSettingsNavigation
                │       ├── существующий заголовок
                │       ├── emailSettingsSmtpNav [Button]
                │       ├── emailSettingsPop3Nav [Button]
                │       └── emailSettingsImapNav [Button]
                └── emailSettingsContentScrollBox
                    └── emailSettingsAccordion
                        ├── smtpSettingsSection
                        ├── pop3SettingsSection
                        └── imapSettingsSection
```

## 4. Модель поведения и интерактивность

Выбор является взаимоисключающим:

| Действие | Раскрывается | Сворачиваются | Фокус |
|---|---|---|---|
| SMTP | `smtpSettingsSection` | POP3, IMAP | `smtpServer` |
| POP3 | `pop3SettingsSection` | SMTP, IMAP | `pop3Server` |
| IMAP | `imapSettingsSection` | SMTP, POP3 | `imapServer` |

При открытии экрана активен SMTP, что соответствует начальному состоянию базового аккордеона.

Введённые значения не сбрасываются при сворачивании секции: `GroupBoxLayout.setExpanded()` меняет только presentation state и не пересоздаёт поля.

## 5. Логика управляющих элементов

Кнопки создаются через штатный `UiComponents`:

- `emailSettingsSmtpNav`;
- `emailSettingsPop3Nav`;
- `emailSettingsImapNav`.

Общий стиль:

```text
borderless settings-section-nav-item
```

Активный стиль:

```text
borderless settings-section-nav-item settings-section-nav-item-active
```

Используются существующие message keys:

- `emailSettingsSmtpSection`;
- `emailSettingsPop3Section`;
- `emailSettingsImapSection`.

Новые message keys и изменения локализации не требуются.

## 6. Визуальная компоновка элементов

Форма визуально не перестраивается:

- сохраняется `emailSettingsNavigation`;
- сохраняется порядок SMTP → POP3 → IMAP;
- кнопки имеют ширину `100%`;
- используются существующие локальные SCSS-классы;
- новый SCSS не добавляется;
- семь тем не получают отдельных изменений;
- глобальные `.v-button`, `.v-label`, `.v-tabsheet` и другие Vaadin-селекторы не изменяются.

## 7. Соответствие CUBA Platform 7.3

1. Legacy-экран расширяется через атрибут `extends` корневого `<window>`.
2. Screen ID `settings` сохраняется.
3. Контроллер наследуется от `ExtSettingsWindow`.
4. UI-компоненты создаются через `UiComponents`.
5. Кнопки используют штатные click listeners.
6. Аккордеоны управляются через `GroupBoxLayout.setExpanded()`.
7. Фокус устанавливается штатным `TextField.focus()`.
8. Data API, lifecycle загрузки и commit базового контроллера не переопределяются.

## 8. Обязательные проверки

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.ExtSettingsWindowEmailNavigationTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидается:

- `ExtSettingsWindowEmailNavigationTest` — 3/3 PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS/N/A, поскольку getters, views и datasource не меняются;
- SCSS — N/A по diff;
- `BUILD SUCCESSFUL`;
- local deploy того же HEAD;
- HTTP `/hrm/` = 200;
- critical Tomcat errors — NONE.

### Smoke Hermes

1. Открыть `settings` и вкладку «Настройка email».
2. Подтвердить, что внешний вид и размеры формы не изменились.
3. Нажать SMTP: раскрыт только SMTP, курсор в `smtpServer`.
4. Ввести тестовое значение без сохранения.
5. Нажать POP3: раскрыт только POP3, курсор в `pop3Server`.
6. Нажать SMTP повторно: введённое значение осталось.
7. Нажать IMAP: раскрыт только IMAP, курсор в `imapServer`.
8. Проверить активную подсветку выбранного пункта слева.
9. Проверить отсутствие горизонтальной прокрутки и обрезки.
10. Сохранить допустимые значения и повторно открыть экран.
11. Убедиться, что загрузка и сохранение работают без регрессии.
12. Проверить AI-предпочтения, аватар и остальные вкладки.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-25 | Добавлена кликабельная навигация SMTP, POP3 и IMAP: выбранный пункт раскрывает соответствующий аккордеон, сворачивает остальные и фокусирует первое поле без изменения формы и бизнес-логики |

# ExtSettingsWindow — настройки пользователя

> Экран HRM HuntTech: `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`.  
> XML: `ext-settings-window.xml`.  
> Базовый класс: `com.haulmont.cuba.web.app.ui.core.settings.SettingsWindow`.  
> Локальный визуальный namespace: `.ext-settings-window`.  
> Связанные документы: [UI/UX-концепция HRM HuntTech](../architecture/HRM_HuntTech_UI_UX_Design_Concept.md), [UserSettings](../entities/user-settings/UserSettings.md), [UserAiProfile](../entities/UserAiProfile.md), [UserAiContextService](../services/UserAiContextService.md).

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`ExtSettingsWindow` объединяет персональные настройки рабочего места пользователя HRM HuntTech:

- вкладка «Обо мне» формирует профессиональный ИИ-профиль;
- вкладка «Интерфейс» определяет режим главного окна, тему, язык, часовой пояс и стартовый экран;
- вкладка «Настройка email» хранит персональные параметры SMTP, POP3 и IMAP;
- вкладка AI управляет персональными подключениями к провайдерам и предпочтением источника API.

Визуальный слой должен быть явно узнаваемой частью общего дизайн-языка HRM HuntTech. Для этого форма использует подтверждённую композицию `JobCandidateEdit`: тёмная контекстная панель слева, светлая рабочая область справа, выраженные вкладки, toolbar, карточки и поля высотой 38 px. Это не новый редизайн и не изменение информационной архитектуры: существующие вкладки, поля, действия и порядок компонентов сохраняются.

### UI Context & Navigation

Экран открывается из стандартного меню настроек CUBA Platform. В `web-screens.xml` screen ID `settings` зарегистрирован на шаблон `/com/company/hunttech/web/screens/extsettingswindow/ext-settings-window.xml`.

| Вкладка | ID | Источник данных и ответственность |
|---|---|---|
| «Обо мне» | `msgMyInfo` | `ExtUser`, `UserAiProfile`, предпросмотр ИИ-контекста |
| «Интерфейс» | `msgInterface` | компоненты и методы базового `SettingsWindow` |
| «Настройка email» | `mailAccessTab` | поля `UserSettings`, заполняемые и собираемые контроллером вручную |
| AI | `aiAccessTab` | `UserSettings.preferPersonalAiApiSettings`, `UserAiConfiguration` |

Все четыре вкладки используют общий корневой визуальный слой `.ext-settings-window`. Внутри каждой вкладки сохраняется существующая двухпанельная структура: контекст и индекс разделов слева, рабочая область с toolbar и карточками справа.

### Behavior Summary

- открытие окна → базовый `SettingsWindow` находит legacy-компоненты по ID → отображаются текущие настройки;
- изменение режима, темы, языка, часового пояса или стартового экрана → работает штатная логика CUBA → значения применяются без нового контроллерного кода;
- открытие вкладки email → `setEmailSettings()` заполняет прежние поля → datasource и порядок сохранения не меняются;
- сохранение окна → `collectEmailSettings()` читает те же `TextField` и `CheckBox` → значения записываются в `UserSettings`;
- нажатие AI-действий → выполняются существующие `invoke`-методы → визуальный слой не инициирует запросы и не меняет маршрутизацию;
- hover, focus, disabled и read-only → меняется только presentation → required, validators, permissions и editable-состояния остаются прежними;
- смена темы → используется одинаковый локальный SCSS-контракт семи тем → component ID, XML и поведение не меняются.

## 1. Технический контекст

| Параметр | Значение |
|---|---|
| Контроллер | `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow` |
| Базовый класс | `SettingsWindow` |
| XML schema | legacy `window.xsd` |
| Data API | legacy `dsContext` |
| Screen ID | `settings` |
| Корневой visual style | `ext-settings-window` |
| TabSheet style | `framed ext-settings-tabs` |
| Footer style | `ext-settings-footer` |
| Поддерживаемые темы | `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark` |

Экран остаётся legacy-экраном CUBA Platform 7.3. Корневые секции `<window>`, `<dsContext>` и `<layout>` сохранены. Java-контроллер, entity, views, JPQL, сервисы, `@Subscribe`, `@Install`, actions и API-контракты визуальной задачей не изменяются.

## 2. Связь с моделью данных

| Datasource | Entity | View | Назначение |
|---|---|---|---|
| `extUserDs` | `ExtUser` | `extUser-view` | пользователь, аватар и fallback почтовых значений |
| `userSettingsDs` | `UserSettings` | `userSettings-view` | почтовые параметры и предпочтение личного API |
| `userAiProfileDs` | `UserAiProfile` | `userAiProfile-view` | профессиональный ИИ-профиль |
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

Почтовые поля намеренно не получают datasource-binding в XML. Контроллер продолжает находить их по ID:

- `smtpServer`, `smtpPort`, `smtpPasswordRequired`, `smtpPassword`;
- `pop3Server`, `pop3Port`, `pop3PasswordRequired`, `pop3Password`;
- `imapServer`, `imapPort`, `imapPasswordRequired`, `imapPassword`.

Поля портов сохраняют `datatype="int"` и `IntegerValidator`.

## 3. Иерархия формы

```text
ext-settings-window
├── settingsTabSheet [ext-settings-tabs]
│   ├── msgMyInfo
│   │   └── userAiProfileMainBox
│   │       ├── userAiProfileSidebar (270 px)
│   │       └── userAiProfileContentScrollBox
│   ├── msgInterface
│   │   └── interfaceSettingsMainBox
│   │       ├── interfaceSettingsSidebar (270 px)
│   │       └── interfaceSettingsContentScrollBox
│   ├── mailAccessTab
│   │   └── emailSettingsMainBox
│   │       ├── emailSettingsSidebar (270 px)
│   │       └── emailSettingsContentScrollBox
│   └── aiAccessTab
│       └── aiSettingsMainBox
│           ├── aiSettingsSidebar (270 px)
│           └── aiSettingsContent
└── buttons [ext-settings-footer]
```

Ширина боковых панелей задана существующим XML и не меняется. При ширине viewport до 1366 px локальный SCSS уменьшает фактическую ширину панели до 250 px, не перестраивая XML и не перенося компоненты.

## 4. Неизменяемые функциональные контракты

### Вкладка «Интерфейс»

Сохраняются component ID:

- `grid`, `mainWindowLabel`, `modeOptions`;
- `visualThemeLabel`, `appThemeField`;
- `languageLabel`, `appLangField`;
- `timeZoneLabel`, `timeZoneBox`, `timeZoneLookup`, `timeZoneAutoField`;
- `defaultScreenLabel`, `defaultScreenField`;
- `changePasswordBtn`, `resetScreenSettingsBtn`.

`appThemeField` остаётся обязательным. Смена пароля и сброс экранных настроек используют прежние действия базового `SettingsWindow`.

### Вкладка «Настройка email»

Порядок внутри каждой карточки сохраняется:

```text
сервер → порт → требование пароля → пароль
```

SMTP, POP3 и IMAP остаются тремя равноправными карточками. Пароли не передаются в ИИ-контекст.

### Вкладка «Обо мне»

Сохраняются datasource, consent-логика, валидация опыта, предпросмотр очищенного контекста и атомарное сохранение. `previewAiContext()` не отправляет HTTP-запрос к LLM.

### Вкладка AI

Сохраняются четыре действия:

- `onAiConfigsCreateBtnClick`;
- `onAiConfigsEditBtnClick`;
- `onAiConfigsRemoveBtnClick`;
- `onAiConfigsTestBtnClick`.

Таблица продолжает использовать `userAiConfigsDs`; endpoint и HTTP-логика провайдера не переносятся в экран.

## 5. Визуальный контракт

### 5.1. Корневой слой

Все правила ограничены:

```scss
.ext-settings-window { ... }
```

Запрещено подключать `.job-candidate-editor` как зависимость, изменять глобальные `.v-table`, `.v-label`, `.v-button`, `.v-tabsheet`, `.v-textfield` и влиять на другие экраны.

### 5.2. Тёмная контекстная панель

Боковые панели всех четырёх вкладок используют тот же визуальный язык, что и `JobCandidateEdit`:

- фон `#172638`;
- градиент `#172638 → #132130 → #0f1b28`;
- текст `#f8fafc`;
- акцент заголовка и активной навигации `#ffb11b`;
- правая граница `rgba(15, 23, 42, 0.78)`;
- тень `5px 0 20px rgba(15, 23, 42, 0.18)`;
- ширина 270 px, при viewport до 1366 px — 250 px;
- подсказки и служебные блоки оформляются полупрозрачными карточками внутри тёмной панели.

Это ключевое отличие от предыдущей реализации, где sidebar использовал обычный `$v-panel-background-color` и визуально почти не отличался от исходной формы.

### 5.3. Вкладки

`TabSheet` сохраняет штатное поведение CUBA, но получает геометрию `JobCandidateEdit`:

- высота строки вкладок — 48 px;
- размер подписи — 15 px;
- активная вкладка — цвет `$v-selection-color` и нижняя граница 3 px;
- рабочая область начинается после разделителя и имеет самостоятельный фон;
- hover не меняет размеры элементов.

### 5.4. Рабочая область, toolbar и карточки

- фон рабочей области вычисляется из `$v-app-background-color` и `$v-panel-background-color`;
- toolbar имеет минимальную высоту 58 px, внутренние отступы и локальную тень;
- карточки имеют радиус 8 px, рамку, внутренний отступ 20–22 px и тень `0 2px 8px`;
- заголовки карточек — 18 px, насыщенность 700;
- заголовки toolbar — 20 px, насыщенность 700;
- email-карточки получают верхнюю акцентную границу 3 px.

### 5.5. Поля и состояния

- `TextField`, `LookupField`, `DateField` — минимальная высота 38 px;
- основной размер текста полей — 15 px;
- `TextArea` сохраняет заданную XML-высоту и получает line-height 1.45;
- локальный focus использует `$v-selection-color` и контур 2 px;
- read-only отличается фоном, disabled — непрозрачностью 0.55;
- captions — 13 px, насыщенность 600;
- кнопки — минимальная высота 38 px, радиус 5 px;
- footer — отдельная панель высотой не менее 62 px.

### 5.6. Темы

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

## 6. Соответствие CUBA Platform 7.3

1. Экран остаётся XML-экраном `window.xsd`.
2. Вкладки остаются дочерними компонентами `TabSheet`.
3. Data API остаётся `dsContext`.
4. Legacy component ID не переименовываются.
5. `datasource`, `property`, `required`, validators, actions и `invoke` не меняются.
6. Визуальный слой подключается только через `stylename` и theme extension.
7. SCSS не инициирует загрузку данных и не вмешивается в lifecycle.
8. Глобальные Vaadin-селекторы не добавляются.

## 7. Обязательные проверки

Hermes проверяет точный HEAD ветки без изменения кода:

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.UserSettingsAiApiPreferenceTest' \
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

- `UserSettingsAiApiPreferenceTest` — 10/10 PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- Data View Integrity — PASS;
- SCSS — PASS;
- `BUILD SUCCESSFUL`;
- локальный deploy того же HEAD;
- HTTP `/hrm/` = 200;
- отсутствие новых критических ошибок в Tomcat logs.

### Визуальный smoke

Во всех семи темах проверить:

- наличие тёмной sidebar на каждой вкладке;
- акцент `#ffb11b` для заголовка и активного пункта;
- высоту вкладок 48 px;
- toolbar 58 px и карточки с выраженной границей;
- поля высотой 38 px;
- отсутствие обрезки и горизонтальной прокрутки при ширине 1200 px;
- focus, hover, read-only, disabled и validation states;
- сохранение и повторное открытие интерфейсных, email и AI-настроек;
- отсутствие влияния на `JobCandidateEdit` и другие формы;
- наличие `.ext-settings-window` и `#172638` в развёрнутом `VAADIN/themes/*/styles.css`;
- hard reload браузера с отключённым cache перед итоговым сравнением.

До отчёта Hermes по точному SHA статус задачи — `WAITING_FOR_HERMES`.

## 8. История изменений

| Дата | Изменение |
|---|---|
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

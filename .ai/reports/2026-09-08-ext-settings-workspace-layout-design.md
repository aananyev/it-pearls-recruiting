# Спецификация UI/UX-дизайна и компоновки экрана ExtSettingsWindow (TabSheet Workspace)

**Дата**: 08.09.2026  
**Субагенты**: Аналитик, UI/UX-дизайнер, Frontend-разработчик  
**Стандарт**: `hunttech-edit-screen-design` (CUBA Platform 7.3 / Vaadin)  
**Объект**: Экран пользовательских настроек `ExtSettingsWindow` (`ext-settings-window.xml`)

---

## 1. Концепция и цели рефакторинга
Экран настроек пользователя `ExtSettingsWindow` построен по архитектуре Two-Pane Split View:
- **Левый сайдбар (312px)**: постоянная контекстная панель пользователя (аватар 120px, 4-уровневая типографика, сводка статуса ИИ-профиля, переключатели вкладок `label-navigation`).
- **Правая рабочая область (Workspace)**: `settingsWorkspaceBox` с полновысотным `settingsTabSheet`, объединяющим 5 ключевых функциональных разделов:
  1. `msgMyInfo` («Мой профиль и ИИ»);
  2. `msgInterface` («Интерфейс»);
  3. `mailAccessTab` («Почта»);
  4. `aiAccessTab` («ИИ Настройки»);
  5. `geoApiAccessTab` («Geo API»).

### Ключевые проблемы предыдущей реализации:
1. **Неоднородность сеток**: на вкладке «Интерфейс» использовался жесткий `<grid>` с фиксированной колонкой подписей `width="220px"`, не помещающийся на узких экранах.
2. **Размытый вертикальный ритм в почтовых настройках**: на вкладке «Почта» поля SMTP/POP3/IMAP шли сплошным вертикальным списком без логической 2-колоночной группировки (сервер + порт, флаг пароля + пароль).
3. **Отсутствие адаптивности**: при изменении ширины браузера от 1280px до 1920px элементы не масштабировались пропорционально, а на разрешениях ниже 1100px возникали горизонтальные полосы прокрутки.

---

## 2. Архитектурный стандарт компоновки вкладок

Каждая вкладка TabSheet следует строгому шаблону:
1. `scrollBox` (`width="100%" height="100%" orientation="vertical" scrollBars="vertical" stylename="edit-workspace-scroll"`).
2. Контейнер контента `vbox` (`width="100%" spacing="true" stylename="edit-workspace-content"`).
3. Тулбар секции `hbox` (`width="100%" spacing="true" stylename="edit-toolbar"`) с заголовком (`h1 edit-toolbar-title`), описанием (`edit-toolbar-description edit-help`) и зоной действий (`edit-toolbar-actions`).
4. Аккордеон-секции `groupBox` (`stylename="edit-accordion-section" showAsPanel="true" collapsable="true"`).
5. Строки полей по стандарту сетки:
   - Двухколоночный ряд 50/50: `<hbox width="100%" spacing="true" stylename="edit-field-row edit-row-half">`
   - Ряд 70/30 (сервер / порт): `<hbox width="100%" spacing="true" stylename="edit-field-row edit-row-server-port">`
   - Ряд на всю ширину: `<hbox width="100%" spacing="true" stylename="edit-field-row edit-row-wide">`
6. Единый стиль полей ввода: `stylename="edit-form-control"`.

---

## 3. Детализация компоновки по 5 вкладкам

### 3.1. Вкладка 1: «Мой профиль и ИИ» (`msgMyInfo`)
- **Тулбар**: Заголовок «Персональный профиль и контекст ИИ», пояснение, кнопки «Предпросмотр контекста ИИ» (`previewAiContextBtn`) и «Очистить профиль» (`clearAiProfileBtn`).
- **Секция 1: Профессиональный профиль** (`professionalProfileGroup`):
  - Ряд 1 (50/50): `currentPositionField` + `functionalRoleField`.
  - Ряд 2 (50/50): `seniorityLevelField` + `professionalExperienceYearsField`.
  - Ряд 3 (50/50): `recruitingExperienceYearsField` + пустое пространство.
  - Ряд 4 (100%): `aboutMeField` (3 строки).
  - Ряд 5 (50/50): `currentResponsibilitiesField` + `educationField`.
  - Ряд 6 (50/50): `certificationsField` + `domainExpertiseField`.
  - Ряд 7 (100%): `industriesField` (2 строки).
- **Секция 2: Профиль рекрутмента** (`recruitingProfileGroup`):
  - Ряд 1 (50/50): `recruitingSpecializationsField` + `targetRolesField`.
  - Ряд 2 (50/50): `candidateLevelsField` + `hiringGeographiesField`.
  - Ряд 3 (50/50): `decisionPrioritiesField` + `clientAndProjectContextField`.
- **Секция 3: Предпочтения по ответам ИИ** (`responsePreferencesGroup`):
  - Ряд 1 (50/50): `preferredLanguageField` + `responseDetailLevelField`.
  - Ряд 2 (50/50): `communicationStyleField` + `terminologyLevelField`.
  - Ряд 3 (100%): `preferredAnswerStructureField`.
  - Ряд 4 (50/50): `customAiInstructionsField` + `communicationConstraintsField`.
- **Секция 4: Цели и профессиональные интересы** (`goalsGroup`):
  - Ряд 1 (50/50): `professionalGoalsField` + `professionalInterestsField`.
  - Ряд 2 (50/50): `developmentAreasField` + `currentPrioritiesField`.
- **Секция 5: Приватность и границы данных** (`privacyGroup`):
  - Чекбокс активации `profileEnabledField`.
  - Карточка согласия на обработку `externalProcessingAllowedField` с пояснением.
  - Карточка fallback на админ-API `adminFallbackConsentField` с пояснением.
  - Метка даты принятия согласий `consentAcceptedAtLabel`.
- **Секция 6: Предпросмотр контекста ИИ** (`previewGroup`):
  - Текстовая область `aiContextPreviewArea` (10 строк, readonly).

### 3.2. Вкладка 2: «Интерфейс» (`msgInterface`)
- **Тулбар**: Заголовок «Настройки интерфейса и отображения», кнопки «Сменить пароль» (`changePasswordBtn`) и «Сброс настроек экрана» (`resetScreenSettingsBtn`).
- **Секция: Внешний вид и поведение** (`interfaceAppearanceCard`):
  - Режим отображения окон `modeOptions`.
  - Ряд 1 (50/50): Тема приложения (`appThemeField`) + Язык (`appLangField`).
  - Ряд 2 (50/50): Часовой пояс (`timeZoneLookup` + чекбокс `timeZoneAutoField`) + Начальный экран (`defaultScreenField`).

### 3.3. Вкладка 3: «Почта» (`mailAccessTab`)
- **Тулбар**: Заголовок «Настройки почтовых протоколов», описание.
- **Секции SMTP, POP3, IMAP** (единая эргономичная сетка):
  - Ряд 1 (70 / 30): Сервер (`smtpServer` / `pop3Server` / `imapServer`) + Порт (`smtpPort` / `pop3Port` / `imapPort`).
  - Ряд 2 (30 / 70): Чекбокс требования пароля (`smtpPasswordRequired` / `pop3PasswordRequired` / `imapPasswordRequired`) + Пароль (`smtpPassword` / `pop3Password` / `imapPassword`).

### 3.4. Вкладка 4: «ИИ Настройки» (`aiAccessTab`)
- **Тулбар**: Заголовок «Управление подключениями к искусственному интеллекту», описание.
- **Карточка приоритетов API**:
  - Двухколоночный ряд (50/50):
    - Карточка предпочтения личного API (`preferPersonalAiApiSettingsField` + хинт).
    - Карточка предпочтения личных промптов (`preferPersonalPromptsField` + хинт).
- **Карточка персональных подключений**:
  - Таблица `aiConfigsTable` с панелью `aiConfigsButtonsPanel` (Создать, Редактировать, Удалить, Тест).

### 3.5. Вкладка 5: «Geo API» (`geoApiAccessTab`)
- **Тулбар**: Заголовок «Настройки картографических сервисов и подсказок», описание, кнопка «Тест подключения» (`testGeoApiBtn`).
- **Карточка DaData**:
  - Ряд 1 (50/50): API-ключ (`geoApiKeyField`) + Секретный ключ (`geoApiSecretField`).
  - Ряд 2 (100%): URL сервиса (`geoApiUrlField`).
- **Карточка дополнительных параметров**:
  - Чекбокс `geoAutoFetchFlagsField` с аккуратной подсказкой `edit-help`.

---

## 4. Адаптивность и поддержка 7 тем оформления
1. **Медиа-запросы адаптивности**:
   - При ширине рабочей области `>= 1100px`: полная двухколоночная сетка 50/50.
   - При ширине рабочей области `< 1100px`: элементы `.edit-row-half` и `.edit-row-server-port` автоматически трансформируются в вертикальный стек со 100% шириной колонок и отступом 10px, предотвращая горизонтальный скролл.
2. **Синхронизация SCSS**:
   - Миксин `settings-window-sections` обновляется и синхронизируется побайтово во всех 7 темах оформления: `halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`.

---

## 5. Неизменность бизнес-логики и контрактов
- Количество компонентов ввода, их `id`, свойства сущностей, `datatype`, валидаторы, события `invoke` и биндинги к `dsContext` сохранены без единого изменения.

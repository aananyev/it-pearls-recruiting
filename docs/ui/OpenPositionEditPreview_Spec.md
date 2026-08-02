# OpenPositionEditPreview — изолированный preview новой компоновки вакансии

## Назначение и бизнес-смысл (What & Why)

`OpenPositionEditPreview` — параллельный экран визуальной проверки новой компоновки карточки вакансии HRM HuntTech до замены действующего `OpenPositionEdit`.

Экран сохраняет существующую модель вакансии и полный бизнес-контракт legacy-редактора: реквизиты, команда или одиночная вакансия, проект, заказчик, локация, количество позиций, зарплата, трудовые договоры, оплаты, описания, файлы, тестовое задание, памятка интервью, шаблон письма, навыки, новости, согласование и комментарии.

К компоновке применяется обязательный общий UI API из `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`. Визуальный язык формы единообразен с другими Edit-экранами во всех семи темах CUBA Platform. Entity, loaders, JPQL, views, validation, actions, DataContext и сохранение не меняются.

Текущий этап дополнительно устраняет выявленные визуальные дефекты runtime-компоновки:

- чрезмерную высоту карточки названия вакансии в sidebar;
- уход label-навигации и сводки ниже видимой области;
- широкие пустые разрывы между связанными полями;
- обрезание правых controls из-за legacy-процентных ширин;
- перегруженную высоту tab bar;
- слабую визуальную иерархию основных и вложенных accordion-секций;
- разнесённые по ширине кнопки завершения.

Архитектура сохраняет место для будущих AI-помощников анализа вакансии и поиска кандидатов. В текущем этапе AI-компоненты и новые бизнес-сценарии отсутствуют.

## UI Context & Navigation

Экран имеет отдельные:

- Screen ID: `hunttech_OpenPosition.editPreview`;
- route: `open-position-edit-preview`;
- controller: `OpenPositionEditPreview`;
- XML: `open-position-edit-preview.xml`.

Preview не зарегистрирован в меню и `OpenPositionBrowse`, не подменяет `hunttech_OpenPosition.edit` и открывается администратором или разработчиком по прямому editor route:

```text
http://localhost:8080/hrm/#main/open-position-edit-preview?id=<encoded-open-position-id>
```

Иерархия:

```text
HrmMainScreen / прямой route
└── OpenPositionEditPreview
    ├── edit-sidebar
    │   ├── compact identity-card
    │   ├── label-navigation
    │   ├── summary / warning / owner
    │   └── sidebar spacer
    └── edit-workspace
        ├── edit-toolbar
        ├── compact edit-tabs
        │   └── edit-workspace-scroll / edit-workspace-content
        │       ├── primary accordion sections
        │       ├── nested subsection
        │       └── responsive field rows
        └── edit-footer-actions
```

## Behavior Summary

- открытие preview по route → CUBA восстанавливает editor entity → preview при необходимости перезагружает существующую `OpenPosition` с полным edit-view и загруженным `positionType` → выполняется штатный `OpenPositionEdit.onBeforeShow`;
- создание XML-компонентов → preview назначает только общие `edit-*`, `label-*` и локальные `open-position-preview-*` stylename → bindings, validators, editable и required не меняются;
- применение layout-polish → существующие HBox-компоненты получают responsive-роли → поля не переставляются между бизнес-разделами и не получают новые значения;
- длинное название вакансии → визуально ограничивается по высоте → полный текст остаётся доступным в tooltip;
- пустой `vacancyTitleSpacerHBox` → скрывается как чисто визуальный legacy-spacer → события и данные не меняются;
- пункт label-навигации → выбирается существующая вкладка `tabSheetOpenPosition` → lazy-loading продолжает выполнять базовый контроллер;
- изменение активной вкладки → сохраняется `label-nav-item`, добавляется или удаляется только `label-nav-item-active`;
- раскрытие GroupBox → меняется только presentation-state → значения и DataContext сохраняются;
- изменение `commandCandidate` → legacy-контроллер управляет вкладкой «Оплаты» → preview синхронизирует только соответствующий navigation-пункт;
- смена темы → сохраняются геометрия, dark-sidebar и responsive-сетка → рабочая область и controls адаптируются через theme variables;
- сохранение и закрытие → выполняется `windowCommitAndClose` с прежними проверками и сервисами;
- отмена → выполняется `windowClose` без сохранения;
- открытие legacy-экрана → используются прежние Java/XML и прежний маршрут → preview не участвует.

## 1. Контракт данных и business logic

Preview использует те же data components, views, loader ID и JPQL, что и `open-position-edit.xml`:

- `openPositionDc` / `openPositionDl`;
- `laborAgreementDc` / `laborAgreementDl`;
- `commentsOpenPositionDc` / `commentsOpenPositionDl`;
- `someFilesesDc` / `someFilesesDl`;
- `openPositionSkillsListsDc` / `openPositionSkillsListsDl`;
- `procAttachmentsDc` / `procAttachmentsDl`;
- `openPositionParentDc` / `openPositionParentDl`;
- `positionTypesDc` / `positionTypesLc`;
- `openPositionNewsDc` / `openPositionNewsLc`;
- `projectNamesDc` / `projectNamesLc`;
- `companyNamesDc` / `companyNamesLc`;
- `companyDepartamentsDc` / `companyDepartamentsLc`;
- `citiesDc` / `citiesDl`;
- `gradeDc` / `gradeDl`;
- `closedVacancyTimer`.

Все существующие `dataContainer`, `property`, `optionsContainer`, `required`, action ID, `invoke`, validators, captions и component ID сохраняются.

`OpenPositionEdit.java`, `open-position-edit.xml`, browse-экраны, меню, entities, services, `views.xml`, существующий JPQL и Liquibase не изменяются.

## 2. Защита прямого URL route

Прямой route может восстановить detached `OpenPosition` с неинициализированной lazy-связью `positionType`. До базового lifecycle preview проверяет `PersistenceHelper.isLoaded`.

Для существующей detached-сущности выполняется загрузка `OpenPosition` через `DataManager` с полным набором edit-полей и загруженными:

- `positionType.positionRuName`;
- `positionType.positionEnName`;
- `positionType.standartDescription`;
- `positionType.whoIsThisGuy`.

После загрузки контейнер получает целиком подготовленную сущность через `getEditedEntityContainer().setItem(...)`, затем вызывается `super.onBeforeShow(event)`. Это исключает обращение к EclipseLink value holder с `null Session` и не меняет бизнес-значение `positionType`.

## 3. Вкладки

Сохраняются порядок и ID 12 вкладок:

1. `tabOpenPosition`;
2. `laborAgreementTab`;
3. `tabPayments`;
4. `tabJobDescription`;
5. `tabFiles`;
6. `tabExercise`;
7. `tabMemoForInterview`;
8. `tabTemplateLetter`;
9. `tabSkills`;
10. `tabOpenPositionNews`;
11. `tabApproval`;
12. `commentsTab`.

`tabPayments` остаётся скрытой для одиночной вакансии и показывается существующей логикой для карточки команды.

Tab bar остаётся частью `TabSheet`: вкладки не заменяются собственным роутером и не удаляются. Локальный CSS уменьшает высоту до 40 px, ограничивает ширину caption и использует штатные overflow-кнопки CUBA, когда все 12 вкладок не помещаются.

## 4. Общий UI API Edit-экранов

### 4.1. Sidebar

Используются:

- `edit-sidebar`;
- `edit-sidebar-visual`;
- `edit-sidebar-identity`;
- `edit-sidebar-title`;
- `edit-sidebar-subtitle`;
- `edit-sidebar-summary`;
- `edit-sidebar-hint`;
- `edit-sidebar-warning`;
- `edit-sidebar-spacer`.

Ширина sidebar соответствует общему контракту:

- базовая — `270px`;
- viewport до `1366px` — `250px` через shared SCSS.

Локальный partial оформляет sidebar в фирменной палитре HRM HuntTech:

```text
#172638 → #132130 → #0f1b28
```

Runtime-polish:

- основной логотип — `112 × 112px`, до `1366px` — `92 × 92px`;
- изображение владельца — `48 × 48px`;
- название — 15 px / 20 px, максимум семь визуальных строк;
- полный текст названия сохраняется в `description` компонента;
- навигация использует компактный вертикальный ритм;
- summary и warning используют уменьшенную типографику, но сохраняют читаемость.

### 4.2. Label-навигация

Используются только утверждённые классы:

- `label-navigation`;
- `label-nav-title`;
- `label-nav-item`;
- `label-nav-item-active`.

Базовый `label-nav-item` остаётся на каждой кнопке постоянно. Контроллер добавляет или удаляет только `label-nav-item-active`, поэтому active-state не меняет размеры и положение соседних пунктов.

Hover: белый текст на `rgba(255,255,255,.08)`. Active-state: `#ffb11b`, фон `rgba(255,177,27,.12)` и жёлтая левая граница.

Навигация переключает только существующие вкладки и не запускает собственные loaders, service calls или commit.

### 4.3. Workspace, toolbar, tabs и footer

Используются:

- `edit-screen-layout`;
- `edit-workspace`;
- `edit-workspace-scroll`;
- `edit-workspace-content`;
- `edit-toolbar`;
- `edit-toolbar-title`;
- `edit-toolbar-description`;
- `edit-tabs`;
- `edit-footer-actions`.

Workspace ограничивает горизонтальное переполнение. Содержимое вкладки имеет внутренние отступы и максимальную рабочую ширину `1480px`, центрированную внутри доступной области.

Footer получает локальный flex-контракт только внутри `.open-position-preview`: штатные `windowCommitAndClose` и `windowClose` группируются справа и не растягиваются по ширине. Action ID, порядок вызовов и обработчики остаются прежними.

### 4.4. Accordion и поля

Каждый фактический `GroupBoxLayout` рабочей области:

- сохраняет исходный ID, caption, `collapsable`, `collapsed`, `width`, `height` и `expand`;
- получает `edit-accordion-section`;
- отображается как panel через `showAsPanel=true`;
- больше не использует роли `light` и `edit-card` как замену accordion-контракту.

Основные секции вкладки «Основное» получают `open-position-preview-primary-section`. Вложенный `commandFieldHBox` получает `open-position-preview-subsection`, что создаёт вторичный уровень иерархии без вложения новой бизнес-структуры.

Каждому типовому полю назначается `edit-form-control` непосредственно на компонент:

- `TextField`;
- `TextArea`;
- `LookupField`;
- `LookupPickerField`;
- `SuggestionPickerField`;
- `DateField`;
- `RichTextArea`.

`CheckBox`, `RadioButtonGroup`, таблицы и action-кнопки не получают этот класс механически. Required, read-only, disabled, validation и picker-actions сохраняются.

### 4.5. Responsive-компоновка существующих HBox

Новые UI-компоненты и новые поля не создаются. Локальные роли назначаются существующим строкам:

| Component ID | Layout role |
|---|---|
| `vacancyNameHBox` | `open-position-preview-row-title` |
| `hboxProject1` | `open-position-preview-row-three` |
| `hboxVacansy` | `open-position-preview-row-position` |
| `hboxProject` | `open-position-preview-row-half` |
| `hboxCompany` | `open-position-preview-row-half` |
| `hboxSalary` | `open-position-preview-row-salary` |
| `space2Box` | `open-position-preview-row-wide` |

Общие правила:

- slot получает `min-width: 0`;
- дочерний control остаётся `width: 100%`;
- inline-процентные ширины legacy XML локально ограничиваются flex-basis;
- при ширине до `1100px` строка переходит в две колонки;
- при ширине до `820px` — в одну колонку;
- action-кнопки не перекрывают picker и text controls;
- checkbox переносится вместе с label и не выходит за карточку.

### 4.6. Порядок CSS-слоёв

Для каждой темы используется порядок:

```text
theme base
→ edit-screen-shared-styles
→ open-position-preview
```

Shared mixin задаёт общую геометрию и типовые роли. Screen-specific partial содержит только:

- фирменную sidebar;
- цветовую адаптацию label-навигации;
- compact tab bar;
- responsive-сетку существующих HBox;
- безопасное containment таблиц, grid/form layout;
- внутренний padding accordion-content;
- локальную группировку footer actions.

Неограниченные `.v-label`, `.v-button`, `.v-table`, `.v-panel` и другие глобальные селекторы отсутствуют.

## 5. Поддержка семи тем

Идентичный локальный partial расположен в:

```text
modules/web/themes/<theme>/com.company.hunttech/open-position-preview.scss
```

Он подключён после `edit-screen-shared-styles` в темах:

- `halo`;
- `havana`;
- `helium`;
- `hover`;
- `hunttech-modern`;
- `hunttech-modern-light`;
- `hunttech-modern-dark`.

Семь физических копий требуются из-за изолированной компиляции CUBA 7.3 и проверяются на идентичность. Геометрия и фирменная sidebar стабильны; фон workspace, panel, текст, borders и selection адаптируются через переменные темы.

Общий `edit-screen-shared-styles.scss` и локальные SCSS других экранов не изменяются.

## 6. Progressive loading

Источником истины остаётся `OpenPositionEdit.onTabSheetOpenPositionSelectedTabChange()`:

- `comment` и `commentEn`;
- `exercise`;
- `memoForInterview`;
- `templateLetter`;
- навыки;
- файлы;
- комментарии;
- трудовые договоры;
- BPM attachments и новости.

Presentation-стили не запускают loaders и не меняют флаги `*Loaded`.

## 7. Ограничения этапа

Запрещены и отсутствуют:

- изменение legacy `OpenPositionEdit` и его XML;
- изменение вызовов legacy editor;
- menu item или browse action для preview;
- изменение сущностей, БД и Liquibase;
- изменение `views.xml`, сервисов и JPQL;
- изменение shared SCSS и SCSS других экранов;
- перемещение полей между вкладками или смысловыми блоками;
- изменение captions, component ID, `dataContainer`, `property`, actions и `invoke`;
- AI-анализ и поиск кандидатов;
- production deploy;
- merge без прямой команды Алексея.

## 8. Проверки Hermes

Hermes проверяет точный HEAD PR:

1. `git diff --check`;
2. разрешённый diff;
3. compile web и test source;
4. `OpenPositionEditPreviewLayoutTest`;
5. `OpenPositionEditPreviewRouteGuardTest`;
6. `OpenPositionEditPreviewSharedStyleContractTest` — 6/6 PASS;
7. `ScreenViewIntegrityTest` — 8/8 PASS;
8. идентичность shared и local SCSS семи тем;
9. порядок import/include после shared mixin;
10. `buildScssThemes` — PASS;
11. `clean assemble` — `BUILD SUCCESSFUL`;
12. local deploy и HTTP `/hrm/` = 200;
13. открытие preview по route без detached/lazy/RPC ошибок;
14. visual smoke всех 12 вкладок в семи темах;
15. visual smoke viewport 1920×1080, 1600×900, 1366×768, 1280×800;
16. sidebar: компактный title, доступные navigation и summary;
17. main tab: отсутствие широких пустых разрывов и обрезанных правых controls;
18. tabs: высота 40 px, overflow-кнопки работают;
19. footer: actions сгруппированы справа;
20. сохранение, отмена и повторное открытие;
21. legacy editor и его вызовы не изменены.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-02 | Родительские box-контейнеры вкладок растянуты на всю ширину: в `projectLocationAccordion` (вкладка «Основное») двухколоночная legacy-раскладка перестроена в одноколоночную — vbox «Проект» 50%→100% с переносом `companyDepartamentField` внутрь (50%→100%), hbox «Компания/Город» 50%→100% с `expand="cityOpenPositionField"`; добавлены смысловые inline-комментарии. Остальные контейнеры вкладок уже были 100%; вне зоны: sidebar (312px), 3-колонка выплат (33% ×3), пустой спейсер `vacancyTitleSpacerHBox`. |
| 2026-08-02 | Логотип sidebar поднят по паттерну JobCandidateEdit: `openPositionPreviewLogoBox` выравнивание `MIDDLE_CENTER` → `TOP_CENTER` (прижат к верху identity-карточки, как фото в `job-candidate-profile-header`); убрана фикс. высота бокса (118px / media 98px) — высота определяется логотипом, наезд на название исключён (7 тем, sha256 `6ee6049c`). |
| 2026-08-02 | Всем 7 RichTextArea гарантированы `width="100%" height="100%"`; 4 редактора вкладки «Описание вакансии» (`openPositionRichTextArea`, `openPositionEnRichTextArea`, `openPositionStandartDescriptionRichTextArea`, `openPositionWhoIsThisGuyRichTextArea`) обёрнуты в vbox `width="100%" height="100%"` внутри табов аккордеона — родительские контейнеры растягиваются на всю вкладку (exercise/memo/template уже имели 100%-родителей). |
| 2026-08-02 | View `someFilesOpenPosition-edit-view`: `fileDescriptor` расширен с `_minimal` до nested `name`+`size` — устранён `IllegalStateException: Cannot get unfetched attribute [size]` при рендере колонки `fileDescriptor.size` вкладки «Файлы» (preview и legacy). |
| 2026-08-02 | Исправлен ClassCastException String→Position при открытии preview: summary-лейблы (`summaryVacansyIDLabel` и др.) освобождены от XML-биндинга `dataContainer`/`property` — значения задаёт только `refreshSummary()` (иначе `setValue(String)` писался в valueSource и кастовался в FK-свойство `grade`/`positionType`). |
| 2026-08-02 | Modern UX polish (по концепции HRM_HuntTech_UI_UX_Design_Concept, только presentation): радиусы карточек унифицированы 8px; фирменная акцентная кромка identity-карточки (rgba #ffb11b); лёгкие тени уровня карточки; плавные переходы 0.15s для навигации/кнопок/полей/заголовков аккордеонов; focus-контуры 2px `$v-selection-color` для полей и кнопок (доступность); hover-фон заголовков аккордеонов; тонкие theme-aware scrollbar рабочей области; нижний акцент toolbar. Применено во всех 7 темах (sha256 `839a64d3`). |
| 2026-08-02 | Tabsheet preview приведён точно к стилям JobCandidateEdit: класс `tabSheetOpenPosition` → `framed job-candidate-tabs edit-tabs` (как `job-candidate-edit.xml`; убран `compact-tabbar`); локальные переопределения `.edit-tabs` (40px, max-width 190px) в `open-position-preview.scss` заменены эталонным блоком `.job-candidate-tabs` из `job-candidate-editor.scss` (48px, max-width 112px, цвета #26384c/#0b63b6, hover #1264b5, content `calc(100% - 49px)` + #f5f7fa + padding 16/18/20, tabcontainer padding 0 12px + #dfe5ec) и media-правилами (96px, padding 10px, font-size 14px) во всех 7 темах (sha256 идентичны `472bac49`). |
| 2026-08-02 | Блок «Требуемые Навыки» восстановлен по структуре legacy-экрана: убрана аккордеон-обёртка `skillsTableAccordion`, `skillsBox` получил `expand="openPositionSkillsListTable"`, кнопке `rescanSkills` возвращён `align="BOTTOM_LEFT"`; PreviewLayoutTest обновлён (проверка плоской структуры вместо вложенности в groupBox). |
| 2026-08-02 | RichTextArea-область вкладки «Описание вакансии» растянута на оставшуюся часть экрана: vbox вкладки получил `height="100%"` и `expand="descriptionTextsAccordion"`, groupBox `descriptionTextsAccordion` — `height="100%"`, accordion `openPositionAccordion` — `height="100%"` (было фиксированное 360px). Остальные вкладки (Тестовое задание, Памятка, Шаблон) уже были растянуты. |
| 2026-08-02 | В сводку «Ключевые параметры» добавлена строка «Оформление» (`summaryRegistrationForWorkLabel`): тип оформления кандидата из кода `registrationForWork` через `StandartRegistrationForWork` (Аутстаффинг / В штат заказчику / Все варианты), обновляется в `refreshSummary()` вместе с остальной сводкой. |
| 2026-08-02 | Вкладка `tabOpenPosition` переименована «Проект» → «Основное» (новый ключ `msgPositionMainTab`; legacy-экран сохраняет «Проект»); сводка «Ключевые параметры» в sidebar получает id и обновляется программно (`refreshSummary()` при AfterShow и `ItemPropertyChangeEvent` контейнера `openPositionDc`) — ID/Должность/Грейд/Проект/Город/Позиции всегда совпадают с данными вкладки; блок логотипа `openPositionPreviewLogoBox` получил фиксированную высоту (118/98px) — логотип больше не наезжает на наименование вакансии. |
| 2026-08-02 | Улучшена runtime-компоновка: компактная identity-card, ограничение длинного названия с tooltip, responsive-сетка существующих HBox, компактные tabs, усиленная иерархия accordion и сгруппированный footer; бизнес-структура и XML-контракт не изменены. |
| 2026-08-02 | Добавлен локальный `open-position-preview.scss` во все семь тем: фирменная dark-sidebar, жёлтый active-state, theme-aware workspace, panel-accordion containment и footer; partial подключён строго после shared mixin. |
| 2026-08-02 | К preview применён общий `edit-*` / `label-*` UI API: sidebar 270/250 px, shared toolbar/tabs/scroll, panel-accordion, типовые `edit-form-control`, общий footer без изменения бизнес-логики. |
| 2026-08-01 | Защита URL lifecycle переведена на загрузку полного edit-набора и замену item контейнера до legacy `onBeforeShow`. |
| 2026-08-01 | Создан изолированный preview новой двухпанельной компоновки без замены legacy-экрана и изменения бизнес-логики. |

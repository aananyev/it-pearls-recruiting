# OpenPositionEditPreview — изолированный preview новой компоновки вакансии

## Назначение и бизнес-смысл (What & Why)

`OpenPositionEditPreview` — параллельный экран визуальной проверки новой компоновки карточки вакансии HRM HuntTech до замены действующего `OpenPositionEdit`.

Экран сохраняет существующую модель вакансии и полный бизнес-контракт legacy-редактора: реквизиты, команда или одиночная вакансия, проект, заказчик, локация, количество позиций, зарплата, трудовые договоры, оплаты, описания, файлы, тестовое задание, памятка интервью, шаблон письма, навыки, новости, согласование и комментарии.

К уже утверждённой компоновке применяется обязательный общий UI API из `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`. Визуальный язык формы становится единым с другими Edit-экранами во всех семи темах CUBA Platform, при этом entity, loaders, JPQL, views, validation, actions и сохранение не меняются.

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
    │   ├── edit-sidebar-visual
    │   ├── edit-sidebar-identity
    │   ├── edit-sidebar-summary
    │   ├── label-navigation
    │   ├── edit-sidebar-hint / edit-sidebar-warning
    │   └── edit-sidebar-spacer
    └── edit-workspace
        ├── edit-toolbar
        ├── edit-tabs
        │   └── edit-workspace-scroll / edit-workspace-content
        │       ├── edit-accordion-section
        │       └── edit-form-control
        └── edit-footer-actions
```

## Behavior Summary

- открытие preview по route → CUBA восстанавливает editor entity → preview при необходимости перезагружает существующую `OpenPosition` с полным edit-view и загруженным `positionType` → выполняется штатный `OpenPositionEdit.onBeforeShow`;
- создание XML-компонентов → preview назначает только общие `edit-*` и `label-*` stylename → bindings, validators, editable и required не меняются;
- пункт label-навигации → выбирается существующая вкладка `tabSheetOpenPosition` → lazy-loading продолжает выполнять базовый контроллер;
- изменение активной вкладки → сохраняется `label-nav-item`, добавляется или удаляется только `label-nav-item-active`;
- раскрытие GroupBox → меняется только presentation-state → значения и DataContext сохраняются;
- изменение `commandCandidate` → legacy-контроллер управляет вкладкой «Оплаты» → preview синхронизирует только соответствующий navigation-пункт;
- смена темы → сохраняются геометрия, dark-sidebar и active-state → рабочая область и controls адаптируются через theme variables;
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
- `companyDepartamentsDc` / `companyDepartamentsDl`;
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

Название вакансии и active-state используют акцент `#ffb11b`. Контекстные карточки, предупреждение, владелец и подписка имеют локальные правила только внутри `.open-position-preview`.

### 4.2. Label-навигация

Используются только утверждённые классы:

- `label-navigation`;
- `label-nav-title`;
- `label-nav-item`;
- `label-nav-item-active`.

Базовый `label-nav-item` остаётся на каждой кнопке постоянно. Контроллер добавляет или удаляет только `label-nav-item-active`, поэтому active-state не меняет размеры и положение соседних пунктов.

Hover соответствует эталону: белый текст на `rgba(255,255,255,.08)`. Active-state: `#ffb11b`, фон `rgba(255,177,27,.12)` и жёлтая левая граница.

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

Toolbar получает общую высоту и theme-aware поверхность. TabSheet сохраняет исходные вкладки, captions и icons. Нижние `windowCommitAndClose` и `windowClose` остаются штатными actions и получают только общий footer-style.

### 4.4. Accordion и поля

Каждый фактический `GroupBoxLayout` рабочей области:

- сохраняет исходный ID, caption, `collapsable`, `collapsed`, `width`, `height` и `expand`;
- получает `edit-accordion-section`;
- отображается как panel через `showAsPanel=true`;
- больше не использует роли `light` и `edit-card` как замену accordion-контракту.

Каждому типовому полю назначается `edit-form-control` непосредственно на компонент:

- `TextField`;
- `TextArea`;
- `LookupField`;
- `LookupPickerField`;
- `SuggestionPickerField`;
- `DateField`;
- `RichTextArea`.

`CheckBox`, `RadioButtonGroup`, таблицы и action-кнопки не получают этот класс механически. Required, read-only, disabled, validation и picker-actions сохраняются.

### 4.5. Порядок CSS-слоёв

Для каждой темы используется порядок:

```text
theme base
→ edit-screen-shared-styles
→ open-position-preview
```

Shared mixin задаёт общую геометрию и типовые роли. Screen-specific partial содержит только:

- фирменную sidebar;
- цветовую адаптацию label-навигации;
- безопасное containment таблиц, grid/form layout;
- внутренний padding accordion-content;
- локальный footer shadow.

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
- копирование CSS `SettingsWindow`, `JobCandidateEdit` или `IteractionListEdit` без локального namespace;
- AI-анализ и поиск кандидатов;
- production deploy;
- merge без прямой команды Алексея.

## 8. Проверки Hermes

Hermes проверяет точный HEAD PR:

1. `git diff --check`;
2. compile web и test source;
3. `OpenPositionEditPreviewLayoutTest`;
4. `OpenPositionEditPreviewRouteGuardTest`;
5. `OpenPositionEditPreviewSharedStyleContractTest`;
6. `ScreenViewIntegrityTest` — 8/8 PASS;
7. идентичность shared и local SCSS семи тем;
8. порядок import/include после shared mixin;
9. `buildScssThemes` — PASS;
10. `clean assemble` — `BUILD SUCCESSFUL`;
11. local deploy и HTTP `/hrm/` = 200;
12. открытие preview по route без detached/lazy/RPC ошибок;
13. visual smoke всех 12 вкладок в семи темах;
14. sidebar 270/250 px, отсутствие горизонтального overflow;
15. toolbar, tabs, accordion, поля и footer соответствуют shared-контракту;
16. сохранение, отмена и повторное открытие;
17. legacy editor и его вызовы не изменены.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-02 | Добавлен локальный `open-position-preview.scss` во все семь тем: фирменная dark-sidebar, жёлтый active-state, theme-aware workspace, panel-accordion containment и footer; partial подключён строго после shared mixin. |
| 2026-08-02 | К preview применён общий `edit-*` / `label-*` UI API: sidebar 270/250 px, shared toolbar/tabs/scroll, panel-accordion, типовые `edit-form-control`, общий footer без изменения бизнес-логики. |
| 2026-08-01 | Защита URL lifecycle переведена на загрузку полного edit-набора и замену item контейнера до legacy `onBeforeShow`. |
| 2026-08-01 | Создан изолированный preview новой двухпанельной компоновки без замены legacy-экрана и изменения бизнес-логики. |

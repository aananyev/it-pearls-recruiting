# OpenPositionEditPreview — изолированный preview новой компоновки вакансии

## Назначение и бизнес-смысл (What & Why)

`OpenPositionEditPreview` — параллельный экран визуальной проверки новой компоновки карточки вакансии HRM HuntTech до замены действующего `OpenPositionEdit`. Текущая форма реализует утверждённый рендер в стиле `JobCandidateEdit`: профиль вакансии расположен в sidebar шириной 312px, а основная работа выполняется в едином workspace с общей навигацией, accordion-секциями и постоянным footer. Legacy `OpenPositionEdit` не подменяется preview-экраном.

Экран сохраняет существующую модель вакансии и полный бизнес-контракт legacy-редактора: реквизиты, команда или одиночная вакансия, проект, заказчик, локация, количество позиций, зарплата, трудовые договоры, оплаты, описания, файлы, тестовое задание, памятка интервью, шаблон письма, навыки, новости, согласование и комментарии.

К компоновке применяется обязательный общий UI API из `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`. Визуальный язык формы единообразен с другими Edit-экранами во всех семи темах CUBA Platform. Entity, loaders, JPQL, views, validation, actions, DataContext и сохранение не меняются.

Текущий этап дополнительно устраняет выявленные визуальные дефекты runtime-компоновки:

- наезд `OvaFallbackImage` на название вакансии при пересчёте высоты Vaadin-layout;
- чрезмерную высоту карточки названия вакансии в sidebar;
- невидимые или обрезанные значения и большие вертикальные интервалы в summary GridLayout;
- уход label-навигации и сводки ниже видимой области;
- широкие пустые разрывы между связанными полями;
- обрезание правых controls из-за legacy-процентных ширин;
- перегруженную высоту tab bar;
- слабую визуальную иерархию основных и вложенных accordion-секций;
- разрыв единого сценария оформления вакансии между вкладками договоров и оплаты;
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
    │   │   ├── fixed logo visual-stage
    │   │   └── clamped title + tooltip
    │   ├── label-navigation
    │   ├── compact two-column summary
    │   ├── warning / owner
    │   └── sidebar spacer
    └── edit-workspace
        ├── edit-toolbar
        ├── edit-tabs (11 видимых разделов)
        │   └── edit-workspace-scroll / edit-workspace-content
        │       ├── primary accordion sections
        │       ├── nested subsection
        │       └── responsive field rows
        └── edit-footer-actions
```

## Behavior Summary

- открытие preview по route → CUBA восстанавливает editor entity → preview при необходимости перезагружает существующую `OpenPosition` с полным edit-view и загруженным `positionType` → выполняется штатный `OpenPositionEdit.onBeforeShow`;
- создание XML-компонентов → preview назначает только общие `edit-*`, `label-*` и локальные `open-position-preview-*` stylename → bindings, validators, editable и required не меняются;
- logo visual-stage → резервирует высоту под существующий `projectLogoImage` → название вакансии всегда начинается ниже изображения;
- длинное название вакансии → визуально ограничивается четырьмя строками, на низком viewport — тремя → полный текст остаётся доступным в tooltip;
- summary GridLayout → локально отображается как компактная сетка `caption/value` → порядок компонентов и значения `refreshSummary()` не меняются;
- применение layout-polish → существующие HBox-компоненты получают responsive-роли → поля не переставляются между бизнес-разделами и не получают новые значения;
- пустой `vacancyTitleSpacerHBox` → скрывается как чисто визуальный legacy-spacer → события и данные не меняются;
- пункт label-навигации → выбирается существующая вкладка `tabSheetOpenPosition` → lazy-loading продолжает выполнять базовый контроллер;
- изменение активной вкладки → сохраняется `label-nav-item`, добавляется или удаляется только `label-nav-item-active`;
- раскрытие GroupBox → меняется только presentation-state → значения и DataContext сохраняются;
- открытие «Трудовых договоров» → в одном scroll-контейнере доступны параметры оформления, таблица договоров и три секции оплаты → существующие bindings и расчёты остаются прежними;
- изменение `commandCandidate` → legacy-контроллер продолжает управлять доступностью платёжных компонентов → отдельный пункт и tab оплаты скрыты локальным presentation-слоем;
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

Пользователю доступны 11 вкладок:

1. `tabOpenPosition`;
2. `laborAgreementTab`;
3. `tabJobDescription`;
4. `tabFiles`;
5. `tabExercise`;
6. `tabMemoForInterview`;
7. `tabTemplateLetter`;
8. `tabSkills`;
9. `tabOpenPositionNews`;
10. `tabApproval`;
11. `commentsTab`.

`tabPayments` сохраняется пустой и скрытой только как техническая цель inherited `@Named("tabSheetOpenPosition.tabPayments")`. Пункт `previewNavPayments` также сохраняет прежние ID и invoke, но скрыт. Все прежние платёжные компоненты находятся после таблицы договоров внутри `laborAgreementTab` в порядке: «Оплата компании» → «Оплата ресерчерам» → «Оплата рекрутерам».

Tab bar остаётся частью `TabSheet`: вкладки не заменяются собственным роутером и не удаляются. Локальный CSS не обрезает captions через ellipsis; при нехватке места используется горизонтальный overflow tabcontainer CUBA, поэтому заголовки видны полностью и не расширяют workspace за границу формы.

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

Ширина sidebar в preview закреплена локально:

- базовая и компактная — `312px`;
- та же ширина задана Vaadin slot-обёртке `.v-slot-edit-sidebar`, поэтому workspace
  начинается строго после левой панели и не заезжает под неё.

Локальный partial оформляет sidebar в фирменной палитре HRM HuntTech:

```text
#172638 → #132130 → #0f1b28
```

Runtime-polish:

- отдельный `open-position-preview-logo-box` резервирует `166px` высоты, до `1366px` или высоты viewport `820px` — `126px`;
- основной логотип — `150 × 150px`, в компактном режиме — `112 × 112px`;
- изображение владельца сохраняет существующий размер и видимость;
- название — 14 px / 19 px, максимум четыре визуальные строки; в компактном режиме — три строки;
- полный текст названия сохраняется в `description` компонента;
- название центрировано и располагается отдельным slot ниже logo-stage;
- навигация использует ритм эталона `IteractionListEdit`: заголовок 11px/16px,
  пункты 38px/13px с вертикальным центрированием текста;
- summary GridLayout преобразуется только CSS-слоем в две колонки `72px + minmax(0,1fr)`, в компактном режиме `66px + minmax(0,1fr)`;
- значения summary ограничиваются двумя строками, captions имеют вторичную типографику;
- warning и owner остаются отдельными существующими компонентами.

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
→ open-position-preview-sidebar-usability
```

Shared mixin задаёт общую геометрию и типовые роли. `open-position-preview` содержит основной screen-specific дизайн. `open-position-preview-sidebar-usability` — узкий corrective layer только для identity/summary sidebar по фактическому runtime-скриншоту.

Неограниченные `.v-label`, `.v-button`, `.v-table`, `.v-panel` и другие глобальные селекторы отсутствуют: все правила находятся внутри `.open-position-preview`.

## 5. Поддержка семи тем

Идентичные локальные partial расположены в:

```text
modules/web/themes/<theme>/com.company.hunttech/open-position-preview.scss
modules/web/themes/<theme>/com.company.hunttech/open-position-preview-sidebar-usability.scss
```

Они подключены после `edit-screen-shared-styles` в темах:

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
- изменение preview Java/XML, data contract и lifecycle;
- изменение вызовов legacy editor;
- menu item или browse action для preview;
- изменение сущностей, БД и Liquibase;
- изменение `views.xml`, сервисов и JPQL;
- изменение shared SCSS и SCSS других экранов;
- перемещение полей вне согласованного объединения договоров и оплаты;
- изменение captions, component ID, `dataContainer`, `property`, actions и `invoke`;
- AI-анализ и поиск кандидатов;
- production deploy;
- merge без прямой команды Алексея.

## 8. Проверки Hermes

Hermes проверяет точный HEAD PR:

1. `git diff --check`;
2. разрешённый diff;
3. `OpenPositionEditPreviewSidebarUsabilityContractTest` — PASS;
4. `OpenPositionEditPreviewLayoutTest` — PASS;
5. `OpenPositionEditPreviewRouteGuardTest` — PASS;
6. `OpenPositionEditPreviewSharedStyleContractTest` — PASS;
7. `ScreenViewIntegrityTest` — 8/8 PASS;
8. идентичность corrective SCSS семи тем;
9. порядок import/include после `open-position-preview`;
10. `buildScssThemes` — PASS;
11. `clean assemble` — `BUILD SUCCESSFUL`;
12. local deploy и HTTP `/hrm/` = 200;
13. открытие preview по route без detached/lazy/RPC ошибок;
14. visual smoke 11 видимых вкладок в семи темах и отсутствие отдельной вкладки оплаты;
15. visual smoke viewport 1920×1080, 1600×900, 1366×768, 1280×800 и высоты 768/820 px;
16. logo и title не пересекаются при первом открытии и после переключения темы;
17. полный title доступен через tooltip;
18. summary показывает caption и value для всех строк без обрезания по правой границе;
19. navigation и summary доступны в sidebar scroll;
20. legacy editor и его вызовы не изменены.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-03 | Реализован утверждённый рендер формы: sidebar и логотип приведены к геометрии `JobCandidateEdit` 312/150px; отдельная вкладка оплаты скрыта, а существующие платёжные компоненты с прежними ID и bindings перенесены в «Трудовые договоры» тремя accordion-секциями; tab captions, theme-aware workspace и responsive-поля синхронизированы во всех семи темах. Бизнес-логика, loaders, JPQL, services и production не менялись. |
| 2026-08-03 | Левая часть OpenPositionEditPreview приведена к визуальному эталону `IteractionListEdit`: sidebar и его Vaadin slot закреплены на 312px, label-навигация 38px/13px больше не сжимается в компактном viewport, tab captions видны полностью без ellipsis через локальный horizontal overflow. Legacy `OpenPositionEdit`, Java/XML, data bindings, loaders и бизнес-логика не менялись. |
| 2026-08-02 | По runtime-скриншоту добавлен corrective layer `open-position-preview-sidebar-usability`: отдельный visual-stage 112/94px исключает наезд `OvaFallbackImage` на title; логотип 96/82px; title 4/3 строки с tooltip; summary GridLayout преобразован в компактную сетку caption/value. Изменены только локальные SCSS семи тем и их import/include, Java/XML и бизнес-логика не затронуты. |
| 2026-08-02 | Родительские box-контейнеры вкладок растянуты на всю ширину: в `projectLocationAccordion` (вкладка «Основное») двухколоночная legacy-раскладка перестроена в одноколоночную — vbox «Проект» 50%→100% с переносом `companyDepartamentField` внутрь (50%→100%), hbox «Компания/Город» 50%→100% с `expand="cityOpenPositionField"`; добавлены смысловые inline-комментарии. Остальные контейнеры вкладок уже были 100%; вне зоны: sidebar (312px), 3-колонка выплат (33% ×3), пустой спейсер `vacancyTitleSpacerHBox`. |
| 2026-08-02 | Логотип sidebar поднят по паттерну JobCandidateEdit: `openPositionPreviewLogoBox` выравнивание `MIDDLE_CENTER` → `TOP_CENTER`; убрана фиксированная высота бокса. Runtime-скриншот показал, что custom `OvaFallbackImage` всё равно требует отдельного corrective visual-stage; это уточнение реализовано следующей строкой истории выше. |
| 2026-08-02 | Всем 7 RichTextArea гарантированы `width="100%" height="100%"`; 4 редактора вкладки «Описание вакансии» обёрнуты в полноразмерные контейнеры. |
| 2026-08-02 | View `someFilesOpenPosition-edit-view`: `fileDescriptor` расширен до nested `name`+`size` — устранён `IllegalStateException` вкладки «Файлы». |
| 2026-08-02 | Исправлен ClassCastException String→Position при открытии preview: summary-лейблы освобождены от XML-биндинга, значения задаёт `refreshSummary()`. |
| 2026-08-02 | Modern UX polish: унифицированы радиусы, focus-контуры, hover, scrollbar и toolbar accent во всех семи темах. |
| 2026-08-02 | Tabsheet preview приведён к стилям JobCandidateEdit через `job-candidate-tabs`. |
| 2026-08-02 | Блок «Требуемые Навыки» восстановлен по структуре legacy-экрана. |
| 2026-08-02 | RichTextArea-область вкладки «Описание вакансии» растянута на оставшуюся часть экрана. |
| 2026-08-02 | В сводку «Ключевые параметры» добавлена строка «Оформление». |
| 2026-08-02 | Вкладка `tabOpenPosition` переименована «Проект» → «Основное» только в preview; сводка обновляется программно. |
| 2026-08-02 | Улучшена runtime-компоновка: compact identity, responsive HBox, tabs, accordion и footer без изменения бизнес-структуры. |
| 2026-08-02 | Добавлен локальный `open-position-preview.scss` во все семь тем. |
| 2026-08-02 | К preview применён общий `edit-*` / `label-*` UI API. |
| 2026-08-01 | Защита URL lifecycle переведена на загрузку полного edit-набора и замену item контейнера до legacy `onBeforeShow`. |
| 2026-08-01 | Создан изолированный preview новой двухпанельной компоновки без замены legacy-экрана и изменения бизнес-логики. |

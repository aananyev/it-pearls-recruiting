# OpenPositionEdit — UI Spec

> Форма: редактирование позиции (Edit) · Controller: `hunttech_OpenPosition.edit`
> Дескриптор: `modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml`
> Контроллер: `OpenPositionEdit.java` (READ_ONLY, не изменён) · Проект: **HRM HuntTech** (CUBA 7.3)
> Статус раздела «Визуальная компоновка»: обновлён 2026-08-05 (визуальный редизайн по UI-контракту)

---

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Форма — «паспорт» вакансии: рекрутер ведёт всю информацию о позиции от названия и внутреннего ID до схемы оплаты подрядчиков и текстов для кандидатов. Здесь задаются: принадлежность
проекту/компании/городу, тип позиции и грейд, приоритет и дата автозакрытия, вилка зарплаты и зарплата ИП, условия для ресурсера и рекрутера (процент/сумма, НДФЛ), описание на русском и
английском, тестовое задание, памятка к собеседованию, шаблон сопроводительного письма, дерево навыков, файлы, новости и комментарии-рейтинги. Форма интегрирована с BPM-согласованием и
рассылкой уведомлений (email/Telegram) подписчикам при открытии/закрытии позиции.

Визуальный редизайн 2026-08-05 привёл форму к общему UI API Edit-экранов HRM HuntTech
(`HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md`): двухпанельная композиция
«тёмный sidebar → рабочая область», единые `edit-*` / `label-*` stylename, эталонная
label-навигация `IteractionListEdit`, карточки-аккордеоны, responsive-строки полей и
плотные варианты таблиц/редакторов. Изменён **только presentation-слой**: XML-компоновка
и stylename, локальный SCSS, визуальные подписи и документация. Бизнес-логика, entity,
views, loaders, JPQL, actions, invoke, required/visible/enabled и Java-контроллер не изменялись.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из `hunttech_OpenPosition.browse` (действия create/edit), из lookup-экранов подбора и из `JobCandidateEdit` (просмотр позиции кандидата). Из формы доступны: окно выбора городов
`hunttech_SelectCitiesLocation` (кнопка «Добавить города»), справочники-просмотры picker-полей (проект, компания, департамент, город), вкладка «Согласование» (фрагмент
`bpm_ProcActionsFragment`), окно подписки (кнопка «Подписаться»). После сохранения — возврат в browse; при открытии/закрытии — синхронизация дочерних позиций.

Навигация внутри формы: постоянный sidebar (визуальный образ → название → label-навигация
разделов активной вкладки → контекст вакансии → предупреждение) и рабочая область
(toolbar → 12 вкладок TabSheet → прокручиваемый контент → постоянный footer с действиями).
Пункты label-навигации — визуальные указатели секций вкладки «О вакансии» (borderless-кнопки
без invoke: Java READ_ONLY, допустимо по §3.5 общего контракта).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие → загрузка `openPositionDc` (view `openPosition-edit-view`, без LOB) → `onAfterShow` догружает LOB основной вкладки, логотипы, новости, настройки и approval-процесс.
- Переключение вкладок → lazy-загрузка LOB/коллекций при первом открытии (8 флагов `*Loaded`).
- Изменение проекта/компании/типа позиции → каскадная подстановка (департаменты, город, описания, название вакансии).
- Сохранение → валидация вилки зарплаты и уникальности `vacansyID` → сбор подписчиков и уведомления (email/Telegram) → после коммита синхронизация дочерних позиций.
- Приоритет → авто-дата закрытия +7 дней; таймер 60 с обновляет обратный отсчёт и автозакрывает вакансию.
- Визуальный слой не запускает loaders, не меняет `*Loaded`-флаги, не создаёт новые бизнес-значения и не перехватывает lifecycle.

---

## 1. Точка вызова и контекст (Invocation & Context)

- `@UiController("hunttech_OpenPosition.edit")`, `@UiDescriptor("open-position-edit.xml")`, `@EditedEntityContainer("openPositionDc")`, `@LoadDataBeforeShow`.
- `dialogMode height="900px" width="1400px"` (модальное окно; изменена только ширина/высота по решению арбитра 9-1; openMode DIALOG и вызовы browse не менялись).
- Источники вызова: browse `OpenPositionBrowse` (create/edit), lookup-экраны, `JobCandidateEdit` (только просмотр связанной позиции).

## 2. Связь с моделью данных (Data & Entity Binding)

- Сущность: `OpenPosition`; контейнер `openPositionDc` (instance, view `openPosition-edit-view` — все поля вкладок кроме LOB).
- Коллекции вкладок: `laborAgreementDc` (LaborAgreement через join openPositions), `commentsOpenPositionDc` (OpenPositionComment, dateComment desc), `someFilesesDc` (SomeFilesOpenPosition),
`openPositionSkillsListsDc` (SkillTree, skillName), `procAttachmentsDc` (bpm$ProcAttachment, cacheable), `openPositionNewsDc` (OpenPositionNews + priorityNews, cacheable).
- Options-контейнеры: `openPositionParentDc` (родительские позиции, cacheable), `positionTypesDc` (Position без «(не использовать)», cacheable), `projectNamesDc` (Project не закрытые,
cacheable), `companyNamesDc`, `companyDepartamentsDc`, `citiesDc` (cacheable), `gradeDc` (cacheable).
- Facets: `timer closedVacancyTimer` (delay 60000, autostart=false, repeating) — автозакрытие по `closingDate`.
- Lazy-догрузка LOB: `loadPositionWithDescriptionLobs` (reload с view на LOB-поля) по вкладкам.
- Все `dataContainer`, `property`, `optionsContainer`, `required`, validators, actions и `invoke` сохранены без изменений (см. §7).

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

- Родитель: `OpenPositionBrowse` и lookup-экраны.
- Дочерние/диалоги: `SelectCitiesLocation` (выбор городов), `ProcActionsFragment` (BPM), справочные lookup-экраны picker-полей (Project, Company, City, Grade, Position).
- Фрагменты: `bpm_ProcActionsFragment` (вкладка «Согласование»).
- Связанные экраны: `JobCandidateEdit` использует узкий view `openPosition-edit-view` (строка 1117 views.xml).

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Lifecycle

- `onInit` → карты options; `onBeforeShow` (×2) → инициализация типа позиции, проекта, таймера, новостей, блокировок, иконок файлов, approval-процесса, подписки-группы.
- `onAfterShow` → lazy-загрузка LOB основной вкладки, логотипы проекта/владельца (`initProjectImagesOnOpen`), `setTopLabel` (шапка: комиссии, статус), `setOpenPositionNews`.
- `onTabSheetOpenPositionSelectedTabChange` → lazy-загрузка вкладки при первом открытии (флаги `mainTabLobsLoaded`, `exerciseLoaded`, `memoLoaded`, `templateLetterLoaded`, `skillsLoaded`,
`filesLoaded`, `commentsTabLoaded`, `laborAgreementLoaded`).
- `closedVacancyTimer` → тик каждые 60 с: обновление обратного отсчёта, автозакрытие.

### 4.2 Скрытые вычисления

- Каскады: проект → компания/департамент/город/описания/название; тип позиции → описания (RU/EN, «кто этот парень», стандартное) и навыки; приоритет → `setClosingWeek` (+7 дней); город →
название.
- Пересчёт комиссий: `calculateComission` (компания/ресурсер/рекрутер), `calculateResearcherSalary`/`calculateRecrutierSalary`, итоговые подписи
`setResearcherSalaryLabel`/`setRecrutierSalaryLabel`, HTML-шапка `setTopLabel`.
- Генераторы: `openPostionNewsDataGridDetailsGenerator` (детали новости), `skillImageColumnRenderer`, `openPositionSkillsListTableIsCommentColumnGenerator`.
- Сканирование описания: `addShortDescription` (короткое описание) и `rescanJobDescription`/`reloadSkillsForOpenPositionTab` (пересборка навыков).

### 4.3 Валидация/сохранение

- `openPositionFieldSalaryMinValidator`/`MaxValidator` — вилка мин ≤ макс.
- `onBeforeCommitChanges` → `checkDuplicatePositionId` (дубль `vacansyID` → диалог), `publishEventMessage`, сбор подписчиков (`getAllSubscibers`, `getSubscriberMaillist`,
`getRecrutiersMaillist`), `sendOpenPositionMessage`/`sendClosePositionMessage`.
- `onBeforeCommitChanges1/3/4` → синхронизация коллекций (навыки, соглашения, комментарии) и статусов.
- `onAfterCommitChanges` → `notifyTelegramOpenPositionChange`; `onAfterCommitChanges1` → `openCloseChildVacancy` (дочерние позиции).
- Ограничения: `vacansyNameField` required, `positionTypeField`/`remoteWorkField`/`numberPositionField`/`priorityField` required; LOB-редакторы блокируются до полной загрузки
(`screenFullyLoaded`).

## 5. Логика управляющих элементов (Actions & Buttons Logic)

- `generateVacancyNameFieldButton` → `generateNameFieldButton` → `generateVacancyName` (название по типу/проекту/городу).
- `addCity` → `addListCity` — окно `SelectCitiesLocation` (массовый выбор городов по стране).
- `setSalaryFieldButton` → `setSalaryFieldButtonInvoke` — подстановка зарплаты из рейтов аутстафа.
- `scanJDButton` → `addShortDescription` — извлечение короткого описания; `rescanSkills` → `rescanJobDescription` — пересборка навыков.
- `addOpenPositionNewsButton` → `addOpenPositionNewsButton` — создание новости.
- `subscribePositionButton` → `subscribePosition` — подписка рекрутёра.
- `windowCommitAndCloseButton` → `windowCommitAndClose` (сохранить и закрыть); `windowCloseButton` → закрыть без сохранения.
- Чекбоксы вкладок: `needExerciseCheckBox`, `needMemoCheckBox`, `needLetterCheckBox` — показывают/блокируют редакторы; `openClosePositionCheckBox` (скрыт) → `disableEnableFields`;
`signDraftCheckBox` → черновик.
- Таблицы: `laborAgreementDataGrid` (create/edit/remove, inline-редактирование), `someFilesTable` (add/create/edit/remove), `openPostionNewsDataGrid` (create/remove),
`openPositionSkillsListTable` (только просмотр, actions закомментированы).
- Пункты label-навигации sidebar — borderless-кнопки **без invoke** (визуальные указатели секций вкладки «О вакансии»); новых действий и Java-обработчиков не создано. **Исключение — пункт «Вакансия»** (`openPositionEditorNavVacancy`): клик-обработчик `onOpenPositionEditorNavVacancyClick` (подсветка + фокус в `vacansyIDTextField`) — единственная рабочая навигация, добавлена для блока `vacancyInputGroupBox` (арбитраж A).

## 6. Визуальная компоновка элементов (Visual Layout Schema)

### 6.1. Новая двухпанельная структура (редизайн 2026-08-05)

```text
layout (edit-screen-layout open-position-editor, dialogMode 1400×900)
├── vbox openPositionSidebar (edit-sidebar, 270px / 250px ≤1366px — из shared)
│   ├── vbox openPositionSidebarVisual (edit-sidebar-visual)
│   │   ├── hbox openPositionEditorLogoBox (open-position-editor-logo-box, резерв 96px)
│   │   │   ├── projectLogoImage (88×88) + projectOwnerImage (70×70)
│   │   └── vbox openPositionSidebarIdentity (edit-sidebar-identity)
│   │       ├── labelOpenPosition (edit-sidebar-title + open-position-editor-title-clamp)
│   │       └── signDraftLabel (edit-sidebar-subtitle)
│   ├── vbox vacancyPrioritySummary (open-position-editor-priority-summary) — блок срочности (копия IteractionListEdit)
│   │   ├── label «Приоритет:» (open-position-editor-summary-caption, msgPriority)
│   │   ├── hbox vacancyPriorityValueBox (open-position-editor-priority-value-row)
│   │   │   ├── image trafficLighterImage (open-position-editor-priority-image) — светофор StandartPriorityVacancy
│   │   │   └── label currentPriorityLabel (open-position-editor-priority-value) — Draft/Paused/Low/Normal/High/Critical
│   │   ├── label «Работа:» (open-position-editor-summary-caption, msgRemoteWorkSidebar)
│   │   └── label remoteWorkSidebarLabel (open-position-editor-priority-value) — Офис/Удаленная работа/Гибрид (50/50)
│   ├── vbox openPositionEditorNavigation (label-navigation)
│   │   ├── openPositionEditorNavActiveSectionsLabel (label-nav-title «Разделы активной вкладки»)
│   │   └── 7 пунктов borderless label-nav-item (5 — визуальные указатели без invoke; «Вакансия»/«Параметры вакансии»/«Зарплатное предложение» — рабочие label-навигации с кликом; первый — label-nav-item-active)
│   ├── groupBox openPositionEditorInfoCard («Информация», edit-accordion-section + open-position-editor-info-card)
│   │   └── vbox openPositionEditorInfoCardVBox — 6 HTML-label: infoPositionLabel («Должность» → vacansyName),
│   │       infoProjectLabel («Проект» → projectName), infoOwnerLabel («Владелец проекта» → projectOwner),
│   │       infoCityLabel («Город» → cityPosition), infoSalaryMaxTcLabel («Зарплата МАХ (ТК)» → salaryMax),
│   │       infoSalaryMaxIeLabel («Зарплата МАХ (ИП)» → salaryIE) — значения формирует refreshSidebarInfoCard()
│   ├── vbox openPositionEditorSummary (edit-sidebar-summary + open-position-editor-summary-grid)
│   │   ├── openPositionEditorContextLabel (label-nav-title «Контекст вакансии»)
│   │   ├── citiesLabel · labelTopComissionRecrutier · labelTopComissionResearcher
│   ├── closedVacancyInfoLabel (edit-sidebar-warning)
│   └── vbox openPositionSidebarSpacer (edit-sidebar-spacer)
└── vbox openPositionWorkspace (edit-workspace)
    ├── hbox openPositionToolbar (edit-toolbar)
    │   └── vbox openPositionToolbarTitleBox
    │       ├── openPositionEditorToolbarTitle (edit-toolbar-title «Редактирование открытой позиции»)
    │       └── openPositionEditorToolbarDescription (edit-toolbar-description)
    ├── tabSheet tabSheetOpenPosition (framed edit-tabs open-position-editor-tabs) — 12 вкладок
    └── vbox forExpand (edit-footer-actions open-position-editor-footer)
        ├── hbox statusHBox (open-position-editor-owner-row) → ownerTextField (borderless)
        └── hbox editActions (open-position-editor-footer-actions, flex-wrap: wrap)
            ├── subscribePositionButton (open-position-editor-secondary-action)
            └── hbox commitActions (open-position-editor-commit-actions) — группа завершения
                ├── windowCommitAndCloseButton (open-position-editor-primary-action)
                └── windowCloseButton (open-position-editor-secondary-action)
```

### 6.2. Вкладка «Проект» (tabOpenPosition)

Поток карточек в scrollBox `mainTabScrollBox` (вертикальный поток, все секции — на всю ширину контента, `open-position-editor-cards-row` → `flex-direction: column`):

- **Блок «Идентификаторы и статус»** (`openPositionEditorIdentifiersCard`, сверху):
  - **Блок «Вакансия»** (`vacancyInputGroupBox`, `open-position-editor-subsection` + `open-position-editor-vacancy-section`, caption «Вакансия») — 3 строки:
  - **Строка 1** `vacancyNameHBox` (`row-title`): внутренний ID `vacansyIDTextField` (ограничен, ratio 1) + название `vacansyNameField` (растягивается, ratio 5, required) + грейд `gradeLookupPickerField` (ratio 2, options `gradeDc`) + кнопка `generateVacancyNameFieldButton` «Генерировать» (`width=AUTO`, `invoke` сохранён) — все в ряд;
  - **Строка 2** `closingDateFieldsHBox`/`closingDateSignFieldsHBox`: дата закрытия `closingDateDateField` + приоритет `priorityField` (required) + `signDraftCheckBox` «Черновик вакансии» + `internalProjectCheckBox` «Эксклюзивная вакансия» (видимый; Java по-прежнему управляет по ролям) + скрытый `openClosePositionCheckBox` — в ряд;
  - **Строка 3** `priorityFieldsHBox`: комментарий `commentPriority` (caption «Комментарий», на всю ширину).
- **Блок «Параметры вакансии»** (`projectTypeGroupBox`, полная ширина, caption «Параметры вакансии» — ключ `msgVacancyParams`; бывшая секция «Проект, Компания, Тип должности») — единый блок параметров, строки сверху вниз:
  - **Должность** на всю ширину — `positionTypeField` (`vacancyParamsPositionRow`);
  - **Удалёнка + Комментарий** в одну линию — `remoteWorkFieldsHBox`: `remoteWorkField` «Удалёнка» + `remoteWorkCommentField` «Комментарий»;
  - **Проект** на всю ширину — `projectFieldsVBox` (`vacancyParamsProjectRow`): `projectNameField` + фильтры `projectFilterCheckBoxesHBox` (`onlyOpenProjectCheckBox`/`withOpenPositionCheckBox` под полем);
  - **Компания + Департамент** в одну линию — `vacancyParamsCompanyRow`: `companyNameField` + `companyDepartamentField`;
  - **Город + «Добавить»** в одну линию — `vacancyParamsCityRow` → `cityFieldsHBox`: `cityOpenPositionField` + кнопка `addCity`;
  - **Количество персонала** — вложенная секция `personnelCountGroupBox` (`open-position-editor-subsection`): `numberPositionHBox` (`numberPositionField` + `more10NumberPositionField`).
- **Блок «Команда / Вакансия»** (`commandOrVacancyGroupBox`, ниже): `commandOrPosition` (radio 50%) + `parentOpenPositionField`; ячейки `commandOrPositionCellHBox`/`parentPositionCellHBox` — `open-position-editor-field-row` (внутренний wrap, защита от переполнения).
- **Блок «Зарплатное предложение»** (`salaryGroupBox`, caption `msgSalaryProposal`; бывший «Заработная плата») — 4 строки сверху вниз: **строка 1** `salaryMinMaxHBox` — «Зарплата мин» `openPositionFieldSalaryMin` + «Зарплата мах» `openPositionFieldSalaryMax` в линию; **строка 2** `salaryIEHBox` — «Зарплата ИП» `openPositionFieldSalaryIE`; **строка 3** `salaryCheckBoxesRow` (`open-position-editor-checkboxes-row`) — чекбоксы «Ориентируемся на запрос кандидата» `salaryCandidateRequestCheckBox` + «Фиксированный лимит зарплаты» `salaryStrongLimitCheckBox` в линию; **строка 4** `salaryCommentRow` — «Комментарий» `salaryCommentTextFiels` на всю ширину.

Каждая секция: `edit-accordion-section` + `showAsPanel="true"`, `collapsable/collapsed` сохранены. Поля — `edit-form-control`; строки — `open-position-editor-field-row` с вариантами
(`row-title`, `row-grade`, `row-position`, `row-half`, `row-salary`, `row-wide`).

**Sidebar-навигация** (`openPositionEditorNavigation`): активный пункт синхронизирован с открытой вкладкой (вердикт арбитра `00-arbitration-sidebar-active.md`): на вкладке «Проект» активен `openPositionEditorNavIdentifiers` («Идентификаторы»), на остальных вкладках активный пункт снимается; базовый `label-nav-item` не изменяется; управление — `addStyleName/removeStyleName("label-nav-item-active")` в `onTabSheetOpenPositionSelectedTabChange` (пять пунктов остаются визуальными указателями без `invoke`). **Рабочая label-навигация** (клик снимает подсветку со всех пунктов, подсвечивает выбранный и переводит фокус в первое поле секции — штатная прокрутка ScrollBox; паттерн `selectSection` из эталона IteractionListEdit): «Вакансия» (`openPositionEditorNavVacancy` → `vacansyIDTextField`), «Параметры вакансии» (`openPositionEditorNavProject` → `positionTypeField`, пункт переименован с «Проект и локация») и «Зарплатное предложение» (`openPositionEditorNavSalary` → `openPositionFieldSalaryMin`, пункт переименован с «Заработная плата», caption `msgSalaryProposal`). Кнопки навигации — flex-контейнеры (`display: flex; align-items: center` в shared `edit-screen-shared-styles.scss`): текст пункта центрируется внутри подсветки (раньше из-за `display: block` у Vaadin-кнопки текст выпадал ниже подсветки).

**Иконки опций полей** (`optionIconProvider`/`optionStyleProvider`): «Приоритет вакансии»
(`priorityField`), «Удалёнка» (`remoteWorkField`), «Регистрация для работы»
(`registrationForWorkField`) показывают иконку выбранной опции слева от текста.
Локальное правило `.open-position-editor .edit-form-control.v-filterselect > .v-icon`
(20px, по центру слева) + `padding-left: 40px` у `input` — иконка не наезжает на текст
(shared-селектор `.edit-form-control .v-filterselect` не матчится, т.к. stylename стоит
на самом `.v-filterselect`). `priorityField` дополнительно получил локальный класс
`open-position-editor-priority-field` (`flex: 0 0 150px`) — узкая строка «Настройки
вакансии» не сжимает поле, текст опции читается на всех viewport.

### 6.3. Вкладка «Трудовой договор» (laborAgreementTab)

`laborAgreementGroupBox` (edit-accordion-section): параметры оформления `outstaffParamsHBox` (`registrationForWorkField`, `outstaffingCostTextField`, `setSalaryFieldButton`) и
`laborAgreementDataGrid` (open-position-editor-table-variant5) с buttonsPanel. После таблицы — платёжные секции, перенесённые из скрытой вкладки `tabPayments` (решение арбитра 9-2), в порядке:
`groupBoxPaymentsDetail` (детали оплаты, collapsed=true сохранён) → `groupBoxPaymentsResearcher` → `groupBoxPaymentsRecrutier`. ID, bindings и visibility-контракты сохранены; вкладка
`tabPayments` остаётся скрытой технической вкладкой (инвариант `@Named("tabSheetOpenPosition.tabPayments")`).

### 6.4. Остальные вкладки

- **Описание должности**: `workExperienceGroupBox` (collapsed=true), аккордеон `openPositionAccordion` (4 RichTextArea → `edit-form-control` + `open-position-editor-richtext-variant5`),
ряд `shortDescriptionHBox` (`shortDescriptionTextArea` + `scanJDButton`).
- **Файлы / Навыки / Новости**: таблицы `someFilesTable`, `openPositionSkillsListTable` (treeDataGrid), `openPostionNewsDataGrid` → `open-position-editor-table-variant5`.
- **Тестовое / Памятка / Шаблон письма**: checkBox + RichTextArea (`edit-form-control` + richtext-variant5).
- **Согласование**: `procActionsBox` → `edit-accordion-section` + `open-position-editor-group-tab` (резерв 44px под caption).
- **Комментарии**: `commentsScrollBox` → `open-position-editor-comments-scroll`.

### 6.5. Стили

- Общие классы (shared `edit-screen-shared-styles.scss`, не изменялся): `edit-*` и `label-*`; ширина sidebar 270px/250px — из shared.
- Локальный namespace `.open-position-editor` (7 идентичных theme-local partial `open-position-editor.scss`): тёмная sidebar `#172638 → #132130 → #0f1b28`, label-навигация по эталону
`IteractionListEdit` (hover белый на rgba(255,255,255,.08), active `#ffb11b` на rgba(255,177,27,.12) + жёлтая border-left), карточки, responsive-строки, таблицы/редакторы variant5,
footer, primary/secondary actions. Локальные классы: `open-position-editor` (root), `-logo-box`, `-title-clamp`, `-tabs`, `-field-row` (+row-варианты), `-cards-row`, `-subsection`,
`-primary-section`, `-group-tab`, `-summary-grid`, `-summary-caption/value`, `-payment-section`, `-payments-columns`, `-labor-tab-content`, `-labor-params`, `-table-variant5`,
`-richtext-variant5`, `-comments-scroll`, `-footer`, `-footer-actions`, `-owner-row`, `-primary-action`, `-secondary-action`, `-spacer`, `-tab-content`, `-table-view`, `-project-section`,
`-commit-actions` (группа OK/Отмена в footer), `-checkboxes-row` (строка признаков «Черновик»/«Эксклюзивная»),
`-richtext-section`, `-row-remote`, `-priority-field` (фикс. ширина 150px для поля приоритета с иконкой).
- Порядок слоёв в каждой теме: `theme base → edit-screen-shared-styles → open-position-editor`.

### 6.6. Последовательность заполнения

1. **Основные параметры**: ID и название вакансии (генерация), грейд → настройки: дата закрытия, приоритет, черновик → команда/вакансия и родительская позиция.
2. **Проект и локация**: тип позиции и формат удалённой работы → проект и департамент → компания и город (+массовое добавление городов).
3. **Количество персонала и заработная плата**: число позиций («более 10»), вилка зарплаты мин/макс, зарплата ИП, комментарий и лимит.
4. **Оформление (вкладка «Трудовой договор»)**: регистрация для работы, стоимость аутстафа, соглашения, затем схемы оплаты компании/ресерчерам/рекрутерам.
5. **Тексты и материалы**: описания RU/EN, стандартное описание, «кто это», короткое описание, тестовое задание, памятка, шаблон письма, навыки, новости, комментарии.

### 6.7. Responsive-контракт

| Viewport | Sidebar | Workspace | Поведение |
|---|---|---|---|
| >1366px | 270px (shared) | content max-width 1480px, центрирован | полный вид |
| ≤1366px | 250px (shared media) | сжатие | пары карточек/полей переходят в одну колонку через flex-wrap |
| любые | вертикальный scroll внутри, горизонтальный запрещён | без горизонтального скролла (кроме tab bar и таблиц) | `min-width: 0; max-width: 100%; box-sizing: border-box` |

Адаптация выполняется flex-контейнерами и `flex-wrap` без вложенных `@media` (ограничение Sass CUBA 7.3).

## 7. Контракт компонентов (Component ID | Тип | Binding | Визуальный раздел | UNCHANGED)

Полная карта 120 функциональных компонентов с source/target и allowed_visual_change зафиксирована в
`.team/OpenPositionEdit/02-component-map.csv` (разделитель «;»). Ниже — ключевые реперные компоненты:

| Component ID | Тип | Binding | Визуальный раздел | UNCHANGED |
|---|---|---|---|---|
| `labelOpenPosition` | label | — (значение: Java `setTopLabel`) | sidebar identity (title) | id, value-контракт Java |
| `signDraftLabel` | label | — (значение: Java) | sidebar identity (subtitle) | id, value-контракт Java |
| `projectLogoImage` / `projectOwnerImage` | ovaFallbackImage | — | sidebar visual (88×88 / 70×70) | id, fallback, Java-инъекции |
| `citiesLabel` | label | — (значение: Java) | sidebar summary | id, value-контракт Java |
| `labelTopComissionRecrutier` / `labelTopComissionResearcher` | label | — (значение: Java) | sidebar summary | id, htmlEnabled, value-контракт Java |
| `closedVacancyInfoLabel` | label | — (значение: Java-таймер) | sidebar warning | id, icon, value-контракт Java |
| `vacansyIDTextField` / `vacansyNameField` / `gradeLookupPickerField` | textField / lookupPickerField | `vacansyID` / `vacansyName` / `grade` | блок «Вакансия» (`vacancyInputGroupBox`) / карточка «Идентификаторы и статус» | id, dataContainer, property, required |
| `commandFieldHBox` | groupBox | — | subsection «Настройки вакансии» | id, caption, collapsable/collapsed |
| `commandOrPosition` / `parentOpenPositionField` | radioButtonGroup / lookupPickerField | `commandCandidate` / `parentOpenPosition` | карточка «Команда / Вакансия» | id, dataContainer, property, required, options |
| `positionTypeField` | lookupPickerField | `positionType` | projectTypeGroupBox (focusComponent) | id, dataContainer, property, options, required |
| `projectNameField` / `companyDepartamentField` / `companyNameField` / `cityOpenPositionField` | lookupPickerField | `projectName` / — / — / `cityPosition` | projectTypeGroupBox | id, options, property, required, actions |
| `numberPositionField` / `openPositionFieldSalaryMin` / `openPositionFieldSalaryMax` | textField | `numberPosition` / `salaryMin` / `salaryMax` | карточки «Количество персонала» / «Зарплата» | id, dataContainer, property, validators |
| `laborAgreementDataGrid` | dataGrid | `laborAgreementDc` | laborAgreementTab (table-variant5) | id, dataContainer, actions, columns, editorEnabled |
| `groupBoxPaymentsDetail` / `groupBoxPaymentsResearcher` / `groupBoxPaymentsRecrutier` | groupBox | поля внутри (bindings сохранены) | laborAgreementTab (после таблицы) | id, captions, collapsed, Java-видимость |
| `tabPayments` | tab | — | скрытая техническая вкладка | id, visible=false, @Named-путь |
| `openPositionAccordion` + 4 tabs | accordion | LOB-поля richTextArea | tabJobDescription | id, @Named-пути, required/editable |
| `someFilesTable` / `openPostionNewsDataGrid` / `openPositionSkillsListTable` | table / dataGrid / treeDataGrid | collection containers | вкладки Файлы/Новости/Навыки | id, actions, columns, renderers |
| `ownerTextField` | textField | `owner` | footer (owner-row) | id, dataContainer, editable=false, enable=false |
| `subscribePositionButton` / `windowCommitAndCloseButton` / `windowCloseButton` | button | actions/invoke | footer-actions | id, invoke/action, порядок |
| `tabSheetOpenPosition` | tabSheet | — | workspace (edit-tabs) | id, 12 вкладок, lazy-загрузка Java |
| `closedVacancyTimer` | timer (facet) | — | facets | id, delay, autostart=false, repeating |

## 8. UI-референсы

- `docs/ui/images/OpenPositionEdit/01_open_position_tab_main_halo_1920x1080.png` — утверждённый проектный рендер (halo, 1920×1080): двухпанельная компоновка, toolbar «Редактирование открытой
позиции», 12 вкладок, sidebar с «РАЗДЕЛЫ АКТИВНОЙ ВКЛАДКИ» (6 пунктов) и «КОНТЕКСТ ВАКАНСИИ». Рендер отражает целевой визуальный язык; точное воспроизведение отдельных бизнес-элементов
рендера ограничено решениями арбитра (см. §9): вкладка «Оплата и контакты» остаётся скрытой, статус «• позиция открыта» не дублируется, sidebar 270/250px по общему контракту.
- Эталон визуального языка: `docs/ui/OpenPositionEditPreview_Spec.md` (preview-форма, паттерны variant5, group-tab, footer, field-row).

## 9. Что не изменялось

- Java: `OpenPositionEdit.java` — изменён ТОЛЬКО по вердикту арбитра `00-arbitration-sidebar-active.md` (пункт A): добавлены 6 `@Named`-инъекций nav-кнопок и визуальная синхронизация `label-nav-item-active` в существующем обработчике `onTabSheetOpenPositionSelectedTabChange`. Бизнес-логика, lazy-загрузка, вычисления, сохранение — без изменений.
- Entity, enum, справочники, сервисы, loaders, JPQL, `views.xml`, DataContext, Liquibase, БД.
- Другие формы, включая `open-position-edit-preview.xml` / `OpenPositionEditPreview.java` и browse.
- Shared SCSS `edit-screen-shared-styles.scss` (7 копий) и SCSS других экранов.
- Component ID, `dataContainer`, `property`, `optionsContainer`, actions, существующие `invoke` (в т.ч. `generateNameFieldButton`), `required`, `visible`, `enabled`, `editable`, `readonly`, validators, captions (msg-ключи).
- `visible="false"` у `tabPayments`, `openClosePositionCheckBox`, `internalProjectCheckBox`, `commanExperienceRadioButton`, `lastOpenVacancyDateField`.
- Закомментированные actions вкладки «Навыки» не восстанавливались.

## 10. Функциональные границы (корректирующий этап «Проект», 2026-08-05)

```text
component IDs: UNCHANGED
bindings: UNCHANGED
actions: UNCHANGED
handlers: UNCHANGED
entity: UNCHANGED
reference data: UNCHANGED
loaders: UNCHANGED
JPQL: UNCHANGED
views: UNCHANGED
other screens: UNCHANGED
```

Исключение (CONDITIONALLY_ALLOWED, вердикт арбитра `00-arbitration-sidebar-active.md`): `OpenPositionEdit.java`
+27 строк — 6 `@Named`-инъекций nav-кнопок и визуальная синхронизация `label-nav-item-active` в существующем
обработчике `onTabSheetOpenPositionSelectedTabChange` (lazy-загрузка и бизнес-логика не тронуты).
Полное подтверждение — `06-cuba-verification.md` (FUNCTIONAL_CONTRACT: UNCHANGED, STATUS: VERIFIED).

## 11. Runtime-проверка (корректирующий этап «Проект»)

- **HEAD**: `2ff1f129ec1378c043293a8d7ba30f77316e0988` (local deploy, Tomcat, http://localhost:8080/hrm/ → HTTP 200)
- **Темы**: halo (light), hunttech-modern-dark (dark) — обе PASS
- **Viewport**: 1920×1080, 1440×900, 1366×768, 1280×800 — PASS (без горизонтальной прокрутки, bodyHscroll=0)
- **Проверенные состояния**: sidebar active «Идентификаторы» на вкладке «Проект»; снят на «Трудовой договор»;
  восстановлен при возврате; ID+Вакансия в строке; Грейд+«Генерировать» в строке (кнопка компактная w=121px);
  карточки ID/«Команда / Вакансия» и «Количество персонала»/«Заработная плата» вертикально (w=1354px)
- **Скриншоты**: `.ai/reports/open-position-edit-project-tab/2ff1f129ec1378c043293a8d7ba30f77316e0988/screenshots/`
  (`light-{1920,1440,1366,1280}-project-tab.png`, `dark-{1920,1366}-project-tab.png`,
  `sidebar-project-active.png`, `sidebar-neighbor-active.png`)
- **Независимая инспекция**: `05-visual-review.md` — VISUAL_CONTRACT: PASS (12/12 пунктов)
- **CUBA-верификация**: `06-cuba-verification.md` — FUNCTIONAL_CONTRACT: UNCHANGED, STATUS: VERIFIED
- **Результат**: замечания предыдущего browser-раунда (непокрытие DOM-обёртки `v-expand` селекторами
  cards-row/row-grade/row-title) найдены и исправлены; повторная полная проверка — PASS.
  Функциональные замечания: нет.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-06 | Устранены наезды блоков OpenPositionEdit: Vaadin выносил шапку GroupBox над рамкой (inline `margin-top:-44px` у `v-panel-captionwrap`), из-за чего caption каждой карточки перекрывал низ предыдущего блока (16–30px: «Идентификаторы и статус» ↔ «Команда / Вакансия», «Команда / Вакансия» ↔ «Параметры вакансии», «Параметры вакансии» ↔ «Зарплатное предложение»); добавлено правило `.open-position-editor .edit-accordion-section > .v-panel-captionwrap { margin-top: 0 !important }` (шапка внутри панели, зазоры gap 14px работают честно); резерв group-tab («Согласование») сокращён с 58px до 14px (пустота устранена); браузерная проверка: наездов 0 на вкладке «О вакансии», вкладка «Согласование» чистая |
| 2026-08-06 | Заголовки блоков OpenPositionEdit приведены к единому стилю «шапки блока»: все `edit-accordion-section .v-panel-caption` — 16px w700 с акцентной плашкой слева (border-left 3px `$v-selection-color`); вложенные `open-position-editor-subsection` («Вакансия», «Настройки вакансии», «Количество персонала») — 13px w600 с полупрозрачной плашкой; плашка у `open-position-editor-primary-section` перенесена с панели на caption; добавлен отсутствовавший заголовок блока «Трудовой договор» (`laborAgreementGroupBox` caption `msgLaborAgreement`, новый msg-ключ) |
| 2026-08-06 | Первый блок «Вакансия» (`vacancyInputGroupBox`) перестроен в 3 строки: строка 1 `vacancyNameHBox` — ID (`vacansyIDTextField`, ratio 1) + Вакансия (`vacansyNameField`, ratio 5) + Грейд (`gradeLookupPickerField`, ratio 2) + кнопка `generateVacancyNameFieldButton` «Генерировать» (перенесены из удалённой строки `gradeActionRowHBox`); строка 2 `closingDateSignFieldsHBox` — Дата закрытия + Приоритет + `signDraftCheckBox` «Черновик вакансии» + `internalProjectCheckBox` «Эксклюзивная вакансия» (стал видимым, убран visible=false; Java-управление по ролям сохранено) + скрытый `openClosePositionCheckBox`; строка 3 `priorityFieldsHBox` — комментарий `commentPriority` с caption «Комментарий» (`msgOpenPositionComment`) на всю ширину; удалены `gradeActionRowHBox`, `vacancyTitleSpacerHBox`, `closingDateCheckBoxesHBox` (не контрактные); контрактные groupBox-секции (`commandFieldHBox` и др.) сохранены; preview-XML синхронизирован |
| 2026-08-06 | В sidebar OpenPositionEdit под наименованием вакансии добавлен блок срочности `vacancyPrioritySummary` (копия IteractionListEdit, stylename `open-position-editor-priority-summary`): первый элемент — приоритет со светофором (label `msgPriority` «Приоритет:», hbox `vacancyPriorityValueBox` с image `trafficLighterImage` и label `currentPriorityLabel`, значения и иконки `StandartPriorityVacancy` — Draft/Paused/Low/Normal/High/Critical); второй элемент — формат работы: label `msgRemoteWorkSidebar` «Работа:» + `remoteWorkSidebarLabel` (Офис / Удаленная работа / Гибрид (50/50) по `remoteWork` 0/1/2); значения формирует `refreshSidebarPriority()` (presentation-only, вызывается из `refreshSidebarInfoCard()` — onBeforeShow + listener openPositionDc); новые msg-ключи `msgPriority`, `msgRemoteWorkSidebar`; иконки traffic-lights_*/remove.png скопированы в темы hunttech-modern* (отсутствовали); preview-XML дополнен скрытыми компонентами (контракт @Inject) |
| 2026-08-06 | В sidebar OpenPositionEdit после label-навигации добавлена информационная карточка в рамке `openPositionEditorInfoCard` (caption `msgSidebarInfoCard` «Информация», `edit-accordion-section` + `open-position-editor-info-card`): вертикальный список HTML-label `infoPositionLabel` («Должность» → `vacansyName`), `infoProjectLabel` («Проект» → `projectName.projectName`), `infoOwnerLabel` («Владелец проекта» → `projectName.projectOwner` firstName+secondName), `infoCityLabel` («Город» → `cityPosition.cityRuName`), `infoSalaryMaxTcLabel` («Зарплата МАХ (ТК)» → `salaryMax`) и `infoSalaryMaxIeLabel` («Зарплата МАХ (ИП)» → `salaryIE`); значения формирует `refreshSidebarInfoCard()` (presentation-only, HTML с экранированием) — вызывается в onBeforeShow и через `openPositionDc.addItemPropertyChangeListener` при изменении любого атрибута; данные доступны в view (projectName → project-edit-view → projectOwner → person-owner-view), View Integrity пройдена; новые msg-ключи `msgSidebarInfoCard` |
| 2026-08-06 | Секция «Заработная плата» перестроена в блок **«Зарплатное предложение»** (`salaryGroupBox`, caption `msgSalaryProposal`): строка 1 `salaryMinMaxHBox` — мин/мах зарплата в линию, строка 2 `salaryIEHBox` — зарплата ИП, строка 3 `salaryCheckBoxesRow` — чекбоксы «Ориентируемся на запрос кандидата» + «Фиксированный лимит зарплаты», строка 4 `salaryCommentRow` — комментарий на всю ширину; отдельная label-навигация: пункт `openPositionEditorNavSalary` переименован в «Зарплатное предложение» (caption `msgSalaryProposal`), клик → подсветка + фокус в `openPositionFieldSalaryMin` (`onOpenPositionEditorNavSalaryClick`); бизнес-id, bindings и свойства полей сохранены; preview-XML не изменялся |
| 2026-08-06 | Поля «ID» (`vacansyIDTextField`) и «Вакансия» (`vacansyNameField`) объединены в единый блок ввода `vacancyInputGroupBox` (subsection `open-position-editor-vacancy-section`, caption «Вакансия») с расположением в одну линию (строка `vacancyNameHBox`, ID ~130px + название на расширение); добавлена отдельная label-навигация: пункт `openPositionEditorNavVacancy` (mainMsg-ключ `openPositionEditorNavVacancy=Вакансия`) — клик снимает подсветку со всех пунктов, подсвечивает «Вакансия» и фокусирует `vacansyIDTextField` (штатная прокрутка ScrollBox; рефакторинг: общий `resetNavigationActiveStyles()` вместо дублирующих removeStyleName); в preview-XML добавлена скрытая кнопка для контракта @Named; business-id, bindings и required не изменялись |
| 2026-08-06 | Секция «Проект, Компания, Тип должности» перестроена в единый блок **«Параметры вакансии»** (`projectTypeGroupBox`, caption `msgVacancyParams`): Должность на всю ширину (`vacancyParamsPositionRow` → `positionTypeField`), «Удалёнка» + «Комментарий» в линию (`remoteWorkFieldsHBox`), «Проект» на всю ширину (`vacancyParamsProjectRow` → `projectFieldsVBox` + фильтры), «Компания» + «Департамент» в линию (`vacancyParamsCompanyRow`), «Город» + «Добавить» в линию (`vacancyParamsCityRow` → `cityFieldsHBox`), «Количество персонала» — вложенная секция `personnelCountGroupBox` (перенесена из cardsRow2, добавлен `open-position-editor-subsection`; cardsRow2 теперь только «Заработная плата»); отдельная label-навигация: пункт `openPositionEditorNavProject` переименован в «Параметры вакансии» (значение ключа `openPositionEditorNavProject`), переставлен после «Настройки вакансии», клик → подсветка + фокус в `positionTypeField` (`onOpenPositionEditorNavProjectClick`); новые msg-ключи `msgVacancyParams`; business-id, bindings, required, invoke и фильтры проектов сохранены; preview-XML не изменялся |
| 2026-08-05 | Исправлено смещение текста пунктов label-навигации sidebar OpenPositionEdit: текст выпадал ниже подсветки (из-за `display: block` у Vaadin-кнопки); кнопки `.v-button-label-nav-item` стали flex-контейнерами (`display: flex; align-items: center`, wrap `flex: 1`) в shared `edit-screen-shared-styles.scss` (7 тем синхронно) — текст центрируется внутри подсветки; проверено на 1440/1366 в halo и hunttech-modern-dark, сценарий переключения вкладок работает |
| 2026-08-05 | Исправлен наезд пиктограмм опций на текст в picker-полях OpenPositionEdit («Приоритет вакансии», «Удалёнка», «Регистрация для работы»): локальное правило `.edit-form-control.v-filterselect > .v-icon` (иконка 20px по центру слева) + `padding-left: 40px` у input (shared-селектор не матчился, т.к. stylename стоит на самом `.v-filterselect`); полю `priorityField` добавлен локальный класс `open-position-editor-priority-field` (`flex: 0 0 150px`) — текст опции читается на 1920/1440/1366/1280, light/dark |
| 2026-08-05 | Скорректирована визуальная компоновка вкладки «Проект» OpenPositionEdit: синхронизирована label-навигация (вердикт арбитра `00-arbitration-sidebar-active.md` — 6 `@Named` + `label-nav-item-active` в `onTabSheetOpenPositionSelectedTabChange`), устранено переполнение Tab, восстановлены горизонтальные строки ID/Вакансия и Грейд/«Генерировать» (`gradeActionRowHBox`), блоки ID/«Команда разработчиков» и «Количество персонала»/«Заработная плата» размещены вертикально (`open-position-editor-cards-row` → column); бизнес-логика не изменялась. |
| 2026-08-05 | Выполнен визуальный редизайн формы по UI-контракту: двухпанельная компоновка `edit-screen-layout` + sidebar/workspace, label-навигация (эталон IteractionListEdit, пункты без invoke), карточки `edit-accordion-section`, responsive-строки полей, варианты таблиц/редакторов variant5, footer с primary/secondary actions; диалог увеличен до 1400×900 (арбитр 9-1); платёжные секции перенесены во вкладку «Трудовой договор» (арбитр 9-2), `tabPayments` остаётся скрытой; добавлены 7 идентичных partial `open-position-editor.scss` и их подключение в 7 темах; добавлены визуальные msg-ключи и контрактный тест `OpenPositionEditLayoutContractTest`. Бизнес-логика, сущности, справочники и другие формы не изменялись. |
| 2026-08-01 | Создание Spec; полное inline-документирование XML (168 комментариев), бизнес-id всем элементам (buttonsPanel→laborAgreement/someFiles/openPositionNewsButtonsPanel, id для всех hbox/vbox/groupBox-контейнеров и кнопок), javadoc-покрытие контроллера (163 метода + класс). |

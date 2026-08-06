# OpenPositionEdit — 00. Источник и текущее состояние (Source State)

> Артефакт аналитики для **исключительно визуального** редизайна legacy-формы.
> Роль автора: Системный аналитик / автор UI-контракта.
> Дата фиксации состояния: 2026-08-05.

---

## 1. Репозиторий и версии

| Параметр | Значение |
|---|---|
| Repository | `aananyev/it-pearls-recruiting` (локальный путь `/Users/alekseyananyev/StudioProjects/hunttech_recruiting`) |
| Ветка | `agent/open-position-edit-redesign` |
| HEAD | `2ff1f129ec1378c043293a8d7ba30f77316e0988` (полный SHA) |
| Base (master) | тот же SHA `2ff1f129ec1378c043293a8d7ba30f77316e0988` — ветка не имеет собственных коммитов относительно master |
| Рабочее дерево | чистое для анализируемых файлов (вне scope: `backupbase.log`, `.team/`, `docs/performance-archive/...`, `docs/ui/images/`) |

## 2. Прочитанные файлы (полный перечень)

### Код экрана
- `modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml` — 1287 строк, дескриптор формы (полный, включая inline-комментарии).
- `modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEdit.java` — 3526 строк, контроллер (`StandardEditor<OpenPosition>`).
- `modules/global/src/com/company/hunttech/entity/OpenPosition.java` — 710 строк, entity-контракт.

### Документация
- `docs/ui/OpenPositionEdit_Spec.md` — 99 строк, UI Spec legacy-формы.
- `docs/ui/OpenPositionEditPreview_Spec.md` — 390 строк, эталон утверждённого визуального языка (preview-форма).
- `docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md` — 602 строки, общий контракт Edit-экранов (читать через python; файл содержит длинные строки).
- `docs/architecture/HRM_HuntTech_UI_UX_Design_Concept.md` — 468 строк, общая UI/UX-концепция.
- `docs/ui/images/OpenPositionEdit/01_open_position_tab_main_halo_1920x1080.png` — УТВЕРЖДЁННЫЙ рендер ChatGPT, 1920×1080, halo. OCR-структура зафиксирована в задании и в `01-ui-contract.md` §7.

### SCSS (эталоны)
- `modules/web/themes/hover/com.company.hunttech/edit-screen-shared-styles.scss` — 391 строка, общий UI API (`edit-*`, `label-*`).
- `modules/web/themes/hover/com.company.hunttech/open-position-preview.scss` — 1200 строк, эталонный локальный SCSS preview.
- `modules/web/themes/hover/com.company.hunttech/open-position-preview-sidebar-usability.scss` — 214 строк, corrective-слой sidebar preview.
- `modules/web/themes/hover/com.company.hunttech/iteraction-list-visual-alignment.scss` — 636 строк, эталон label-навигации/тёмного sidebar.
- `modules/web/themes/hover/styles.scss` — порядок `@import`/`@include` (shared → preview → preview-usability → iteraction-*).

### Тесты (паттерны)
- `modules/core/test/com/company/hunttech/core/OpenPositionEditPreviewLayoutTest.java` — 373 строки.
- `modules/core/test/com/company/hunttech/core/OpenPositionEditPreviewSharedStyleContractTest.java` — 323 строки.
- `modules/core/test/com/company/hunttech/core/OpenPositionEditPreviewSidebarUsabilityContractTest.java` — 137 строк.
- `modules/core/test/com/company/hunttech/core/OpenPositionScreenDocumentationTest.java` — 162 строки.
- `modules/core/test/com/company/hunttech/core/ScreenViewIntegrityTest.java` — 94 строки.

### Справочные ключи
- `modules/web/src/com/company/hunttech/web/messages.properties` — общие ключи (часть ключей формы лежит в пакете экрана, `messagesPack="com.company.hunttech.web.screens.openposition"`).

## 3. Текущая структура экрана (layout-дерево legacy XML)

Корень: `<window caption="msg://editorCaption" focusComponent="positionTypeField" icon="COMPASS" dialogMode height="800px" width="1100px">`.

```
window (dialogMode 1100×800, focusComponent=positionTypeField)
├── <data> (15 контейнеров, см. §4)
├── <facets> (timer closedVacancyTimer, delay=60000, autostart=false, repeating)
└── layout (expand="tabSheetGroupBox", spacing)
    ├── dateField lastOpenVacancyDateField (visible=false)
    ├── groupBox positionHeaderGroupBox (collapsable, caption=msgTitle)          ← «шапка»
    │   ├── hbox positionHeaderHBox (expand=labelOpenPosition)
    │   │   ├── label signDraftLabel (stylename h2)                              ← «Черновик»
    │   │   ├── label labelOpenPosition (stylename table-wordwrap)               ← название вакансии
    │   │   ├── label labelTopComissionRecrutier (h4, htmlEnabled)               ← комиссия рекрутера
    │   │   ├── label labelTopComissionResearcher (h4, htmlEnabled)              ← комиссия ресурсера
    │   │   ├── ovaFallbackImage projectLogoImage (70×70, widget-border)         ← логотип проекта
    │   │   └── ovaFallbackImage projectOwnerImage (70×70, oval, widget-border)  ← аватар владельца
    │   └── label closedVacancyInfoLabel (icon=WARNING)                          ← предупреждение автозакрытия
    ├── vbox tabSheetGroupBox (expand=tabSheetOpenPosition)
    │   └── tabSheet tabSheetOpenPosition (stylename framed)                     ← 12 вкладок, см. §5
    └── vbox forExpand (align=BOTTOM_RIGHT, expand=statusHBox)                   ← footer
        ├── hbox statusHBox
        │   └── textField ownerTextField (editable=false, enable=false, borderless)
        └── hbox editActions (align=BOTTOM_RIGHT)
            ├── button subscribePositionButton (invoke=subscribePosition, icon=BELL)
            ├── button windowCommitAndCloseButton (action=windowCommitAndClose)
            └── button windowCloseButton (action=windowClose)
```

### Вкладка `tabOpenPosition` («О вакансии», icon=USERS) — внутренняя структура

```
tab tabOpenPosition (margin, spacing)
└── scrollBox mainTabScrollBox (vertical)
    └── vbox citiesLabelHBox (expand=vacancyNameHBox)
        ├── hbox vacancyNameHBox (expand=vacansyNameField)                        ← «Идентификаторы»
        │   ├── textField vacansyIDTextField (large)                             ← ID вакансии
        │   ├── textField vacansyNameField (large, required)                     ← название
        │   ├── lookupPickerField gradeLookupPickerField (large, options=gradeDc) ← грейд
        │   └── button generateVacancyNameFieldButton (invoke=generateNameFieldButton)
        ├── hbox vacancyTitleSpacerHBox                                           ← пустой legacy-спейсер
        ├── hbox hboxProject1
        │   └── groupBox commandFieldHBox (light, collapsable, caption=msgCommanSetup) ← «Настройки вакансии»
        │       ├── hbox closingDateFieldsHBox
        │       │   ├── hbox closingDateSignFieldsHBox
        │       │   │   ├── dateField closingDateDateField (description=msgClosingDateDesc)
        │       │   │   ├── checkBox signDraftCheckBox
        │       │   │   ├── checkBox openClosePositionCheckBox (visible=false)
        │       │   │   └── checkBox internalProjectCheckBox (visible=false)
        │       │   └── hbox priorityFieldsHBox (expand=commentPriority)
        │       │       ├── lookupField priorityField (required, nullOptionVisible=false)
        │       │       └── textField commentPriority
        ├── groupBox commandOrVacancyGroupBox (light, collapsable, caption=msgCommandOrVacancy) ← «Команда/Вакансия»
        │   └── hbox commandOrVacancyHBox
        │       ├── hbox commandOrPositionCellHBox
        │       │   └── radioButtonGroup commandOrPosition (required, orientation=horizontal)
        │       └── hbox parentPositionCellHBox
        │           └── lookupPickerField parentOpenPositionField (options=openPositionParentDc)
        ├── groupBox projectTypeGroupBox (light, collapsable, caption=msgTypeOfProject) ← «Тип проекта / Проект и локация»
        │   ├── hbox hboxVacansy
        │   │   ├── lookupPickerField positionTypeField (required, options=positionTypesDc, nullOptionVisible=false)
        │   │   └── hbox remoteWorkFieldsHBox
        │   │       ├── lookupField remoteWorkField (required, nullOptionVisible=false)
        │   │       └── textField remoteWorkCommentField
        │   ├── hbox hboxProject
        │   │   ├── vbox projectFieldsVBox
        │   │   │   ├── lookupPickerField projectNameField (required, options=projectNamesDc, nullOptionVisible=false)
        │   │   │   └── hbox projectFilterCheckBoxesHBox
        │   │   │       ├── checkBox onlyOpenProjectCheckBox
        │   │   │       └── checkBox withOpenPositionCheckBox
        │   │   └── lookupPickerField companyDepartamentField (required, options=companyDepartamentsDc, nullOptionVisible=false)
        │   ├── hbox hboxCompany
        │   │   ├── lookupPickerField companyNameField (required, options=companyNamesDc, nullOptionVisible=false)
        │   │   └── hbox cityFieldsHBox (expand=cityOpenPositionField)
        │   │       ├── lookupPickerField cityOpenPositionField (required, options=citiesDc, nullOptionVisible=false)
        │   │       └── button addCity (invoke=addListCity)
        │   └── hbox citiesLabelHBox (width=50%)
        │       └── label citiesLabel (h4)
        ├── groupBox personnelCountGroupBox (light, collapsable, caption=msgCountOfPersonel) ← «Количество персонала»
        │   └── hbox numberPositionHBox
        │       ├── textField numberPositionField (required)
        │       └── checkBox more10NumberPositionField
        └── groupBox salaryGroupBox (light, collapsable, caption=msgSalary) ← «Зарплата»
            ├── hbox hboxSalary
            │   ├── textField openPositionFieldSalaryMin
            │   ├── textField openPositionFieldSalaryMax
            │   ├── textField openPositionFieldSalaryIE
            │   └── checkBox salaryCandidateRequestCheckBox
            └── hbox space2Box (expand=salaryCommentTextFiels)
                ├── textField salaryCommentTextFiels
                └── checkBox salaryStrongLimitCheckBox
```

### Вкладка `laborAgreementTab` («Трудовые соглашения», icon=CUBES)

```
tab laborAgreementTab (expand=laborAgreementDataGrid)
└── groupBox laborAgreementGroupBox (light, expand=laborAgreementDataGrid)
    ├── hbox outstaffParamsHBox
    │   ├── lookupField registrationForWorkField (required, nullOptionVisible=false)
    │   ├── textField outstaffingCostTextField (datatype=decimal)
    │   └── button setSalaryFieldButton (invoke=setSalaryFieldButtonInvoke)
    └── dataGrid laborAgreementDataGrid (editorEnabled=true, dataContainer=laborAgreementDc)
        ├── actions: create / edit / remove
        ├── columns: perhaps (editable, 50–100px) / company / laborAgreementType
        ├── rowsCount (autoLoad)
        └── buttonsPanel laborAgreementButtonsPanel
            ├── button createBtn (action=laborAgreementDataGrid.create)
            ├── button editBtn (action=laborAgreementDataGrid.edit)
            └── button removeBtn (action=laborAgreementDataGrid.remove)
```

### Вкладка `tabPayments` («Оплата», icon=BULK_EDIT_ACTION, **visible=false**)

```
tab tabPayments (visible=false)
├── groupBox groupBoxPaymentsResearcher (orientation=horizontal, caption=msgResearcherPayment)
│   └── label labelResearcherSalary (htmlEnabled)
├── groupBox groupBoxPaymentsRecrutier (caption=msgRecrutierSalary)
│   └── label labelRecrutierSalary (htmlEnabled)
└── groupBox groupBoxPaymentsDetail (collapsable, collapsed=true, orientation=horizontal)
    └── hbox paymentsColumnsHBox
        ├── vbox companyPaymentsVBox (width=33%)
        │   ├── radioButtonGroup radioButtonGroupPaymentsType (property=typeCompanyComission)
        │   ├── checkBox checkBoxUseNDFL (property=useTaxNDFL)
        │   └── vbox companyPercentFieldsVBox
        │       ├── textField textFieldPercentOrSum (property=percentComissionOfCompany)
        │       └── textField textFieldCompanyPayment (расчётное, без dataContainer)
        ├── vbox researcherPaymentsVBox (width=33%)
        │   ├── radioButtonGroup radioButtonGroupResearcherSalary (property=typeSalaryOfRecrutier)
        │   └── vbox researcherPercentFieldsVBox
        │       ├── textField textFieldResearcherSalaryPercentOrSum (property=percentSalaryOfResearcher)
        │       └── textField textFieldResearcherSalary (расчётное)
        └── vbox recrutierPaymentsVBox
            ├── radioButtonGroup radioButtonGroupRecrutierSalary (property=typeSalaryOfResearcher)
            └── vbox recrutierPercentFieldsVBox
                ├── textField textFieldRecrutierPercentOrSum (property=percentSalaryOfRecrutier)
                └── textField textFieldRecrutierSalary (расчётное)
```

### Вкладка `tabJobDescription` («Описание должности», icon=DRIVERS_LICENSE)

```
tab tabJobDescription (expand=jobDescriptionVBox)
└── scrollBox jobDescriptionVBox
    ├── groupBox workExperienceGroupBox (collapsable, collapsed=true, caption=msgWorkExperience)
    │   ├── radioButtonGroup workExperienceRadioButton (required, orientation=horizontal)
    │   └── radioButtonGroup commanExperienceRadioButton (visible=false, orientation=horizontal)
    ├── hbox descriptionsAccordionHBox
    │   └── accordion openPositionAccordion
    │       ├── tab openPositionRuTabAccordion → richTextArea openPositionRichTextArea (required, 300px)
    │       ├── tab openPositionEnTabAccordion → richTextArea openPositionEnRichTextArea (300px)
    │       ├── tab openPositionStandartDescriptionAccorden → richTextArea openPositionStandartDescriptionRichTextArea (editable=false, 300px)
    │       └── tab openPositionWhoIsThisGuyAccorden → richTextArea openPositionWhoIsThisGuyRichTextArea (editable=false, 300px)
    └── hbox shortDescriptionHBox (expand=shortDescriptionTextArea)
        ├── textField shortDescriptionTextArea (maxLength=250)
        └── button scanJDButton (invoke=addShortDescription)
```

### Вкладки-контейнеры (остальные)

| Вкладка | id | Контейнер | Содержимое |
|---|---|---|---|
| Файлы | `tabFiles` | `someFilesVbox` (expand=someFilesTable) | `table someFilesTable` (300px, columns fileDescription / fileType.nameFileType / fileDescriptor.size / fileOwner.name; actions add/create/edit/remove; buttonsPanel `someFilesButtonsPanel` → someFilesCreateBtn/someFilesEditBtn/someFilesRemoveBtn) |
| Тестовое задание | `tabExercise` | `exerciseVbox` (expand=exerciseRichTextArea) | `checkBox needExerciseCheckBox` + `richTextArea exerciseRichTextArea` |
| Памятка | `tabMemoForInterview` | `memoVbox` | `checkBox needMemoCheckBox` + `richTextArea memoForInterviewRichTextArea` |
| Шаблон письма | `tabTemplateLetter` | `templateLetterBox` | `checkBox needLetterCheckBox` + `richTextArea templateLetterRichTextArea` |
| Навыки | `tabSkills` | `skillsBox` (expand=openPositionSkillsListTable) | `button rescanSkills` (invoke=rescanJobDescription) + `treeDataGrid openPositionSkillsListTable` (hierarchyColumn=skillName, hierarchyProperty=skillTree; columns fileImageLogo (componentRenderer) / skillName / specialisation / wikiPage / isComment (iconRenderer); rowsCount; actions закомментированы) |
| Новости | `tabOpenPositionNews` | `openPostitonNewsBrowseBox` (expand=openPostionNewsDataGrid) | `dataGrid openPostionNewsDataGrid` (actions create/remove; columns dateNews 200px / subject / candidates / author 250px; buttonsPanel `openPositionNewsButtonsPanel` → addOpenPositionNewsButton (invoke) / removeOpenPositionNewsButton) + `checkBox priorityNewsCheckBox` |
| Согласование | `tabApproval` | `procActionsBox` (groupBox, width=AUTO) | `fragment procActionsFragment` (screen=bpm_ProcActionsFragment) |
| Комментарии | `commentsTab` | `commentsScrollBox` (scrollBox) | лента комментариев (наполняется программно) |

## 4. Данные: containers / loaders / facets

### Instance
| Container | Класс | View | Loader | Параметры |
|---|---|---|---|---|
| `openPositionDc` | OpenPosition | extends `openPosition-edit-view` | `openPositionDl` | — |

### Collections (вкладки)
| Container | Класс | View | Loader | JPQL-условие |
|---|---|---|---|---|
| `laborAgreementDc` | LaborAgreement | `laborAgreement-openPosition-tab-view` | `laborAgreementDl` | join openPositions, op = :openPosition |
| `commentsOpenPositionDc` | OpenPositionComment | `openPositionComment-edit-view` | `commentsOpenPositionDl` | e.openPosition = :openPosition, order by dateComment desc |
| `someFilesesDc` | SomeFilesOpenPosition | `someFilesOpenPosition-edit-view` | `someFilesesDl` | e.openPosition = :openPosition |
| `openPositionSkillsListsDc` | SkillTree | `skillTree-openPosition-tab-view` | `openPositionSkillsListsDl` | e.openPosition = :openPosition, order by skillName |
| `procAttachmentsDc` | bpm$ProcAttachment | `procAttachment-browse` | `procAttachmentsDl` (**cacheable**) | procInstance.entity.entityId = :entityId, order by createTs |
| `openPositionNewsDc` | OpenPositionNews | `openPositionNews-edit-view` | `openPositionNewsLc` (**cacheable**) | e.openPosition = :openPosition AND e.priorityNews = :priorityNews, order by dateNews desc |

### Collections (справочники / options)
| Container | Класс | View | Loader | JPQL-условие |
|---|---|---|---|---|
| `openPositionParentDc` | OpenPosition | extends `openPosition-picker-view` | `openPositionParentDl` (**cacheable**) | openClose=false, commandCandidate != 0 |
| `positionTypesDc` | Position | extends `position-picker-view` | `positionTypesLc` (**cacheable**) | без «(не использовать)» |
| `projectNamesDc` | Project | extends `project-picker-view` + nested (projectLogo _minimal, projectDepartment → companyDepartament-picker-view → companyName → company-picker-view → cityOfCompany _local, projectOwner → person-owner-view + nested) | `projectNamesLc` (**cacheable**) | projectIsClosed=false + фильтры (department, projectClosed, withOpenPosition) |
| `companyNamesDc` | Company | extends `company-picker-view` | `companyNamesLc` | order by comanyName |
| `companyDepartamentsDc` | CompanyDepartament | extends `companyDepartament-picker-view` + companyName→cityOfCompany | `companyDepartamentsLc` | без «(не использовать)», e.companyName = :company |
| `citiesDc` | City | `city-picker-view` | `citiesDl` (**cacheable**) | order by cityRuName |
| `gradeDc` | Grade | `grade-picker-view` | `gradeDl` (**cacheable**) | order by gradeName |

### Facets
- `closedVacancyTimer` — timer, delay=60000, autostart=false, repeating=true (обратный отсчёт автозакрытия; запускается из Java при наличии closingDate).

### Actions / invoke (полный перечень)
| Компонент | Тип | Действие |
|---|---|---|
| `generateVacancyNameFieldButton` | invoke | `generateNameFieldButton` |
| `addCity` | invoke | `addListCity` |
| `setSalaryFieldButton` | invoke | `setSalaryFieldButtonInvoke` |
| `scanJDButton` | invoke | `addShortDescription` |
| `rescanSkills` | invoke | `rescanJobDescription` |
| `addOpenPositionNewsButton` | invoke | `addOpenPositionNewsButton` |
| `subscribePositionButton` | invoke | `subscribePosition` |
| `windowCommitAndCloseButton` | action | `windowCommitAndClose` |
| `windowCloseButton` | action | `windowClose` |
| `laborAgreementDataGrid` | actions | create / edit / remove (кнопки createBtn/editBtn/removeBtn) |
| `someFilesTable` | actions | add / create / edit / remove (кнопки someFilesCreateBtn/someFilesEditBtn/someFilesRemoveBtn) |
| `openPostionNewsDataGrid` | actions | create / remove (кнопки addOpenPositionNewsButton / removeOpenPositionNewsButton) |
| `gradeLookupPickerField`, `positionTypeField`, `projectNameField`, `companyDepartamentField`, `companyNameField`, `cityOpenPositionField` | picker actions | lookup / open (внутренние actions picker-полей) |

## 5. Вкладки (12)

1. `tabOpenPosition` — «О вакансии» (USERS)
2. `laborAgreementTab` — «Трудовые соглашения» (CUBES)
3. `tabPayments` — «Оплата» (BULK_EDIT_ACTION, **visible=false**)
4. `tabJobDescription` — «Описание должности» (DRIVERS_LICENSE)
5. `tabFiles` — «Файлы» (FILE_O)
6. `tabExercise` — «Тестовое задание» (ADJUST)
7. `tabMemoForInterview` — «Памятка» (ADJUST)
8. `tabTemplateLetter` — «Шаблон письма» (font-icon:MAIL_FORWARD)
9. `tabSkills` — «Навыки» (FILTER)
10. `tabOpenPositionNews` — «Новости» (NEWSPAPER_O)
11. `tabApproval` — «Согласование» (NEWSPAPER_O)
12. `commentsTab` — «Комментарии» (STAR_O)

## 6. Java-контроллер: инъекции, подписки, lifecycle

### @Inject UI-компоненты (для component map — java_references)
`closedVacancyInfoLabel, closingDateDateField, cityOpenPositionField, companyDepartamentField, companyNameField, numberPositionField, positionTypeField, needExerciseCheckBox, exerciseRichTextArea, projectNameField, vacansyNameField, priorityField, openClosePositionCheckBox, radioButtonGroupPaymentsType, radioButtonGroupResearcherSalary, radioButtonGroupRecrutierSalary, groupBoxPaymentsDetail, groupBoxPaymentsResearcher, groupBoxPaymentsRecrutier, textFieldPercentOrSum, textFieldCompanyPayment, openPositionFieldSalaryMin, openPositionFieldSalaryMax, checkBoxUseNDFL, textFieldResearcherSalaryPercentOrSum, textFieldResearcherSalary, textFieldRecrutierPercentOrSum, textFieldRecrutierSalary, labelResearcherSalary, labelRecrutierSalary, remoteWorkField, labelOpenPosition, labelTopComissionResearcher, labelTopComissionRecrutier, workExperienceRadioButton, commanExperienceRadioButton, internalProjectCheckBox, commandOrPosition, parentOpenPositionField, citiesLabel, openPositionRichTextArea, openPositionSkillsListTable, shortDescriptionTextArea, openPositionStandartDescriptionRichTextArea, openPositionWhoIsThisGuyRichTextArea, needMemoCheckBox, registrationForWorkField, lastOpenVacancyDateField, openPostionNewsDataGrid, templateLetterRichTextArea, ownerTextField, signDraftCheckBox, signDraftLabel, procActionsFragment, gradeLookupPickerField, openPositionFieldSalaryIE, vacansyIDTextField, outstaffingCostTextField, salaryCommentTextFiels, commentsScrollBox, onlyOpenProjectCheckBox, withOpenPositionCheckBox, projectLogoImage, projectOwnerImage, tabSheetOpenPosition, openPositionDc, laborAgreementDc, laborAgreementDl, commentsOpenPositionDc, commentsOpenPositionDl, someFilesesDc, someFilesesDl, openPositionSkillsListsDc, openPositionSkillsListsDl, procAttachmentsDl, openPositionParentDc (не инжектится, только контейнер), positionTypesLc, projectNamesLc, companyDepartamentsLc, openPositionNewsLc, gradeDc, commentsScrollBox, closedVacancyTimer`.

### @Named (пути с вложенностью — критично при репарентинге)
- `@Named("tabSheetOpenPosition.tabPayments")` → `VBoxLayout tabPayments` — путь зависит от ID вкладки и TabSheet; вкладка обязана существовать с ID `tabPayments` внутри `tabSheetOpenPosition`.
- `@Named("openPositionAccordion.openPositionStandartDescriptionAccorden")` → `VBoxLayout` — путь зависит от Accordion ID и Tab ID.
- `@Named("openPositionAccordion.openPositionWhoIsThisGuyAccorden")` → `VBoxLayout`.

### @Subscribe (id → сигнатура)
| Target | Сигнатура |
|---|---|
| `closedVacancyTimer` | `Timer.TimerActionEvent` |
| `closingDateDateField` | `HasValue.ValueChangeEvent<Date>` |
| `tabSheetOpenPosition` | `TabSheet.SelectedTabChangeEvent` (lazy-загрузка вкладок) |
| `onlyOpenProjectCheckBox` | `HasValue.ValueChangeEvent<Boolean>` |
| `withOpenPositionCheckBox` | `HasValue.ValueChangeEvent<Boolean>` |
| `needExerciseCheckBox` | `HasValue.ValueChangeEvent<Boolean>` |
| `openPositionFieldSalaryMin` (×2) | `HasValue.ValueChangeEvent<BigDecimal>` |
| `openPositionFieldSalaryMax` (×2) | `HasValue.ValueChangeEvent<BigDecimal>` |
| `openPositionRichTextArea` (×2) | `HasValue.ValueChangeEvent<String>` |
| `templateLetterRichTextArea` | `HasValue.ValueChangeEvent<String>` |
| `vacansyNameField` | `HasValue.ValueChangeEvent<String>` |
| `companyDepartamentField` | `HasValue.ValueChangeEvent<CompanyDepartament>` |
| `companyNameField` | `HasValue.ValueChangeEvent<Company>` |
| `commandOrPosition` | `HasValue.ValueChangeEvent` |
| `parentOpenPositionField` | `HasValue.ValueChangeEvent<OpenPosition>` |
| `priorityNewsCheckBox` | `HasValue.ValueChangeEvent<Boolean>` |
| `openClosePositionCheckBox` | `HasValue.ValueChangeEvent<Boolean>` |
| `memoForInterviewRichTextArea` | `HasValue.ValueChangeEvent<String>` |
| `priorityField` | `HasValue.ValueChangeEvent<Integer>` |
| `radioButtonGroupPaymentsType` | `HasValue.ValueChangeEvent<Integer>` |
| `checkBoxUseNDFL` | `HasValue.ValueChangeEvent<Boolean>` |
| `textFieldPercentOrSum` | `HasValue.ValueChangeEvent<String>` |
| `radioButtonGroupResearcherSalary` | `HasValue.ValueChangeEvent` |
| `radioButtonGroupRecrutierSalary` | `HasValue.ValueChangeEvent` |
| `textFieldRecrutierPercentOrSum` | `HasValue.ValueChangeEvent<String>` |
| `textFieldResearcherSalaryPercentOrSum` | `HasValue.ValueChangeEvent<String>` |
| `textFieldRecrutierSalary` | `HasValue.ValueChangeEvent<String>` |
| `textFieldResearcherSalary` | `HasValue.ValueChangeEvent<String>` |
| `labelRecrutierSalary` | `HasValue.ValueChangeEvent<String>` |
| `labelResearcherSalary` | `HasValue.ValueChangeEvent<String>` |
| `projectNameField` (×2) | `HasValue.ValueChangeEvent<Project>` |
| `positionTypeField` (×2) | `HasValue.ValueChangeEvent<Position>` |
| `cityOpenPositionField` | `HasValue.ValueChangeEvent<City>` |
| `more10NumberPositionField` | `HasValue.ValueChangeEvent<Boolean>` |
| `signDraftCheckBox` | `HasValue.ValueChangeEvent<Boolean>` |
| `salaryCandidateRequestCheckBox` | `HasValue.ValueChangeEvent<Boolean>` |
| `gradeLookupPickerField` | `HasValue.ValueChangeEvent<Grade>` |
| `someFilesesDc` (Target.DATA_CONTAINER) | `CollectionContainer.CollectionChangeEvent<SomeFilesOpenPosition>` |
| Target.DATA_CONTEXT | `DataContext.ChangeEvent` |
| `@Subscribe` без id (lifecycle) | `InitEvent`, `BeforeShowEvent` (×2), `AfterShowEvent`, `AfterCommitChangesEvent` (×2), `BeforeCommitChangesEvent` (×4) |

### @Install
| Target | Subject |
|---|---|
| `someFilesTable.create` | `newEntitySupplier` |
| `openPostionNewsDataGrid` | `detailsGenerator` |
| `openPositionFieldSalaryMax` | `validator` |
| `openPositionFieldSalaryMin` | `validator` |
| `registrationForWorkField` | `optionIconProvider` / `optionStyleProvider` |
| `remoteWorkField` | `optionIconProvider` |
| `priorityField` | `optionIconProvider` |
| `openPositionSkillsListTable.isComment` | `columnGenerator` / `styleProvider` |
| `openPositionSkillsListTable` | `rowDescriptionProvider` |

### Lifecycle-ключевое
- `onBeforeShow`: загрузка LOB основной вкладки (`loadMainTabLobs`, `ensurePositionLobsLoaded`), `setTopLabel` (шапка: статус, комиссии), `setInternalProject`, `setHiddeField`, `setDisableTwoField`, каскады, `initProjectNameField`, approval-процесс.
- `onTabSheetOpenPositionSelectedTabChange`: lazy-загрузка по 8 флагам (`mainTabLobsLoaded, exerciseLoaded, memoLoaded, templateLetterLoaded, skillsLoaded, filesLoaded, commentsTabLoaded, laborAgreementLoaded`).
- `onAfterShow`: догрузка LOB, иконки файлов, новости, `screenFullyLoaded=true` (блокировка RichTextArea ValueChange до полной загрузки).
- Сохранение: валидация вилки зарплаты, `checkDuplicatePositionId`, уведомления email/Telegram, синхронизация дочерних позиций (`openCloseChildVacancy`), `notifyTelegramOpenPositionChange`.

## 7. Связанные сущности и справочники

- **Основная**: `OpenPosition` (StandardEntity; поля: openClose, rating, signDraft, lastOpenDate, vacansyName (not null, 250), vacansyID (16), grade (Grade), remoteWork (not null), registrationForWork, remoteComment (40), commandCandidate (not null), salaryMin/Max, salaryIE, salaryFixLimit, salaryCandidateRequest, salaryComment, outstaffingCost, cityPosition (City), cities (OneToMany CASCADE), positionType (Position), projectName (Project, optional=false), numberPosition, more10NumberPosition, workExperience (not null), commandExperience, comment/commentEn (Lob), shortDescription (250), templateLetter (Lob), needLetter, exercise (Lob), needExercise, priority, priorityComment, skillTree/навыки (ManyToMany), openPositionComments (OneToMany CASCADE), laborAgreement (ManyToMany), someFiles (OneToMany CASCADE), owner (ExtUser), closingDate (Date), parentOpenPosition (OpenPosition), needMemoForInterview, memoForInterview (Lob), paymentsType, typeCompanyComission, typeSalaryOfResearcher, typeSalaryOfRecrutier, useTaxNDFL, internalProject, percentComissionOfCompany (5), percentSalaryOfResearcher (5), percentSalaryOfRecrutier (5)).
- **Справочники (options)**: Position, Project, Company, CompanyDepartament, City, Grade, OpenPosition (родительские), LaborAgreement, SkillTree.
- **Связанные коллекции**: OpenPositionComment, SomeFilesOpenPosition, OpenPositionNews, bpm$ProcAttachment.
- **Сервисы (Java, вне визуального scope)**: TelegramService, ApplicationSetupService, TextManipulationService, GetRoleService, PdfParserService, StarsAndOtherService, OpenPositionService, DataManager, FileLoader.

## 8. Полный перечень запрещённых изменений (scope guard)

1. Бизнес-логика, entity `OpenPosition`, справочники, сервисы — НЕ менять.
2. Loaders, JPQL, views, `views.xml`, DataContext — НЕ менять.
3. Handlers, validators, actions, `invoke`, conditions — НЕ менять.
4. `required` / `visible` / `enabled` / `editable` / `readonly` — НЕ менять (включая `visible=false` у `tabPayments`, `openClosePositionCheckBox`, `internalProjectCheckBox`, `commanExperienceRadioButton`, `lastOpenVacancyDateField`).
5. Java (`OpenPositionEdit.java` — READ_ONLY; `ALLOWED_JAVA_FILES: NONE`) — НЕ менять.
6. Другие формы, глобальные стили (shared `edit-screen-shared-styles.scss`), меню, browse, route — НЕ менять.
7. БД, Liquibase, entities, views — НЕ менять.
8. Component ID, `dataContainer`, `property`, `optionsContainer`, captions (msg-ключи), action ID — НЕ менять.
9. Иконки вкладок, иконки кнопок, порядок вызова invoke — НЕ менять (иконки визуальны, но привязаны к бизнес-контракту кнопок; смена иконки допустима только как чисто визуальный tweak — в этом контракте не применяется).
10. `focusComponent` и `dialogMode` — НЕ менять без решения арбитра (см. §9; dialogMode — кандидат на арбитраж).

**Разрешено**: layout-контейнеры, перестановка компонентов между визуальными контейнерами, width/height/spacing/margin/expand/align, локальные stylename, локальный SCSS в namespace `.open-position-editor`, визуальное оформление TabSheet/таблиц/полей/toolbar/footer, label-навигация (общие классы), responsive, light/dark, theme-aware.

## 9. Наблюдения вне scope (OUT_OF_SCOPE)

- **OUT_OF_SCOPE-1 (арбитраж)**: `dialogMode height="800px" width="1100px"` — legacy-форма открывается в модальном диалоге 1100×800. Утверждённый рендер 1920×1080 подразумевает полноэкранную двухпанельную компоновку. При dialogMode 1100px после sidebar 270px останется ~800px workspace — полноэкранный вид рендера недостижим без изменения dialogMode/способа открытия. Изменение dialogMode не входит в перечень разрешённых визуальных правок → `ARBITRATION_REQUIRED` (подробнее в `01-ui-contract.md` §9).
- **OUT_OF_SCOPE-2 (арбитраж)**: рендер показывает вкладку «Оплата и контакты» как **видимую**, а legacy `tabPayments` имеет `visible="false"` и скрывается Java (`setHiddeField`, `disableEnableFields` по `commandCandidate`). Изменение `visible` запрещено → `DESIGN_REQUIRES_FORBIDDEN_FUNCTIONAL_CHANGE` (подробнее в `01-ui-contract.md` §9).
- **OUT_OF_SCOPE-3**: рендер показывает статус «• позиция открыта» в toolbar — в legacy нет компонента статуса в toolbar; `setTopLabel()` формирует HTML в `labelOpenPosition`/`labelTopComission*`. Дублирование статуса новым компонентом = создание нового компонента/значения → запрещено; визуально статус может быть представлен существующими label-компонентами (`signDraftLabel`, `labelOpenPosition`) без изменения их значений.
- **OUT_OF_SCOPE-4**: рендер показывает «+Добавить» и «Только открытые проекты / Только с открытыми вакансиями» — это существующие `addCity` и `onlyOpenProjectCheckBox`/`withOpenPositionCheckBox`; новые подписи/действия не создаются.
- **OUT_OF_SCOPE-5**: рендер 312px sidebar соответствует JobCandidateEdit (документированное исключение), а не контрактным 270/250px. Ширина sidebar — решение арбитра (см. `01-ui-contract.md` §4.2 и §9).
- **OUT_OF_SCOPE-6**: `OpenPositionEditPreview` (параллельный экран) не является заменой legacy-формы; его стили (`open-position-preview-*`, `job-candidate-tabs`) не переносятся как зависимость в legacy (запрещено §9 UI/UX-концепции: использование namespace другой формы).
- **OUT_OF_SCOPE-7**: `vacancyTitleSpacerHBox` — пустой legacy-спейсер; скрытие/удаление визуального спейсера допустимо, удаление самого компонента из XML — под вопрос (Java не инжектит его, но удаление меняет структуру; рекомендовано `visible=false`/SCSS-скрытие, не удаление).
- **OUT_OF_SCOPE-8**: Вкладка «Навыки» имеет закомментированные actions (add/edit/remove) — восстановление действий запрещено (функциональное изменение).
- **OUT_OF_SCOPE-9**: `lastOpenVacancyDateField` (visible=false) — техническое поле таймера автозакрытия; не трогать.

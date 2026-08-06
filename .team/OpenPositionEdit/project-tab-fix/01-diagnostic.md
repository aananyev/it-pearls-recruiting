# OpenPositionEdit — 01. Диагностика вкладки «Проект» (tabOpenPosition)

> Корректирующий этап: 5 визуальных дефектов вкладки «Проект» формы OpenPositionEdit.
> Роль субагента: **UI-контракт и диагностика** — только чтение кода, только запись файлов в `.team/OpenPositionEdit/project-tab-fix/`. Код приложения НЕ изменялся.
> Дата: 2026-08-05 · Ветка: `agent/open-position-edit-redesign` · HEAD: `2ff1f129ec1378c043293a8d7ba30f77316e0988` (= актуальный master). Все файлы прочитаны из РАБОЧЕГО ДЕРЕВА (незакоммиченные изменения редизайна учтены).

---

## 1. Прочитанные источники

| Файл | Строк | Что использовано |
|---|---|---|
| `modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit.xml` | 1559 | структура вкладки «Проект», sidebar-навигация, стили/ширины/expand |
| `modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEdit.java` | 3525 | Java-контроллер: обработчики вкладок, стили, ссылки на компоненты |
| `modules/web/src/com/company/hunttech/web/screens/openposition/OpenPositionEditPreview.java` | 616 | эталон активной label-навигации (`setNavigationActive` / `updateNavigationState`) |
| `modules/web/themes/hover/com.company.hunttech/open-position-editor.scss` | 1086 | локальный namespace `.open-position-editor` (7 копий идентичны, md5 `f4758dc4…`) |
| `modules/web/themes/hover/com.company.hunttech/edit-screen-shared-styles.scss` | 391 | общие классы `label-nav-*`, `edit-*` |
| `docs/architecture/HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md` | 602 | §3 label-навигация, §5.5 защита от переполнения, §10 запреты |
| `docs/ui/OpenPositionEdit_Spec.md` | 262 | спецификация редизайна (2026-08-05), §6.2 вкладка «О вакансии» |
| `modules/web/src/com/company/hunttech/web/screens/openposition/open-position-edit-preview.xml` | 568 | эталонная превью-форма (nav по вкладкам, invoke-обработчики) |
| `modules/core/test/com/company/hunttech/core/OpenPositionEditLayoutContractTest.java` | 323 | инварианты контрактного теста редизайна |
| `.team/OpenPositionEdit/01-ui-contract.md`, `06-arbitration.md` | — | контракт и арбитраж предыдущего этапа редизайна |

**Факт по сообщениям:** `msgNamePosition = «Проект»` (вкладка `tabOpenPosition` называется «Проект»), `msgGenerateName = «Генерировать»`, `msgVacansyID = «ID»`, `msgVacansy = «Вакансия»`, `msgGrade = «Грейд»`, `msgCommandOrVacancy = «Команда / Вакансия»`, `msgCountOfPersonel = «Количество персонала»`, `msgSalary = «Заработная плата»` (модуль `modules/web/src/com/company/hunttech/web/screens/openposition/messages.properties`).

---

## 2. Фактическое состояние: каркас формы (dialogMode 1400×900)

```
<layout stylename="open-position-editor" width=100% height=100% spacing=false expand=openPositionMainLayout>   (XML 251)
└── hbox openPositionMainLayout (edit-screen-layout open-position-editor, expand=openPositionWorkspace)        (257)
    ├── vbox openPositionSidebar (edit-sidebar, 270px; 250px при ≤1366px — из shared)                         (269)
    │   ├── vbox openPositionSidebarVisual (edit-sidebar-visual) — логотип/аватар/название/статус              (276)
    │   ├── vbox openPositionEditorNavigation (label-navigation)                                               (330)
    │   └── vbox openPositionEditorSummary (edit-sidebar-summary) + warning + spacer                          (371–404)
    └── vbox openPositionWorkspace (edit-workspace, expand=tabSheetOpenPosition)                               (407)
        ├── hbox openPositionToolbar (edit-toolbar)                                                            (414)
        ├── tabSheet tabSheetOpenPosition (framed edit-tabs open-position-editor-tabs, 12 вкладок)             (434)
        └── footer (edit-footer-actions)                                                                       (~1520)
```

---

## 3. Фактическое состояние: sidebar label-навигация (требование A)

Контейнер `openPositionEditorNavigation` (XML 330–369, `stylename="label-navigation"`), заголовок `openPositionEditorNavActiveSectionsLabel` («Разделы активной вкладки»). Пункты — borderless-кнопки **без `invoke`** (комментарий XML 328–329: «Кнопки без invoke (Java READ_ONLY) — только presentation, по §3.5 общего контракта»):

| # | id | caption | stylename в XML | строка XML |
|---|---|---|---|---|
| 1 | `openPositionEditorNavIdentifiers` | «Идентификаторы» | `borderless label-nav-item label-nav-item-active` | 340–343 |
| 2 | `openPositionEditorNavSettings` | «Настройки вакансии» | `borderless label-nav-item` | 345–348 |
| 3 | `openPositionEditorNavTeam` | «Команда / Вакансия» | `borderless label-nav-item` | 350–353 |
| 4 | `openPositionEditorNavProject` | «Проект и локация» | `borderless label-nav-item` | 355–358 |
| 5 | `openPositionEditorNavPersonnel` | «Количество персонала» | `borderless label-nav-item` | 360–363 |
| 6 | `openPositionEditorNavSalary` | «Заработная плата» | `borderless label-nav-item` | 365–368 |

### 3.1. ОТВЕТ ПО ПУНКТУ A: «active-state статический в XML»

**Факт: `label-nav-item-active` захардкожен в XML (строка 343) на пункте «Идентификаторы». Java-логика active-state ОТСУТСТВУЕТ.**

Проверка `OpenPositionEdit.java`:
- `@Subscribe("tabSheetOpenPosition") public void onTabSheetOpenPositionSelectedTabChange(SelectedTabChangeEvent event)` (строки 424–460) **существует**, но делает ТОЛЬКО lazy-загрузку LOB/коллекций (`loadExerciseLob`, `loadMemoForInterviewLob`, …). Никаких `addStyleName/removeStyleName/setStyleName` для nav-пунктов, никакого обращения к `openPositionEditorNav*` (поиск по всему репозиторию: `openPositionEditorNav*` встречается ТОЛЬКО в XML, в Java — ноль ссылок).
- В `OpenPositionEdit.java` нет `BASE_NAV_STYLE` / `ACTIVE_NAV_STYLE` / `setNavigationActive` / `updateNavigationState` / `initSidebar`.
- `setStyleName/addStyleName` в контроллере применяются только к несвязанным компонентам (иконки, `tailName`, `signDraftCheckBox` и т.п.) — к навигации отношения не имеют.
- По умолчанию открыта первая вкладка `tabOpenPosition` («Проект»); `setSelectedTab` в Java не вызывается.

**Эталонная реализация (существует в проекте!):** `OpenPositionEditPreview.java` (наследник `OpenPositionEdit`) содержит полный паттерн управления active-state:
- `BASE_NAV_STYLE = "label-nav-item"` (стр. 59), `ACTIVE_NAV_STYLE = "label-nav-item-active"` (стр. 60);
- nav — по ВКЛАДКАМ (12 пунктов, `previewNavMain` → `tabOpenPosition`, …), кнопки с `invoke="previewOpen*"` → `selectTab(tabId)` (стр. 533–584);
- `@Subscribe("tabSheetOpenPosition") onPreviewTabChanged` → `updateNavigationState(event.getSelectedTab().getName())` (стр. 526–529);
- `setNavigationActive(button, active)`: базовый `label-nav-item` не снимается, добавляется/удаляется **только** `label-nav-item-active` (стр. 609–615).

**ВЫВОД:** для синхронизации active-state с реально открытой вкладкой требуется Java-контроллер (расширение существующего `onTabSheetOpenPositionSelectedTabChange` по образцу `setNavigationActive` превью ИЛИ рекомпозиция набора навигации). Это изменение `OpenPositionEdit.java`, который по контракту предыдущего этапа — READ_ONLY. **Решение за арбитром → `ARBITRATION_REQUIRED` (см. 02-layout-contract.md §A).**

---

## 4. Фактическое состояние: вкладка «Проект» (tabOpenPosition) — полное layout-дерево

```
<tab id="tabOpenPosition" caption="msg://msgNamePosition" margin="true,true,true,true" spacing="true" icon="USERS">   (440)
└── scrollBox mainTabScrollBox (vertical, spacing=false, width=100%, height=100%, edit-workspace-scroll)              (446)
    └── vbox citiesLabelHBox (spacing=false, edit-workspace-content)                                                  (453)
        ├── hbox openPositionEditorCardsRow1 (spacing=true, width=100%, open-position-editor-cards-row)               (455)
        │   ├── groupBox openPositionEditorIdentifiersCard «Идентификаторы и статус»                                  (460)
        │   │   │   (collapsable=true, collapsed=false, showAsPanel=true, spacing=true, width=100%,
        │   │   │    edit-accordion-section open-position-editor-primary-section)
        │   │   ├── hbox vacancyNameHBox (spacing=true, width=100%, expand=vacansyNameField,                          (469)
        │   │   │   │   open-position-editor-field-row open-position-editor-row-title)
        │   │   │   ├── textField vacansyIDTextField        (box.expandRatio=1, width=100%, edit-form-control, caption «ID»)      (475)
        │   │   │   ├── textField vacansyNameField          (expand, box.expandRatio=8, width=100%, edit-form-control, required, caption «Вакансия») (483)
        │   │   │   ├── lookupPickerField gradeLookupPickerField (box.expandRatio=1, width=100%, edit-form-control, caption «Грейд», optionsContainer=gradeDc) (492)
        │   │   │   └── button generateVacancyNameFieldButton (width=AUTO, align=BOTTOM_RIGHT, caption «Генерировать», invoke=generateNameFieldButton) (506)
        │   │   ├── hbox vacancyTitleSpacerHBox (open-position-editor-spacer → display:none)                          (514)
        │   │   └── groupBox commandFieldHBox «Настройки вакансии» (edit-accordion-section open-position-editor-subsection) (517)
        │   │       └── hbox closingDateFieldsHBox: closingDateDateField, signDraftCheckBox, openClosePositionCheckBox(hidden), internalProjectCheckBox(hidden) (525)
        │   │       └── hbox priorityFieldsHBox (expand=commentPriority): priorityField, commentPriority              (565)
        │   └── groupBox commandOrVacancyGroupBox «Команда / Вакансия»                                                (592)
        │       │   (spacing=true, collapsable=true, collapsed=false, showAsPanel=true, width=100%, edit-accordion-section)
        │       └── hbox commandOrVacancyHBox (spacing=true, width=100%)   ← БЕЗ stylename field-row                  (600)
        │           ├── hbox commandOrPositionCellHBox (width=100%, colspan=1, open-position-editor-field-row)        (603)
        │           │   └── radioButtonGroup commandOrPosition (width=50%, orientation=horizontal, required, caption «Команда / Вакансия») (608)
        │           └── hbox parentPositionCellHBox (width=100%, colspan=1, open-position-editor-field-row)           (618)
        │               └── lookupPickerField parentOpenPositionField (width=100%, edit-form-control, caption «Команда», optionsContainer=openPositionParentDc) (623)
        ├── groupBox projectTypeGroupBox «Проект, Компания, Тип должности» (width=100%, edit-accordion-section open-position-editor-project-section) (636)
        │   ├── hbox hboxVacansy (field-row row-position): positionTypeField + hbox remoteWorkFieldsHBox (row-remote: remoteWorkField, remoteWorkCommentField) (645)
        │   ├── hbox hboxProject (field-row row-half): vbox projectFieldsVBox (projectNameField + projectFilterCheckBoxesHBox) + companyDepartamentField (690)
        │   └── hbox hboxCompany (field-row row-half): companyNameField + hbox cityFieldsHBox (field-row, expand=cityOpenPositionField: cityOpenPositionField + addCity) (740)
        └── hbox openPositionEditorCardsRow2 (spacing=true, width=100%, open-position-editor-cards-row)               (792)
            ├── groupBox personnelCountGroupBox «Количество персонала» (spacing=false, width=100%, edit-accordion-section) (797)
            │   └── hbox numberPositionHBox (field-row, align=TOP_RIGHT):                                            (805)
            │       ├── textField numberPositionField (width=100%, edit-form-control, required)                      (811)
            │       └── checkBox more10NumberPositionField (width=100%, caption «Более 10 открытых вакансий»)        (820)
            └── groupBox salaryGroupBox «Заработная плата» (spacing=false, width=100%, edit-accordion-section)        (829)
                ├── hbox hboxSalary (field-row row-salary):                                                          (838)
                │   ├── textField openPositionFieldSalaryMin (width=100%, edit-form-control)                         (843)
                │   ├── textField openPositionFieldSalaryMax (width=100%, edit-form-control, валидатор)              (850)
                │   ├── textField openPositionFieldSalaryIE (width=100%, edit-form-control)                          (857)
                │   └── checkBox salaryCandidateRequestCheckBox (width=AUTO)                                         (864)
                └── hbox space2Box (width=100%, height=100%, expand=salaryCommentTextFiels, field-row row-wide)      (872)
                    ├── textField salaryCommentTextFiels (width=100%, edit-form-control)                             (879)
                    └── checkBox salaryStrongLimitCheckBox (align=BOTTOM_RIGHT)                                      (886)
```

---

## 5. Существующие общие и локальные классы (инвентарь)

### 5.1. Shared (7 идентичных копий `edit-screen-shared-styles.scss`, слой до локального)
- `label-navigation` (71–81), `label-nav-title` (83–96), `label-nav-item` / `.v-button-label-nav-item` (98–123), `.v-button-wrap` flex-центрирование (125–132), `.v-button-caption` (134–140), hover (142–148), `label-nav-item-active` (150–157) — геометрия: width 100%, min-height 24px, padding 3px 10px, font 13px/600, border-left 3px transparent; active: `$v-selection-color` + `rgba(...,0.08)` + левая граница.
- `edit-workspace` (159–162), `edit-workspace-scroll` (164–168: width/height 100%, **overflow-x: hidden !important**), `edit-workspace-content` (170–173), `edit-tabs` (208–219), `edit-accordion-section` (221–228, 341–364: caption 50px, content `overflow: visible !important`), `edit-form-control` (253–339), media ≤1366px sidebar 250px (383–390).

### 5.2. Локальный namespace `.open-position-editor` (7 идентичных копий `open-position-editor.scss`, md5 `f4758dc4…`; тест требует их идентичности)
- Тёмный sidebar `#172638→#132130→#0f1b28` (37–46), `.edit-sidebar > .v-slot` width 100% (48–53).
- Навигация: `.label-navigation` (185–188), `.label-nav-title` (190–200), `.label-nav-item` (202–228: min-height 27px, padding 3px 10px, font 13px/600, border-radius 0 5px 5px 0), hover (230–236: белый на `rgba(255,255,255,.08)`), active (238–244: `#ffb11b` на `rgba(255,177,27,.12)` + жёлтая border-left) — эталон IteractionListEdit 1:1.
- Tabs: tabcontainer (342–348), `.edit-tabs > .v-tabsheet-tabcontainer` overflow-x auto (350–353), tabitem 48px (369–377), caption 48px (383–398), content: `width 100%; height calc(100% - 49px); padding 14px 16px 18px; overflow: auto` (409–415), tabsheetpanel min-width 0 (417–421).
- `edit-workspace-scroll` (423–428: padding 14px 16px 22px, **overflow-x: hidden !important**), `edit-workspace-content` (430–436: width 100%, max-width 1480px, margin 0 auto).
- `edit-accordion-section` (442–…: margin-bottom 12px), `open-position-editor-primary-section` (473–475), `open-position-editor-subsection` (478–497).
- **`open-position-editor-cards-row` (499–533):** `display: flex !important; align-items: stretch; flex-wrap: wrap; gap: 14px; width 100% !important; height auto; margin: 0 0 12px`; `.v-spacing { display:none }`; `.v-slot { width: auto !important; min-width: 0 !important; max-width: 100% !important; flex: 1 1 420px }`; `.v-slot > * { width: 100% !important; min-width: 0; max-width: 100% }`.
- **`open-position-editor-field-row` (539–615):** `display:flex; align-items:flex-end; flex-wrap:wrap; gap:14px; width 100%`; `.v-slot { flex: 1 1 240px }`, `.v-slot > * { width:100% !important }`; **`.v-expand { width: 100% !important }` (585) — expand-ребёнок растягивается на всю строку, если вариант строки не задаёт flex**.
- Варианты строк: `row-title` (623–640: nth-child(1) → `flex: 0 1 130px`; nth-child(3)/.v-expand → `flex: 3 1 420px`; nth-child(5) → `flex: 1 1 320px`; **nth-child(7) → `min-width: 180px !important; flex: 0 0 auto` — мёртвые правила: в vacancyNameHBox 4 ребёнка, nth-child(5)/(7) не существуют**), `row-position` (642–648), `row-remote` (650–652), `row-half` (654–656), `row-salary` (658–660), `row-wide` (662–670).
- `open-position-editor-spacer` (783–786: display:none — vacancyTitleSpacerHBox скрыт).
- Запрещено тестом: вложенные `@media` в локальном SCSS (CUBA Sass), глобальные селекторы `.v-label {` и т.п., классы `open-position-preview-*` / `job-candidate-*`.

---

## 6. Найденные дефекты (сопоставление с требованиями A–E)

| Требование | Факт (корень проблемы) | Источник |
|---|---|---|
| **A** active-state | `label-nav-item-active` статичен в XML (стр. 343); Java active-state не управляет (обработчик 424–460 — только lazy-load). Пункты — section-level только для вкладки «Проект», без invoke; другие вкладки своего пункта не имеют; при переключении вкладок active не меняется. | XML 340–368, Java 424–460, grep по `openPositionEditorNav*` = только XML |
| **B** ID + «Команда / Вакансия» вертикально | Обе карточки `width="100%"` лежат в одном hbox `openPositionEditorCardsRow1`; вертикальность достигается только flex-wrap (SCSS 499–533, `flex: 1 1 420px`). При ширине диалога 1400px (workspace ≈ 1130px, контент ≈ 1066px) обе карточки встают РЯДОМ (≈ 526px каждая) → не «друг под другом», поля сжимаются. | XML 455–634; SCSS 499–533 |
| **C** выход блоков за границы Tab | Несколько источников горизонтального переполнения (см. §7). Ключевой: сумма flex-basis 4 полей `vacancyNameHBox` = 130+420+420+240+3×14 = **1252px > ~490px** внутренней ширины карточки → flex-wrap сбрасывает кнопку «Генерировать» на отдельную строку и растягивает её на 100% (`width:100% !important` из 571–575); при других ширинах строка переносит иначе (недетерминированно). Переполнение срезается `overflow-x: hidden` (SCSS 426, shared 167) → «блоки выходят за границы» визуально обрезаются. | XML 469–512; SCSS 539–640; shared 164–168 |
| **D** «Количество персонала» + «Зарплата» вертикально | Та же карточная пара в `openPositionEditorCardsRow2` (792–893) — рядом по flex; `hboxSalary` 4 поля `flex: 1 1 170px` (сумма 722px > ~490px) → перенос 3+1, поля узкие, captions переносятся. | XML 792–893; SCSS 658–660 |
| **E** (1) ID+Вакансия; (2) Грейд+«Генерировать» | Один hbox `vacancyNameHBox` содержит ВСЕ 4 компонента (ID, Вакансия, Грейд, кнопка). Существующие row-title-правила рассчитаны на 4 слота: ID `0 1 130px`, Вакансия (expand) `3 1 420px`, Грейд (nth-child(3)) `3 1 420px`, кнопка — generic `1 1 240px` + `width:100% !important` (кнопка НЕ компактная). Точки переноса зависят от ширины — недетерминированно. Требуются две ЯВНЫЕ строки. | XML 469–512; SCSS 623–640, 571–575 |

---

## 7. Источник переполнения Tab — детальный разбор (требование C)

Геометрия: dialogMode 1400×900 (XML 249–250) → workspace ≈ 1400−270(sidebar) = 1130px → tabsheet content padding 16×2 (SCSS 412) → scrollBox ≈ 1098px → scrollBox padding 16×2 (SCSS 425) → контент `citiesLabelHBox` ≈ **1066px** (max-width 1480 не ограничивает).

| # | Источник | Механизм | Результат |
|---|---|---|---|
| C1 | `openPositionEditorCardsRow1` / `Row2`: два `groupBox width="100%"` в одном hbox | Без CSS-обёртки (display:flex, 499–533) это 200% ширины; CSS спасает, но slot `flex: 1 1 420px` при контенте > min-content карточки (≈526px) переполняется | при узких местах — обрезка содержимого по краю карточки/скролла |
| C2 | `vacancyNameHBox`: 4 компонента, сумма min flex-basis ≈ 1252px > ~490px | flex-wrap: перенос кнопки на отдельную строку; кнопка растягивается `width:100% !important` (571–575); при других ширинах перенос идёт по-другому | недетерминированная компоновка; кнопка некомпактная; при min-content > свободного места — горизонтальный выход, срезанный `overflow-x:hidden` |
| C3 | `hboxSalary`: 4 поля `flex: 1 1 170px`, сумма 722px > ~490px (карточка в паре) | перенос 3+1; поля ≤ 110px с длинными captions | тесная, рваная строка; при сужении — выход за границу |
| C4 | `commandOrVacancyHBox` — **обычный hbox БЕЗ stylename `open-position-editor-field-row`** (XML 600) | два вложенных hbox `width="100%"` без flex-CSS; Vaadin делит 50/50, сжатие ниже min-content невозможно | переполнение/сжатие при длинных подписях; «Команда / Вакансия» radio width=50% внутри cell |
| C5 | `.v-expand { width: 100% !important }` (SCSS 585) | generic-правило для expand-ребёнка любой field-row; для `cityFieldsHBox` (без row-варианта) поле города занимает всю строку, кнопка `addCity` переносится вниз | компоновка «город + кнопка под ним» — существующее поведение, не дефект, но источник неоднородности |
| C6 | Мёртвые правила `row-title > .v-slot:nth-child(5)` (flex 1 1 320px) и `nth-child(7)` (min-width 180px) (SCSS 633–640) | в vacancyNameHBox 4 слота; правила не срабатывают | отсутствие эффекта сейчас; при изменении числа слотов — сюрпризы |
| C7 | Таб-контент: `margin="true,true,true,true"` + `spacing="true"` на `<tab>` (XML 442–443) | отступы 6px вокруг scrollBox; единственный ребёнок — влияния на overflow нет | безвредно, сохранить |
| C8 | `edit-workspace-scroll { overflow-x: hidden !important }` (shared 167 + локальный 426) | любой горизонтальный выход (C1–C4) НЕ показывает скролл, а обрезается | видимый дефект «блоки выходят за границы» |

**Главный вывод по C:** переполнение создаётся парами карточек 100%+100% в одном hbox (C1) и многоколоночными строками с суммой flex-basis > доступной ширины (C2, C3) при фиксированном диалоге 1400px. Лечение: все секции вкладки — на всю ширину (вертикальный поток, требования B и D), а титульные строки — детерминированные две строки (E). Тогда минимальная сумма basis строк ≤ ~1200px < ~1000px доступной ширины полной карточки — переполнение устраняется структурно, а не маскируется.

---

## 8. Java-привязки компонентов вкладки «Проект» (что НЕЛЬЗЯ трогать)

| Компонент | Привязки в `OpenPositionEdit.java` |
|---|---|
| `vacansyIDTextField` | `@Inject` (338); `String vacancyID = vacansyIDTextField.getValue();` (1928, метод генерации названия) |
| `vacansyNameField` | `@Inject` (159); `@Subscribe("vacansyNameField")` (1453); чтения (1620–1653) |
| `gradeLookupPickerField` | `@Inject` (332); `@Subscribe("gradeLookupPickerField")` (3336); чтения (3386–3389) |
| `generateVacancyNameFieldButton` | id в Java НЕ используется; связь только через `invoke="generateNameFieldButton"` → метод `generateNameFieldButton()` (3367) |
| `commandOrPosition` | `@Inject` (250); `@Subscribe("commandOrPosition")` (1555); `setOptionsMap` (2813); чтение (401) |
| `parentOpenPositionField` | `@Inject` (252); `@Subscribe("parentOpenPositionField")` (1578); `setEditable` (401, 1560, 1568) |
| `numberPositionField` | `@Inject` (149); **`setCaption("Количество персонала"/"Количество команд")` (1565, 1573)**; `setEditable` (1724, 1733); `setRequired(false)` (3283) |
| `more10NumberPositionField` | `@Subscribe("more10NumberPositionField")` (3278) |
| `openPositionFieldSalaryMin/Max/IE` | `@Inject` (206/208/336); подписки (1319, 1347 validator, 3328/3332 setEnabled, 3507 setValue) |
| `salaryCandidateRequestCheckBox` | `@Subscribe` (3322) |
| `salaryCommentTextFiels` | `@Inject` (342); чтение (1661); `setValue` (3508) |
| `salaryStrongLimitCheckBox` | **ссылок в Java нет** (только XML) |
| `projectNameField`, `companyNameField`, `companyDepartamentField`, `cityOpenPositionField`, `positionTypeField`, `remoteWorkField`, `remoteWorkCommentField`, `closingDateDateField`, `priorityField`, `commentPriority`, `signDraftCheckBox`, `openClosePositionCheckBox`, `internalProjectCheckBox`, `onlyOpenProjectCheckBox`, `withOpenPositionCheckBox` | `@Inject`/`@Subscribe`/`@Install` — id-привязки; переродитель не влияет (привязка по id) |
| Контейнеры `openPositionEditorCardsRow1/2`, `commandOrVacancyGroupBox`, `openPositionEditorIdentifiersCard`, `personnelCountGroupBox`, `salaryGroupBox`, `projectTypeGroupBox`, `commandOrVacancyHBox`, `vacancyNameHBox`, `commandFieldHBox`, `hboxSalary`, `space2Box`, `mainTabScrollBox`, `citiesLabelHBox` | **в Java НЕ инжектятся и не упоминаются** → свободно переставляются/рестайлятся |

**Инварианты привязок:** component ID, `dataContainer`, `property`, `optionsContainer`, `invoke`, `required`, `visible`, `enabled`, `editable`, валидаторы — не меняются (предыдущий контракт §1.2, спецификация §9). Captions — msg-ключи не менять; `numberPositionField` caption переопределяется Java динамически.

---

## 9. Инварианты контрактного теста (что фиксирует `OpenPositionEditLayoutContractTest`)

1. XML парсится; содержит роли: `edit-screen-layout`, `open-position-editor`, `edit-sidebar*`, `label-navigation`, `label-nav-title`, `label-nav-item`, `label-nav-item-active`, `edit-workspace`, `edit-toolbar*`, `edit-tabs`, `edit-form-control`, `edit-accordion-section`, `edit-footer-actions` (46–87).
2. Порядок sidebar → workspace; `dialogMode height="900px" width="1400px"` (82–86).
3. `edit-form-control` — НЕПОСРЕДСТВЕННО в теге 35 полей, включая `vacansyIDTextField`, `vacansyNameField`, `gradeLookupPickerField`, `parentOpenPositionField`, `positionTypeField`, `remoteWorkField`, `remoteWorkCommentField`, `projectNameField`, `companyDepartamentField`, `companyNameField`, `cityOpenPositionField`, `numberPositionField`, `openPositionFieldSalaryMin/Max/IE`, `salaryCommentTextFiels` (90–135). **При разбиении строк stylename полей не переносится на контейнер.**
4. Секции `openPositionEditorIdentifiersCard`, `commandFieldHBox`, `commandOrVacancyGroupBox`, `projectTypeGroupBox`, `personnelCountGroupBox`, `salaryGroupBox` — `edit-accordion-section` + `showAsPanel="true"` + `collapsable="true"` (+ `collapsed="false"`), `groupBoxPaymentsDetail` collapsed=true (138–182). **id секций и их collapsable-атрибуты сохраняются при переродителе.**
5. `tabPayments` остаётся `visible="false"`, пустой, платёжные секции внутри laborAgreementTab (185–206).
6. 7 копий локального SCSS идентичны; порядок import/include в 7 темах; в локальном SCSS: `@mixin open-position-editor-theme`, `.open-position-editor {`, `#172638`, `#ffb11b`, `.label-nav-item-active`, `rgba(255,255,255,.08)`, `rgba(255,177,27,.12)`, `-table-variant5`, `-richtext-variant5`, `-footer-actions`; **нет вложенных `@media`** (209–250).
7. Нет глобальных селекторов `.v-label {`/`.v-button {`/… (253–271).
8. **`OpenPositionEdit.java` не содержит строк `open-position-editor`, `edit-footer-actions`, `edit-accordion-section`** (274–289) — любые Java-правки в рамках A не должны добавлять эти строки (поиск по `label-nav-item` в Java тест не запрещает, но Java-изменения в целом = ARBITRATION_REQUIRED). В XML/SCSS не появляются `open-position-preview-*`, `job-candidate-*`.

---

## 10. Что НЕ является дефектом (зафиксировано, не трогать)

- `cityFieldsHBox` (город + `addCity` под ним) — существующее поведение expand-правила (C5); в 5 требований не входит.
- `projectTypeGroupBox` — уже полной ширины; его внутренние row-варианты (position/remote/half) работают корректно.
- `tabPayments` скрыта; платёжные секции во вкладке «Трудовой договор» — вне объёма этапа.
- Мёртвые `nth-child(5)/(7)` row-title — безвредны сейчас, но при реструктуризации E их нужно переписать (иначе станут «живыми» для новой структуры строк).

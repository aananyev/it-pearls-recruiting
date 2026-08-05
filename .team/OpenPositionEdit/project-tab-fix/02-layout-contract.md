# OpenPositionEdit — 02. Layout-контракт корректирующего этапа: вкладка «Проект» (tabOpenPosition)

> Роль субагента: **UI-контракт и диагностика**. Целевая схема 5 исправлений (A–E) для вкладки «Проект».
> База: общий контракт `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md` (§3 label-навигация, §5.5 защита от переполнения, §10 запреты),
> спецификация `docs/ui/OpenPositionEdit_Spec.md`, инварианты `OpenPositionEditLayoutContractTest`, артефакты предыдущего этапа `.team/OpenPositionEdit/`.
> Ограничения: Java `OpenPositionEdit.java` READ_ONLY (до арбитража по A); XML/локальный SCSS менять можно только в рамках visual-контракта; 7 копий SCSS синхронно.

---

## 0. Неизменяемые инварианты (жёсткие)

1. **Component ID, `dataContainer`, `property`, `optionsContainer`, `invoke`, actions, `required`, `visible`, `enabled`, `editable`, валидаторы, msg-ключи captions — НЕ меняются** (предыдущий контракт §1.2; спецификация §9).
2. **Секции сохраняют**: `edit-accordion-section` + `showAsPanel="true"` + `collapsable="true"`/`collapsed` — в своём теге (тест 138–182). Полю не переносить `edit-form-control` на контейнер (тест 90–135).
3. **Java**: по требованию A разрешена ТОЛЬКО минимальная визуальная синхронизация active-state (вердикт `00-arbitration-sidebar-active.md`, CONDITIONALLY_ALLOWED): 6 `@Named`-инъекций nav-кнопок + обновление `label-nav-item-active` в существующем обработчике `onTabSheetOpenPositionSelectedTabChange`; lazy-загрузку, бизнес-логику, invoke НЕ трогать; при добавлении кода запрещены строки `open-position-editor`, `edit-footer-actions`, `edit-accordion-section` (тест 281–283). Всё остальное в Java — без изменений.
4. Локальный SCSS: только namespace `.open-position-editor`, без вложенных `@media`, без глобальных селекторов, без `open-position-preview-*`/`job-candidate-*`; сохранить маркеры теста (`#172638`, `#ffb11b`, `.label-nav-item-active`, `rgba(255,177,27,.12)`, `rgba(255,255,255,.08)`, `-table-variant5`, `-richtext-variant5`, `-footer-actions`, `@mixin open-position-editor-theme`); все 7 копий правятся синхронно (тест 209–250).
5. `tabPayments` остаётся `visible="false"`; вкладок 12, их id/caption/иконки/порядок не меняются.
6. Разрешено: перестановка компонентов между визуальными контейнерами, новые визуальные контейнеры, width/height/spacing/margin/expand/align, `box.expandRatio`, локальные stylename, локальный SCSS.
7. Диалог 1400×900 — неизменен; sidebar 270/250px — из shared, не переопределять.

---

## A. Синхронизация активного пункта sidebar (требование A)

### A.1. Факты (из 01-diagnostic.md §3)
- Active-state **статический** в XML: `openPositionEditorNavIdentifiers` имеет `label-nav-item-active` (XML 343). Java active-state не управляет: `onTabSheetOpenPositionSelectedTabChange` (Java 424–460) делает только lazy-load; `openPositionEditorNav*` в Java не упоминаются вовсе.
- Пункты навигации — section-level (6 секций вкладки «Проект»), без `invoke`; пунктов других вкладок нет.
- Эталон уже реализован в `OpenPositionEditPreview.java`: `setNavigationActive(button, active)` (609–615) — добавляет/удаляет **только** `label-nav-item-active` при неизменном `label-nav-item`; вызывается из `@Subscribe("tabSheetOpenPosition")` (526–529) через `updateNavigationState(tabName)` (586–599). Пункты превью — tab-level с `invoke="previewOpen*"`.

### A.2. ARBITRATION RESOLVED (вердикт от 2026-08-05, файл `00-arbitration-sidebar-active.md` в этой папке)
Синхронизация active-state **разрешена минимальной Java-правкой** `OpenPositionEdit.java` (CONDITIONALLY_ALLOWED, одобрено пользователем):
1. Добавить 6 `@Named`-инъекций кнопок: `openPositionEditorNavIdentifiers`, `openPositionEditorNavSettings`, `openPositionEditorNavTeam`, `openPositionEditorNavProject`, `openPositionEditorNavPersonnel`, `openPositionEditorNavSalary`.
2. В **существующем** обработчике `onTabSheetOpenPositionSelectedTabChange` (Java 424–460), НЕ создавая новый: по `event.getSelectedTab().getName()` снять `label-nav-item-active` со всех 6 кнопок и установить его на пункт, соответствующий активной вкладке. Логику lazy-загрузки (if-блоки `loadExerciseLob` и т.д.) НЕ трогать.
3. `invoke` на nav-кнопки НЕ добавлять; кнопки остаются визуальными указателями (клик не переключает вкладку).

**Окончательный маппинг «вкладка → активный пункт» (фиксирует субагент UI-контракта, как делегировано вердиктом):**
| Вкладка (tab id) | Активный пункт навигации |
|---|---|
| `tabOpenPosition` («Проект») | `openPositionEditorNavIdentifiers` (первый пункт набора — секции вкладки «Проект») |
| `laborAgreementTab`, `tabJobDescription`, `tabFiles`, `tabExercise`, `tabMemoForInterview`, `tabTemplateLetter`, `tabSkills`, `tabOpenPositionNews`, `tabApproval`, `commentsTab`, скрытая `tabPayments` | **ни один пункт не активен** (набор описывает только секции вкладки «Проект»; active снимается со всех 6) |
- Это удовлетворяет требованию A в минимальной трактовке: для «Проект» есть свой пункт, при уходе с вкладки активное выделение исчезает, при возврате — восстанавливается, одновременно активен ≤ 1 пункт, состояние не статично.
- Расширение «свой пункт для каждой вкладки» (tab-level, 12 пунктов как в эталоне preview) — ВЫХОДИТ за рамки вердикта; не реализовывать без отдельного решения.

### A.3. Целевой контракт (обязателен при реализации Java-синхронизации)
- Классы: строго `label-nav-item` (базовый, в XML у всех 6, не снимается) + `label-nav-item-active` (единственный управляемый state-класс) — §3.2/§3.4 общего контракта; локальных аналогов не создавать.
- В Java использовать только `addStyleName("label-nav-item-active")` / `removeStyleName("label-nav-item-active")`; НЕ добавлять строки `open-position-editor`, `edit-footer-actions`, `edit-accordion-section` (инвариант теста 281–283 — строка `label-nav-item-active` тестом не запрещена).
- XML: статический `label-nav-item-active` на `openPositionEditorNavIdentifiers` остаётся как начальное состояние (форма открывается на вкладке «Проект»); Java поддерживает актуальность при каждом переключении.
- active-state не меняет высоту/ширину/padding пунктов (SCSS shared 150–157 + локальный 238–244 не трогать).
- Кнопки остаются borderless; `invoke` не добавлять.

---

## B. «Идентификаторы и статус» + «Команда / Вакансия» — вертикально, на всю ширину

### B.1. Цель
`openPositionEditorIdentifiersCard` (блок ID) — сверху, `commandOrVacancyGroupBox` («Команда / Вакансия», в задании — «Команда разработчиков») — ниже, оба `width=100%` родителя (`citiesLabelHBox`), стандартный вертикальный отступ.

### B.2. Целевое дерево (вариант 1 — CSS-only, РЕКОМЕНДОВАН)
```
vbox citiesLabelHBox (edit-workspace-content)
├── hbox openPositionEditorCardsRow1 (open-position-editor-cards-row, flex-direction: COLUMN)   ← CSS-изменение
│   ├── groupBox openPositionEditorIdentifiersCard (100%, сверху)
│   └── groupBox commandOrVacancyGroupBox (100%, ниже)
├── groupBox projectTypeGroupBox (100%)
└── hbox openPositionEditorCardsRow2 (open-position-editor-cards-row, flex-direction: COLUMN)   ← CSS-изменение (для D)
```
XML не меняется (ид/родители сохраняются — максимально безопасно для теста и Java). Изменяется блок `.open-position-editor-cards-row` в локальном SCSS:
- `flex-direction: column !important;` (либо `flex-wrap: nowrap` + принудительный перенос);
- `.v-slot { flex: 0 0 auto !important; width: 100% !important; }` (вместо `1 1 420px`);
- `gap: 14px` остаётся (вертикальный отступ 14px; стандартный отступ карточек `edit-accordion-section { margin-bottom: 12px }` — оба в допустимом диапазоне 12–14px);
- `.v-slot > * { width: 100% !important }` уже есть — карточки полной ширины.

### B.3. Вариант 2 — XML-переродитель (альтернатива, если CSS-only недостаточно)
`commandOrVacancyGroupBox` переносится прямым ребёнком `citiesLabelHBox` сразу после `openPositionEditorCardsRow1` (или row1 удаляется). Разрешено (контейнер не инжектится в Java; тест не проверяет id рядов). Требует аккуратности с `spacing` vbox: добавить вертикальный отступ 12–14px (margin группы или spacing).

### B.4. Приёмка
- Обе карточки занимают 100% ширины контента (~1066px) всегда (в т.ч. при 1366px и меньше);
- порядок: ID сверху, «Команда / Вакансия» снизу;
- вертикальный отступ между карточками 12–14px;
- содержимое карточек не выходит за границы (см. C).

---

## C. Устранение выхода блоков за границы родительского Tab

### C.1. Правила (единый контракт для всей вкладки «Проект»)
1. **Ни один блок вкладки не может иметь сумму горизонтальных размеров > ширины контента.** Контент ≈ 1066px (1400−270−16×2−16×2). После B и D все секции — полной ширины: внутренние строки получают ~1000px и их суммы basis (≤ ~1250px у строки title — см. E, ~722px у salary) перестают переполняться структурно.
2. Сохранить на пути контента: `box-sizing: border-box; min-width: 0; max-width: 100%` (shared 17–20 + локальный 28–30; у `citiesLabelHBox` width=100% уже задаётся CSS `.edit-workspace-content`).
3. `edit-workspace-scroll { overflow-x: hidden !important }` — оставить как страховку (shared 167, локальный 426); горизонтальный скролл НЕ вводить.
4. **Переписать мёртвые правила row-title** (SCSS 633–640): `nth-child(5)` и `nth-child(7)` удалить или переориентировать на новую структуру E — иначе при разбиении строк они начнут применяться не к тем слотам.
5. `commandOrVacancyHBox` (XML 600) — обычный hbox без field-row: при варианте 1 (CSS-column) оставить как есть (две ячейки 50/50 через `colspan`/width), ПРИ УСЛОВИИ что карточка полной ширины даёт ячейкам ≥ ~480px каждая; при меньших ширинах (≤1366px) добавить ячейкам `open-position-editor-field-row`-подобный wrap (локальный класс) либо `flex-wrap` — решает имплементер, контракт: **никакая ячейка не обрезается и не выходит за карточку**.
6. `radioButtonGroup commandOrPosition width="50%"` (XML 608–616) — при полной ширине карточки 50% ячейки достаточно; не менять `width` без необходимости (опции горизонтальные).
7. Убрать риск двойного отступа: `<tab margin="true" spacing="true">` (XML 442–443) — единственный ребёнок scrollBox, оставить без изменений.
8. Запрещены фиксированные ширины > ширины контента, `min-width` на блоках, отрицательные margin (общий контракт §5.5, §10).

### C.2. Проверочный список (сопоставление с источниками 01-diagnostic.md §7)
- C1: карточные ряды → вертикальный поток (B/D) — устранено;
- C2: титульная строка → две детерминированные строки (E) — устранено;
- C3: salary-строка → карточка полной ширины (D) — устранено;
- C4: `commandOrVacancyHBox` — карточка полной ширины + wrap-страховка ячеек;
- C5/C7/C8: без изменений (существующее поведение/страховки).

---

## D. «Количество персонала» + «Заработная плата» — вертикально, на всю ширину

### D.1. Целевое дерево
```
vbox citiesLabelHBox
└── hbox openPositionEditorCardsRow2 (open-position-editor-cards-row, flex-direction: COLUMN)
    ├── groupBox personnelCountGroupBox «Количество персонала» (100%)
    │   └── hbox numberPositionHBox (field-row): numberPositionField (растёт) + more10NumberPositionField
    └── groupBox salaryGroupBox «Заработная плата» (100%)
        ├── hbox hboxSalary (field-row row-salary): salaryMin / salaryMax / salaryIE / salaryCandidateRequestCheckBox
        └── hbox space2Box (field-row row-wide): salaryCommentTextFiels (растёт) + salaryStrongLimitCheckBox
```
Реализуется тем же CSS-изменением `open-position-editor-cards-row` (см. B.2) — обе строки карточек становятся колонками; XML не меняется.

### D.2. Поведение после исправления
- `personnelCountGroupBox` — сверху, `salaryGroupBox` — ниже, 100% ширины, отступ 12–14px;
- `hboxSalary` при ширине карточки ~1000px: 4 поля `flex: 1 1 170px` (сумма 722px) помещаются в одну строку без переноса; checkbox «Ориентируемся на запрос кандидата» не сжимает соседние поля (существующие row-salary-правила 658–660 сохраняются);
- `space2Box` — row-wide (662–670): комментарий растягивается, чекбокс «Фиксированный лимит» компактный справа;
- `numberPositionField` caption динамический из Java (1565/1573) — не трогать; `more10NumberPositionField` — существующий стиль.

---

## E. Две детерминированные горизонтальные строки: (1) ID + Вакансия; (2) Грейд + «Генерировать»

### E.1. Целевое дерево (требует XML-реструктуризации `vacancyNameHBox` + SCSS)
```
groupBox openPositionEditorIdentifiersCard
├── hbox vacancyNameHBox (open-position-editor-field-row open-position-editor-row-title, expand=vacansyNameField)
│   ├── textField vacansyIDTextField      (ID — ОГРАНИЧЕН: flex 0 1 130px, box.expandRatio малый)
│   └── textField vacansyNameField        (Вакансия — РАСТЯГИВАЕТСЯ: expand, flex 3 1 420px, required)
├── hbox gradeActionRowHBox (НОВЫЙ id визуального контейнера, open-position-editor-field-row open-position-editor-row-grade, expand=gradeLookupPickerField)
│   ├── lookupPickerField gradeLookupPickerField   (Грейд — РАСТЯГИВАЕТСЯ: expand, flex 3 1 420px, edit-form-control)
│   └── button generateVacancyNameFieldButton      («Генерировать» — КОМПАКТНАЯ: flex 0 0 auto, width=AUTO, align=BOTTOM_RIGHT, invoke сохраняется)
├── hbox vacancyTitleSpacerHBox (spacer, display:none — сохранить)
└── groupBox commandFieldHBox «Настройки вакансии» (без изменений)
```

### E.2. Правила
- **ID `vacansyIDTextField`**: ограничен по ширине (`flex: 0 1 130px`; `box.expandRatio` 1→малый или без изменения — решает имплементер в рамках visual); id/dataContainer/property/`edit-form-control` не меняются (тест 90–135, Java 338/1928).
- **`vacansyNameField`**: остаётся `expand`-ребёнком первой строки (Java 1453 — подписка по id, привязка не зависит от родителя).
- **`gradeLookupPickerField`**: переносится во вторую строку (родитель меняется, id не меняется — Java 332/3336/3386–3389 привязки по id сохраняются).
- **`generateVacancyNameFieldButton`**: переносится во вторую строку; `invoke="generateNameFieldButton"` (метод Java 3367) сохраняется; компактность — через SCSS (ниже), XML `width="AUTO"` уже задан (506–511).
- **Новый контейнер** `gradeActionRowHBox` — визуальный, без Java-привязок; id уникален в рамках экрана.
- **SCSS (локальный, namespace `.open-position-editor`)**: переписать блок `row-title` (623–640):
  - `> .v-slot:nth-child(1)` → `flex: 0 1 130px` (ID — сохранить существующее правило);
  - `> .v-expand, > .v-expand > .v-slot` → `flex: 3 1 420px` (Вакансия — сохранить);
  - удалить `> .v-slot:nth-child(3)` (Грейд уходит из первой строки), `nth-child(5)` и `nth-child(7)` (мёртвые);
  - новый вариант `open-position-editor-row-grade`: `> .v-expand, > .v-expand > .v-slot { flex: 3 1 420px; }` (Грейд) и `> .v-slot:last-child { flex: 0 0 auto !important; min-width: 0; }` (кнопка);
  - **компактная кнопка**: переопределить принудительное `width:100% !important` (общее правило 571–575) для кнопок строк: `.open-position-editor-field-row .v-button { width: auto !important; }` (либо `.open-position-editor-row-grade > .v-slot:last-child > * { width: auto !important; }`) — точечно, без глобального `.v-button`.
- **Responsive**: при сужении (≤1366px) строка 2 переносит кнопку под Грейд (flex-wrap уже задан 539–547); строка 1 — ID+Вакансия неразрывно в одну строку (ID 130px + Вакансия min ≥ 240px).

---

## F. Целевое полное дерево вкладки «Проект» ПОСЛЕ исправлений (A–E)

```
tab tabOpenPosition «Проект» (margin/spacing — без изменений)
└── scrollBox mainTabScrollBox (edit-workspace-scroll, overflow-x hidden)
    └── vbox citiesLabelHBox (edit-workspace-content, width 100%, max-width 1480)
        ├── hbox openPositionEditorCardsRow1 (cards-row, COLUMN)                        [B]
        │   ├── groupBox openPositionEditorIdentifiersCard (100%)                       [B]
        │   │   ├── hbox vacancyNameHBox (field-row row-title, expand=vacansyNameField) [E]
        │   │   │   ├── vacansyIDTextField        (ограничен ~130px)                    [E]
        │   │   │   └── vacansyNameField          (растягивается, required)             [E]
        │   │   ├── hbox gradeActionRowHBox (field-row row-grade, expand=gradeLookupPickerField) [E]
        │   │   │   ├── gradeLookupPickerField    (растягивается)                       [E]
        │   │   │   └── generateVacancyNameFieldButton (компактная, AUTO)               [E]
        │   │   ├── vacancyTitleSpacerHBox (spacer, скрыт)
        │   │   └── groupBox commandFieldHBox «Настройки вакансии» (subsection)
        │   └── groupBox commandOrVacancyGroupBox «Команда / Вакансия» (100%)           [B]
        │       └── commandOrVacancyHBox: commandOrPosition (radio 50%) + parentOpenPositionField
        ├── groupBox projectTypeGroupBox «Проект, Компания, Тип должности» (100%, без изменений)
        └── hbox openPositionEditorCardsRow2 (cards-row, COLUMN)                        [D]
            ├── groupBox personnelCountGroupBox «Количество персонала» (100%)           [D]
            │   └── numberPositionHBox: numberPositionField + more10NumberPositionField
            └── groupBox salaryGroupBox «Заработная плата» (100%)                       [D]
                ├── hboxSalary (row-salary): salaryMin, salaryMax, salaryIE, salaryCandidateRequestCheckBox
                └── space2Box (row-wide): salaryCommentTextFiels + salaryStrongLimitCheckBox
```
Sidebar `openPositionEditorNavigation` — по вердикту арбитра A (без изменений, либо Java-синхронизация active-state).

---

## G. Сводка изменений (граница «что и где правится»)

| Область | Файл | Изменения |
|---|---|---|
| XML | `open-position-edit.xml` | Только E: новый контейнер `gradeActionRowHBox`, перенос `gradeLookupPickerField` + `generateVacancyNameFieldButton` во вторую строку. B/D: без изменений (CSS-only). A: XML без изменений (статический `label-nav-item-active` остаётся начальным состоянием; синхронизация — Java по вердикту). |
| SCSS (7 копий синхронно) | `*/com.company.hunttech/open-position-editor.scss` | B/D: `open-position-editor-cards-row` → `flex-direction: column`, слоты `flex: 0 0 auto; width 100%`. E: переписать `row-title` (удалить nth-child(3)/(5)/(7)), добавить `row-grade` (Грейд expand, кнопка `0 0 auto`), компактная кнопка в field-row. C: ничего нового (правила уже есть), только переписать мёртвые селекторы. |
| Java | `OpenPositionEdit.java` | Только по вердикту A (00-arbitration-sidebar-active.md): 6 `@Named`-инъекций + синхронизация `label-nav-item-active` в существующем `onTabSheetOpenPositionSelectedTabChange` (Java 424–460); lazy-load и бизнес-логика не трогаются. |
| Shared SCSS | `edit-screen-shared-styles.scss` | НЕ меняется (7 копий, вне объёма). |
| Превью/другие формы | — | НЕ меняются. |
| Тест | `OpenPositionEditLayoutContractTest` | Должен остаться зелёным (см. 01-diagnostic.md §9). При добавлении Java по A — проверить тест 274–289 (запрещённые строки). |

## H. Responsive-поведение (сводка)
- **>1366px (диалог 1400×900)**: sidebar 270px; контент ~1066px; все секции полной ширины; строки: title (ID 130px + Вакансия), grade (Грейд + кнопка), salary (4 поля в одну строку) — без переносов.
- **≤1366px (sidebar 250px из shared)**: контент ~1090px; пары карточек уже вертикальны (B/D); переносы только внутри строк полей (flex-wrap 539–547), кнопка «Генерировать» может уйти под Грейд — допустимо.
- **Горизонтальный скролл** запрещён везде, кроме tab bar и таблиц (§6.7 спецификации); страховка `overflow-x: hidden` на scrollBox сохраняется.

## I. Критерии приёмки (сопоставление с требованиями A–E)
1. **A**: (по вердикту `00-arbitration-sidebar-active.md`) активный пункт sidebar соответствует открытой вкладке: на «Проект» — `openPositionEditorNavIdentifiers`; при уходе на другую вкладку активный пункт снимается; при возврате — восстанавливается; одновременно активен ≤ 1 пункт; base `label-nav-item` не снимается; lazy-загрузка вкладок не затронута.
2. **B**: ID-карточка сверху, «Команда / Вакансия» снизу, обе 100% ширины, отступ 12–14px — при 1400px и 1366px.
3. **C**: ни один блок/поле не выходит за границы Tab; отсутствует горизонтальная прокрутка контента; срезы `overflow-x: hidden` не обрезают видимый контент.
4. **D**: «Количество персонала» сверху, «Заработная плата» снизу, обе 100%; salary-строка в одну линию без переноса.
5. **E**: строка 1 = ID (ограничен) + Вакансия (расширяется); строка 2 = Грейд (расширяется) + «Генерировать» (компактная); кнопка не растягивается на всю ширину.
6. Контрактный тест `OpenPositionEditLayoutContractTest` — все 9 тестов зелёные; visual smoke на одной теме обязателен (общий контракт §10).

## J. Арбитраж по A — РАЗРЕШЁН (00-arbitration-sidebar-active.md, 2026-08-05)
- **A**: вердикт CONDITIONALLY_ALLOWED — минимальная Java-синхронизация active-state (6 `@Named` + `label-nav-item-active` в существующем обработчике вкладок; без invoke; lazy-load не трогать). Окончательный маппинг «вкладка → пункт» зафиксирован в §A.2: `tabOpenPosition` → `openPositionEditorNavIdentifiers`, остальные вкладки — без активного пункта. Расширение набора до tab-level (12 пунктов, эталон preview) — вне рамок вердикта, не реализовывать.

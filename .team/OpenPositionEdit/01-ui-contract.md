# OpenPositionEdit — 01. UI-контракт визуального редизайна

> Целевой визуал: утверждённый рендер `docs/ui/images/OpenPositionEdit/01_open_position_tab_main_halo_1920x1080.png`.
> База: общий контракт `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md` (далее — «Контракт»),
> UI/UX-концепция `HRM_HuntTech_UI_UX_Design_Concept.md`, эталон реализации `OpenPositionEditPreview_Spec.md`.
> Статус: **проект контракта для арбитра**. Все отклонения от рендера, требующие функционального изменения, — в §9 «Требует арбитража».

Обязательное подтверждение по §0 Контракта:
> Обязательные UI-документы прочитаны. Общие edit-* и label-* stylename использованы преимущественно. Локальные отклонения перечислены и обоснованы. Бизнес- и CUBA-контракты формы сохранены.

---

## 1. Цели и границы

1. **Исключительно визуальный** редизайн legacy `OpenPositionEdit` в двухпанельную компоновку sidebar + workspace по общему UI API.
2. НЕ изменяются: Java (`OpenPositionEdit.java` READ_ONLY), entity, справочники, сервисы, loaders, JPQL, views, DataContext, handlers, validators, actions, `invoke`, conditions, `required/visible/enabled/editable/readonly`, component ID, `dataContainer`, `property`, `optionsContainer`, captions (msg-ключи), другие формы, глобальные стили, БД, Liquibase.
3. Разрешено: layout-контейнеры, перестановка компонентов между визуальными контейнерами, width/height/spacing/margin/expand/align, локальные stylename, локальный SCSS в namespace `.open-position-editor`, визуальное оформление TabSheet/таблиц/полей/toolbar/footer, label-навигация, responsive, light/dark, theme-aware.
4. Приоритет: общие классы `edit-*` / `label-*` из `edit-screen-shared-styles.scss`; локальные классы — только при отсутствии общего аналога (§2.5).

## 2. Принципы

1. Все 12 вкладок TabSheet сохраняются с прежними ID, captions, иконками и порядком (рендер подтверждает 12 вкладок).
2. TabSheet остаётся штатным `tabSheetOpenPosition`; lazy-загрузка вкладок (`onTabSheetOpenPositionSelectedTabChange`) не затрагивается — редизайн не меняет `*Loaded`-флаги и не запускает loaders.
3. Sidebar — постоянная контекстная область; поля в sidebar НЕ дублируют рабочую область (§4.1 Контракта). Значения sidebar формируются **существующими** компонентами (`labelOpenPosition`, `projectLogoImage`, `projectOwnerImage`, `signDraftLabel`, `closedVacancyInfoLabel`, `citiesLabel`, `labelTopComission*`), без создания новых label-значений.
4. label-навигация переключает только существующие вкладки/раскрывает секции текущей вкладки (§3.5 Контракта): пункты «РАЗДЕЛОВ АКТИВНОЙ ВКЛАДКИ» — borderless-кнопки с `invoke` на переключение TabSheet; изменения только presentation (добавление/удаление `label-nav-item-active`).
5. Состояния (focus/hover/read-only/disabled/required/validation, collapsed/expanded, длинные значения) оформляются единообразно (§5.6 Контракта); новых бизнес-состояний не создаётся.
6. Горизонтальная прокрутка основной формы — дефект (§5.3); исключение — tab bar (12 вкладок) и специализированные таблицы (§4.6 preview-спеки).
7. Все CSS-селекторы — внутри namespace `.open-position-editor`; неограниченные `.v-label/.v-button/.v-tabsheet/...` запрещены (§1.7, §6.3 Контракта).

## 3. Корневая структура (root layout)

```
<hbox width="100%" height="100%" expand="openPositionWorkspace"
      stylename="edit-screen-layout open-position-editor">     ← root namespace
    <vbox id="openPositionSidebar" width="270px" height="100%"
          stylename="edit-sidebar">                            ← ширина — см. §4.2 (арбитраж)
        <!-- edit-sidebar-visual → edit-sidebar-identity → label-navigation → edit-sidebar-summary → edit-sidebar-hint -->
    </vbox>
    <vbox id="openPositionWorkspace" width="100%" height="100%"
          stylename="edit-workspace">
        <!-- edit-toolbar → TabSheet edit-tabs → scroll edit-workspace-scroll/edit-workspace-content -->
        <!-- edit-footer-actions (вне scroll, низ рабочей области) -->
    </vbox>
</hbox>
```

Порядок sidebar по §4 Контракта (с учётом §4.1: «навигация располагается до детальных summary-блоков» и XML-примера §5.2 «visual → identity → label-navigation → summary → hint»):
**visual → identity (title/subtitle) → label-navigation → summary (контекст вакансии) → hint/warning → spacer** — точно соответствует рендеру (логотип → название → «РАЗДЕЛЫ АКТИВНОЙ ВКЛАДКИ» → «КОНТЕКСТ ВАКАНСИИ»).

## 4. Sidebar

### 4.1. Состав (существующие компоненты → общие роли)

| Роль | Общий stylename | Существующие компоненты (id сохраняется) |
|---|---|---|
| Визуальный образ | `edit-sidebar-visual` + локальный `open-position-editor-logo-box` | `projectLogoImage` (ovaFallbackImage 88×88, по эталону preview 88px; legacy 70×70 меняется только width/height в XML — разрешено), `projectOwnerImage` (70×70, остаётся вторичным/парным) |
| Наименование | `edit-sidebar-identity` → `edit-sidebar-title` + `edit-sidebar-subtitle` | `labelOpenPosition` (title; значение задаёт `setTopLabel()` — не менять), `signDraftLabel` (subtitle-статус «Черновик», значение задаёт Java) |
| Индекс разделов | `label-navigation`, `label-nav-title`, `label-nav-item`, `label-nav-item-active` | статические заголовки `label-nav-title` (новые label-компоненты навигации — допускаются, т.к. это визуальная навигация, не бизнес-значения) + borderless-кнопки пунктов |
| Сводка/контекст | `edit-sidebar-summary` | `citiesLabel` (города), `labelTopComissionRecrutier`, `labelTopComissionResearcher` (комиссии, htmlEnabled) — переносятся из шапки; значения не меняются |
| Предупреждение | `edit-sidebar-warning` | `closedVacancyInfoLabel` (обратный отсчёт автозакрытия; значение задаёт Java-таймер) |
| Подсказка | `edit-sidebar-hint` | не обязателен; при отсутствии — пропускается (§5.4 Контракта) |
| Служебный expand | `edit-sidebar-spacer` | пустой expand-компонент sidebar |

> Важно: `labelOpenPosition` и `labelTopComission*` имеют Java-инъекции (`@Inject Label<String> labelOpenPosition` и т.д.) — перенос в sidebar не меняет ID и Java-контракт. `labelOpenPosition` в legacy имеет `table-wordwrap`; в sidebar получает `edit-sidebar-title` (+ при необходимости локальный clamp, см. §2.5).

### 4.2. Ширина sidebar — расхождение с рендером (АРБИТРАЖ)

- Контракт §4.2: базовая 270px, при viewport ≤1366px — 250px (shared CSS фиксирует `270px !important` / media-правило 250px).
- Рендер 1920×1080: sidebar 312px (x 0–312) — ширина JobCandidateEdit (документированное исключение).
- Preview-форма зафиксировала 264px (собственное решение preview).
- **Рекомендация аналитика**: следовать Контракту — **270px / 250px (≤1366px)**. Обоснование: Контракт нормативен; JobCandidateEdit 312px — задокументированное исключение, а не стандарт; 270px сохраняет полезную ширину workspace в диалоге 1100px (см. §9-1) и не конфликтует с §4.2 («локальный экран не переопределяет ширину sidebar без зафиксированной в UI-спецификации причины»).
- **Решение**: `ARBITRATION_REQUIRED` — если арбитр утверждает рендер дословно (312px), то 312px фиксируется и в `.edit-sidebar`, и в `.v-slot-edit-sidebar` локально (правило §4.2: одинаковое значение корню и slot), с пометкой-исключением в UI-спецификации.

### 4.3. Фирменная палитра (эталон iteraction-list-visual-alignment.scss, §6.2 Контракта)

```scss
.edit-sidebar { background-color: #172638; background-image: linear-gradient(180deg, #172638 0%, #132130 56%, #0f1b28 100%); }
```
Текст: `#f8fafc`; вторичный `rgba(248,250,252,.62)`; акцент `#ffb11b`. Sidebar прокручивается вертикально внутри себя (overflow-y: auto, overflow-x: hidden), горизонтальная прокрутка запрещена (§4.1).

## 5. Label-навигация (единственное имя — §3.1 Контракта)

### 5.1. Состав «РАЗДЕЛЫ АКТИВНОЙ ВКЛАДКИ» (активная вкладка `tabOpenPosition`)

Пункты соответствуют существующим секциям вкладки «О вакансии» (рендер, y≈277):

| Пункт (рендер) | Секция вкладки | Механизм (§3.5) |
|---|---|---|
| Идентификаторы (активный по умолчанию) | `vacancyNameHBox` + `commandFieldHBox` («Идентификаторы и статус» / «Настройки вакансии») | раскрытие/фокус секции |
| Настройки вакансии | `commandFieldHBox` | раскрытие/фокус |
| Команда / Вакансия | `commandOrVacancyGroupBox` | раскрытие/фокус |
| Проект и локация | `projectTypeGroupBox` | раскрытие/фокус |
| Количество персонала | `personnelCountGroupBox` | раскрытие/фокус |
| Заработная плата | `salaryGroupBox` | раскрытие/фокус |

При переключении вкладки состав набора обновляется на релевантный (§3.4: «при переключении вкладки показывается релевантный label-navigation либо обновляется его состав»). Для остальных вкладок допускается минимальный набор (например, «Параметры и таблица» для договоров, «Тексты» для описания и т.п.) — состав фиксируется при реализации; пункты всегда переключают только presentation.

### 5.2. «КОНТЕКСТ ВАКАНСИИ» (summary-детализация)

Рендер (y≈533): HRM HuntTech, Разработка продуктов, ООО «ХантТек», Москва, полностью удалённо, 250 000–320 000 ₽, 3 200 ₽/ч, Владелец Алексей А., 12.08.2026.
- Значения формируются **существующими** компонентами/значениями сущности, без создания новых бизнес-значений: проект/департамент/компания/город — из `openPositionDc` (значения уже в edit-view), удалёнка — `remoteWork`, зарплата — `salaryMin/salaryMax/salaryIE`, владелец — `owner`, дата — `closingDate`.
- Реализация: `edit-sidebar-summary` + локальный класс `open-position-editor-summary-grid` (преобразование GridLayout в компактную сетку caption/value — по эталону preview `grid-template-columns: 66px minmax(0,1fr)`; обоснование локального — §2.5). Java не меняется: значения читаются из entity в XML-лейблах (static labels не создаются, либо создаются как чисто визуальные label, отображающие существующие свойства через dataContainer — допустимо, т.к. не меняет контракты).

### 5.3. Геометрия и состояния (эталон IteractionListEdit, §3.1 Контракта — 1:1)

- `label-nav-title`: 11px/700, uppercase, `rgba(248,250,252,.62)`, padding 6px 10px 8px.
- `label-nav-item`: 13px/600, `rgba(248,250,252,.82)`, `min-height` 27–32px, padding 3–6px 10px, `border-left: 3px solid transparent`, `border-radius: 0 5px 5px 0`.
- hover: белый `#ffffff` на `rgba(255,255,255,.08)`.
- active: `#ffb11b` на `rgba(255,177,27,.12)` + `border-left-color: #ffb11b`.
- Центрирование маркера: `.v-button-label-nav-item .v-button-wrap { display:flex; align-items:center; }` БЕЗ min-height/фиксированной высоты (правило §3.1) — не переопределять.
- Контроллер добавляет/удаляет **только** `label-nav-item-active`, базовый `label-nav-item` не снимается (§3.4).
- Запрещены локальные переопределения цвета/геометрии/active-state вне эталона (§3.1).

## 6. Рабочая область (workspace)

### 6.1. Структура

```
edit-workspace
├── hbox edit-toolbar (+ open-position-editor-toolbar-status)
│   ├── edit-toolbar-title        ← «Редактирование открытой позиции» (рендер, верх)
│   ├── edit-toolbar-description  ← статус «• позиция открыта» (рендер) — существующий signDraftLabel/статусный текст без новых бизнес-значений
│   └── edit-toolbar-actions      ← существующие действия шапки (при необходимости: labelTopComission*)
├── tabSheet tabSheetOpenPosition (stylename="framed edit-tabs open-position-editor-tabs")   ← 12 вкладок, 48px (§5.3 Контракта)
└── scrollBox openPositionWorkspaceScroll (stylename="edit-workspace-scroll")
    └── vbox openPositionWorkspaceContent (stylename="edit-workspace-content")  ← max-width 1480px, центрировано (эталон preview)
```

Footer (низ формы, вне scroll): `vbox forExpand` → `hbox statusHBox` (`ownerTextField`, editable=false) + `hbox editActions` — получают `edit-footer-actions` + локальный `open-position-editor-footer`; кнопки «Сохранить и закрыть» / «Закрыть» / «Подписаться» группируются справа, одинаковой высоты 40px (эталон preview §4.3). Action ID и порядок вызовов не меняются.

### 6.2. Секции вкладки «О вакансии» (карты groupBox → edit-accordion-section)

Рендер группирует поля в карточки: «Идентификаторы и статус» + «Команда / Вакансия» (2 колонки, y≈190); «Настройки вакансии»; «Проект, компания, должность и локация»; «Количество персонала» + «Заработная плата» (2 колонки).

| Legacy groupBox | Общий stylename | Целевая карточка (рендер) | Действие |
|---|---|---|---|
| `positionHeaderGroupBox` (шапка) | разбирается | sidebar + toolbar | репарентинг: `projectLogoImage`/`projectOwnerImage` → `edit-sidebar-visual`; `labelOpenPosition` → `edit-sidebar-identity`; `signDraftLabel` → identity/subtitle; `labelTopComission*` → `edit-sidebar-summary` или `edit-toolbar-actions`; `closedVacancyInfoLabel` → `edit-sidebar-warning` |
| `commandFieldHBox` | `edit-accordion-section` (+ локальный `open-position-editor-subsection` для вторичного уровня, по эталону preview) | «Настройки вакансии» | stylename + showAsPanel |
| `commandOrVacancyGroupBox` | `edit-accordion-section` | «Команда / Вакансия» | stylename + showAsPanel |
| `projectTypeGroupBox` | `edit-accordion-section` | «Проект, компания, должность и локация» | stylename + showAsPanel; поля остаются с прежними bindings |
| `personnelCountGroupBox` | `edit-accordion-section` | «Количество персонала» | stylename + showAsPanel |
| `salaryGroupBox` | `edit-accordion-section` | «Заработная плата» | stylename + showAsPanel |

Правила (эталон preview §4.4):
- каждый фактический `GroupBoxLayout` сохраняет ID, caption, `collapsable`, `collapsed`, `width`, `height`, `expand`; получает `edit-accordion-section` и `showAsPanel=true`; роли `light`/`edit-card` как замена accordion-контракту не используются.
- Каждое типовое поле получает `edit-form-control` **непосредственно** (§5.5 Контракта): TextField, TextArea, LookupField, LookupPickerField, SuggestionPickerField, DateField, RichTextArea. CheckBox/RadioButtonGroup/таблицы/action-кнопки — без механического класса.
- Поля выравниваются рядами (локальный `open-position-editor-field-row`): пары 50/50, полноширинные строки, `flex-wrap` по доступной ширине (эталон preview §4.5, без вложенных `@media` — старый Sass-компилятор CUBA 7.3 их не выводит).

### 6.3. Вкладки «Трудовые соглашения» и «Оплата»

- `laborAgreementTab`: `laborAgreementGroupBox` → `edit-accordion-section`; `outstaffParamsHBox` — параметры оформления (registrationForWork, outstaffingCost, setSalaryFieldButton); `laborAgreementDataGrid` получает локальный визуальный вариант плотной таблицы (эталон preview `open-position-preview-table-variant5`: theme-aware header, ритм строк 42px, ellipsis, единый editor, компактный buttonsPanel). Actions/columns/editorEnabled не меняются.
- `tabPayments` (visible=false): платёжные секции (`groupBoxPaymentsResearcher`, `groupBoxPaymentsRecrutier`, `groupBoxPaymentsDetail` + 3 колонки) визуально переносятся **внутрь** `laborAgreementTab` (паттерн preview — «Оплата компании → Оплата ресерчерам → Оплата рекрутерам»), а `tabPayments` остаётся существовать скрытой технической вкладкой (инвариант `@Named("tabSheetOpenPosition.tabPayments")`). **Внимание**: рендер показывает видимую вкладку «Оплата и контакты» — конфликт с `visible=false` → §9-2 (арбитраж). Реализация без арбитража: вкладка остаётся скрытой, платёжные поля — в договорном разделе.

### 6.4. Вкладки описаний/текстов/таблиц

- `tabJobDescription`: `workExperienceGroupBox` (collapsed, сохраняется), `openPositionAccordion` (4 richTextArea, `edit-form-control` + единый вариант RichTextArea: toolbar 42px, content min-height 220px, по эталону `open-position-preview-richtext-variant5`), `shortDescriptionTextArea` + `scanJDButton`.
- `tabFiles`: `someFilesTable` — визуальный вариант таблицы (как §6.3), columns/actions не меняются.
- `tabExercise`/`tabMemoForInterview`/`tabTemplateLetter`: checkBox + richTextArea, поля получают `edit-form-control`.
- `tabSkills`: `rescanSkills` + `openPositionSkillsListTable` (treeDataGrid, hierarchyColumn=skillName; визуально — вариант таблицы; actions закомментированы — не восстанавливать).
- `tabOpenPositionNews`: `openPostionNewsDataGrid` + `priorityNewsCheckBox` (вариант таблицы; actions create/remove не меняются).
- `tabApproval`: `procActionsBox` + `fragment procActionsFragment` (BPM) — визуально в `edit-accordion-section`.
- `commentsTab`: `commentsScrollBox` — стилизация ленты (спейсинг, карточки комментариев через общие классы; содержимое формирует Java — не менять).
- Вкладки, корневым содержимым которых является GroupBox, получают локальный `open-position-editor-group-tab` (резерв 44px под отрицательное смещение caption CUBA — дефект, подтверждённый в preview-истории; обоснование §2.5).

### 6.5. Toolbar и footer

- `edit-toolbar`: min-height 58px (§5.3), заголовок 19–20px/700, описание 12px; нижняя граница + лёгкая тень; `edit-toolbar-actions` — существующие действия шапки справа.
- `edit-footer-actions`: min-height 58px, кнопки справа; primary («Сохранить и закрыть», `windowCommitAndCloseButton`) — локальный `open-position-editor-primary-action` (акцент `$v-selection-color`, min-width 184px, 700); secondary («Закрыть», `windowCloseButton`) — `open-position-editor-secondary-action`; `subscribePositionButton` — в footer рядом (рендер: «Подписаться», «Сохранить и закрыть», «Закрыть»). Капшены кнопок не меняются (legacy: «Подписаться», windowCommitAndClose, windowClose — капшены из actions).

## 7. Таблица соответствия рендера (референс → реализация)

| Элемент рендера (1920×1080) | Реализуется компонентом | Действие |
|---|---|---|
| Заголовок «Редактирование открытой позиции» | toolbar: `edit-toolbar-title` (новый статический label) | создание визуального label (не бизнес-значение) |
| Статус «• позиция открыта» | `edit-toolbar-description` / `signDraftLabel` | визуальное представление существующего статуса без изменения значений |
| Tab bar (12 вкладок) | `tabSheetOpenPosition` + `edit-tabs` + `open-position-editor-tabs` | stylename; 48px; horizontal overflow tabcontainer, captions без ellipsis (эталон preview §3) |
| Логотип HT (y≈62) | `projectLogoImage` | репарентинг в `edit-sidebar-visual`; 88×88 |
| Название «senior java-разработчик» (y≈165) | `labelOpenPosition` | репарентинг в `edit-sidebar-identity` (title) |
| «РАЗДЕЛЫ АКТИВНОЙ ВКЛАДКИ» (y≈277) | `label-navigation` + `label-nav-title` + 6 пунктов | новые визуальные пункты навигации (borderless-кнопки, presentation-only) |
| «КОНТЕКСТ ВАКАНСИИ» (y≈533) | `edit-sidebar-summary` + `open-position-editor-summary-grid` | отображение существующих значений entity |
| «Идентификаторы и статус» + «Команда / Вакансия» (2 колонки) | `commandFieldHBox` + `commandOrVacancyGroupBox` → `edit-accordion-section` | перегруппировка в 2-колоночную сетку карточек (допустимая перестановка) |
| «Настройки вакансии» (дата закрытия, Приоритет *, комментарий, Черновик, Эксклюзивная) | `commandFieldHBox` поля | stylename; расположение полей |
| «Проект, компания, должность и локация» | `projectTypeGroupBox` поля | stylename; расположение полей; «Только открытые проекты / Только с открытыми вакансиями» = существующие checkBox |
| «Количество персонала» + «Заработная плата» (2 колонки) | `personnelCountGroupBox` + `salaryGroupBox` | перегруппировка в 2-колоночную сетку |
| «+Добавить» | `addCity` | существующая кнопка |
| Footer: «Алексей А. (OpenPosition owner): только чтение» | `ownerTextField` (editable=false) | stylename, перенос в footer |
| Кнопки «Подписаться», «Сохранить и закрыть», «Закрыть» | `subscribePositionButton`, `windowCommitAndCloseButton`, `windowCloseButton` | `edit-footer-actions` + локальные primary/secondary |

## 8. Responsive-правила

| Viewport | Sidebar | Workspace | Поведение |
|---|---|---|---|
| 1920×1080 | 270px (или 312px по решению арбитра, §4.2) | content max-width 1480px, центрирован | полный вид рендера |
| 1920×1200 | 270px | то же | — |
| 1680×1050 | 270px | то же | — |
| 1600×900 | 270px | то же | — |
| 1440×900 | 270px | то же | — |
| 1366×768 | **250px** (Контракт §4.2) | сжатие; пары 50/50 переходят в одну колонку при нехватке ширины | flex-wrap строк, без горизонтального скролла |
| 1280×800 | 250px | сжатие; секции в одну колонку | без горизонтального скролла |

- Адаптация выполняется flex-контейнерами и `flex-wrap` (без вложенных `@media` — ограничение CUBA Sass, эталон preview §4.5).
- Sidebar: вертикальный scroll внутри, горизонтальный запрещён (§4.1).
- Минимальная полезная ширина поля важнее формального числа колонок (§4 UI/UX-концепции).
- Хиты `min-width: 0; max-width: 100%; box-sizing: border-box` для всех контейнеров/slot/полей (§5.5 Контракта).

## 9. Требует арбитража

| № | Формулировка | Тип |
|---|---|---|
| 9-1 | `dialogMode height="800px" width="1100px"` не позволяет полноэкранный двухпанельный вид рендера 1920×1080 (после sidebar остаётся ~800px workspace). Изменение dialogMode/способа открытия не входит в разрешённый перечень визуальных правок. Рекомендация: открытие формы в полноэкранном режиме — отдельное решение арбитра (возможно, технически меняет вызов browse/route). | `ARBITRATION_REQUIRED` |
| 9-2 | Рендер показывает видимую вкладку «Оплата и контакты»; legacy `tabPayments` имеет `visible="false"` и управляется Java (`setHiddeField`, `disableEnableFields` по `commandCandidate`). Изменение `visible` запрещено. Рекомендация: до арбитража вкладка остаётся скрытой, платёжные секции визуально переносятся в `laborAgreementTab` (паттерн preview). | `DESIGN_REQUIRES_FORBIDDEN_FUNCTIONAL_CHANGE` |
| 9-3 | Ширина sidebar: рендер 312px vs Контракт 270/250px vs JobCandidateEdit 312px (исключение). Рекомендация: 270/250px по Контракту; 312px — только по явному решению арбитра с фиксацией в UI-спецификации. | `ARBITRATION_REQUIRED` |
| 9-4 | Рендер показывает status «• позиция открыта» в toolbar и статус «позиция открыта» — в legacy нет отдельного компонента статуса в toolbar; `setTopLabel()` пишет HTML в label-шапки. Новый label статуса — чисто визуальный (не бизнес-значение), допустим; дублирование значения открытости (openClose) в новом компоненте — вопрос арбитра (значение уже читается из entity). | `ARBITRATION_REQUIRED` |
| 9-5 | Рендер группирует «Идентификаторы и статус» рядом с «Команда / Вакансия» в 2 колонки — требует перестановки groupBox-секций внутри вкладки (разрешено) и возможно `collapsed`-состояния (сохраняется, т.к. collapsable/collapsed не меняются). Конфликта нет; пункт приведён для полноты. | — (без конфликта) |
| 9-6 | Рендер показывает 12 вкладок без вкладки «Оплата» отдельно (в списке «Оплата и контакты» присутствует) — дублирует 9-2. | см. 9-2 |

## 10. Измеримые критерии визуальной проверки (visual acceptance)

1. Ширина sidebar: 270px (>1366px) / 250px (≤1366px) — либо утверждённое арбитром значение, одинаковое для `.edit-sidebar` и `.v-slot-edit-sidebar`; workspace начинается строго после sidebar (нет наложения).
2. Toolbar: min-height ≥58px; заголовок виден целиком.
3. Tabs: высота строки 48px; все 12 captions видны без ellipsis (horizontal overflow tabcontainer); активная вкладка — акцент + нижняя граница.
4. Поля: min-height 38px, font 15px; picker-actions 38×38; фокус-кольцо 2px `rgba($v-selection-color,.38)`.
5. Карточки/аккордеоны: radius 7–8px; caption ≥44px; внутренний padding 16–22px; light shadow.
6. Label-навигация: `label-nav-title` 11px/700 uppercase; `label-nav-item` 13px/600; hover белый на rgba(255,255,255,.08); active `#ffb11b` на rgba(255,177,27,.12) + жёлтая border-left; вертикальное центрирование flex'ом без min-height wrap; active-state не меняет размеры соседних пунктов.
7. Sidebar: фирменный градиент #172638→#132130→#0f1b28; вертикальный scroll; отсутствие горизонтального скролла; дочерние блоки не выходят за правую границу.
8. Отсутствие горизонтального скролла основной формы на всех viewport'ах §8 (кроме tab bar и специализированных таблиц).
9. Отсутствие перекрытий: logo/title не пересекаются при первом открытии и после смены темы; caption-зоны полей не накладываются на input; footer-actions не перекрывают контент.
10. Семь тем (halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark): геометрия идентична; цвета/фон/selection адаптируются через переменные темы; dark sidebar стабилен.
11. Regression-правила §5.7 Контракта проверены на 1700×950, 1366×768, 1100×760.
12. `OpenPositionScreenDocumentationTest` и `ScreenViewIntegrityTest` остаются зелёными; `OpenPositionEdit.java` — без изменений (git diff по Java = пустой).

## 11. Локальные stylename (полный перечень с обоснованием)

Правило: локальный класс добавляется **рядом** с общим, только если общего аналога нет (§1.10, §5.1, §10 Контракта; §9 UI/UX-концепции).

| Локальный класс | Назначение | Почему нет общего аналога | Почему локальный | Подтверждение изоляции |
|---|---|---|---|---|
| `open-position-editor` | корневой namespace формы | root-класс экрана — всегда локальный (§1.5 Контракта) | обязателен по §1.5/§9 концепции | все селекторы внутри; не влияет на другие формы |
| `open-position-editor-logo-box` | visual-stage под `OvaFallbackImage` (резерв 96px, центрирование 88px логотипа) | `edit-sidebar-visual` не резервирует высоту под OvaFallbackImage (собственная реализация размера); дефект наезда подтверждён в preview-истории 2026-08-02 | уникальная геометрия OvaFallbackImage | селекторы внутри `.open-position-editor .edit-sidebar-visual` |
| `open-position-editor-title-clamp` | ограничение названия вакансии (2–4 строки, полный текст в tooltip) | `edit-sidebar-title` задаёт перенос, но не clamp | уникальная геометрия длинных названий вакансий | применяется к `labelOpenPosition` (Java-инъекция сохраняется) |
| `open-position-editor-tabs` | tab bar 12 вкладок: horizontal overflow, captions без ellipsis, 48px | `edit-tabs` задаёт 48px, но не управляет overflow/captions 12 вкладок | уникальная геометрия многокладочного tab bar | селекторы только `.edit-tabs` внутри namespace |
| `open-position-editor-field-row` | responsive flex-строки полей (wrap, gap 11px, min-width:0) | общего аналога в shared нет (только контейнерная геометрия) | уникальная responsive-геометрия строк | селекторы внутри namespace; поля сохраняют bindings |
| `open-position-editor-subsection` | вторичный уровень иерархии (вложенная секция внутри карточки, напр. `commandFieldHBox` внутри «Идентификаторов») | `edit-accordion-section` — полноширинный без вложенности (§5.4) | уникальный двухуровневый аккордеон | селекторы внутри namespace |
| `open-position-editor-group-tab` | резерв 44px под отрицательное смещение caption GroupBox-вкладок | общего класса нет; дефект CUBA подтверждён в preview-истории | уникальная коррекция CUBA | применяется к корневым vbox вкладок |
| `open-position-editor-summary-grid` | компактная сетка caption/value в `edit-sidebar-summary` (66px + minmax(0,1fr)) | `edit-sidebar-summary` не содержит grid-контракта | уникальная геометрия сводки | селекторы внутри namespace (эталон preview-sidebar-usability) |
| `open-position-editor-footer` | flex-контракт footer (кнопки справа, высота 40px, gap) | `edit-footer-actions` задаёт поверхность, но не flex-раскладку кнопок | уникальная раскладка трёх кнопок | селекторы внутри namespace |
| `open-position-editor-primary-action` | акцентная кнопка «Сохранить и закрыть» | в shared нет primary/secondary-контракта кнопок | уникальная семантика главного действия | применяется только к `windowCommitAndCloseButton` |
| `open-position-editor-secondary-action` | вторичная кнопка «Закрыть» | в shared нет secondary-контракта | уникальная семантика | применяется только к `windowCloseButton` |
| `open-position-editor-table-variant5` (опционально, имя уточняется при реализации) | плотная theme-aware таблица (header 42px, ритм строк, ellipsis, editor) | `edit-form-control` не покрывает таблицы | уникальная геометрия таблиц формы | селекторы внутри namespace (эталон preview variant5) |
| `open-position-editor-richtext-variant5` (опционально) | единая поверхность RichTextArea (toolbar 42px, content min-height 220px) | `edit-form-control` покрывает поля, но не RichTextArea-поверхность | уникальная геометрия редакторов | селекторы внутри namespace |

**Запрещено**: дублировать общие роли (`edit-sidebar`, `edit-toolbar`, `edit-form-control`, `label-navigation` и т.д.) локальными классами; использовать namespace `open-position-preview-*`, `job-candidate-*`, `iteraction-list-*` как зависимость (§10 Контракта, §9 концепции).

## 12. Порядок CSS-слоёв (7 тем)

```text
theme base → edit-screen-shared-styles → open-position-editor (screen-specific partial)
```

- Семь идентичных theme-local partial: `modules/web/themes/<theme>/com.company.hunttech/open-position-editor.scss` (§6.1 Контракта).
- Подключение в каждой теме: `@import "com.company.hunttech/open-position-editor";` + `@include open-position-editor-theme;` внутри root-класса темы, после shared mixin (§6.4).
- sha256-идентичность 7 копий + `buildScssThemes` — критерий приёмки (§9-15 Контракта).
- Shared `edit-screen-shared-styles.scss` НЕ изменяется.

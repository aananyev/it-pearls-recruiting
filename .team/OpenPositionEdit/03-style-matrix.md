# OpenPositionEdit — 03. Style Matrix (общие классы → локальные модификаторы)

> Таблица сопоставления визуальных ролей формы с классами общего UI API
> (`edit-screen-shared-styles.scss`) и обоснованными локальными модификаторами
> в namespace `.open-position-editor`.
> Источник контракта — разделы `HRM_HuntTech_Edit_Screen_Shared_Style_Contract.md` (далее «§»).
> Правило: общий класс задаёт стандартную роль; локальный класс — только уникальную геометрию (§5.1, §10).

## 1. Матрица

| Элемент | Общий класс | Источник контракта | Локальный модификатор | Обоснование |
|---|---|---|---|---|
| Корневая композиция | `edit-screen-layout` | §5.1, §5.2 | `open-position-editor` (root namespace) | root-класс формы всегда локальный (§1.5); все селекторы внутри |
| Sidebar | `edit-sidebar` (+ `.v-slot-edit-sidebar`) | §4, §4.2, §6.2 | — (фирменный градиент и ширина наследуются от shared; при решении арбитра 312px — локальное правило ширины, §4.2) | shared фиксирует 270/250px и поверхность; фирменная палитра #172638→#132130→#0f1b28 — по §6.2 |
| Sidebar visual | `edit-sidebar-visual` | §4 | `open-position-editor-logo-box` | OvaFallbackImage имеет собственную реализацию размера; visual-stage 96px исключает наезд на title (дефект подтверждён в preview-истории) |
| Identity | `edit-sidebar-identity` | §4 | — | shared задаёт контейнер; достаточно |
| Title | `edit-sidebar-title` | §4, §4.1 | `open-position-editor-title-clamp` | clamp названия вакансии (2–4 строки) + полный текст в tooltip; общий класс не ограничивает высоту |
| Subtitle | `edit-sidebar-subtitle` | §4, §4.1 | — | статус/вторичный контекст (`signDraftLabel`) |
| Summary | `edit-sidebar-summary` | §4, §4.1 | `open-position-editor-summary-grid` | компактная сетка caption/value (66px + minmax(0,1fr)) по эталону preview; в shared нет grid-контракта |
| Hint | `edit-sidebar-hint` | §4 | — | при отсутствии роли блок пропускается (§5.4) |
| Warning | `edit-sidebar-warning` | §4 | — | `closedVacancyInfoLabel`; shared задаёт поверхность |
| Spacer | `edit-sidebar-spacer` | §4 | — | служебный expand-компонент |
| Label-навигация (контейнер) | `label-navigation` | §3.1, §3.2 | — | единственное имя блока; локальные имена запрещены |
| Заголовок навигации | `label-nav-title` | §3.2 | — | 11px/700 uppercase, вторичный цвет |
| Пункт навигации | `label-nav-item` | §3.2, §3.1 | — | геометрия/цвета только shared, эталон IteractionListEdit 1:1 |
| Активный пункт | `label-nav-item-active` | §3.2, §3.4 | — | #ffb11b на rgba(255,177,27,.12) + жёлтая border-left; только совместно с `label-nav-item` |
| Workspace | `edit-workspace` | §5.1 | — | фон mix(app,panel,82%) из shared; theme-aware |
| Workspace scroll | `edit-workspace-scroll` | §5.1 | — | вертикальный scroll, overflow-x hidden |
| Workspace content | `edit-workspace-content` | §5.1 | — | поток карточек; локально: max-width 1480px, центрирование (эталон preview §4.3) — допускается в локальном SCSS как уникальная геометрия, без нового класса |
| Toolbar | `edit-toolbar` | §5.1, §5.3 | — | min-height 58px, поверхность, нижняя граница — shared |
| Toolbar title | `edit-toolbar-title` | §5.1 | — | 19–20px/700 |
| Toolbar description | `edit-toolbar-description` | §5.1 | — | статус «• позиция открыта» (существующее значение) |
| Toolbar actions | `edit-toolbar-actions` | §5.1 | — | существующие действия шапки (labelTopComission*) |
| Tabs | `edit-tabs` | §5.1, §5.3 | `open-position-editor-tabs` | tab bar 12 вкладок: 48px, horizontal overflow tabcontainer, captions без ellipsis — уникальная геометрия многокладочного TabSheet |
| Карточка | `edit-card` | §5.1, §5.4 | — | постоянно видимые тематические блоки (если понадобятся, напр. «Идентификаторы и статус»); radius 8px |
| Заголовок карточки | `edit-card-title` | §5.1 | — | — |
| Accordion-секция | `edit-accordion-section` | §5.1, §5.4 | `open-position-editor-subsection` (вложенный уровень), `open-position-editor-group-tab` (коррекция GroupBox-вкладок) | `edit-accordion-section` — полноширинный без вложенности (§5.4); subsection — вторичный уровень иерархии; group-tab — резерв 44px под отрицательное смещение caption CUBA (дефект подтверждён) |
| Поле формы | `edit-form-control` | §5.1, §5.5 | `open-position-editor-field-row` (строка полей) | 38px, font 15px, focus/read-only/disabled — shared; flex-wrap строк полей — уникальная responsive-геометрия |
| Пояснение | `edit-help` | §5.1 | — | краткий вторичный текст |
| Footer actions | `edit-footer-actions` | §5.1, §5.4 | `open-position-editor-footer`, `open-position-editor-primary-action`, `open-position-editor-secondary-action` | surface из shared; flex-раскладка трёх кнопок справа, высота 40px, акцент primary — уникальная семантика footer формы |
| Таблицы (dataGrid/table/treeDataGrid) | — | — | `open-position-editor-table-variant5` (имя уточняется при реализации) | общий класс для таблиц отсутствует; плотный theme-aware вариант по эталону preview (header 42px, ритм строк, ellipsis, editor) |
| RichTextArea | — | — | `open-position-editor-richtext-variant5` | общий класс не покрывает поверхность RichTextArea; единый toolbar/content-контракт по эталону preview |

## 2. Сводка локальных классов (11)

| Класс | Роль | Общий аналог | Причина локальности |
|---|---|---|---|
| `open-position-editor` | root namespace | — (root всегда локальный) | §1.5 Контракта, §9 концепции |
| `open-position-editor-logo-box` | visual-stage 96px под OvaFallbackImage | `edit-sidebar-visual` (не резервирует высоту) | собственная реализация размера OvaFallbackImage |
| `open-position-editor-title-clamp` | clamp названия (2–4 строки) + tooltip | `edit-sidebar-title` (не ограничивает высоту) | длинные названия вакансий |
| `open-position-editor-tabs` | tab bar 12 вкладок (overflow, без ellipsis) | `edit-tabs` (задаёт 48px, не overflow) | многокладочный TabSheet |
| `open-position-editor-field-row` | responsive flex-строки полей | — | общий класс строк отсутствует |
| `open-position-editor-subsection` | вложенный уровень аккордеона | `edit-accordion-section` (без вложенности) | двухуровневая иерархия секций |
| `open-position-editor-group-tab` | резерв 44px GroupBox-вкладок | — | дефект CUBA (отрицательное смещение caption) |
| `open-position-editor-summary-grid` | сетка caption/value в summary | `edit-sidebar-summary` (без grid-контракта) | компактная сводка контекста вакансии |
| `open-position-editor-footer` | flex-раскладка кнопок footer | `edit-footer-actions` (поверхность) | три кнопки: Подписаться / Сохранить и закрыть / Закрыть |
| `open-position-editor-primary-action` | акцентная кнопка save | — | семантика главного действия |
| `open-position-editor-secondary-action` | вторичная кнопка cancel | — | семантика отмены |

## 3. Гарантии изоляции

1. Все локальные правила — внутри `.open-position-editor { ... }`; неограниченные `.v-label`, `.v-button`, `.v-tabsheet`, `.v-textfield`, `.v-panel` запрещены (§1.7, §6.3).
2. Локальные классы не дублируют роли shared (`edit-toolbar`, `edit-card`, `edit-form-control`, `label-navigation` и т.д.) — они только добавляют уникальную геометрию рядом с общим классом (§5.1: `stylename="edit-form-control open-position-editor-field-row"`).
3. Запрещено использовать namespace других форм (`open-position-preview-*`, `job-candidate-*`, `iteraction-list-*`) как зависимость (§10 Контракта, §9 концепции).
4. Семь идентичных theme-local partial: `modules/web/themes/<theme>/com.company.hunttech/open-position-editor.scss`; подключение после shared (§6.1, §6.4); sha256-идентичность 7 копий; shared `edit-screen-shared-styles.scss` не изменяется.
5. Все селекторы таблиц/редакторов — внутри namespace (эталон preview: «CUBA table и editor styles на других экранах остаются без изменений»).

## 4. Измеряемые значения (источники)

| Метрика | Значение | Источник |
|---|---|---|
| Sidebar | 270px / 250px (≤1366px) | §4.2, shared CSS |
| Toolbar min-height | 58px | §5.3 |
| Tabs height | 48px | §5.3, §5 UI/UX-концепции |
| Поля min-height | 38px | §5.3, §5.5 |
| Поля font-size | 15px | §5.5 |
| Радиус карточек | 8px (7–8px) | §5.3 |
| Padding карточек | 16–22px | §5.3 |
| Accordion caption min-height | 44–50px | shared + preview |
| label-nav-title | 11px/700 uppercase | §3.1, shared |
| label-nav-item | 13px/600, min-height 24–32px | §3.1, shared + preview |
| Hover | белый на rgba(255,255,255,.08) | §3.1 |
| Active | #ffb11b на rgba(255,177,27,.12), border-left #ffb11b | §3.1 |
| Sidebar градиент | #172638 → #132130 → #0f1b28 | §6.2 |
| Workspace max-width | 1480px (центрировано) | эталон preview §4.3 |

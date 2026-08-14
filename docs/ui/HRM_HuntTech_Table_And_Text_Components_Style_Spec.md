# HRM HuntTech — Table & Text Components Style Spec

Спецификация визуального дизайна стандартных табличных и текстовых компонентов CUBA Platform 7.3 / Vaadin 8: **DataGrid**, **Table**, **TreeTable**, **TextArea**, **RichTextArea** — во всех темах HRM HuntTech.

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

Таблицы и текстовые поля — основная рабочая поверхность рекрутинговой системы: кандидаты, вакансии, взаимодействия, резюме и комментарии рекрутер просматривает и редактирует часами. Единый, спокойный и читаемый дизайн таблиц и полей снижает визуальный шум, ускоряет сканирование строк и уменьшает утомляемость при длительной работе. Дизайн реализован на уровне SCSS-токенов и не меняет поведение, данные и lifecycle компонентов.

### 2. UI Context & Navigation (UI Context & Navigation)

Компоненты встречаются на всех типах экранов:

- **DataGrid** — browse-экраны (JobCandidateBrowse, IteractionListBrowse, person-browse, simple-browse кандидатов), таблицы на edit-формах (ext-user-edit, project-edit, company-edit);
- **Table** — legacy-списки (iteraction-list-browse, company-edit, project-edit, person-browse, internal-emailer-template-browse);
- **TreeTable** — иерархический справочник взаимодействий (IteractionTreeBrowse, iteraction-tree-browse.xml);
- **TextArea** — многострочные поля описаний (company-edit, iteraction-edit, open-position-news-edit, sign-icons-edit, application-recruitment-edit);
- **RichTextArea** — HTML-редакторы документов (company-edit, company-departament-edit, skill-tree-edit, labor-agreement-edit, фрагмент onlyText).

Все темы используют один структурный SCSS-контракт; различия — только в дизайн-токенах.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие browse → таблица загружает данные по существующим loaders; дизайн не влияет на запросы, сортировку, выбор строк и пагинацию.
- Открытие edit → поля форм отображают сущность; дизайн полей не меняет validators, required, read-only и disabled-состояния.
- Переключение темы → компоненты перерисовываются по токенам текущей темы без перезагрузки экранов.

---

## 4. Матрица тем

| Тема | Тип | Статус регистрации (themeConfig) | Утверждённый вариант | Направление дизайна |
|---|---|---|---|---|
| `Halo` | светлая | зарегистрирована | **вариант 1** | светлый голубой, тонкие границы, чистый header, заметный focus |
| `havana` | светлая | зарегистрирована | по выбору дизайнера | классическая панельная: нейтральный серо-голубой, акцент #3d6285 |
| `helium` | светлая | зарегистрирована | по выбору дизайнера | фирменный голубой #197de1, выраженный selected |
| `hover` | светлая (дефолтная) | зарегистрирована | по выбору дизайнера | нейтральный белый, синий акцент #0c66e4, спокойный header |
| `hunttech-modern` | светлая | **не зарегистрирована** (директория существует) | по выбору дизайнера | нейтральный белый, красный акцент #c1211f, минимальная зебра |
| `hunttech-modern-light` | светлая | зарегистрирована | **вариант 3** | тёплый светло-серый, молочные поверхности, серо-бежевые границы |
| `hunttech-modern-dark` | тёмные компоненты | зарегистрирована | **вариант 5** | тёмная нейтральная поверхность, светлый текст, акцентная граница selected |

**Ограничение (зафиксировано при анализе):** палитра `hunttech-modern-dark` в `hunttech-modern-dark-defaults.scss` фактически светлая (копия light). В рамках задачи по варианту 5 тёмными выполнены компоненты (таблицы, деревья, поля, RichTextArea) через токены `$hrm-*`; общий фон темы не менялся — вне разрешённой области задачи. При необходимости полного тёмного режима темы — отдельная задача.

## 5. Дизайн-токены

Общие токены определяются в `<theme>-table-text-tokens.scss` и потребляются идентичным во всех темах mixin'ом `hrm-table-text-components` (`table-text-components.scss`):

```scss
// Табличные поверхности
$hrm-table-bg, $hrm-table-text, $hrm-table-border,
$hrm-table-header-bg, $hrm-table-header-text, $hrm-table-divider,
$hrm-table-hover, $hrm-table-selected, $hrm-table-selected-border,
$hrm-table-focus, $hrm-table-zebra, $hrm-table-sort, $hrm-table-resize

// Текстовые поля и редакторы
$hrm-editor-bg, $hrm-editor-text, $hrm-editor-border,
$hrm-editor-focus, $hrm-editor-invalid,
$hrm-editor-disabled-bg, $hrm-editor-readonly-bg, $hrm-editor-placeholder

// Скроллбары и toolbar RichTextArea
$hrm-scrollbar-track, $hrm-scrollbar-thumb,
$hrm-rich-toolbar-bg, $hrm-rich-toolbar-border
```

Ключевые значения утверждённых вариантов:

| Токен | Halo (вариант 1) | hunttech-modern-light (вариант 3) | hunttech-modern-dark (вариант 5) |
|---|---|---|---|
| `table-bg` | `#ffffff` | `#fbfaf8` (молочный) | `#232a33` (тёмный) |
| `table-text` | `#1d2b3a` | `#3d3a35` | `#dde4ec` |
| `table-border` | `#d3dee9` (голубоватый) | `#e2ddd4` (серо-бежевый) | `#39434f` |
| `table-header-bg` | `#f0f5fa` | `#f2efe9` | `#2b333d` |
| `table-selected-border` | `#7fa8d0` | `#c1211f` (акцент) | `#5c9dff` (акцент) |
| `table-focus` | `#2f6fb2` | `#a03a30` (тёплый) | `#6aa8ff` |
| `editor-focus` | `#2f6fb2` | `#a03a30` | `#6aa8ff` |
| `editor-invalid` | `#c0392b` | `#b3261e` | `#ff6b5e` |

Палитры всех 7 тем уникальны (проверяется контрактным тестом).

## 6. Состояния компонентов

| Состояние | Оформление |
|---|---|
| normal | фон `table-bg` / `editor-bg`, граница `table-border` / `editor-border`, скругление 8px (таблицы), 6px (поля) |
| hover | `table-hover` (слабее selected, без границы) |
| selected | `table-selected` + акцентная граница `selected-border` (inset 1px) — минимум два признака |
| focused | `table-focus` / `editor-focus` (inset 2px у строк/ячеек; ring 2px у полей) |
| editable | dataGrid editor-ячейки — фон `editor-bg`, рамка `editor-border` |
| invalid | `editor-invalid` (граница + мягкое кольцо rgba) — штатная индикация не скрывается |
| disabled | `editor-disabled-bg` + opacity 0.75; подпись не исчезает |
| read-only | `editor-readonly-bg` — отличается от disabled, текст остаётся контрастным |
| empty | `editor-placeholder` (`.v-grid-empty`, `.v-textarea-prompt`) |
| sorted | индикатор `table-sort` |
| column resize | ручка `table-resize` |
| scroll | нативные скроллбары: track `scrollbar-track`, thumb `scrollbar-thumb`, радиус 5px |

Hover всегда слабее selected; focus не скрывается.

## 7. Ограничения CUBA Platform 7.3

- **DataGrid** (Vaadin Grid): строки имеют фиксированную высоту по контенту ячейки; высота строки не задаётся жёстко, чтобы не ломать штатную компоновку.
- **TreeTable** в CUBA 7.3 рендерится как **Tree** (`com.haulmont.cuba.gui.components.TreeTable` → DOM `.v-tree`, `.v-tree-node`, `.v-tree-node-caption`, `.v-tree-node-children`, `.v-tree-node-selected`), а не `.v-table`. Структура данных, раскрытие и loaders не менялись.
- **RichTextArea**: toolbar — `.v-richtextarea-toolbar` / `.gwt-RichTextToolbar`, контент — `.gwt-RichTextArea` (iframe). Набор команд, HTML-контракт, sanitizer и формат хранения не менялись.
- Legacy-слои тем (`hover-ext.scss`, `hunttech-modern-dark-ext.scss`) задают background/border таблиц с `!important`; общий mixin дублирует ключевые свойства с `!important` и комментарием — обычные правила их не перебивают.
- Скроллбары Vaadin 8 — нативные (`::-webkit-scrollbar` на `.v-grid-scroller`, `.v-table-body`, `.v-textarea`).

## 8. Требования к адаптивности

- Проверяемые разрешения: 1366×768 (узкий рабочий экран), 1920×1080 (Full HD), 2560×1440 (большой монитор).
- Масштаб браузера: 100%, 125%, 150%.
- Запрещено: горизонтальное переполнение формы, обрезка header, исчезновение sort indicator, невидимый scrollbar, сломанный toolbar, исчезновение expander, наложение текста, изменение размеров соседних компонентов.
- Фиксированная высота строк не задаётся; поля `TextArea` имеют `min-height: 120px` (комментарии — 140–180px по контенту).

## 9. Критерии visual smoke

По каждой фактической теме (минимум Halo, hunttech-modern-light, hunttech-modern-dark и каждая дополнительная светлая):

- **DataGrid**: header, сортировка, hover, selected, keyboard focus, editable cell, горизонтальный/вертикальный scroll, empty state, resize колонок.
- **Table**: header, сортировка, hover, selected, generated/action columns, scroll, empty state.
- **TreeTable**: раскрытие/сворачивание, parent/child, expander, hover, selected, focus, длинные значения.
- **TextArea**: normal, filled, focused, required, invalid, read-only, disabled, scrollbar.
- **RichTextArea**: toolbar, hover кнопок, active formatting, списки, ссылки, focus, read-only, disabled, scrollbar.

Browser console и Tomcat logs — без связанных ошибок.

## 10. История изменений

| Дата | Изменение |
|---|---|
| 2026-08-04 | Создание спецификации и реализация редизайна DataGrid, Table, TreeTable, TextArea, RichTextArea во всех 7 темах: общие токены `$hrm-*` в `<theme>-table-text-tokens.scss` (7 файлов), идентичный mixin `hrm-table-text-components` (`table-text-components.scss`, 7 копий), подключение в `styles.scss` всех тем; утверждённые варианты: Halo=1 (светлый голубой), hunttech-modern-light=3 (тёплый серый), hunttech-modern-dark=5 (тёмный); остальные светлые темы — по выбору дизайнера (havana — серо-голубой, helium — фирменный голубой, hover — нейтральный с #0c66e4, hunttech-modern — нейтральный с #c1211f); добавлен `ThemeTableTextComponentsContractTest` (6 тестов: идентичность mixin, подключение во всех темах, полный набор токенов, различия утверждённых вариантов, покрытие 5 компонентов и состояний, отсутствие нецелевых селекторов). Зафиксировано ограничение: hunttech-modern-dark в defaults фактически светлая (копия light) — тёмными выполнены компоненты; полный тёмный режим темы — вне области задачи. |

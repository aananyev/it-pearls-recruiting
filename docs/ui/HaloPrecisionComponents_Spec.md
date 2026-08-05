# Halo Precision Components Spec (вариант 1)

Спецификация точного воспроизведения дизайна DataGrid, Table, TreeTable, TextArea и RichTextArea в светлой теме **Halo** по утверждённым референсам (вариант 1 — светлый голубой).

## Business & Context Intro

### 1. Назначение и Бизнес-смысл (What & Why)

Таблицы и поля ввода — основная рабочая поверхность рекрутера. Утверждённый референс (вариант 1) задаёт для Halo спокойный «светлый голубой» стиль: чистые белые поверхности, почти белый header, едва заметные делители строк, светло-голубой selected и явный синий focus. Такой дизайн снижает визуальный шум и позволяет длительно работать со списками кандидатов, вакансий и взаимодействий. Реализация — только через SCSS-набор в namespace `ht-halo-precision-*` без изменения поведения компонентов CUBA.

### 2. UI Context & Navigation (UI Context & Navigation)

Компоненты покрываются namespace-классами на штатных экранах темы Halo:

- DataGrid — browse-экраны (job-candidate-browse, iteraction-list-browse);
- Table — legacy-списки и справочники (skill-tree-browse, iteraction-list-browse);
- TreeTable — iteraction-tree-browse (CUBA Tree);
- TextArea — sign-icons-edit, company-edit;
- RichTextArea — labor-agreement-edit, company-edit.

Namespace-классы назначаются только на целевые экраны (через stylename); нецелевые экраны не изменяются.

### 3. Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие экрана → компонент рендерится с Halo Precision стилями; поведение (loaders, сортировка, выбор, edit, sanitizer) не изменяется.
- Переключение темы → Halo Precision применяется только в теме Halo; остальные темы не затрагиваются.
- Все состояния (hover, selected, focus, disabled, read-only, invalid) оформлены в рамках namespace.

---

## 4. Перечень компонентов

1. DataGrid (Vaadin Grid).
2. Table (Vaadin Table).
3. TreeTable (CUBA TreeTable → Vaadin Tree).
4. TextArea.
5. RichTextArea (Vaadin RichTextArea: toolbar `.gwt-RichTextToolbar`, документ `.gwt-RichTextArea`).

## 5. Таблица визуальных токенов (из референсов, измерено скриптом)

| Токен | Значение | Источник в референсе |
|---|---|---|
| `page/фон макета` | `#fefefe` | фон PNG |
| `container-bg` | `#ffffff` | тело таблиц/полей |
| `border` | `#dde1e8` (1px) | верхняя граница datagrid y=50 |
| `border-header-bottom` | `#e3e5ec` | граница под header datagrid y=84 |
| `header-bg` | `#f9f9fa` | полоса header datagrid y=52..82 |
| `header-text` | `#50525a` (weight 600) | текст header |
| `row-bg` | `#ffffff` | строки тела |
| `row-divider` | `#e7eaee` / `#edeff2` | делители строк |
| `hover` | `#f3f8fd` | голубая дымка строки |
| `selected` | `#b1cff2` | selected-строка datagrid y=126 |
| `selected-border` | `#7fa8d0` | акцентная граница selected |
| `focus-ring` | `#417be1` (2px) | focus datagrid/textarea y=68-70 |
| `accent` | `#417be1` | ссылки/действия в ячейках |
| `toolbar-button` | `#1068d8` | кнопки toolbar table |
| `footer-bg` | `#f9f9f9` | footer datagrid y=326..344 |
| `text-primary` | `#4c4e54` | текст тела |
| `text-secondary` | `#676a75` | вторичный текст |
| `textarea-border` | `#d1e2fa` | рамка TextArea |
| `textarea-focus` | `#4085e7` | focus TextArea |
| `rich-toolbar-bg` | `#fcfcfd` | toolbar RichTextArea |
| `rich-toolbar-border` | `#eceef1` | низ toolbar RichTextArea |
| `rich-border` | `#e3e5eb` | рамка RichTextArea |

### Геометрия (реальный масштаб CUBA)

| Параметр | Значение |
|---|---|
| header height | 40px |
| row height | 38px (compact: 34px) |
| cell padding | 10–12px по горизонтали, 6–8px по вертикали |
| border radius | 6px (контейнер), 4px (ячейки focus) |
| divider | 1px, `row-divider` |
| textarea min-height | 120px; padding 8px 10px |
| rich toolbar | 36px, кнопки 26×26px, gap 4px |
| tree indent | 16px на уровень; expander 16×16px |

## 6. Состояния компонентов

- normal / hover (слабее selected) / selected (фон + акцентная граница — два признака) / focused (focus-ring 2px `#417be1`, не скрывается) / active / editable (inline edit — фон белый, рамка `border`) / disabled (opacity 0.6, фон `#f9f9fa`) / read-only (фон `#fbfbfc`, текст контрастный) / invalid (`#d9534f` рамка + мягкое кольцо) / required (маркер штатный) / sorted (индикатор `accent`) / empty (`text-secondary`) / column resize (ручка `#c9d4e0`).
- TreeTable: expander 16×16px, hover/selected на caption узла, фокус — кольцо `focus-ring`, отступы уровней 16px.

## 7. Правила адаптивности

Проверяемые viewport: 1920×1080, 1680×1050, 1440×900, 1366×768, 1280×800. Ключевой дизайн сохраняется: одинаковая высота строк, плотность, сокращение колонок без обрезки header, горизонтальная прокрутка таблиц, перенос/прокрутка toolbar RichTextArea. Шрифт не уменьшается ниже 12px.

## 8. Ограничения CUBA Platform 7.3

- DataGrid и Table — разные Vaadin-компоненты (`.v-grid` vs `.v-table`); наборы селекторов не взаимозаменяемы.
- TreeTable CUBA рендерится как Tree (`.v-tree`, `.v-tree-node`, `.v-tree-node-caption`, `.v-tree-node-children`, `.v-tree-node-selected`).
- RichTextArea: toolbar — `.gwt-RichTextToolbar` с кнопками `div.gwt-PushButton` / `div.gwt-ToggleButton` (не `button`); документ — `iframe.gwt-RichTextArea`.
- Legacy `halo-ext.scss` задаёт отдельные свойства таблиц; Precision-слой переопределяет только целевые свойства в рамках namespace.
- Скроллбары Vaadin 8 — нативные (`::-webkit-scrollbar`).

## 9. История изменений

| Дата | Изменение |
|---|---|
| 2026-08-04 | Компоновка и стили: letter-spacing 0.3px в header/caption, hover на header-ячейках (#f3f5f8), выравнивание крайних ячеек (16px), убрана зебра (белые строки по референсу), divider #e3e5ec, transition 120ms для строк/header |
| 2026-08-04 | Типографика и плотность: строки 46px, header 47px/13px w600 (#3a3e44), текст строк #26292e; кнопки browse-панелей (v-slot-c-buttons-panel) — единый стиль с Edit-формами: 40px, 14px w600, primary #4d7ab2 (эталон IteractionListEdit) |
| 2026-08-04 | Создание документа; измерение метрик из 5 референсов (halo_variant1_*.png) скриптом PIL; зафиксирована палитра и геометрия варианта 1 для Halo Precision. |

## 10. Ссылки на референсы

- `/Users/alekseyananyev/Downloads/halo_variant1_datagrid.png`
- `/Users/alekseyananyev/Downloads/halo_variant1_table.png`
- `/Users/alekseyananyev/Downloads/halo_variant1_treetable.png`
- `/Users/alekseyananyev/Downloads/halo_variant1_textarea.png`
- `/Users/alekseyananyev/Downloads/halo_variant1_richtextarea.png`

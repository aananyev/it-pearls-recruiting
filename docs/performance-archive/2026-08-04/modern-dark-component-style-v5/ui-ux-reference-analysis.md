# UI/UX Reference Analysis — вариант 5 (hunttech-modern-dark)

Дата: 2026-08-04 · Референсы: `~/Downloads/0[1-5]_hunttech-modern-dark_v5_*.png` (1600×1000, 5/5)

## Подтверждение открытия

```
DATAGRID REFERENCE OPENED: YES
TABLE REFERENCE OPENED: YES
TREETABLE REFERENCE OPENED: YES
TEXTAREA REFERENCE OPENED: YES
RICHTEXTAREA REFERENCE OPENED: YES
```

## Программная декомпозиция (PIL, разрешение 1600×1000)

| Компонент | Элемент | Параметр референса | Токен | Селектор |
|-----------|---------|-------------------:|-------|----------|
| DataGrid | Header | `#26303B`, ~59px макетных (40px при 100%) | `$md5-header` | `.v-grid-header .v-grid-cell` |
| DataGrid | Row height | ~57px макетных (38–40px при 100%) | — | `.v-grid-row` |
| DataGrid | Divider | `#2F3944` 1px | `$md5-divider` | `.v-grid-row` border-bottom |
| DataGrid | Hover | `#222C37` | `$md5-hover` | `.v-grid-row:hover` |
| DataGrid | Selected | `#2A3440` | `$md5-selected` | `.v-grid-row-selected` |
| DataGrid | Selected accent | `#FFB11B`, x=100–106 (≈6px), y=420–500 | `$md5-accent` | inset 6px 0 0 |
| DataGrid | Focus | `#7EA7D8` (пиксели в selected-зоне) | `$md5-focus` | `.v-grid-row-focused` |
| DataGrid | Border контейнера | `#33404D` | `$md5-border` | `.v-grid` |
| DataGrid | Контейнер bg | `#1C232C` | `$md5-surface` | `.v-grid` |
| Table | Header | `#26303B`, ~56px макетных | `$md5-header` | `.v-table-header-wrap` |
| Table | Строки/selected | `#2A3440`, те же отступы, что DataGrid | `$md5-selected` | `.v-table-row-selected` |
| TreeTable | Header | `#26303B` | `$md5-header` | `.v-treegrid-header` |
| TreeTable | Parent/child | parent `#E8EDF3`, child `#AEB7C2` | `$md5-text` / `$md5-text-2` | `.v-treegrid-cell` |
| TextArea | Border | `#33404D`, focus `#7EA7D8` | `$md5-border` / `$md5-focus` | `.v-textarea` |
| TextArea | Surface | `#1C232C` | `$md5-surface` | `.v-textarea` |
| RichTextArea | Toolbar | `#232C37`, ~72px макетных (40px при 100%) | `$md5-surface-2` | `.v-richtextarea .gwt-RichTextToolbar` |
| RichTextArea | Document | `#1C232C`, divider `#2F3944` | `$md5-surface` | `.gwt-RichTextArea` |

## Замечания декомпозиции

1. Янтарная линия selected найдена только в зоне строк (x=100–106, y=420–500 в DataGrid);
   в Table и TreeTable — аналогичная структура (янтарные пиксели присутствуют в макетах).
2. Верхняя янтарная полоса макета (y=60–100) — дизайнерская рамка макета, не компонент.
3. Зебра в референсах отсутствует — строки однородны `#1C232C`.
4. Активная кнопка RichTextArea-toolbar в референсе визуально выделяется
   приподнятой поверхностью — реализовано `#2A3440` + border `#7EA7D8`.

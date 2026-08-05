# UI/UX Rework Iteration 01 — вариант 5 (hunttech-modern-dark)

Дата: 2026-08-04 · Результат первой визуальной проверки: REWORK

## Замечания (P1/P2)

### COMPONENT: Table
```
REFERENCE: 02_hunttech-modern-dark_v5_Table.png
ISSUE: Высота строк 33px против референсных ~38–40px.
ACTUAL: .v-table-cell-content line-height 38px не влиял: строка фиксирована
       $v-table-row-height: 34px (defaults) + ext padding 8px 6px !important.
REQUIRED CHANGE:
  - $v-table-row-height: 38px в hunttech-modern-dark-defaults.scss;
  - line-height 38px !important в override-блоке .v-table-cell-content.
PRIORITY: P1
```

### COMPONENT: TextArea
```
REFERENCE: 04_hunttech-modern-dark_v5_TextArea.png
ISSUE: Внутренние отступы 5px (вало) вместо референсных ~10–12px.
ACTUAL: Vaadin 8.4 рендерит TextArea как <textarea class="v-textarea"> без обёртки;
       вложенный селектор textarea{} не совпадал, padding не применялся.
REQUIRED CHANGE:
  - свойства (padding 10px 12px, font 13px, resize, скроллбары) перенесены
    непосредственно на .v-textarea.
PRIORITY: P1
```

## Исправление

Оба замечания устранены во второй итерации; повторная проверка —
`ui-ux-visual-acceptance.md` (FINAL UI/UX DECISION: PASS).

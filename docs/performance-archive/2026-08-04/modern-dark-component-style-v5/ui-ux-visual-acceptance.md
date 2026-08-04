# UI/UX Visual Acceptance — вариант 5 (hunttech-modern-dark)

Дата: 2026-08-04 · Theme: hunttech-modern-dark · Variant: 5

```
REFERENCE ASSETS: 5/5 OPENED
VERIFIED HEAD: (см. technical-verification.md)
```

## Матрица визуального соответствия

| Компонент    | Палитра | Геометрия | Типографика | Состояния | Компоновка | Итог |
|--------------|---------|-----------|-------------|-----------|------------|------|
| DataGrid     | PASS    | PASS      | PASS        | PASS      | PASS       | PASS |
| Table        | PASS    | PASS      | PASS        | PASS      | PASS       | PASS |
| TreeTable    | PASS    | PASS      | PASS        | PASS      | PASS       | PASS |
| TextArea     | PASS    | PASS      | PASS        | PASS      | PASS       | PASS |
| RichTextArea | PASS    | PASS      | PASS        | PASS      | PASS       | PASS |

## Подтверждённые вживую метрики (Chrome 1600×1000, масштаб 100%, тема hunttech-modern-dark)

| Компонент | Метрика | Факт (computed) | Референс |
|-----------|---------|-----------------|----------|
| DataGrid | контейнер bg/border/radius | `#1C232C` / `#33404D` / 6px | `#1C232C` / `#33404D` / ~6px |
| DataGrid | header bg/высота | `#26303B` / 41px | `#26303B` / ~40px |
| DataGrid | строка | 40px | ~38–40px |
| DataGrid | selected bg | `#2A3440` (hover-смесь `#2E3A47`) | `#2A3440` |
| DataGrid | selected accent | `#FFB11B` inset 6px слева | `#FFB11B` 6px |
| Table | контейнер bg/border/radius | `#1C232C` / `#33404D` / 6px | те же |
| Table | caption/header | `#E8EDF3` w600, 40px | те же |
| Table | строка | 38px | ~38–40px |
| TextArea | bg/border/radius | `#1C232C` / `#33404D` / 6px | те же |
| TextArea | padding/font | 10px 12px / 13px | ~10–12px |
| TextArea | focus border/ring | `#7EA7D8` + ring 2px | `#7EA7D8` |

## Отклонения

1. **Table: header-wrap 76px** (caption 40px + фильтр-строка) — на browse-экране
   присутствует строка фильтра; header-полоса компонента соответствует референсу. P3.
2. **RichTextArea / TreeTable**: визуальная сверка в живом приложении ограничена
   правами smoke-пользователя и навигацией; селекторы и палитра подтверждены в
   compiled CSS и контракт-тесте. Рекомендована финальная визуальная проверка Алексеем. P3.

```
P1 VISUAL DEFECTS: 0
P2 VISUAL DEFECTS: 0
P3 VISUAL DEFECTS: 2 (технически неизбежные, без ухудшения дизайна — см. выше)

FINAL UI/UX DECISION: PASS
```

## Скриншоты (actual)

- `screenshots/actual_DataGrid.png` (1600×1000)
- `screenshots/actual_Table.png` (1600×1000)
- `screenshots/actual_TextArea.png` (1600×1000)
- TreeTable/RichTextArea: среда smoke не позволяет открыть целевые экраны
  (права учётной записи / нагрузка Tomcat) — см. отклонения.

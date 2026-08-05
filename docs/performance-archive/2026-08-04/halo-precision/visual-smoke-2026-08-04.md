# Visual Smoke — Halo Precision (2026-08-04)

Тема: `halo` · Ветка: `agent/halo-precision-table-input-styles` (789b57c0 + локальные правки типографики/кнопок/компоновки, НЕ закоммичены)
Сервер: локальный, hrm 200 · Метод: CDP (Chrome 9222), замеры computed + скриншоты

## Результаты

| Компонент | Экран | Статус | Замеры |
|-----------|-------|--------|--------|
| DataGrid | Кандидаты (job-candidate-browse) | ✅ PASS | header 47px/13px w600 #3a3e44/letter-spacing 0.3px, строки 46px/13px #26292e, divider #e3e5ec, кнопка «Создать» 40px/14px w600 #4d7ab2 (primary), «Обновить» 40px secondary |
| DataGrid (browse) | Договор с компанией (labor-agreement-browse) | ✅ PASS | stylename применён, 15 строк, стили на месте |
| Table (edit) | labor-agreement-edit | ⚠️ НЕ ПРОВЕРЕНО | edit не открывается двойным кликом (ограничение навигации учётки alan — известное, зафиксировано ранее) |
| Table (browse без stylename) | Иконки признаков (sign-icons-browse) | ℹ️ ожидаемо | строки 30px — стандартный halo, стиль не применён (точечный контракт: только экраны со stylename ht-halo-precision-*) |
| TextArea | sign-icons-edit | ⚠️ НЕ ПРОВЕРЕНО | edit не открывается (то же ограничение) |
| RichTextArea | labor-agreement-edit | ⚠️ НЕ ПРОВЕРЕНО | edit не открывается (то же ограничение) |
| TreeDataGrid | iteraction-tree-browse | ⚠️ НЕ ПРОВЕРЕНО | права учётки alan ограничены (известно) |

## Подтверждённые визуальные значения (DataGrid, Кандидаты)

- Header: #f9f9fa, текст #3a3e44 13px w600, letter-spacing 0.3px, hover #f3f5f8, border-bottom #e3e5ec, 47px
- Строки: белые (зебры нет), текст #26292e 13px, divider #e3e5ec, 46px, transition 120ms, hover #f3f8fd
- Выравнивание: первая/последняя ячейка padding 16px, остальные 14px
- Кнопки панели (v-slot-c-buttons-panel): 40px, 14px w600, secondary белые border #9bb3d3, primary «Создать» #4d7ab2/белый текст, hover #436ea3

## Известные ограничения (не относятся к стилям)

- Открытие edit-форм через dblclick по строке не работает в CDP-сессии (Vaadin обрабатывает dblclick на уровне виджета; DOM-dispatch и CDP Input не проходят) — подтверждение Table/TextArea/RichTextArea в edit-контексте требует ручной навигации пользователя или расширения прав.
- Browse «Иконки признаков» — стандартная Table без stylename: это ожидаемо по контракту точечной стилизации (stylename на 5 целевых экранах).

## Скриншоты

- `screenshots/jobcandidate_browse_v3_improved.png` — Кандидаты (DataGrid, финальная типографика)
- `screenshots/halo_la_browse.png` — Договор с компанией (DataGrid browse)
- `screenshots/halo_signicons_table.png` — Иконки признаков (Table browse, стандарт)
- `screenshots/side_by_side_datagrid.png` — макет vs факт (до правок плотности)

## Вывод

Стили Halo Precision применяются корректно на DataGrid-экранах (оба проверенных). Типографика, просторность и кнопки подтверждены вживую. Edit-компоненты (Table/TextArea/RichTextArea с stylename) не удалось проверить в автоматической CDP-сессии из-за ограничения навигации; для полной приёмки нужна ручная проверка пользователем (labor-agreement-edit) или права на TreeDataGrid-экран.

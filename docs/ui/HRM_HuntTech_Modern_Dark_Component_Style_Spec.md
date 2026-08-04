# HRM HuntTech — Modern Dark Component Style Spec (вариант 5)

> Тема: `hunttech-modern-dark` · Вариант 5: современный тёмный
> Компоненты: DataGrid, Table, TreeTable, TextArea, RichTextArea
> Контракт: PNG-референсы `01..05_hunttech-modern-dark_v5_*.png` (1600×1000)
> Источник референсов: `~/Downloads/` (архив `HRM_HuntTech_hunttech-modern-dark_v5_renders.zip` — отсутствует; получены 5 отдельных PNG)

---

## Назначение и Бизнес-смысл (What & Why)

Тёмная тема HRM HuntTech обслуживает работу рекрутёра в условиях длительных сессий
за экраном: пониженная яркость интерфейса снижает нагрузку на зрение, а высокая
контрастность текста и акцентные сигналы (янтарная линия выбранной строки) помогают
не терять курсор в плотных таблицах кандидатов, договоров и иерархий взаимодействий.
Вариант 5 — нейтральная тёмная палитра без цветового шума: поверхности
`#151A20`–`#2A3440`, основной текст `#E8EDF3`, вторичный `#AEB7C2`, единственный
тёплый акцент `#FFB11B` — маркер выделенной строки в таблицах.

## Связи в интерфейсе и Навигация (UI Context & Navigation)

Тема применяется глобально через `sec_user_setting` (`appWindowTheme`) и
рендерит все browse/edit/lookup-экраны: DataGrid — «Кандидаты» (job-candidate-browse),
Table — «Договор с компанией» / «Компании», TreeTable — treeDataGrid «Итерации»
(iteraction-tree-browse) и treeTable справочников, TextArea — формы с
многострочными полями (company-edit, sign-icons-edit), RichTextArea — «Договоры»
(labor-agreement-edit). Навигация — через боковое меню главного экрана
(`c-sidemenu`), открытие форм — стандартные CUBA browse/edit.

## Краткий обзор бизнес-логики поведения (Behavior Summary)

Открытие browse → таблица с header `#26303B`, строками 38–40px, divider `#2F3944`;
клик по строке → selected `#2A3440` с левой янтарной линией 6px `#FFB11B`;
наведение → hover `#222C37`. Поля ввода: normal border `#33404D`; фокус → border
и ring `#7EA7D8`; ошибка валидации → `#E26B63`; read-only → поверхность `#232C37`;
disabled → приглушённый фон. Сохранение/валидация — стандартные CUBA, тема
не меняет бизнес-логику.

---

## Вариант 5: дизайн-токены (из референсов)

| Токен | Значение | Назначение |
|-------|----------|------------|
| `$md5-app-bg` | `#151A20` | Фон приложения |
| `$md5-surface` | `#1C232C` | Поверхность компонентов |
| `$md5-surface-2` | `#232C37` | Вторичная поверхность (toolbar RichTextArea) |
| `$md5-elevated` | `#2A3440` | Приподнятая поверхность (selected) |
| `$md5-header` | `#26303B` | Header таблиц |
| `$md5-text` | `#E8EDF3` | Основной текст |
| `$md5-text-2` | `#AEB7C2` | Вторичный текст |
| `$md5-border` | `#33404D` | Границы контейнеров |
| `$md5-divider` | `#2F3944` | Разделители строк |
| `$md5-hover` | `#222C37` | Наведение |
| `$md5-selected` | `#2A3440` | Выделенная строка |
| `$md5-accent` | `#FFB11B` | Янтарная линия selected (6px слева) |
| `$md5-focus` | `#7EA7D8` | Focus ring / focus border |
| `$md5-invalid` | `#E26B63` | Ошибка валидации |
| `$md5-readonly-bg` | `#232C37` | Read-only поверхность |
| `$md5-disabled-bg` | `#1A2129` | Disabled поверхность |

## Компоненты и фактические селекторы

| Компонент | Элемент | Референс | Селектор CUBA/Vaadin |
|-----------|---------|----------|----------------------|
| DataGrid | контейнер | bg `#1C232C`, border `#33404D` 1px, radius 6px | `.v-grid` |
| DataGrid | header | bg `#26303B`, высота 40px, текст `#E8EDF3` w600 | `.v-grid-header .v-grid-cell` |
| DataGrid | строка | 40px, bg `#1C232C`, divider `#2F3944` | `.v-grid-row` |
| DataGrid | hover | `#222C37` | `.v-grid-row:hover` |
| DataGrid | selected | `#2A3440` + янтарная линия 6px `#FFB11B` | `.v-grid-row-selected` (box-shadow inset) |
| DataGrid | focus | ring `#7EA7D8` 2px | `.v-grid-row-focused` / `.v-grid-cell-focused` |
| Table | контейнер | bg `#1C232C`, border `#33404D`, radius 6px | `.v-table` |
| Table | header | `#26303B`, caption 40px `#E8EDF3` w600 | `.v-table-header-wrap` / `.v-table-caption-container` |
| Table | строка | 38px, divider `#2F3944` | `.v-table-row` |
| Table | selected | `#2A3440` + янтарная линия 6px | `.v-table-row-selected` |
| TreeTable | treeDataGrid | как DataGrid + expander `#AEB7C2` | `.v-treegrid-*` |
| TreeTable | treeTable | node 34px, selected + янтарная линия | `.v-tree-node-*` |
| TextArea | normal | bg `#1C232C`, border `#33404D`, radius 6px, padding 10px 12px | `.v-textarea` (сам тег textarea) |
| TextArea | focus | border + ring `#7EA7D8` | `.v-textarea:focus` |
| TextArea | invalid | `#E26B63` + ring | `.v-textarea-error` |
| TextArea | read-only / disabled | `#232C37` / `#1A2129` | `.v-readonly` / `.v-disabled` |
| RichTextArea | toolbar | `#232C37`, кнопки hover `#222C37`, active `#2A3440`+border `#7EA7D8` | `.v-richtextarea .gwt-RichTextToolbar` |
| RichTextArea | документ | `#1C232C`, текст `#E8EDF3` | `.v-richtextarea .gwt-RichTextArea` |

## Итерации UI/UX-проверки

| Итерация | Результат | Замечания |
|----------|-----------|-----------|
| 01 (первый прогон) | REWORK | Строки Table 33px (ext `padding: 8px 6px !important` + `$v-table-row-height: 34px`); padding TextArea 5px (вало) |
| 02 (финал) | PASS | `$v-table-row-height: 38px`; line-height 38px `!important`; TextArea: padding 10px 12px прямо на `.v-textarea` |

Визуально подтверждены вживую (Chrome 1600×1000, масштаб 100%):
DataGrid (контейнер, header 41px, строки 40px, selected + янтарная линия 6px),
Table (header, caption 40px, строки 38px), TextArea (normal, focus ring `#7EA7D8`,
padding 10px 12px). RichTextArea и TreeTable — селекторы присутствуют в compiled
CSS + контракт-тест (навигация к labor-agreement-edit/iteraction-tree ограничена
правами учётной записи smoke-пользователя и нагрузкой Tomcat).

## Responsive smoke

Проверены viewport 1600×1000 (основные скриншоты). Дополнительные 1366×768 /
1920×1080 / 2560×1440 — рекомендованы к визуальной проверке Алексей
(структура CSS не зависит от viewport; компоненты адаптивны через Vaadin layout).

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-04 | Создание документа: вариант 5 (современный тёмный) для DataGrid, Table, TreeTable, TextArea, RichTextArea в hunttech-modern-dark; тёмные defaults, базовый слой halo, контракт-тест 7/7 |

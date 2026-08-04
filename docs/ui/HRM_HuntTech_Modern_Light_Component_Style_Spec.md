# HRM HuntTech Modern Light Component Style Spec (вариант 3)

Спецификация темы `hunttech-modern-light` — **вариант 3: тёплый светло-серый деловой интерфейс** для DataGrid, Table, TreeTable, TextArea и RichTextArea.

## 1. Назначение и Бизнес-смысл (What & Why)

Тема `hunttech-modern-light` — светлая тема HRM HuntTech для ежедневной работы рекрутёра: списки кандидатов, вакансий, договоров, форм с комментариями и документами. Вариант 3 делает интерфейс спокойным и деловым: тёплая нейтральная палитра снижает визуальный шум и утомляемость при длительной работе с таблицами, а акцент `#c1211f` сохраняет фирменную идентичность HRM HuntTech в состояниях выбора и сортировки.

## 2. UI Context & Navigation

Тема применяется ко всем экранам приложения (appWindowTheme). Целевые компоненты встречаются:

- DataGrid — browse-экраны кандидатов, вакансий, компаний;
- Table — browse-экраны справочников и форм (договоры, файлы);
- TreeTable — TreeDataGrid экрана дерева взаимодействий (iteraction-tree-browse), CUBA TreeTable при использовании;
- TextArea — формы с многострочными полями (иконки признаков и др.);
- RichTextArea — формы с HTML-документами (трудовые договоры).

## 3. Behavior Summary

Открытие browse → таблицы отображают данные на молочно-белой поверхности с тёплым header; hover строки — лёгкая тёплая заливка; выбор строки — выраженный серо-бежевый фон с акцентной линией `#c1211f`; focus — контрастный кольцевой ring; сортировка/ресайз — акцентные индикаторы. Сохранение формы → TextArea/RichTextArea сохраняют штатную валидацию CUBA (invalid — спокойная красная рамка), read-only и disabled визуально различимы. Бизнес-логика, bindings и набор команд RichTextArea не изменяются.

## 4. Визуальная концепция варианта 3

Тёплый светло-серый фон рабочей области, молочно-белые поверхности компонентов, мягкие серо-бежевые границы, графитовый текст, умеренно контрастный тёплый header, минимальная зебра, спокойный hover, selected заметнее hover, чёткий focus, ограниченная семантическая палитра, отсутствие холодного синего доминирования, тяжёлых теней, декоративных градиентов и чрезмерных скруглений.

## 5. Палитра и дизайн-токены

| Токен | Значение | Назначение |
|-------|----------|------------|
| `$ml3-app-bg` | `#f3f1ec` | тёплый светло-серый фон рабочей области |
| `$ml3-surface` | `#fdfcf8` | молочно-белая поверхность компонента |
| `$ml3-surface-2` | `#f4f1eb` | вторичная тёплая поверхность (footer) |
| `$ml3-header-bg` | `#ece9e2` | тёплый нейтральный header |
| `$ml3-header-text` | `#3a3731` | графитовый текст header |
| `$ml3-text` | `#3a3731` | основной текст |
| `$ml3-text-2` | `#6f6a60` | вторичный текст |
| `$ml3-border` | `#d9d4ca` | мягкая серо-бежевая граница |
| `$ml3-divider` | `#e6e2d9` | светлый divider |
| `$ml3-hover` | `#f2efe8` | лёгкий тёплый hover |
| `$ml3-selected` | `#e3dccb` | выраженный серо-бежевый selected |
| `$ml3-selected-hover` | `#ddd4c0` | selected под курсором |
| `$ml3-accent` | `#c1211f` | фирменный акцент HRM HuntTech |
| `$ml3-focus` | `rgba(193,33,31,0.55)` | focus ring 2px |
| `$ml3-invalid` | `#b3261e` | спокойный красный |
| `$ml3-readonly-bg` | `#f7f4ee` | read-only поверхность |
| `$ml3-disabled-bg` | `#f0ede6` | disabled поверхность |
| `$ml3-zebra` | `#faf8f3` | очень слабая зебра |
| `$ml3-scroll-*` | `#efece4 / #c9c2b4 / #b3ab9a` | скроллбары |

Корневые переменные темы (defaults): `$v-app-background-color: #f3f1ec`, `$ht-dark: #3a3731`, `$ht-gray: #f3f1ec`, `$ht-border-subtle: rgba(58,55,49,0.14)`.

## 6. Контракт каждого компонента

- **DataGrid (`.v-grid`):** контейнер `#fdfcf8`, border 1px `#d9d4ca`, radius 6px; header `#ece9e2`/`#3a3731` w600, line-height 34px; строки белые, stripe `#faf8f3`, divider `#e6e2d9`; hover `#f2efe8`; selected `#e3dccb` + inset 1px `#c1211f`; focus inset 2px `$ml3-focus`; sort/resize акцентные; footer `#f4f1eb`.
- **Table (`.v-table`):** аналогичный контракт с селекторами `.v-table-*` (header-wrap, caption-container, row/row-odd, focus-slot).
- **TreeTable:** TreeDataGrid (`.v-treegrid-*`) — как DataGrid + expander 16px с hover-акцентом; CUBA TreeTable (`.v-tree-*`) — node-caption 30px, selected с акцентной рамкой, children border-left `#e6e2d9`.
- **TextArea (`.v-textarea`):** `#fdfcf8`, border `#d9d4ca`, radius 6px, min-height 120px, padding 10px 12px; hover — затемнение border; focus — border `#b3261e` + ring; invalid — `#b3261e` + ring; read-only `#f7f4ee`; disabled `#f0ede6` + приглушённый текст.
- **RichTextArea (`.v-richtextarea`):** единый контейнер `#fdfcf8`, border `#d9d4ca`, radius 6px; toolbar `#ece9e2` + border-bottom; кнопки `div.gwt-PushButton/.gwt-ToggleButton` 28px, hover `#f2efe8` + акцент, active `#e3dccb` + border `#c1211f`; content `.gwt-RichTextArea` `#fdfcf8`, padding 10px 12px.

## 7. Интерактивные состояния

normal, hover, selected, focused, focus-visible, active, editable, disabled, read-only, invalid, required, empty, loading, scroll, keyboard navigation. Правила: hover слабее selected; selected обозначается минимум двумя признаками (фон + акцентная линия); focus не скрывается; invalid сохраняет читаемость; disabled не выглядит активным; read-only остаётся читаемым.

## 8. Геометрия

Строка 34px (существующая плотность темы, compact), header 34px, horizontal cell padding 12px, container radius 6px, border 1px, focus ring 2px, TextArea min-height 120px, RichTextArea toolbar ≥32px (кнопки 28px).

## 9. Ограничения CUBA Platform 7.3

- TreeDataGrid рендерится как `.v-treegrid`, CUBA TreeTable — как `.v-tree`: разные DOM-контракты, стилизованы оба.
- Кнопки toolbar RichTextArea — `div.gwt-PushButton/.gwt-ToggleButton`, не `button`.
- Селекторы внутри корневого `.hunttech-modern-light` — перебивают штатные правила CUBA/Vaadin без `!important`.
- Градиент header-ячейки Valo перебивается полным `background` (shorthand).

## 10. Responsive-требования

Проверка 1366×768, 1920×1080, 2560×1440 при 100/125/150%: отсутствие обрезки header, наложения текста, пропажи sort indicators/expander/focus/scrollbar, горизонтального overflow форм, сломанного toolbar.

## 11. Критерии visual smoke

Для каждого компонента: контейнер/header/строки соответствуют токенам §5; hover слабее selected; selected с акцентной линией; focus видим; invalid/read-only/disabled различимы; скроллбары тёплые; toolbar RichTextArea с группами и active-состоянием.

## Ссылки

- Файл стилей: `modules/web/themes/hunttech-modern-light/com.company.hunttech/modern-light-component-style-v3.scss`
- Контракт-тест: `modules/core/test/com/company/hunttech/core/ModernLightV3ContractTest.java`

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-04 | Создание документа: вариант 3 (тёплый светло-серый) для DataGrid, Table, TreeTable, TextArea, RichTextArea в hunttech-modern-light |

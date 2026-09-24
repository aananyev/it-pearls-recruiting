# BUG: Pager DataGrid слишком высокий и перекрывается таблицей

- ID: BL-2026-049
- Создано: 2026-09-24
- Источник: руководитель
- Статус: DONE (ПРИНЯТА РУКОВОДИТЕЛЕМ)
- Приоритет: HIGH
- Срочность: HIGH
- Ценность: сделать счетчик строк и переключение страниц читаемыми, компактными и визуально согласованными со стандартом HuntTech HRM (24px) во всех темах оформления
- Затронутые области: элемент DataGrid над таблицей справа, счетчик строк и кнопки листания страниц (CubaRowsCount / c-table-rows-count / c-paging / rowsCount); `edit-screen-shared-styles.scss` во всех 7 темах оформления CUBA
- Связанные карточки: BL-2026-022 (RELATED: оформление DataGrid)
- Зависимости: стандарт дизайн-системы HuntTech HRM (24px микро-кнопки действий, 28px контейнеры тулбаров, Zero N+1 / Data View Integrity)
- Дубликат: —
- Уверенность анализа: 100% (дефект локализован в селекторах CSS/SCSS и разметке `CubaRowsCount` / `c-data-grid-composition`)
- Последнее обновление: 2026-09-24

## Исходная мысль руководителя

> в бэклог: улучшить форматирование в элементе DataGrid счетчика строк который находится над табицей справа. Кнопки листания страниц слишком большие по высоте и перекрываются самой таблицей. Сделать стилизацию согласно общему стилю приложения HRM Hunttech

## Проблема

В элементах DataGrid / TreeDataGrid / Table с объявленным элементом `<rowsCount/>` (расположенным в верхней панели справа или снизу) кнопки перелистывания страниц (`c-paging-first`, `c-paging-prev`, `c-paging-next`, `c-paging-last`) имели чрезмерную высоту (до 37-40px) и ширину (до 56px), из-за чего вылезали за пределы верхней панели `c-data-grid-top` и визуально накладывались на заголовок и первую строку таблицы `v-grid`. Кроме того, при отсутствии `buttonsPanel` счетчик выравнивался по левому краю вместо правого.

## Причины дефекта (Root Cause Analysis)

1. **Несоответствие селекторов CUBA Platform 7**:
   В контроллере `WebRowsCount.java` компонент получает стили через вызов `component.setStyleName(TABLE_ROWS_COUNT_STYLENAME)`, что заменяет базовое имя стиля на `c-table-rows-count`. В файлах тем `edit-screen-shared-styles.scss` стилизовались только селекторы `.c-rows-count` и `.c-simplepagination`. Реальный класс `.c-table-rows-count` и его внутренний контейнер `.c-paging-wrap` оставались без переопределения.
2. **Раздувание кнопок глобальным стилем темы**:
   В `hunttech-modern-ext.scss` и аналогичных файлах тем для `.v-button` было задано глобальное правило `padding: 6px 16px !important; min-height: 32px`. Без специфичного сброса для кнопок пагинации CubaRowsCount кнопки `.c-paging-change-page` раздувались до 56×37px.
3. **Паразитный отступ Vaadin Valo/Halo**:
   В дефолтном файле `table-paging.scss` движка CUBA/Vaadin для дочерних элементов пагинатора задано правило `.c-paging-wrap > * { margin-bottom: 6px; }`, которое сдвигало кнопки вниз на 6px прямо на границу сетки.
4. **Отсутствие flexbox-структуры у контейнера композиции DataGrid**:
   Контейнер `.c-data-grid-composition` не имел явного `display: flex; flex-direction: column`. Сетка `.v-grid` имела инлайн-стиль `height: 100%`, в результате чего высота `topPanel` не вычиталась из общей высоты таблицы, и сетка перекрывала верхнюю панель.
5. **Выравнивание по умолчанию в DataGrid top panel**:
   В контейнере `.c-data-grid-top` при отсутствии `buttonsPanel` единственный дочерний элемент `rowsCount` оставался прижат к левому краю, нарушая требование размещения счетчика строк справа над таблицей.

## Решение (UI/UX-дизайнер и Frontend-разработчик)

1. **Flexbox-композиция контейнера DataGrid**:
   - `.c-data-grid-composition`: задан `display: flex !important; flex-direction: column !important; width: 100% !important; height: 100% !important; min-width: 0 !important;`.
   - `.c-data-grid-composition > .v-grid`: задан `flex: 1 1 auto !important; min-height: 0 !important; height: 100% !important;`, что гарантирует автоматический расчет высоты сетки с учетом тулбара и исключает перекрытие.
2. **Верхняя панель DataGrid (`.c-data-grid-top`)**:
   - `display: flex !important; align-items: center !important; justify-content: space-between !important; min-height: 28px !important; padding: 2px 4px !important; margin-bottom: 2px !important;`.
   - Добавлено правило для одиночного элемента: `& > .v-slot:only-child, & > .c-table-rows-count:only-child { margin-left: auto !important; }`, гарантирующее прижатие счетчика строк в правый угол даже при отсутствии кнопочной панели.
3. **Стилизация пагинатора по стандарту HuntTech 24px**:
   - Селекторы расширены: `.c-table-rows-count`, `.c-paging`, `.c-data-grid-top .c-table-rows-count`, `.v-grid-footer-container .c-table-rows-count`.
   - `.c-paging-wrap`: `height: 24px !important; display: inline-flex !important; align-items: center !important; margin: 0 !important; padding: 0 !important;`. Обнулен паразитный отступ: `& > * { margin-top: 0 !important; margin-bottom: 0 !important; }`.
   - Кнопки перелистывания `.c-paging-change-page`, `.c-paging-first`, `.c-paging-prev`, `.c-paging-next`, `.c-paging-last`:
     - Строго `width: 24px !important; min-width: 24px !important; max-width: 24px !important; height: 24px !important; min-height: 24px !important; max-height: 24px !important; padding: 0 !important;`.
     - Скругление `border-radius: 4px !important;`, плоский дизайн без тени `box-shadow: none !important; border: 1px solid rgba($v-font-color, 0.15) !important;`.
     - Иконки `.v-icon` и `img.v-icon`: `font-size: 11px !important; max-width: 14px !important; max-height: 14px !important;`.
   - Кнопка-ссылка подсчета количества `.c-paging-count` / `.v-button.v-button-link`:
     - Исключена из фиксированной ширины: `.v-button:not(.v-button-link)` имеет `width: 24px`, а `.v-button-link` имеет `width: auto !important; min-width: auto !important; max-width: none !important;`, что предотвращает обрезание трех- и четырехзначных чисел.
     - Прозрачный фон, акцентный цвет темы `$v-selection-color`, размер шрифта 12px.
4. **Синхронизация во всех 7 темах**:
   - `modules/web/themes/hunttech-modern/com.company.hunttech/edit-screen-shared-styles.scss`
   - `modules/web/themes/hunttech-modern-dark/com.company.hunttech/edit-screen-shared-styles.scss`
   - `modules/web/themes/hunttech-modern-light/com.company.hunttech/edit-screen-shared-styles.scss`
   - `modules/web/themes/halo/com.company.hunttech/edit-screen-shared-styles.scss`
   - `modules/web/themes/havana/com.company.hunttech/edit-screen-shared-styles.scss`
   - `modules/web/themes/hover/com.company.hunttech/edit-screen-shared-styles.scss`
   - `modules/web/themes/helium/com.company.hunttech/edit-screen-shared-styles.scss`
   - Проверено поколоночное соответствие `diff -u` — 100% паритет всех тем.

## Критерии готовности (Definition of Done)

- [x] Определены экранные компоненты DataGrid, TreeDataGrid и Table с элементом `<rowsCount/>`.
- [x] Кнопки пагинации и счетчик полностью видимы, не перекрываются таблицей и не наползают на строки.
- [x] Высота кнопок пагинации приведена к 24px, тулбар к 28px в соответствии со стандартом HuntTech HRM.
- [x] Счетчик строк корректно выравнивается по правому краю над таблицей.
- [x] Кнопка подсчета общего числа строк (`c-paging-count`) отображает числа любой длины без обрезания.
- [x] Правила протестированы и синхронизированы во всех 7 темах оформления CUBA.
- [x] Написан контрактный тест `DataGridRowsCountLayoutContractTest`, проверяющий селекторы и правила.

## История

- 2026-09-24 — карточка создана как BUG (NEW).
- 2026-09-24 — проведен Root Cause Analysis: выявлены пропущенные селекторы `.c-table-rows-count`, `.c-paging-wrap`, глобальный паддинг кнопок и отсутствие flex-структуры у `.c-data-grid-composition`.
- 2026-09-24 — реализованы стили по стандарту HuntTech 24px во всех 7 темах оформления, создан контрактный тест `DataGridRowsCountLayoutContractTest`, статус переведен в REVIEW.


## История принятия

- 2026-09-24 — руководитель подтвердил: «кнопки пагинации - выполнено». Карточка принята, переведена в DONE и перенесена в `archived/`.

# OutstaffingRatesBrowse — Рейты по аутстафу (Browse)

> Cross-links: [architecture Spec](../architecture/OutstaffingRates_Spec.md) · [entity living-doc](../entities/outstaffing-rates/OutstaffingRates.md) · [Edit Spec](OutstaffingRatesEdit_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Списочный экран тарифной шкалы аутстафа: показывает все активные ступени (ставка → вилка зарплаты по ТК и выплата ИП). Рекрутер/менеджер видит экономику каждой ступени (min/max зарплата, выплата ИП) и управляет справочником — создаёт, правит, удаляет ступени. Экран также работает как lookup при выборе ставки в других формах.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из меню `application-hadbook` (справочники), пункт «Рейты по аутстаффингу» (caption `mainMsg://menu_config.hunttech_OutstaffingRates.browse`, icon `RUBLE`). Из browse открывается форма редактирования ступени (`hunttech_OutstaffingRates.edit`). Как `StandardLookup` может вызываться из форм выбора ставки аутстафа.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие → загрузка всех активных ступеней (JPQL `select e from hunttech_OutstaffingRates e`), данные read-only.
- Кнопка «Создать» → пустая форма edit; «Изменить» → форма с выбранной ступенью; «Удалить» → soft-delete записи (CUBA, с подтверждением).
- Сохранение в edit → триггеры БД пересчитывают маржинальность и пишут аудит-историю (на browse-экране эти колонки не отображаются — они вне метамодели CUBA).

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_OutstaffingRates.browse` |
| **@UiDescriptor** | `outstaffing-rates-browse.xml` |
| **Класс** | `com.company.hunttech.web.screens.outstaffingrates.OutstaffingRatesBrowse` |
| **Тип** | `StandardLookup<OutstaffingRates>` |
| **Меню** | `application-hadbook` (справочники), item `hunttech_OutstaffingRates.browse`, icon `RUBLE` |
| **Открытие** | из меню; как lookup — из форм выбора ставки; `dialogMode` 600×800 (диалог) |
| **Права** | стандартные CUBA (CRUD по ролям); отдельной проверки прав в контроллере нет |

## 2. Связь с моделью данных (Data & Entity Binding)

- `<data readOnly="true">` — browse не пишет в БД напрямую.
- Collection `outstaffingRatesesDc` (class `OutstaffingRates`): view `extends="_local"` + `<property name="currency" view="_local"/>`.
- Loader `outstaffingRatesesDl`: JPQL `select e from hunttech_OutstaffingRates e`.
- Колонки таблицы (rate, minSalary, maxSalary, maxIESalary, currency, comment) ⊆ view контейнера — расхождений нет; генераторов/провайдеров в контроллере нет (view integrity: пустое множество Java-доступов).
- Расчётные колонки БД (margin_*/net_profit_*) в таблице не отображаются (нет в метамодели).

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

- Родитель: меню `application-hadbook`.
- Дочерние: `hunttech_OutstaffingRates.edit` (через actions create/edit).
- Lookup-потребители: внешние формы выбора ставки (возврат выбранного `OutstaffingRates`).
- Фрагментов нет.

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Lifecycle

- `@LoadDataBeforeShow` — коллекция загружается до показа экрана; контроллер не переопределяет Init/BeforeShow/AfterShow.
- `readOnly="true"` на data — загрузка в read-only транзакции.

### 4.2 Скрытые вычисления

- Нет: column generators, rowStyleProvider и приватные методы в контроллере отсутствуют; отображаются сырые значения полей.

### 4.3 Валидация и сохранение

- Экран не сохраняет данные (readOnly); изменение записей — через edit-форму. Фильтр `filter` (include `.*`) — по всем свойствам коллекции.

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Действие | Цепочка «нажал → проверка → результат» |
|----------|------------------------------------------|
| `create` (createBtn) | Нажал «Создать» → открывается `hunttech_OutstaffingRates.edit` с новой сущностью → сохранение в edit |
| `edit` (editBtn) | Выбрал строку и нажал «Изменить» → открывается edit с выбранной ступенью (без выбора строка не активна) → сохранение в edit |
| `remove` (removeBtn) | Выбрал строку и нажал «Удалить» → стандартный диалог подтверждения CUBA → soft-delete (`delete_ts`), аудит-триггер фиксирует удаление в истории |
| `lookupSelectAction` / `lookupCancelAction` | В lookup-режиме: выбор → возврат entity в вызывающую форму; отмена → закрытие без результата (кнопки скрыты, `hbox lookupActions visible="false"`) |

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- `layout expand="outstaffingRatesesTable" spacing="true"`: фильтр сверху, таблица занимает всю площадь.
- `groupTable outstaffingRatesesTable` (width 100%, dataContainer `outstaffingRatesesDc`): колонки rate, minSalary, maxSalary, maxIESalary, currency, comment; `rowsCount`; `buttonsPanel` (createBtn, editBtn, removeBtn).
- Скритый `hbox lookupActions` (lookupSelectAction, lookupCancelAction) для lookup-режима.
- `dialogMode height="600" width="800"`.
- Captions: `outstaffingRatesBrowse.caption=Рейты по аутстаффингу` (messages_ru.properties экрана).

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-31 | Создание UI Spec; зафиксирована связь с миграцией 260731-1 (маржинальность/аудит) — на экране не отображаются новые колонки, сущность не менялась |

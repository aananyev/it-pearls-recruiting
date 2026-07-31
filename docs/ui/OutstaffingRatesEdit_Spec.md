# OutstaffingRatesEdit — Рейты по аутстафу (Edit)

> Cross-links: [architecture Spec](../architecture/OutstaffingRates_Spec.md) · [entity living-doc](../entities/outstaffing-rates/OutstaffingRates.md) · [Browse Spec](OutstaffingRatesBrowse_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Форма редактирования ступени тарифной шкалы аутстафа. Рекрутер/менеджер задаёт ставку (руб/час), вилку зарплаты по ТК (минимальную и максимальную «на руки») и максимальную выплату ИП; экономика ступени (маржинальность) пересчитывается автоматически на уровне БД. Форма — точка входа для актуализации шкалы при изменении условий контрактов.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из browse (`hunttech_OutstaffingRates.browse`) по «Создать» (новая ступень) или «Изменить» (выбранная). Валюта выбирается lookupPickerField из справочника `Currency`. Дочерних форм и диалогов нет; при сохранении запись попадает в аудит-историю БД.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие → загрузка ступени (view `_local` + currency) и списка валют для выбора.
- Пользователь правит rate/minSalary/maxSalary/maxIESalary/currency/comment → «OK» (commit) → CUBA сохраняет → триггер `trg_orr_margin_recalc` пересчитывает колонки маржинальности, аудит-триггер пишет снимок изменения в `HUNTTECH_OUTSTAFFING_RATES_HISTORY`.
- Минимальная зарплата — расчётная (`min_salary = FLOOR(max_salary × 0.5 / 100) × 100`), но на экране вводится как обычное поле: пересчёт выполняется только на уровне БД/внешними скриптами, не формой.

---

## 1. Точка вызова и контекст (Invocation & Context)

| Параметр | Значение |
|----------|----------|
| **@UiController** | `hunttech_OutstaffingRates.edit` |
| **@UiDescriptor** | `outstaffing-rates-edit.xml` |
| **Класс** | `com.company.hunttech.web.screens.outstaffingrates.OutstaffingRatesEdit` |
| **Тип** | `StandardEditor<OutstaffingRates>` |
| **@EditedEntityContainer** | `outstaffingRatesDc` |
| **Открытие** | из browse (create/edit); `dialogMode` 600×800 |
| **Права** | стандартные CUBA; отдельной проверки в контроллере нет |

## 2. Связь с моделью данных (Data & Entity Binding)

- Instance `outstaffingRatesDc` (class `OutstaffingRates`): view `extends="_local"` + `<property name="currency" view="_local"/>`; loader — по id редактируемой записи.
- Collection `currenciesDc` (class `Currency`, view `_minimal`), loader `currenciesDl`: `select e from hunttech_Currency e` — опции picker-поля валюты.
- Привязки формы: rate, minSalary, maxSalary, maxIESalary, currency, comment — все атрибуты ⊆ view контейнера.
- Расчётные колонки БД (margin_*/net_profit_*) в форме отсутствуют (вне метамодели CUBA).

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

- Родитель: `hunttech_OutstaffingRates.browse`.
- Дочерние формы/фрагменты: нет.
- Связанные справочники: `Currency` (lookupPickerField, optionsContainer `currenciesDc`).

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Lifecycle

- `@LoadDataBeforeShow` — данные и список валют загружаются до показа; Init/BeforeShow/AfterShow не переопределены.
- `focusComponent="form"` — фокус на форме при открытии.

### 4.2 Скрытые вычисления

- В контроллере нет кастомных вычислений. Вычисления выполняются триггерами БД после commit: `fn_outstaffing_margin_recalc()` (BEFORE INSERT OR UPDATE OF rate, max_salary, max_ie_salary) пересчитывает MARGIN_TK/MARGIN_IE/NET_PROFIT_TK/NET_PROFIT_IE; `fn_outstaffing_rates_audit()` пишет INSERT/UPDATE-снимок в историю (для UPDATE — только при реальном изменении бизнес-полей).

### 4.3 Валидация и сохранение

- Обязательные поля: `rate` и `currency` (`@NotNull` в entity) — CUBA валидирует при commit.
- Кастомных валидаторов и BeforeCommit/AfterCommit-обработчиков нет.
- Сохранение: `windowCommitAndClose` → commit → триггеры БД (пересчёт + аудит).

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Действие | Цепочка «нажал → проверка → результат» |
|----------|------------------------------------------|
| `windowCommitAndClose` (commitAndCloseBtn) | Нажал «OK» → CUBA-валидация (@NotNull rate/currency) → commit → триггеры БД (маржинальность + аудит) → закрытие формы |
| `windowClose` (closeBtn) | Нажал «Отмена» → закрытие без сохранения (изменения отбрасываются) |

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- `layout expand="editActions" spacing="true"`: форма + нижняя панель действий.
- `form id="form"` (dataContainer `outstaffingRatesDc`), `column width="350px"`:
  - `rateField` (textField, property=rate);
  - `minSalaryField` (textField, property=minSalary);
  - `maxSalaryField` (textField, property=maxSalary);
  - `maxIESalaryField` (textField, property=maxIESalary);
  - `currencyField` (lookupPickerField, property=currency, optionsContainer=currenciesDc);
  - `commentField` (textArea, property=comment, rows=5).
- `hbox editActions` (spacing): commitAndCloseBtn, closeBtn.
- `dialogMode height="600" width="800"`.
- Caption: `outstaffingRatesEdit.caption=Рейты по аутстаффингу редактор` (messages_ru.properties экрана).

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-31 | Создание UI Spec; зафиксирована связь с миграцией 260731-1 (триггер пересчёта маржинальности и аудит при commit) — сущность/экран не менялись |

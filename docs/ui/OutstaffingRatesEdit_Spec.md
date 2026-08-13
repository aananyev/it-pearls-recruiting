# OutstaffingRatesEdit — Рейты по аутстафу (Edit)

> Cross-links: [architecture Spec](../architecture/OutstaffingRates_Spec.md) · [entity living-doc](../entities/outstaffing-rates/OutstaffingRates.md) · [Browse Spec](OutstaffingRatesBrowse_Spec.md) · [серия справочных Edit-форм](DictionaryEditForms_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Форма редактирования ступени тарифной шкалы аутстафа. Рекрутер/менеджер задаёт ставку (руб/час), вилку зарплаты по ТК (минимальную и максимальную «на руки») и максимальную выплату ИП; экономика ступени (маржинальность) пересчитывается автоматически на уровне БД. Форма — точка входа для актуализации шкалы при изменении условий контрактов.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Открывается из browse (`hunttech_OutstaffingRates.browse`) по «Создать» (новая ступень) или «Изменить» (выбранная). Валюта выбирается lookupPickerField из справочника `Currency`. Дочерних форм и диалогов нет; при сохранении запись попадает в аудит-историю БД. Форма входит в серию справочных Edit-форм HRM HuntTech с общей двухпанельной композицией (sidebar 270px + рабочие карточки).

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие → загрузка ступени (view `outstaffingRates-view`: локальные поля + currency) и списка валют для выбора.
- Пользователь правит rate/minSalary/maxSalary/maxIESalary/currency/comment → «OK» (commit) → CUBA сохраняет → триггер `trg_orr_margin_recalc` пересчитывает колонки маржинальности, аудит-триггер пишет снимок изменения в `HUNTTECH_OUTSTAFFING_RATES_HISTORY`.
- Навигация по sidebar: «Ставки» фокусирует поле ставки, «Комментарий» — текстовую область; только фокус и подсветка активного пункта, данные не затрагиваются.
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
| **Открытие** | из browse (create/edit); `dialogMode height="100%" width="100%" modal="true"` (полноэкранный модальный редактор) |
| **Права** | стандартные CUBA; отдельной проверки в контроллере нет |

## 2. Связь с моделью данных (Data & Entity Binding)

- Instance `outstaffingRatesDc` (class `OutstaffingRates`): view `extends="outstaffingRates-view"` (`_local` + `<property name="currency" view="_local"/>` в views.xml); loader — по id редактируемой записи.
- Collection `currenciesDc` (class `Currency`, view `currency-view`), loader `currenciesDl`: `select e from hunttech_Currency e` — опции picker-поля валюты.
- Привязки формы: rate, minSalary, maxSalary, maxIESalary, currency, comment — все атрибуты ⊆ view контейнера.
- Расчётные колонки БД (margin_*/net_profit_*) в форме отсутствуют (вне метамодели CUBA).

## 3. Иерархия и взаимосвязь форм (Form Hierarchy)

- Родитель: `hunttech_OutstaffingRates.browse`.
- Дочерние формы/фрагменты: нет.
- Связанные справочники: `Currency` (lookupPickerField, optionsContainer `currenciesDc`).

## 4. Модель поведения и интерактивность (Behavior Model)

### 4.1 Lifecycle

- `@LoadDataBeforeShow` — данные и список валют загружаются до показа; Init/BeforeShow/AfterShow не переопределены.
- `focusComponent="rateField"` — фокус на поле ставки при открытии.

### 4.2 Скрытые вычисления

- В контроллере нет кастомных вычислений; добавлена только presentation-навигация: `focusRatesSection()` фокусирует `rateField` и подсвечивает `ratesNav` (снимает подсветку с `commentNav`), `focusCommentSection()` фокусирует `commentField` и подсвечивает `commentNav`. Вычисления выполняются триггерами БД после commit: `fn_outstaffing_margin_recalc()` (BEFORE INSERT OR UPDATE OF rate, max_salary, max_ie_salary) пересчитывает MARGIN_TK/MARGIN_IE/NET_PROFIT_TK/NET_PROFIT_IE; `fn_outstaffing_rates_audit()` пишет INSERT/UPDATE-снимок в историю (для UPDATE — только при реальном изменении бизнес-полей).

### 4.3 Валидация и сохранение

- Обязательные поля: `rate` и `currency` (`@NotNull` в entity) — CUBA валидирует при commit.
- Кастомных валидаторов и BeforeCommit/AfterCommit-обработчиков нет.
- Сохранение: `windowCommitAndClose` → commit → триггеры БД (пересчёт + аудит).

## 5. Логика управляющих элементов (Actions & Buttons Logic)

| Действие | Цепочка «нажал → проверка → результат» |
|----------|------------------------------------------|
| `ratesNav` (sidebar «Ставки») | Нажал пункт навигации → фокус на `rateField` → пункт «Ставки» подсвечен, «Комментарий» снят с подсветки |
| `commentNav` (sidebar «Комментарий») | Нажал пункт навигации → фокус на `commentField` → пункт «Комментарий» подсвечен, «Ставки» снят с подсветки |
| `windowCommitAndClose` (commitAndCloseBtn) | Нажал «OK» → CUBA-валидация (@NotNull rate/currency) → commit → триггеры БД (маржинальность + аудит) → закрытие формы |
| `windowClose` (closeBtn) | Нажал «Отмена» → закрытие без сохранения (изменения отбрасываются) |

## 6. Визуальная компоновка элементов (Visual Layout Schema)

- Двухпанельная композиция по контракту серии Dictionary: `layout stylename="outstaffing-rates-editor"` → `hbox mainLayout stylename="edit-screen-layout"` (sidebar + workspace).
- Sidebar 270px (`edit-sidebar`): визуальный блок `edit-sidebar-visual` со штатной заглушкой-логотипом `ovaFallbackImage` 176×176 (`fallbackThemePath=icons/hunttech-logo.png`, stylename `dictionary-logo-image`); блок идентификации (`edit-sidebar-subtitle` «Рейт по аутстафу», `edit-sidebar-title` ← свойство rate); label-навигация `label-navigation` с полосой-заголовком `label-nav-title dictionary-navigation-title` и пунктами `ratesNav`/`commentNav` (`label-nav-item`, активный — `label-nav-item-active`); подсказка `edit-sidebar-hint`.
- Workspace (`edit-workspace`): toolbar (`edit-toolbar-title`, `edit-toolbar-description`), `scrollBox edit-workspace-scroll` с карточками:
  - `ratesSection` (`groupBox` «Ставки», `edit-card`, `showAsPanel="true"`): `rateField`, `minSalaryField`, `maxSalaryField`, `maxIESalaryField` (textField), `currencyField` (lookupPickerField, optionsContainer `currenciesDc`);
  - `commentSection` (`groupBox` «Комментарий», `edit-card`, `showAsPanel="true"`): `commentField` (textArea, rows=5).
- Footer: `hbox editActions stylename="edit-footer-actions"` — commitAndCloseBtn, closeBtn.
- Caption: `outstaffingRatesEdit.caption=Рейты по аутстаффингу редактор` (messages_ru.properties экрана).

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-13 | Форма приведена к контракту серии справочных Edit-форм: двухпанельная композиция (sidebar 270px + edit-card), полоса-заголовок навигации `dictionary-navigation-title`, штатная заглушка-логотип `OvaFallbackImage` 176×176, `showAsPanel="true"` у карточек, полноэкранный `dialogMode` (100%/100%/modal), presentation-навигация `focusRatesSection`/`focusCommentSection`; добавлены msg-ключи полей в локальный messages-пакет; caption и data bindings/JPQL не изменялись |
| 2026-07-31 | Создание UI Spec; зафиксирована связь с миграцией 260731-1 (триггер пересчёта маржинальности и аудит при commit) — сущность/экран не менялись |

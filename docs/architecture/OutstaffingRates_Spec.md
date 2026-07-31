# OutstaffingRates — архитектурная спецификация (Рейты по аутстафу)

> Cross-links: living-doc сущности [OutstaffingRates.md](../entities/outstaffing-rates/OutstaffingRates.md) · UI Spec: [OutstaffingRatesBrowse_Spec.md](../ui/OutstaffingRatesBrowse_Spec.md), [OutstaffingRatesEdit_Spec.md](../ui/OutstaffingRatesEdit_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

`OutstaffingRates` — тарифная шкала аутстафа HRM HuntTech. Каждая ступень задаёт ставку (руб/час) и связанные с ней экономические параметры: вилку зарплаты по ТК (минимальная/максимальная, «на руки») и максимальную выплату ИП (за 164 ч/мес). Шкала — основа для заключения контрактов аутстафа: по ставке специалиста определяется вознаграждение и маржа компании на ступени, а также сравнение форматов «ИП vs ТК» (ИП выгоднее компании на 6.3–7.2% при тарифе 2026).

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Пункт меню «application-hadbook» (справочники) → «Рейты по аутстаффингу» (`hunttech_OutstaffingRates.browse`, icon `RUBLE`). Browse открывает форму редактирования ступени (`hunttech_OutstaffingRates.edit`); сам browse — `StandardLookup`, т.е. может использоваться как lookup для выбора ставки в других формах. Сущность связана с `Currency` через FK `currency`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие browse → загрузка всех активных ступеней (JPQL `select e from hunttech_OutstaffingRates e`), данные read-only.
- Открытие edit → форма полей ступени; при сохранении CUBA пишет запись, а триггеры БД довершают работу: `trg_orr_margin_recalc` пересчитывает 4 колонки маржинальности (margin_tk/margin_ie/net_profit_tk/net_profit_ie), триггеры аудита пишут снимок INSERT/UPDATE в `HUNTTECH_OUTSTAFFING_RATES_HISTORY`.
- Минимальная зарплата — расчётная величина: `min_salary = FLOOR(max_salary × 0.5 / 100) × 100` (единое правило с 31.07.2026; раньше — ручные «грейдовые» плато).
- Шкала: 1000–6000, шаг 50, 101 активная ступень (3400 восстановлена и 5050–6000 добавлены 31.07.2026).

---

## 1. Архитектура Сущности (Data Model Layer)

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.hunttech.entity.OutstaffingRates` |
| **Имя в CUBA** | `hunttech_OutstaffingRates` |
| **Таблица** | `HUNTTECH_OUTSTAFFING_RATES` |
| **Базовый класс** | `StandardEntity` (uuid PK, version, create_ts/created_by, update_ts/updated_by, delete_ts/deleted_by) |
| **NamePattern** | `%s %s|rate,currency` |

### Поля

| Поле Java | Колонка | Тип | Ограничения | Описание |
|-----------|---------|-----|-------------|----------|
| `rate` | `RATE` | NUMERIC | NOT NULL | ставка, руб/час |
| `minSalary` | `MIN_SALARY` | NUMERIC | | минимальная зарплата по ТК (расчётная: 50% от max, вниз до 100) |
| `maxSalary` | `MAX_SALARY` | NUMERIC | | максимальная зарплата по ТК, на руки |
| `maxIESalary` | `MAX_IE_SALARY` | NUMERIC | | выплата ИП за 164 ч/мес |
| `currency` | `CURRENCY_ID` | FK → `Currency` | NOT NULL, LAZY | валюта |
| `comment` | `COMMENT_` | TEXT (Lob) | | служебный комментарий (даты корректировок DD/MM/YYYY) |

### Расчётные колонки (БД, вне метамодели CUBA)

Созданы миграцией `260731-1-addOutstaffingRatesMarginColumns.sql`; в Java-entity не задекларированы (приложение их не читает/не пишет — решение от 31.07.2026):

| Колонка | Тип | Смысл |
|---------|-----|-------|
| `MARGIN_TK` | NUMERIC(8,2) | маржа по ТК, % (выручка × 227/247 с учётом отпуска 20 раб. дней) |
| `MARGIN_IE` | NUMERIC(8,2) | маржа по ИП, % от выручки (ставка × 164 ч) |
| `NET_PROFIT_TK` | NUMERIC(19,2) | чистая прибыль по ТК, руб/мес |
| `NET_PROFIT_IE` | NUMERIC(19,2) | чистая прибыль по ИП, руб/мес |

### Аудит-таблица

`HUNTTECH_OUTSTAFFING_RATES_HISTORY` (BIGSERIAL PK, `outstaffing_rate_id` UUID без FK — история переживает жёсткое удаление источника; `action` INSERT/UPDATE, `changed_at`, `changed_by`, old/new по rate/min/max/max_ie/comment, полные снимки `old_data`/`new_data` JSONB; индекс `idx_orr_history_on_rate_id`).

### DDL / миграции

- `modules/core/db/update/postgres/26/260731-1-addOutstaffingRatesMarginColumns.sql` — колонки маржинальности + COMMENT; функция `fn_outstaffing_margin_recalc` + триггер `trg_orr_margin_recalc`; аудит-таблица + индекс; функция `fn_outstaffing_rates_audit` + триггеры `trg_orr_audit_insert`/`trg_orr_audit_update`; заполнение существующих строк (`UPDATE ... SET rate = rate`). Идемпотентен; регистрацию в `SYS_DB_CHANGELOG` выполняет `updateDb`.

## 2. Слой Выборок Данных (Fetch Plans / Views Layer)

`modules/global/src/com/company/hunttech/views.xml`:

```xml
<view entity="hunttech_OutstaffingRates" name="outstaffingRates-view" extends="_local">
    <property name="currency" view="_local"/>
</view>
```

- `extends="_local"` покрывает все локальные поля (rate, minSalary, maxSalary, maxIESalary, comment) — новые расчётные колонки в view не включаются, т.к. отсутствуют в метамодели.
- FK `currency` — `_local` (достаточно для отображения в таблице/форме).
- Справочник `Currency`: `currency-view` (`extends="_local"`), в edit-экране используется `_minimal`.

## 3. Списочные интерфейсы (Browse Screens)

- Дескриптор: `modules/web/src/com/company/hunttech/web/screens/outstaffingrates/outstaffing-rates-browse.xml`
- Контроллер: `OutstaffingRatesBrowse.java` — `StandardLookup<OutstaffingRates>`, `@UiController("hunttech_OutstaffingRates.browse")`, `@LookupComponent("outstaffingRatesesTable")`, `@LoadDataBeforeShow`; кастомной логики нет.
- Данные: `<data readOnly="true">`, collection `outstaffingRatesesDc` (view `_local` + currency `_local`), loader `outstaffingRatesesDl`: `select e from hunttech_OutstaffingRates e` (все активные ступени).
- Таблица `groupTable` `outstaffingRatesesTable`: колонки rate, minSalary, maxSalary, maxIESalary, currency, comment; `rowsCount`; `buttonsPanel` (createBtn/editBtn/removeBtn); actions create/edit/remove.
- Фильтр `filter` (applyTo=outstaffingRatesesTable, dataLoader=outstaffingRatesesDl, `include=".*"`).
- Lookup-режим: скрытый `hbox lookupActions` (lookupSelectAction/lookupCancelAction).
- `dialogMode height="600" width="800"`.

## 4. Формы редактирования (Edit Screens)

- Дескриптор: `modules/web/src/com/company/hunttech/web/screens/outstaffingrates/outstaffing-rates-edit.xml`
- Контроллер: `OutstaffingRatesEdit.java` — `StandardEditor<OutstaffingRates>`, `@UiController("hunttech_OutstaffingRates.edit")`, `@EditedEntityContainer("outstaffingRatesDc")`, `@LoadDataBeforeShow`; кастомной логики нет.
- Данные: instance `outstaffingRatesDc` (view `_local` + currency), loader; collection `currenciesDc` (Currency, view `_minimal`) — опции для picker.
- Форма `form` (dataContainer=outstaffingRatesDc), column 350px: rateField, minSalaryField, maxSalaryField, maxIESalaryField (textField), currencyField (lookupPickerField, optionsContainer=currenciesDc), commentField (textArea, rows=5).
- Действия: `windowCommitAndClose`, `windowClose`; `dialogMode 600×800`.
- Валидация: обязательные `rate` и `currency` (`@NotNull` entity); кастомных валидаторов нет.
- Сохранение: commit CUBA → триггеры БД (пересчёт маржинальности + аудит-запись).

## 5. Компоненты и Фрагменты (UI Fragments & Dialogs)

- Фрагментов и дочерних диалогов у экранов нет.
- Lookup-потребители: browse является `StandardLookup` и может вызываться из форм выбора ставки; выбранная запись возвращается вызывающему экрану.
- DialogMode (600×800) — экраны открываются как модальные диалоги.

## 6. Инструкция по развертыванию с нуля (Deployment Guide)

1. **Миграция БД:** `updateDb` собирает `modules/core/db/update/**/*.sql` и выполняет незарегистрированные, фиксируя в `SYS_DB_CHANGELOG`; скрипт 260731-1 идемпотентен (повторное выполнение безопасно — `IF NOT EXISTS` / `DROP TRIGGER IF EXISTS` / `CREATE OR REPLACE`).
2. **Свежая prod-база:** после загрузки дампа проверить запись `70-hunttech_recruiting/update/postgres/26/260731-1-addOutstaffingRatesMarginColumns.sql` в `SYS_DB_CHANGELOG`; при отсутствии — `updateDb` применит скрипт (безопасно, данные не теряются).
3. **Смена законодательства** (взносы 15%/7.6%, ЕПВБ 2 979 000, НДФЛ, травматизм): обновить `fn_outstaffing_margin_recalc()` новой миграцией и выполнить `UPDATE HUNTTECH_OUTSTAFFING_RATES SET rate = rate` для пересчёта всех строк.
4. **FTS / entity cache:** сущность включена в полнотекстовый поиск — `modules/core/src/com/company/hunttech/fts.xml`, `<entity class="com.company.hunttech.entity.OutstaffingRates"><include re=".*"/></entity>` (индексируются все поля). Entity cache (`cuba.entityCache.*` в `app.properties`) для неё не настроен — менять не требуется.
5. **Сборка и проверка:** `./gradlew restart --no-daemon`; HTTP `/hrm/` = 200; открыть browse (колонки) и edit (сохранение ступени) — без ошибок в логах.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-31 | Создание спецификации; миграция 260731-1 (колонки маржинальности, триггер пересчёта, аудит-таблица и триггеры); шкала 1000–6000; min_salary = FLOOR(max × 0.5 / 100) × 100 |

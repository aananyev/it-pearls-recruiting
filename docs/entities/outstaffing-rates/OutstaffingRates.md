# OutstaffingRates — рейты по аутстафу

> Шкала ставок аутстафа (руб/час) для контрактов по ТК и с ИП. Каждая ступень задаёт вилку зарплаты и выплату ИП; маржинальность пересчитывается триггером БД, изменения фиксируются в аудит-таблице.
> Cross-links: [архитектурная спецификация](../architecture/OutstaffingRates_Spec.md) · [UI Spec Browse](../ui/OutstaffingRatesBrowse_Spec.md) · [UI Spec Edit](../ui/OutstaffingRatesEdit_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Справочник `OutstaffingRates` — тарифная шкала аутстафа HRM HuntTech: ставка (руб/час) → максимальная зарплата по ТК (на руки), максимальная выплата ИП (за 164 ч/мес) и минимальная зарплата по ТК. Шкала используется при заключении контрактов: по ставке специалиста определяется вилка вознаграждения и маржа компании (ИП vs ТК) на каждой ступени.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Экраны: `hunttech_OutstaffingRates.browse` (список ступеней, колонки rate/minSalary/maxSalary/maxIESalary/currency/comment) и `hunttech_OutstaffingRates.edit` (форма ступени). Справочник привязан к `Currency` через FK `currency`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

- Открытие browse → загрузка всех активных ступеней (1000–6000, шаг 50) через `outstaffingRatesesDc` (view `_local` + currency).
- Сохранение/edit → изменение rate, maxSalary, maxIeSalary; после commit триггер `trg_orr_margin_recalc` пересчитывает колонки маржинальности, триггеры аудита пишут снимок изменения в `HUNTTECH_OUTSTAFFING_RATES_HISTORY`.
- Минимальная зарплата — расчётная: `min_salary = FLOOR(max_salary × 0.5 / 100) × 100` (правило действует с 31.07.2026).

---

## 1. Обзор

| Параметр | Значение |
|----------|----------|
| **Java-класс** | `com.company.hunttech.entity.OutstaffingRates` |
| **Имя в CUBA** | `hunttech_OutstaffingRates` |
| **Таблица БД** | `HUNTTECH_OUTSTAFFING_RATES` |
| **Тип данных** | справочник (тарифная шкала) |
| **Ожидаемый объём** | сотни записей (101 активная ступень, шаг 50) |
| **NamePattern** | `%s %s|rate,currency` |

## 2. Поля сущности

| Поле Java | Колонка БД | Тип | Ограничения | Описание |
|-----------|------------|-----|-------------|----------|
| `rate` | `RATE` | NUMERIC | NOT NULL | ставка, руб/час (1000–6000, шаг 50) |
| `minSalary` | `MIN_SALARY` | NUMERIC | | минимальная зарплата по ТК; `FLOOR(MAX_SALARY × 0.5 / 100) × 100` |
| `maxSalary` | `MAX_SALARY` | NUMERIC | | максимальная зарплата по ТК, на руки |
| `maxIESalary` | `MAX_IE_SALARY` | NUMERIC | | выплата ИП за 164 ч/мес |
| `currency` | `CURRENCY_ID` | FK → Currency | NOT NULL, LAZY | валюта ставки |
| `comment` | `COMMENT_` | TEXT (Lob) | | служебный комментарий (даты корректировок DD/MM/YYYY) |

### Расчётные колонки (БД, вне метамодели CUBA)

Добавлены миграцией `260731-1-addOutstaffingRatesMarginColumns.sql`; в Java-entity НЕ задекларированы (приложение их не читает и не пишет — по решению от 31.07.2026):

| Колонка БД | Тип | Смысл |
|------------|-----|-------|
| `MARGIN_TK` | NUMERIC(8,2) | маржа по ТК, % (выручка × 227/247 с учётом отпуска) |
| `MARGIN_IE` | NUMERIC(8,2) | маржа по ИП, % от выручки (ставка × 164 ч) |
| `NET_PROFIT_TK` | NUMERIC(19,2) | чистая прибыль по ТК, руб/мес |
| `NET_PROFIT_IE` | NUMERIC(19,2) | чистая прибыль по ИП, руб/мес |

## 3. Представления (views.xml)

- `outstaffingRates-view` — `extends="_local"` + `currency` (`view="_local"`). Локальные поля подхватываются автоматически; расчётные колонки в view не включаются (их нет в метамодели).

## 4. Экраны

- `hunttech_OutstaffingRates.browse` — `outstaffing-rates-browse.xml` / `OutstaffingRatesBrowse.java` (`StandardLookup<OutstaffingRates>`), колонки: rate, minSalary, maxSalary, maxIESalary, currency, comment; readOnly data; create/edit/remove.
- `hunttech_OutstaffingRates.edit` — `outstaffing-rates-edit.xml` / `OutstaffingRatesEdit.java`: поля rate, minSalary, maxSalary, maxIESalary, currency (lookup), comment.

## 5. Бизнес-логика (триггеры БД)

Реализована на уровне PostgreSQL (не в Java) — поведение консистентно для CUBA, ботов и прямых UPDATE.

- **Пересчёт маржинальности:** `fn_outstaffing_margin_recalc()` + триггер `trg_orr_margin_recalc` (BEFORE INSERT OR UPDATE OF rate, max_salary, max_ie_salary). Константы модели 2026: 164 ч/мес; НДС 5/105 + УСН 6%; взносы ТК 15% до ЕПВБ 2 979 000, 7.6% сверх; травматизм 0.2%; НДФЛ 13/15%; отпуск — выручка × 227/247.
- **Аудит изменений:** таблица `HUNTTECH_OUTSTAFFING_RATES_HISTORY` (BIGSERIAL, снимки old/new + JSONB) + `fn_outstaffing_rates_audit()` + триггеры `trg_orr_audit_insert` (AFTER INSERT) и `trg_orr_audit_update` (AFTER UPDATE ... WHEN хотя бы одно бизнес-поле реально изменилось — no-op апдейты не логируются).
- При изменении законодательства: обновить функцию в миграции и выполнить `UPDATE HUNTTECH_OUTSTAFFING_RATES SET rate = rate`.

## 6. База данных и миграции

| Файл | Назначение |
|------|------------|
| `modules/core/db/update/postgres/26/260731-1-addOutstaffingRatesMarginColumns.sql` | 4 колонки маржинальности + COMMENT; функция и триггер пересчёта; аудит-таблица + индекс; функция и 2 триггера аудита; заполнение существующих строк (`UPDATE rate = rate`). Идемпотентен (IF NOT EXISTS / DROP TRIGGER IF EXISTS / CREATE OR REPLACE); регистрацию в `SYS_DB_CHANGELOG` выполняет `updateDb`. |

Шкала: 1000–6000, шаг 50, 101 активная ступень (3400 восстановлена 31.07.2026; 5050–6000 добавлены 31.07.2026). Актуальные правила расчёта — в навыке `outstaffing-rates-adjustment`.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-07-31 | Миграция 260731-1: колонки маржинальности, триггер пересчёта, аудит-таблица и триггеры; шкала расширена до 6000; min_salary = FLOOR(max × 0.5 / 100) × 100; ступень 3400 восстановлена |

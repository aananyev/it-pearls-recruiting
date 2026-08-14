# Stage 4: baseline и выбор стратегии suggestion-поиска компаний

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `55094e64cacd1ed494e1f5f75a75f549abfc3260`  
**Тип этапа:** измерение и технический выбор без изменения поведения

## 1. Основание

Cleanup Stage 3 завершён:

- `currentCompaniesDc/currentCompaniesLc` удалены;
- полный справочник компаний не участвует в initial open;
- create-company выполняет точечную загрузку по UUID;
- unit-тесты, `ScreenViewIntegrityTest`, сборка, HTTP 200 и ручные сценарии пройдены.

Следующий потенциальный источник задержки — server-side suggestion-поиск `currentCompanyField`.

## 2. Текущий контракт

XML-параметры:

| Параметр | Значение |
|---|---|
| Компонент | `SuggestionPickerField<Company>` |
| Минимальная длина | 2 символа |
| Лимит | 50 |
| Задержка | 300 мс |
| Семантика | contains |
| View | `company-picker-view` |

Текущий JPQL:

```jpql
select e
from hunttech_Company e
where lower(e.comanyName) like lower(:searchString)
order by e.comanyName, e.companyShortName
```

Параметр формируется как `%строка%`.

Контроллер также устанавливает программный `SearchExecutor` с той же семантикой. Stage 4 должен подтвердить, какой executor фактически обслуживает runtime-запрос после инициализации `tabMain`.

## 3. Жёсткие границы Stage 4

Запрещено изменять:

- `JobCandidateEdit.java`;
- `job-candidate-edit.xml`;
- JPQL и search executor;
- `minSearchStringLength`;
- `suggestionsLimit`;
- `asyncSearchDelayMs`;
- actions `lookup`, `open`, `createCompany`;
- сущность `Company`;
- views;
- Liquibase;
- индексы, расширения и конфигурацию PostgreSQL;
- production.

Разрешены только read-only замеры, SQL `EXPLAIN`, отчёт и обновление coordination.

## 4. Проверяемые гипотезы

### H1 — текущий contains-поиск достаточен

При размере справочника около 5 623 строк текущий запрос может укладываться в допустимую задержку без индекса и без изменения пользовательской семантики.

### H2 — два символа дают нестабильный объём результата

Короткие выражения могут просматривать значительную часть таблицы и возвращать лимит 50, увеличивая materialization и передачу данных.

### H3 — prefix-поиск быстрее, но меняет функциональное поведение

`строка%` потенциально лучше использует B-tree/functional index, но перестаёт находить совпадения внутри названия. Он может быть рекомендован только при доказанном выигрыше и отдельном согласовании бизнес-поведения.

### H4 — trigram-индекс сохраняет contains-семантику

`pg_trgm` и GIN/GiST могут ускорить `%строка%`, но требуют изменения БД и Liquibase. На Stage 4 допускается только read-only проверка доступности расширения и расчёт ожидаемой пользы.

## 5. Методика измерения

### 5.1 Поисковые выражения

Проверить минимум:

- `ян` — короткое частичное совпадение;
- `сб` — короткое выражение;
- `ооо` — распространённый фрагмент организационно-правовой формы;
- `тех` — распространённый технологический фрагмент;
- одно выражение, которое возвращает 0 строк;
- одно выражение, которое возвращает 1–5 строк.

Для каждого выражения выполнить:

- 1 прогревочный запуск;
- 10 измерительных запусков текущего runtime-поиска;
- 10 read-only SQL-запусков current contains;
- 10 read-only SQL-запусков prefix-варианта;
- отдельный contains-тест с первыми тремя символами, когда выражение длиннее двух.

### 5.2 Метрики

Зафиксировать:

- SQL MIN, MAX, AVG, P50, P95;
- пользовательскую задержку от ввода до появления popup;
- planning time и execution time;
- actual rows и rows removed by filter;
- shared hit/read buffers;
- количество возвращённых Company;
- объём сериализованного ответа, если доступен;
- наличие лимита 50 в фактическом SQL;
- число SQL-запросов на один ввод;
- какой executor фактически отработал: XML query или программный `SearchExecutor`.

### 5.3 SQL-планы

Для каждого класса выражений выполнить:

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT ...
FROM hunttech_company
WHERE lower(comany_name) LIKE lower('%строка%')
ORDER BY comany_name, company_short_name
LIMIT 50;
```

И read-only comparison:

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT ...
FROM hunttech_company
WHERE lower(comany_name) LIKE lower('строка%')
ORDER BY comany_name, company_short_name
LIMIT 50;
```

Использовать реальные имена колонок из production-compatible schema. Не создавать временные или постоянные индексы.

### 5.4 Проверка индексов и расширений

Read-only запросами проверить:

- индексы `HUNTTECH_COMPANY`;
- наличие индекса по `lower(comany_name)`;
- доступность `pg_trgm`;
- наличие trigram-индекса;
- размер таблицы и число активных строк;
- статистику `pg_stats` по `comany_name`.

## 6. Критерии решения

### KEEP_CURRENT_SUGGESTION

Выдать этот вердикт, если:

- runtime P95 от ввода до popup приемлем;
- SQL P95 стабилен;
- число строк и сериализация не создают заметной задержки;
- альтернативы не дают существенного выигрыша.

### TUNE_MIN_LENGTH_LIMIT

Выдать, если основная проблема проявляется только на двух символах или при возврате 50 записей, а три символа/меньший лимит дают существенное улучшение.

Это изменение поведения нельзя реализовывать без отдельного согласования.

### PREFIX_SEARCH_RECOMMENDED

Выдать, если prefix-вариант существенно быстрее и текущая contains-семантика является основной причиной задержки.

В отчёте обязательно перечислить реальные компании, которые перестанут находиться при prefix-поиске.

### TRIGRAM_INDEX_RECOMMENDED

Выдать, если contains-семантику требуется сохранить, SQL остаётся узким местом и планы показывают пользу trigram-индекса.

Нужны проект Liquibase changelog, rollback и отдельное разрешение на изменение БД.

### STAGE_4_BLOCKED

Выдать, если невозможно получить SQL-планы, runtime timings или однозначно определить фактический executor.

## 7. Обязательный отчёт Hermes

Сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-company-suggestion-stage-4-baseline/
stage-4-suggestion-hermes-report.md
```

Отчёт должен содержать:

1. точный SHA;
2. подтверждение отсутствия code/XML/DB изменений;
3. фактический runtime executor;
4. таблицу всех запусков;
5. P50/P95;
6. планы `EXPLAIN (ANALYZE, BUFFERS)`;
7. сравнение contains/prefix/трёх символов;
8. результаты проверки индексов и `pg_trgm`;
9. влияние лимита 50;
10. один итоговый вердикт из раздела 6;
11. рекомендацию следующего минимального изменения или вывод «изменение не требуется».

## 8. Приёмка

Stage 4 считается завершённым только после review отчёта. До этого запрещено:

- менять пользовательскую семантику поиска;
- создавать индексы;
- менять минимальную длину, лимит или задержку;
- начинать следующий implementation stage.

# Stage 7 — baseline загрузки городов в JobCandidateEdit

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `a73764e4ebdc3ce044c1a603b88e3837c03aefda`  
**Тип этапа:** read-only performance baseline

## 1. Основание

Stage 6 принят. Коллекция должностей переведена на `position-picker-view`, бизнес-поведение сохранено.

Единственный оставшийся полный reference loader вкладки «Основное», для которого нет подтверждённых измерений, — `citiesDl`:

```xml
<collection id="citiesDc" class="com.company.hunttech.entity.City">
    <view extends="city-picker-view"/>
    <loader id="citiesDl" cacheable="true">
        <query><![CDATA[
            select e from hunttech_City e order by e.cityRuName
        ]]></query>
    </loader>
</collection>
```

Поле:

```xml
<lookupPickerField id="jobCityCandidateField"
                   optionsContainer="citiesDc"
                   property="cityOfResidence"
                   required="true">
    <actions>
        <action id="lookup" type="picker_lookup"/>
    </actions>
</lookupPickerField>
```

## 2. Цель

Определить, является ли полная загрузка `City` измеримым узким местом открытия `JobCandidateEdit`, и выбрать минимальную стратегию:

- оставить текущий lookup;
- отложить loader до первого взаимодействия;
- заменить поле на server-side suggestion;
- использовать middleware cache;
- оптимизировать SQL или индекс.

На Stage 7 никакая стратегия не реализуется.

## 3. Жёсткие границы

Запрещено изменять:

- `JobCandidateEdit.java`;
- `job-candidate-edit.xml`;
- `views.xml`;
- тип `jobCityCandidateField`;
- `citiesDc`, `citiesDl`, JPQL и `cacheable`;
- сущность `City`;
- Liquibase, индексы и PostgreSQL extensions;
- production;
- SCSS;
- бизнес-логику сохранения кандидата.

Разрешены:

- read-only SQL;
- `EXPLAIN (ANALYZE, BUFFERS)`;
- runtime profiling локального приложения;
- анализ network payload и loader events;
- отчёт и coordination.

## 4. Обязательные измерения

### 4.1 Размер справочника

Зафиксировать:

- общее число строк `City`;
- число активных/неудалённых строк с учётом CUBA soft deletion;
- число строк с пустым `cityRuName`;
- число дубликатов `lower(cityRuName)`;
- среднюю и максимальную длину названия;
- размер таблицы и индексов.

### 4.2 View graph

Инвентаризировать `city-picker-view`:

- базовый view (`_minimal`, `_local` или иной);
- все поля;
- вложенные associations;
- системные свойства;
- поля, реально используемые `jobCityCandidateField`, `JobCandidateEdit` и instance name.

Составить Data View Integrity matrix:

| Getter/рендеринг | Требуемое поле | Есть в view | Результат |
|---|---|---|---|

### 4.3 SQL baseline

Выполнить 1 прогревочный и 10 измерительных запусков:

```sql
SELECT ...
FROM hunttech_city
WHERE delete_ts IS NULL
ORDER BY city_ru_name;
```

Использовать фактические имена таблицы и колонок текущей схемы.

Зафиксировать:

- MIN;
- MAX;
- AVG;
- P50;
- P95;
- planning time;
- execution time;
- actual rows;
- shared hit/read buffers;
- sort method и memory;
- rows removed by filter.

Получить:

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
```

### 4.4 Runtime baseline

Для существующего и нового кандидата выполнить:

- 1 прогревочное открытие;
- 10 измерительных открытий.

Отдельно измерить:

- время от начала `citiesDl.load()` до `PostLoadEvent`;
- число SQL-запросов `City` на одно открытие;
- размер `citiesDc` после загрузки;
- approximate heap delta;
- размер server response/payload, если доступен;
- время до доступности `jobCityCandidateField`;
- cold и warm cache;
- повторное открытие в той же сессии;
- влияние `cacheable="true"`.

Instrumentation должна быть временной и удалена до итогового коммита. Предпочтительны JFR, SQL log, CUBA performance statistics или внешняя обёртка, не изменение production-кода.

### 4.5 Read-only сравнения

Без изменения приложения сравнить:

1. полный текущий список;
2. запрос с `LIMIT 50`;
3. prefix-поиск `lower(cityRuName) like lower('строка%')`;
4. contains-поиск `lower(cityRuName) like lower('%строка%')`;
5. загрузку только текущего выбранного города по UUID.

Проверить существующие индексы и доступность `pg_trgm`, ничего не создавая.

## 5. Проверяемые гипотезы

### H1 — KEEP_FULL_LOOKUP

Полный список мал, SQL и payload стабильны, loader не влияет на P95 открытия.

### H2 — DEFER_LOADER_UNTIL_INTERACTION

SQL сам по себе быстрый, но materialization/payload заметны; большинство открытий не используют поле города.

### H3 — CONVERT_TO_SUGGESTION

Справочник велик, полный payload дорог, server-side поиск значительно дешевле. Это изменение UX требует отдельного implementation stage.

### H4 — NARROW_PICKER_VIEW

`city-picker-view` содержит лишние поля или associations. Разрешено рекомендовать только при доказанной Data View Integrity.

### H5 — MIDDLEWARE_CACHE_RECOMMENDED

Список стабилен и часто переиспользуется, но повторная materialization остаётся дорогой.

### H6 — INDEX_RECOMMENDED

SQL sort/filter является узким местом и существующие индексы не покрывают запрос. Нужны отдельные Liquibase и rollback.

## 6. Допустимые вердикты

Для Stage 7 выбрать ровно один основной вердикт:

```text
KEEP_FULL_LOOKUP
NARROW_PICKER_VIEW
DEFER_LOADER_UNTIL_INTERACTION
CONVERT_TO_SUGGESTION
MIDDLEWARE_CACHE_RECOMMENDED
INDEX_RECOMMENDED
STAGE_7_BLOCKED
```

Допускается одна вторичная рекомендация, но implementation scope следующего этапа должен быть один.

## 7. Обязательный отчёт Hermes

Сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-city-loader-stage-7-baseline/
stage-7-city-loader-hermes-report.md
```

Отчёт должен содержать:

1. точный базовый и итоговый SHA;
2. подтверждение отсутствия Java/XML/view/DB изменений;
3. row counts и размер таблицы;
4. полный состав `city-picker-view`;
5. Data View Integrity matrix;
6. все 10 SQL-замеров и агрегаты;
7. `EXPLAIN (ANALYZE, BUFFERS)`;
8. все 10 runtime-замеров и агрегаты;
9. число SQL, размер коллекции, heap/payload;
10. cold/warm сравнение;
11. read-only сравнение full/limit/prefix/contains/by-id;
12. проверку индексов и `pg_trgm`;
13. один основной вердикт;
14. точный минимальный контракт Stage 8 или вывод «изменение не требуется».

## 8. Приёмка

Stage 7 не принимается по факту HTTP 200 или деплоя. Нужны численные измерения и репозиторный отчёт.

До review отчёта запрещено:

- менять поле города;
- блокировать автоматическую загрузку;
- добавлять listener focus/click;
- менять view;
- создавать индекс;
- начинать Stage 8 implementation.

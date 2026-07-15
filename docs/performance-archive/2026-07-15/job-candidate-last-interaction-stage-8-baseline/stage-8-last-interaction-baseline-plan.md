# Stage 8 — baseline загрузки последнего взаимодействия JobCandidateEdit

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Базовый SHA:** `853e8bc22b08dbc882ec1c68cc468860e5d16277`  
**Тип этапа:** read-only performance baseline

## 1. Назначение и бизнес-смысл

Последнее взаимодействие кандидата используется экраном HRM HuntTech для определения текущего состояния работы с кандидатом и последующих действий рекрутера.

Сейчас объект загружается синхронно в `JobCandidateEdit.onBeforeShow()`:

```java
lastIteraction = interactionService.getLastIteraction(getEditedEntity());
lastIteractionLoaded = true;
```

Вызов выполняется до полного отображения формы и входит в критический путь initial open.

Stage 8 должен определить, является ли этот middleware-вызов измеримым источником задержки, и можно ли безопасно:

- оставить его синхронным;
- сузить view;
- ускорить SQL индексом;
- перенести загрузку после показа формы;
- переиспользовать результат ленивой загрузки вкладки взаимодействий.

На Stage 8 никакая стратегия не реализуется.

## 2. Текущий технический контракт

### 2.1 Вызов экрана

`JobCandidateEdit.onBeforeShow()` вызывает сервис только для уже открываемого экрана. Для нового кандидата сервис получает entity с UUID и возвращает отсутствие результата, если взаимодействий нет.

Вызов располагается после настройки основных labels, links и изображения кандидата, но до завершения `BeforeShowEvent`.

### 2.2 Реализация сервиса

```java
private static final String QUERY_LAST_BY_CANDIDATE =
        "select e from hunttech_IteractionList e "
                + "where e.candidate = :candidate "
                + "order by e.numberIteraction desc";

public IteractionList getLastIteraction(JobCandidate jobCandidate) {
    if (jobCandidate == null || jobCandidate.getId() == null) {
        return null;
    }
    return dataManager.load(IteractionList.class)
            .query(QUERY_LAST_BY_CANDIDATE)
            .parameter("candidate", jobCandidate)
            .maxResults(1)
            .view("iteractionList-picker-view")
            .optional()
            .orElse(null);
}
```

### 2.3 View

Используется `iteractionList-picker-view`. Необходимо проверить полный graph и сопоставить его с фактическими getter'ами после загрузки.

### 2.4 Ожидаемый SQL-паттерн

```sql
SELECT ...
FROM hunttech_iteraction_list
WHERE candidate_id = ?
  AND delete_ts IS NULL
ORDER BY number_iteraction DESC
LIMIT 1;
```

Фактические имена таблицы и колонок необходимо получить из текущей схемы.

## 3. Жёсткие границы Stage 8

Запрещено изменять:

- `JobCandidateEdit.java`;
- `InteractionService.java`;
- `InteractionServiceBean.java`;
- `job-candidate-edit.xml`;
- `views.xml`;
- сущности;
- Liquibase;
- индексы;
- PostgreSQL extensions;
- SCSS;
- production;
- порядок lifecycle-событий;
- бизнес-логику работы с взаимодействиями.

Разрешены:

- read-only SQL;
- `EXPLAIN (ANALYZE, BUFFERS, VERBOSE)`;
- runtime profiling локального приложения;
- временная локальная instrumentation при условии полного удаления до commit;
- JFR и CUBA performance statistics;
- отчёт и coordination.

Итоговый commit Stage 8 не должен содержать Java/XML/DB изменений.

## 4. Обязательная инвентаризация использования результата

До измерений найти все обращения в `JobCandidateEdit` к:

- `lastIteraction`;
- `lastIteractionLoaded`;
- `interactionService.getLastIteraction(...)`;
- `QUERY_GET_LAST_ITERACTION`.

Для каждого обращения составить таблицу:

| Метод | Lifecycle/действие | Используемые поля | Нужно до first paint | Допустима отложенная загрузка |
|---|---|---|---|---|

Отдельно определить:

- влияет ли `lastIteraction` на видимые элементы вкладки `tabMain`;
- используется ли значение только при создании нового взаимодействия;
- используется ли значение только при сохранении;
- может ли оно быть вычислено из уже лениво загруженной `iteractionList`;
- существует ли риск изменения поведения при переносе в `AfterShow` или вкладку взаимодействий.

## 5. Профили кандидатов для измерений

Использовать минимум пять профилей:

1. новый кандидат;
2. существующий кандидат без взаимодействий;
3. кандидат с 1 взаимодействием;
4. кандидат с 10–50 взаимодействиями;
5. тяжёлый кандидат с максимальным или близким к максимальному числом взаимодействий.

Для каждого зафиксировать:

- UUID кандидата в обезличенном виде или внутреннюю метку;
- количество активных взаимодействий;
- максимальный `numberIteraction`;
- наличие soft-deleted строк;
- наличие строк с `numberIteraction IS NULL`;
- дубликаты максимального номера, если есть.

## 6. SQL baseline

### 6.1 Распределение данных

Read-only запросами получить:

- общее число строк `IteractionList`;
- число активных строк;
- число кандидатов с взаимодействиями;
- MIN/AVG/P50/P95/MAX количества взаимодействий на кандидата;
- число NULL в `candidate_id`;
- число NULL в `number_iteraction`;
- число дубликатов `number_iteraction` внутри одного кандидата;
- размер таблицы и индексов.

### 6.2 Текущий запрос

Для каждого профиля выполнить:

- 1 прогревочный запуск;
- 10 измерительных запусков текущего запроса.

Зафиксировать:

- planning time;
- execution time;
- MIN;
- MAX;
- AVG;
- P50;
- P95;
- actual rows;
- rows removed by filter;
- shared hit/read buffers;
- sort method;
- memory;
- выбранный index/scan;
- число обращений к heap;
- результат при отсутствии строк.

Обязательно выполнить:

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
```

### 6.3 Существующие индексы

Read-only проверить:

- все индексы таблицы взаимодействий;
- наличие индекса по `candidate_id`;
- наличие индекса по `number_interaction`;
- наличие составного индекса `(candidate_id, number_interaction DESC)`;
- наличие partial index с `WHERE delete_ts IS NULL`;
- статистику колонок в `pg_stats`;
- актуальность статистики таблицы.

Не создавать индексы и не выполнять `ANALYZE` без отдельного разрешения.

## 7. Read-only сравнение вариантов

Сравнить планы и timings без изменения приложения.

### Вариант A — текущий

```sql
WHERE candidate_id = :candidateId
ORDER BY number_interaction DESC
LIMIT 1
```

### Вариант B — legacy MAX-subquery

В контроллере существует константа старого запроса с условием:

```sql
number_interaction = (
    SELECT max(number_interaction)
    FROM ...
    WHERE candidate_id = :candidateId
)
```

Проверить только как read-only comparison. Не возвращать этот вариант в код без доказанного преимущества и проверки дубликатов максимального номера.

### Вариант C — выбор только ID

```sql
SELECT id
FROM ...
WHERE candidate_id = :candidateId
ORDER BY number_interaction DESC
LIMIT 1
```

Оценить:

- выигрыш SQL;
- необходимость второго запроса для entity;
- итоговую стоимость двух запросов;
- влияние DataManager/view.

### Вариант D — получение из загруженной коллекции

Без изменения кода оценить, может ли последняя запись определяться после `ensureInteractionsLoaded()` без отдельного SQL.

Этот вариант допустим только если результат не нужен до открытия вкладки взаимодействий.

## 8. View baseline

Инвентаризировать `iteractionList-picker-view`:

- базовый view;
- все scalar properties;
- associations и вложенные views;
- system properties;
- поля instance name;
- фактически материализованные joins/дополнительные SQL.

Составить Data View Integrity matrix:

| Getter/использование после `getLastIteraction` | Требуемый path | Есть в view | Нужен до first paint |
|---|---|---|---|

Read-only сравнить:

- текущий `iteractionList-picker-view`;
- `_minimal`;
- специально рассчитанный минимальный набор полей.

На Stage 8 новый view не добавлять.

## 9. Runtime baseline

Для каждого профиля кандидата выполнить:

- 1 прогревочное открытие;
- 10 измерительных открытий.

Зафиксировать:

- полное время `JobCandidateEdit` до отображения;
- длительность `interactionService.getLastIteraction()`;
- middleware round-trip;
- время DataManager materialization;
- число SQL на вызов;
- heap delta;
- cold/warm cache;
- повторное открытие в той же сессии;
- долю вызова в P50 и P95 initial open.

Instrumentation должна разделять:

1. начало `BeforeShow`;
2. начало вызова сервиса;
3. возврат сервиса;
4. завершение `BeforeShow`;
5. первый доступный UI response.

Временные probes должны быть полностью удалены до итогового commit.

## 10. Проверяемые гипотезы

### H1 — KEEP_SYNC_CURRENT

Запрос стабильно быстрый, view узкий, вызов не влияет на P95 initial open.

### H2 — NARROW_INTERACTION_VIEW

SQL быстрый, но materialization или association graph текущего view создаёт заметную стоимость.

### H3 — COMPOSITE_INDEX_RECOMMENDED

Основная стоимость — поиск и сортировка большого набора строк одного кандидата; составной partial index устраняет sort/scan.

### H4 — DEFER_LAST_INTERACTION

Результат не нужен до first paint и может быть загружен в `AfterShow` либо непосредственно перед первым бизнес-действием, которому он требуется.

### H5 — REUSE_LAZY_INTERACTIONS_RESULT

Отдельный запрос можно удалить, вычисляя последнюю запись после существующей ленивой загрузки взаимодействий, без изменения поведения до момента использования.

### H6 — BACKGROUND_LOAD_RECOMMENDED

Результат нужен вскоре после показа, но не обязан блокировать first paint; безопасна фоновая загрузка с UI update в `done()`.

## 11. Допустимые вердикты

Выбрать ровно один основной вердикт:

```text
KEEP_SYNC_CURRENT
NARROW_INTERACTION_VIEW
COMPOSITE_INDEX_RECOMMENDED
DEFER_LAST_INTERACTION
REUSE_LAZY_INTERACTIONS_RESULT
BACKGROUND_LOAD_RECOMMENDED
STAGE_8_BLOCKED
```

Допускается одна вторичная рекомендация, но следующий implementation stage должен иметь один минимальный scope.

## 12. Обязательный отчёт Hermes

Сохранить:

```text
docs/performance-archive/2026-07-15/
job-candidate-last-interaction-stage-8-baseline/
stage-8-last-interaction-hermes-report.md
```

Отчёт должен содержать:

1. точный SHA;
2. подтверждение отсутствия итоговых Java/XML/DB изменений;
3. call graph использования `lastIteraction`;
4. таблицу профилей кандидатов;
5. распределение количества взаимодействий;
6. полный состав `iteractionList-picker-view`;
7. Data View Integrity matrix;
8. все SQL timings и P50/P95;
9. `EXPLAIN (ANALYZE, BUFFERS, VERBOSE)`;
10. список существующих индексов;
11. runtime timings сервиса и initial open;
12. сравнение current/MAX-subquery/ID-only/reuse;
13. один основной вердикт;
14. точный контракт следующего минимального изменения или вывод «изменение не требуется».

## 13. Приёмка

Stage 8 считается завершённым только после review численного отчёта.

HTTP 200 и факт deployment сами по себе не являются результатом performance baseline.

До review запрещено начинать implementation stage или изменять сервис, экран, view и БД.

# Stage 5: baseline загрузки городов и должностей в JobCandidateEdit

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`  
**Тип этапа:** read-only performance baseline  
**Объект:** `JobCandidateEdit.tabMain`

## 1. Основание

После cleanup справочника компаний в критическом пути основной вкладки остаются два collection loader:

- `personPositionsLc` → `personPositionsDc`;
- `citiesDl` → `citiesDc`.

Контроллер блокирует их автоматический запуск до готовности основной вкладки, но `initTabCandidate()` вызывает `ensureReferenceLoadersLoaded()`, после чего обе коллекции загружаются полностью. Поскольку `tabMain` является основной рабочей вкладкой, эти запросы могут входить во время до полной готовности формы.

Stage 4 suggestion-поиска закрыт по решению пользователя без изменения кода. Отчёт Hermes зафиксировал только текущие параметры и факт деплоя; измерения contains/prefix в репозитории отсутствуют. Поэтому на Stage 5 запрещено делать выводы об индексах или менять company suggestion.

## 2. Фактический контракт

### 2.1 Должности

```jpql
select e
from hunttech_Position e
where e.positionRuName not like '%(не использовать)%'
order by e.positionRuName
```

- container: `personPositionsDc`;
- loader: `personPositionsLc`;
- view: `position-view`;
- компонент: `personPositionField`;
- тип: `LookupPickerField<Position>`;
- loader cacheable: `true`.

### 2.2 Города

```jpql
select e
from hunttech_City e
order by e.cityRuName
```

- container: `citiesDc`;
- loader: `citiesDl`;
- view: `city-picker-view`;
- компонент: `jobCityCandidateField`;
- тип: `LookupPickerField<City>`;
- loader cacheable: `true`.

### 2.3 Lifecycle

В `onInit()` оба loader защищены `preventAutoLoadUntilReady(..., referenceLoadersInitialized)`.

При инициализации `tabMain`:

```java
private void ensureReferenceLoadersLoaded() {
    if (referenceLoadersInitialized) {
        return;
    }
    referenceLoadersInitialized = true;
    citiesDl.load();
    personPositionsLc.load();
}
```

Stage 5 должен доказать, когда именно метод вызывается относительно первого отображения формы и сколько времени занимает каждый loader отдельно.

## 3. Жёсткие границы

На Stage 5 запрещено изменять:

- `JobCandidateEdit.java`;
- `job-candidate-edit.xml`;
- типы `LookupPickerField`;
- component ID, dataContainer, property, actions и required;
- JPQL loader'ов;
- views `position-view` и `city-picker-view`;
- сущности `Position`, `City`, `JobCandidate`;
- Liquibase, индексы и PostgreSQL extensions;
- SCSS и визуальную компоновку;
- production.

Разрешены:

- read-only SQL;
- `EXPLAIN (ANALYZE, BUFFERS)`;
- временное диагностическое логирование только вне commit либо инструментирование JVM без изменения ветки;
- отчёт в каталоге Stage 5;
- обновление coordination.

## 4. Проверяемые гипотезы

### H1 — оба справочника достаточно малы

Если число строк, view-граф и сериализация малы, текущие `LookupPickerField` следует сохранить. Замена на suggestion добавит сложность без измеримого выигрыша.

### H2 — `position-view` перегружен

Полная загрузка должностей может быть приемлема по количеству строк, но дорогой из-за вложенных свойств view. Тогда безопаснее сузить только options view без изменения типа поля и пользовательского поведения.

### H3 — один из справочников существенно больше второго

Города и должности должны оцениваться отдельно. Допускается разный вердикт для каждого поля.

### H4 — loader'ы выполняются после первого визуального отображения

Даже если SQL короткий, materialization и передача коллекций могут задерживать готовность компонентов основной вкладки. Нужно измерить UI timing, а не только SQL.

### H5 — CUBA cache устраняет повторную стоимость

`cacheable=true` может ускорять повторные открытия, но холодный запуск и первый пользовательский запрос должны измеряться отдельно.

## 5. Методика измерения

### 5.1 Объём данных

Для каждой сущности зафиксировать:

- общее количество строк;
- количество строк после фильтра loader;
- число удалённых или soft-deleted строк;
- средний размер строки;
- размер таблицы и индексов;
- число свойств и вложенных views фактически загружаемого view;
- примерный размер сериализованной коллекции.

### 5.2 SQL

Выполнить один прогрев и не менее десяти измерительных запусков каждого фактического запроса.

Зафиксировать:

- planning time;
- execution time;
- MIN, MAX, AVG, P50, P95;
- actual rows;
- rows removed by filter;
- shared hit/read buffers;
- sort method и memory;
- используемые индексы.

Выполнить:

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT ... FROM hunttech_city ORDER BY city_ru_name;
```

```sql
EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT ... FROM hunttech_position
WHERE position_ru_name NOT LIKE '%(не использовать)%'
ORDER BY position_ru_name;
```

Использовать фактические production-compatible имена таблиц и колонок.

### 5.3 Runtime CUBA

Для холодного и тёплого открытия измерить отдельно:

- время от начала `JobCandidateEdit` до вызова `ensureReferenceLoadersLoaded()`;
- длительность `citiesDl.load()`;
- длительность `personPositionsLc.load()`;
- количество вызовов каждого loader на одно открытие;
- число сущностей в контейнере после загрузки;
- время до доступности `jobCityCandidateField` и `personPositionField`;
- время до первого полностью интерактивного состояния `tabMain`;
- наличие дополнительных запросов из вложенных views.

Сценарии:

1. тяжёлый существующий кандидат;
2. кандидат без города или должности;
3. новый кандидат;
4. повторное открытие в той же сессии;
5. открытие после перезапуска приложения или очистки релевантного кэша.

Для каждого сценария: один прогрев и пять измерительных запусков, MIN/MAX/AVG/P50/P95.

### 5.4 Read-only сравнения

Без изменения приложения рассчитать потенциальный эффект:

- узкого picker-view только с полями, необходимыми caption и существующим controller access;
- отложенной загрузки до первого focus/open поля;
- замены одного поля на `SuggestionPickerField`;
- сохранения полного lookup для малого справочника;
- кэширования справочника на уровне middleware.

Не реализовывать варианты на Stage 5.

## 6. Data View Integrity

Для каждого поля перечислить все getters, которые вызываются:

- caption provider;
- entity instance name;
- option image provider, если есть;
- текущим контроллером после выбора;
- валидацией и duplicate-check;
- соседними listeners.

Проверить:

```text
все getters ⊆ фактически загружаемый view
```

Отдельно указать минимально безопасный view для `City` и `Position`.

## 7. Допустимые вердикты

Для каждого справочника выдать один вердикт.

### KEEP_FULL_LOOKUP

Полная коллекция мала, P95 приемлем, изменение не требуется.

### NARROW_PICKER_VIEW

Основная стоимость связана с избыточным view; тип поля и полный lookup сохраняются.

### DEFER_LOADER_UNTIL_INTERACTION

Запрос приемлем, но не нужен до первого взаимодействия с полем. Потребуется отдельный implementation stage и проверка отображения уже выбранного значения.

### CONVERT_TO_SUGGESTION

Полная коллекция объективно велика; требуется отдельное согласование изменения UX и компонентного типа.

### MIDDLEWARE_CACHE_RECOMMENDED

Справочник стабилен, повторная стоимость существенна, а cacheable loader не устраняет проблему полностью.

### STAGE_5_BLOCKED

Нет достоверных runtime timings, SQL-планов или view-инвентаризации.

Допускаются разные вердикты для City и Position.

## 8. Обязательный отчёт Hermes

Создать:

```text
docs/performance-archive/2026-07-15/
job-candidate-reference-loaders-stage-5-baseline/
stage-5-reference-loaders-hermes-report.md
```

Отчёт должен содержать:

1. точный тестируемый SHA;
2. подтверждение отсутствия Java/XML/DB изменений;
3. количество City и Position;
4. inventory `city-picker-view` и `position-view`;
5. таблицу SQL-замеров и P50/P95;
6. `EXPLAIN (ANALYZE, BUFFERS)`;
7. таблицу runtime-замеров обоих loader;
8. cold/warm comparison;
9. количество SQL на одно открытие;
10. Data View Integrity matrix;
11. отдельный вердикт City;
12. отдельный вердикт Position;
13. одно минимальное предложение для Stage 6.

## 9. Приёмка

Stage 5 считается завершённым после review отчёта ChatGPT.

До этого запрещено:

- менять типы полей;
- удалять containers/loaders;
- менять views;
- добавлять индексы;
- менять бизнес-логику или UX.

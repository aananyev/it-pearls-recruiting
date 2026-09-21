# Отчет: Приоритетная очередь фонового определения навыков кандидатов

**Дата:** 21 сентября 2026 г.  
**Компонент:** Candidate Skills Enrichment & Monitoring  
**Ветка:** `agent/antigravity-dev`  
**Роли:** Главный разработчик, Технический писатель, QA  

---

## 1. Контекст и бизнес-требование
При создании нового кандидата или подгрузке нового резюме рекрутер ожидает максимально быстрого (мгновенного) автоматического извлечения структурированных навыков. До внесения изменений новые резюме сканировались фоновым воркером лениво по общему списку `CandidateCV` с сортировкой по `datePost desc` (дата составления резюме). В результате архивные резюме кандидатов оказывались в хвосте общей очереди.

**Реализовано:**
1. При создании кандидата с резюме или подгрузке нового резюме задача автоматически регистрируется в очереди `CandidateCvSkillAnalysis` со статусом `NOT_ANALYZED` и наивысшим приоритетом (`priority = 100`).
2. Воркер `CandidateSkillsEnrichmentWorker` при выборке следующей задачи обрабатывает срочные задачи рекрутеров в первую очередь (`priority >= 100`), минуя фоновые задачи и отложенные ретраи, и немедленно просыпается без ожидания 10-секундного тика планировщика.
3. В дашборде «Фоновое определение навыков» (`CandidateSkillsEnrichmentMonitoring`) очередь отсортирована так, что незавершенные срочные задачи отображаются в самом начале списка (вверху таблицы), добавлена колонка «Приоритет» с бейджем «⚡ СРОЧНО», статус «⏳ В ОЧЕРЕДИ» и фильтр «В очереди».

---

## 2. Архитектурные изменения и компоненты

### 2.1. База данных и Liquibase
- Создан SQL-скрипт `modules/core/db/update/postgres/26/260921-2-addPriorityToCandidateCvSkillAnalysis.sql`:
  - Добавление колонки `PRIORITY integer DEFAULT 0` в таблицу `HUNTTECH_CANDIDATE_CV_SKILL_ANALYSIS`.
  - Добавление композитного индекса `IDX_CAND_CV_SKILL_ANALYSIS_PRIORITY` по `(STATUS, PRIORITY DESC, CREATE_TS ASC)`.
- Создан changelog `modules/core/db/changelog/260921-2-addPriorityToCandidateCvSkillAnalysis.xml` с откатом `dropColumn` / `dropIndex`.
- Изменения зарегистрированы в `modules/core/db/changelog/db.changelog-master.xml`.

### 2.2. Сущность `CandidateCvSkillAnalysis`
- Добавлено поле `protected Integer priority = 0;` с геттером и сеттером.
- Синхронизирована декларация `@Index(name = "IDX_CAND_CV_SKILL_ANALYSIS_PRIORITY", columnList = "STATUS, PRIORITY, CREATE_TS")`.

### 2.3. Сущность `CandidateCV`
- Добавлена аннотация `@PublishEntityChangedEvents` для публикации событий жизненного цикла `EntityChangedEvent<CandidateCV, UUID>` в контекст Spring.

### 2.4. Слушатель сущностей `CandidateCvChangedListener`
- Перехватчик `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)` на `EntityChangedEvent<CandidateCV, UUID>`.
- Реагирует на:
  - `Type.CREATED` (создание резюме рекрутером в форме кандидата, форме резюме, мастере Smart Import или парсерах);
  - `Type.UPDATED` при изменении поля `textCV`.
- Вызывает `enrichmentService.enqueueCandidateCvPriority(candidateCvId)`.

### 2.5. Сервис `CandidateSkillEnrichmentService`
- Добавлены константы:
  - `PRIORITY_DEFAULT = 0`
  - `PRIORITY_HIGH = 100`
- Добавлены методы:
  - `void enqueueCandidateCvPriority(UUID candidateCvId)`
  - `void enqueueCandidateCv(UUID candidateCvId, int priority)`
- В `CandidateSkillEnrichmentServiceBean`:
  - Создание/актуализация записи `CandidateCvSkillAnalysis` в статусе `NOT_ANALYZED` с `priority = 100`;
  - Проверка неизменности хэша для существующих записей `FRESH` (дедупликация повторных вызовов);
  - Сброс `priority` в `PRIORITY_DEFAULT (0)` после успешного анализа `FRESH`;
  - Немедленный вызов `enrichmentWorker.triggerImmediateProcessing()`.

### 2.6. Воркер `CandidateSkillsEnrichmentWorker`
- Добавлен метод `triggerImmediateProcessing()` для немедленного пробуждения планировщика в пуле потоков.
- В методе `selectAndLockNextCandidateCv()` реализована приоритетная выборка:
  - **Шаг 0:** задачи `NOT_ANALYZED` с `priority >= PRIORITY_HIGH` (order by `priority desc, createTs asc`) — берутся ПЕРВЫМИ;
  - **Шаг 1:** задачи `RETRY` с наступившим временем `nextRetryAt <= now`;
  - **Шаг 2:** задачи `STALE`;
  - **Шаг 3:** обычные фоновые задачи `NOT_ANALYZED` с `priority < PRIORITY_HIGH`;
  - **Шаг 4:** исторические неразмеченные резюме из базы.

### 2.7. Дашборд «Фоновое определение навыков» (`hunttech_CandidateSkillsEnrichmentMonitoring`)
- `candidate-skills-enrichment-monitoring.xml`:
  - В загрузчик `analysesDl` добавлена приоритетная сортировка: `order by (case when e.status in (10, 20) then 0 else 1 end), coalesce(e.priority, 0) desc, e.createTs desc, e.processingStartedAt desc nulls last`. Активная очередь всегда выводится в самом верху таблицы.
  - Добавлена колонка `priority` («Приоритет») первой колонкой таблицы.
- `CandidateSkillsEnrichmentMonitoring.java`:
  - Рендерер колонки `priority`: бейдж «⚡ СРОЧНО» для задач с высоким приоритетом.
  - Рендерер колонки `status`: бейдж «⚡ В ОЧЕРЕДИ» или «⏳ В ОЧЕРЕДИ» для статуса `NOT_ANALYZED`.
  - Фильтр статусов: добавлен пункт «В очереди» (`QUEUED`), согласующийся с KPI-панелью.
  - Добавлена локализация в `messages_ru.properties` и `messages.properties`.

---

## 3. Результаты тестирования и верификации
1. **OCR Review (Alibaba Open Code Review):**
   - Проведено предварительное и повторное ревью. Все замечания (аннотация `@PublishEntityChangedEvents`, `fallbackExecution = true`, сброс приоритета после успешного анализа, параметризация порогов константой `PRIORITY_HIGH`, выравнивание индексов) полностью устранены.
2. **Модульные и контрактные тесты:**
   - `CandidateSkillsPriorityQueueTest`: успешное прохождение тестов приоритетов, создания записей очереди, триггера воркера и миграций БД.
   - `CandidateSkillsEnrichmentMonitoringContractTest`: успешная валидация структуры XML, наличия колонки `priority` и параметров сортировки.
   - `CandidateSkillEnrichmentServiceTest`: успешное прохождение всех тестов сервиса.
   - Общий прогон тестов подсистемы: `BUILD SUCCESSFUL`.
3. **Компиляция:**
   - `agent-gradle.sh compileJava`: успешно без ошибок.

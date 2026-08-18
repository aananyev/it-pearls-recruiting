# Правила миграции административных AI-промптов на прод (2026-08-18)

## Назначение и бизнес-смысл (What & Why)

Прод-снапшот, загруженный `get_base.sh` (архив `2026-08-18 HUNTTECH DataBase.tgz`,
прод был жив до 11:38), **не содержит ни одной таблицы AI Control Plane и ни одного
административного промпта**:

- `HUNTTECH_AI_FUNCTION_CONFIGURATION` — отсутствует;
- `HUNTTECH_ADMIN_AI_CONFIGURATION` — отсутствует;
- `HUNTTECH_USER_AI_FUNCTION_OVERRIDE` — отсутствует;
- `HUNTTECH_VACANCY_PROMPT_TEMPLATE` / `itpearls_vacancy_prompt_template` — пустые (0 строк).

Код HRM HuntTech (AI Control Plane, `AiExecutionService`) хранит административные
промпты в БД, а не в Java. Без миграции на прод ни одна AI-функция не загрузится:
`loadFunction` упадёт с «Активная AI-функция … не найдена».

Данная миграция — **самодостаточные правила приведения прода к актуальному состоянию**:
создаёт таблицы Control Plane (если отсутствуют) и загружает **ВСЕ канонические
административные промпты AI-функций**, используемых кодом.

## Поведение миграции (Behavior Summary)

- таблицы Control Plane отсутствуют → создаются (`HUNTTECH_ADMIN_AI_CONFIGURATION`,
  `HUNTTECH_AI_FUNCTION_CONFIGURATION`, `HUNTTECH_USER_AI_FUNCTION_OVERRIDE`
  с индексами и FK);
- таблицы уже существуют (среда применяла `260812-1`) → секция DDL — no-op;
- функция отсутствует → создаётся с последней канонической редакцией промпта;
- функция существует и остаётся исходным migration-seed (не редактировалась
  администратором) → промпт обновляется до последней редакции;
- функция изменена администратором (`UPDATED_BY != 'migration'` или version выше seed)
  → **не перезаписывается** (контракт 260814-3/260816-5);
- повторное применение скрипта → безопасно (INSERT `WHERE NOT EXISTS`, DDL `IF NOT EXISTS`).

## Состав загружаемых административных промптов

| CODE | Capability | Источник канонической редакции |
|---|---|---|
| `PROJECT_DESCRIPTION_GENERATE` | `DOCUMENT_ANALYSIS` | 260813-1 (русская локализация) |
| `PROJECT_LOGO_IMAGE_GENERATE` | `IMAGE_GENERATION` | 260813-2 |
| `PROJECT_SHORT_DESCRIPTION_GENERATE` | `TEXT_GENERATION` | 260814-4 (два предложения, MAX_TOKENS 250) |
| `SKILLS_EXTRACT` | `TEXT_GENERATION` | 260816-5 (уровни без дублей + один навык опыта) |
| `TEXT_SMART_FORMAT_HTML` | `TEXT_GENERATION` | 260816-4 (удаление пустых строк/абзацев) |
| `TEXT_SMART_FORMAT_PLAIN` | `TEXT_GENERATION` | 260816-1 |
| `STANDARDIZE_VACANCY` | `TEXT_TRANSFORMATION` | новая; канонический промпт из `.ai/skills/hunttech-vacancy-opening/references/standardized-description-prompt.txt`; legacy-контракт 260812-4 (`USER_REQUIRED`/`NO_FALLBACK`) |

`STANDARDIZE_VACANCY` не имела seed-миграции: legacy-перенос (260812-4) работал
только при наличии строк в `HUNTTECH_VACANCY_PROMPT_TEMPLATE`, а на проде таблица
пуста. Поэтому функция добавлена явно по legacy-контракту `TEXT_TRANSFORMATION`,
`USER_REQUIRED`, `NO_FALLBACK` — безопасный стартовый policy без расхода
корпоративного API до явной настройки администратора.

## Артефакты

| Назначение | Файл |
|---|---|
| Liquibase (CUBA updateDb / автоматические среды) | `modules/core/db/changelog/260818-1-addAdminAiPromptSeed.xml` |
| Ручной деплой на прод (PostgreSQL) | `modules/core/db/update/postgres/26/260818-1-addAdminAiPromptSeed.sql` |
| Контрактный тест | `modules/core/test/com/company/hunttech/core/AdminAiPromptSeedContractTest.java` |

Обе копии (.xml и .sql) синхронны по ключевым требованиям промптов; расхождение
ловит контракт-тест.

## Правила применения на прод (миграционные правила)

1. **Порядок**: миграция самодостаточна — индивидуальные файлы `260812-2..260816-5`
   на свежем проде применять НЕ нужно (и нельзя: их .sql падают `RAISE EXCEPTION`
   при отсутствии таблицы Control Plane). Достаточно одного скрипта
   `260818-1-addAdminAiPromptSeed.sql`. Прочие ожидающие миграции (колонки сущностей,
   боты) применяются в обычном порядке по номеру версии.
2. **Команда** (с предупреждением об изменении прод-БД и согласованием с владельцем):
   ```bash
   psql -h <prod-host> -U <user> -d hunttech -v ON_ERROR_STOP=1 \
        -f modules/core/db/update/postgres/26/260818-1-addAdminAiPromptSeed.sql
   ```
3. **Идемпотентность**: повторный прогон безопасен (существующие строки не дублируются,
   админские настройки не перезаписываются).
4. **Проверка после применения**:
   ```sql
   SELECT code, capability, execution_policy, fallback_policy,
          configuration_version, created_by, updated_by,
          length(system_prompt) AS sys_len
   FROM hunttech_ai_function_configuration
   WHERE delete_ts IS NULL
   ORDER BY code;
   ```
   Ожидается ровно 7 строк с `created_by = 'migration'`:
   `PROJECT_DESCRIPTION_GENERATE`, `PROJECT_LOGO_IMAGE_GENERATE`,
   `PROJECT_SHORT_DESCRIPTION_GENERATE`, `SKILLS_EXTRACT`, `STANDARDIZE_VACANCY`,
   `TEXT_SMART_FORMAT_HTML`, `TEXT_SMART_FORMAT_PLAIN`.
5. **Админские промпты не затираются**: если администратор уже настроил собственный
   русский промпт функции, миграция оставит его без изменений (UPDATE ограничен
   `CREATED_BY = 'migration'` + `COALESCE(UPDATED_BY, 'migration') = 'migration'`).
6. **Credentials**: миграция не создаёт и не меняет `HUNTTECH_ADMIN_AI_CONFIGURATION`
   (корпоративные API-ключи) и `HUNTTECH_USER_AI_CONFIGURATION`. Корпоративный
   credential настраивается администратором через экран «Управление AI» после деплоя.

## Локальная верификация (выполнена 2026-08-18)

Скрипт применён к локальной БД `hunttech` (прод-снапшот из `get_base.sh`):
- созданы 3 таблицы Control Plane + индексы + FK;
- загружены 7 функций (проверены ключевые фразы промптов и version);
- повторный прогон — без ошибок, дубликатов нет (7 строк).

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-18 | Консолидированная миграция административных AI-промптов: создание таблиц Control Plane для прод-снапшота + загрузка всех канонических промптов (последние редакции 260813-1..260816-5 + новая `STANDARDIZE_VACANCY`) |

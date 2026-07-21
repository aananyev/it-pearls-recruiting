# Runbook Hermes: безопасная production-миграция системных промптов AI

**Проект:** HRM HuntTech  
**Дата:** 2026-07-22  
**Production host:** `hr.hunttech.ru`  
**Активные контексты:** `/hrm`, `/hrm-core`  
**Активная база:** `hunttech`  
**Ветка разработки:** `feat/ai-entity-analysis`  
**Минимальный SHA с исправлением UUID:** `58ea5b50201aebac45275156b7e8a022ec342f5e`

## 1. Назначение

Документ определяет безопасное инкрементальное внедрение функционала «Системные промпты AI» в действующую production-базу `hunttech` без потери существующих данных.

Эта операция не является повторной миграцией `itpearls → hunttech`. Запрещено повторно создавать базу `hunttech`, выполнять namespace-преобразование `itpearls_* → hunttech_*`, запускать старые трансформационные скрипты или изменять сохранённую базу `itpearls`.

Production-действия разрешаются только после:

1. принятия текущего модуля ChatGPT;
2. фиксации точного Git SHA;
3. успешной локальной проверки Hermes;
4. успешного dry run на восстановленной копии production;
5. отдельного прямого разрешения Алексея на окно работ.

## 2. Фактическая production-топология

Перед каждым окном Hermes обязан подтвердить, а не предполагать:

- host: `hr.hunttech.ru`;
- PostgreSQL: версия 11.22 либо фактически установленная совместимая версия;
- service: `tomcat9`;
- webapps: `/var/lib/tomcat9/webapps`;
- app home: `/opt/app_home`;
- активные контексты: `/hrm`, `/hrm-core`;
- активная база: `hunttech`;
- старая база `itpearls` сохранена и не изменяется;
- `cuba.automaticDatabaseUpdate=false`;
- production WAR-модель: `hrm.war` и `hrm-core.war`.

Текущий `deploy-prod.sh` нельзя запускать с настройками по умолчанию: исторические defaults могут содержать старые service/path/context/database значения. Любое использование скрипта допускается только после отдельного review конфигурации и dry run.

## 3. Состав DB-изменений

CUBA должна применить PostgreSQL update scripts версии 27 в порядке имён.

### 3.1. Создание таблицы

`modules/core/db/update/postgres/27/270721-001-createAiPromptTemplate.sql`

Создаёт `HUNTTECH_AI_PROMPT_TEMPLATE` с техническими полями CUBA и бизнес-полями:

- `NAME`;
- `CODE`;
- `ENTITY_CLASS`;
- `PROMPT_TEXT`;
- `AVAILABLE_PLACEHOLDERS`;
- `DESCRIPTION`;
- `ACTIVE`.

`CODE` уникален. В исходном скрипте `ID` создаётся как `varchar(36)`.

### 3.2. Начальные данные

`modules/core/db/update/postgres/27/270721-002-seedAiPrompts.sql`

Добавляет три системных шаблона:

| ID | CODE | Назначение |
| --- | --- | --- |
| `00000000-0000-0000-0000-000000000001` | `RESUME_ANALYSIS` | Анализ резюме |
| `00000000-0000-0000-0000-000000000002` | `VACANCY_ANALYSIS` | Расшифровка вакансии |
| `00000000-0000-0000-0000-000000000003` | `INTERACTION_ANALYSIS` | Анализ взаимодействий |

### 3.3. Корректировка типа ID

`modules/core/db/update/postgres/27/270722-001-fixAiPromptTemplateIdType.sql`

```sql
ALTER TABLE HUNTTECH_AI_PROMPT_TEMPLATE
    ALTER COLUMN ID TYPE uuid
    USING ID::uuid;
```

Финальный тип `ID` обязан быть PostgreSQL `uuid`. Это обязательный ORM-контракт, поскольку `AiPromptTemplate` наследует `StandardEntity`. Если тип останется `varchar`, EclipseLink получает `String` и падает с `ClassCastException` при материализации entity.

## 4. Жёсткие запреты

Hermes не должен:

- применять production SQL вручную по отдельности при штатной миграции;
- вручную изменять `SYS_DB_CHANGELOG`;
- включать `cuba.automaticDatabaseUpdate`;
- запускать `updateDb` при работающем Tomcat;
- использовать автоматическое согласие `-y` для production mutations;
- повторно выполнять seed при уже существующих `CODE`;
- удалять или заменять пользовательские шаблоны;
- изменять `itpearls` или другие базы PostgreSQL;
- восстанавливать полный dump поверх действующей базы после появления новых записей;
- исправлять production SQL или Java самостоятельно;
- открывать пользовательский доступ без отдельной фразы Алексея.

При любом отклонении от ожидаемого состояния завершить операцию как `FAILED_SAFE`.

## 5. Подготовка immutable release

Перед dry run и production:

```bash
cd /Users/alekseyananyev/StudioProjects/hunttech_recruiting

git fetch origin
git switch feat/ai-entity-analysis
git pull --ff-only origin feat/ai-entity-analysis

git status --short
git branch --show-current
git rev-parse HEAD
git log -1 --format=fuller
```

Требования:

- рабочая директория чистая;
- ветка соответствует заданию;
- SHA записан в change ticket;
- полный diff после `58ea5b...` повторно принят ChatGPT;
- SQL scripts, WAR и документация получены из одного SHA.

Зафиксировать контрольные суммы:

```bash
sha256sum \
  modules/core/db/update/postgres/27/270721-001-createAiPromptTemplate.sql \
  modules/core/db/update/postgres/27/270721-002-seedAiPrompts.sql \
  modules/core/db/update/postgres/27/270722-001-fixAiPromptTemplateIdType.sql
```

## 6. Read-only preflight production

До остановки приложения выполнить только read-only проверки.

```bash
hostname -f
systemctl status tomcat9 --no-pager
sudo -iu postgres psql -d hunttech -v ON_ERROR_STOP=1
```

SQL:

```sql
SELECT current_database(), current_user, version();

SELECT pg_size_pretty(pg_database_size('hunttech'));

SELECT to_regclass('public.hunttech_ai_prompt_template') AS prompt_table;

SELECT column_name, data_type, udt_name, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'hunttech_ai_prompt_template'
ORDER BY ordinal_position;

SELECT count(*) AS prompt_count
FROM hunttech_ai_prompt_template;

SELECT id::text, code, name, active
FROM hunttech_ai_prompt_template
ORDER BY code;

SELECT code, count(*)
FROM hunttech_ai_prompt_template
GROUP BY code
HAVING count(*) > 1;

SELECT count(*) AS active_connections
FROM pg_stat_activity
WHERE datname = 'hunttech';

SELECT count(*) AS prepared_transactions
FROM pg_prepared_xacts
WHERE database = 'hunttech';
```

Если таблица отсутствует, запросы к ней выполнять только после проверки `to_regclass()`.

Проверить структуру `SYS_DB_CHANGELOG`:

```sql
\d+ sys_db_changelog
```

После этого выполнить read-only поиск записей `270721` и `270722` с учётом фактических имён колонок.

## 7. Классификация состояния таблицы

| Состояние | Таблица | ID | Строки | Действие |
| --- | --- | --- | ---: | --- |
| S0 | отсутствует | — | — | штатно применить все scripts через CUBA `updateDb` |
| S1 | существует | `varchar` | 0 | STOP: частично применённая миграция, анализ changelog |
| S2 | существует | `varchar` | 3 | возможно применены create+seed; применять fix только через CUBA `updateDb` после сверки changelog |
| S3 | существует | `uuid` | 3 | сверить наличие всех scripts в changelog; не пересевать |
| S4 | существует | `uuid` | больше 3 | сохранить все пользовательские строки; не пересевать и не удалять |
| SX | иное | иное | иное | `FAILED_SAFE`, отдельный DBA-анализ |

Stop conditions:

- ID, не приводимые к UUID;
- дубликаты `CODE`;
- schema и changelog противоречат друг другу;
- обнаружены неизвестные пользовательские изменения таблицы;
- production SHA не утверждён;
- backup или test restore не подтверждены;
- active user writes невозможно остановить;
- недостаточно свободного диска.

## 8. Baseline до изменений

Сохранить read-only baseline минимум для:

```sql
SELECT 'sec_user' AS object_name, count(*) FROM sec_user
UNION ALL SELECT 'sec_role', count(*) FROM sec_role
UNION ALL SELECT 'sec_user_role', count(*) FROM sec_user_role
UNION ALL SELECT 'sec_permission', count(*) FROM sec_permission
UNION ALL SELECT 'sys_file', count(*) FROM sys_file
UNION ALL SELECT 'hunttech_job_candidate', count(*) FROM hunttech_job_candidate
UNION ALL SELECT 'hunttech_company', count(*) FROM hunttech_company
UNION ALL SELECT 'hunttech_open_position', count(*) FROM hunttech_open_position;
```

Также сохранить:

- число таблиц public schema;
- число constraints;
- число invalid constraints;
- число индексов;
- число строк `SYS_DB_CHANGELOG`;
- состояние runtime safety flags без вывода секретов.

Результаты сохранить вне Git, а в отчёт включить только безопасные агрегаты.

## 9. Финальный backup

После объявления maintenance window и остановки пользовательских записей:

```bash
systemctl stop tomcat9
systemctl is-active tomcat9
```

Подтвердить отсутствие connections и prepared transactions.

Создать каталог:

```bash
TS=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR=/var/backups/hunttech-hrm/${TS}-ai-prompts
sudo install -d -m 700 -o postgres -g postgres "$BACKUP_DIR"
```

Снять:

- custom-format dump `hunttech`;
- schema-only dump;
- `pg_restore --list`;
- backup активных WAR;
- backup Tomcat contexts;
- backup runtime properties без публикации секретов;
- backup fileStorage либо подтверждение актуального полного backup согласно утверждённой политике.

```bash
sudo -iu postgres pg_dump \
  --format=custom \
  --verbose \
  --no-password \
  --file="$BACKUP_DIR/hunttech_${TS}.dump" \
  hunttech

sudo -iu postgres pg_dump \
  --schema-only \
  --no-password \
  --file="$BACKUP_DIR/hunttech_schema_${TS}.sql" \
  hunttech

sudo -iu postgres pg_restore --list \
  "$BACKUP_DIR/hunttech_${TS}.dump" \
  > "$BACKUP_DIR/pg_restore_list_${TS}.txt"

cd "$BACKUP_DIR"
sha256sum * > SHA256SUMS
sha256sum -c SHA256SUMS
```

Backup считается действительным только при exit code 0, непустом dump, успешном `pg_restore --list` и успешной проверке SHA-256.

## 10. Обязательный test restore и dry run

Production update запрещён без восстановления финального или максимально свежего dump в отдельную базу, например:

`hunttech_ai_prompts_dryrun_<TS>`

Требования:

- не использовать имена `hunttech`, `itpearls` и других действующих баз;
- не восстанавливать globals автоматически в общий cluster;
- после restore сравнить baseline counts;
- запустить CUBA `updateDb` против dry-run database через временный override Gradle task;
- проверить `SYS_DB_CHANGELOG`, тип UUID, три seed-записи и отсутствие дубликатов;
- проверить повторный запуск `updateDb`: новых scripts быть не должно;
- выполнить тестовый запуск WAR в изолированном контуре или минимум core/schema validation.

Dry run должен быть задокументирован со статусом `PASS`. При `FAIL` production запрещён.

## 11. Production updateDb

Tomcat должен оставаться остановленным.

Миграции применяются только задачей CUBA Platform, чтобы сохранить согласованность `SYS_DB_CHANGELOG`:

```bash
./gradlew :app-core:assembleDbScripts --no-daemon --stacktrace
```

Далее выполнить `:app-core:updateDb` через утверждённый временный Gradle override или SSH tunnel, указывающий строго на production-базу `hunttech`.

Обязательные параметры:

- host и port подтверждены;
- dbName строго `hunttech`;
- пользователь имеет только необходимые права;
- пароль не выводится в log и не коммитится;
- exit code фиксируется;
- при ошибке повторный запуск без анализа запрещён.

`cuba.automaticDatabaseUpdate` остаётся `false` до, во время и после операции.

## 12. DB-валидация после updateDb

```sql
SELECT data_type, udt_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name = 'hunttech_ai_prompt_template'
  AND column_name = 'id';
```

Ожидается `uuid / uuid`.

```sql
SELECT pg_typeof(id), count(*)
FROM hunttech_ai_prompt_template
GROUP BY pg_typeof(id);

SELECT id::text, code, name, active
FROM hunttech_ai_prompt_template
ORDER BY code;

SELECT code, count(*)
FROM hunttech_ai_prompt_template
GROUP BY code
HAVING count(*) > 1;
```

Обязательные коды:

- `RESUME_ANALYSIS`;
- `VACANCY_ANALYSIS`;
- `INTERACTION_ANALYSIS`.

Сверить changelog для всех трёх scripts. Ручные INSERT/UPDATE в `SYS_DB_CHANGELOG` запрещены.

Повторить baseline. Допустимы только:

- новая таблица `hunttech_ai_prompt_template`;
- три seed-записи, если таблицы не было;
- увеличение `SYS_DB_CHANGELOG` на фактически применённые scripts.

Количество строк существующих бизнес- и security-таблиц должно совпасть.

## 13. Сборка и deployment WAR

Сборка выполняется из того же SHA, из которого применялись DB scripts:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew clean --no-daemon --stacktrace
./gradlew test --no-daemon --stacktrace
./gradlew buildWar --no-daemon --stacktrace
```

Зафиксировать SHA-256 `hrm.war` и `hrm-core.war`.

При остановленном Tomcat:

1. сохранить предыдущие WAR и exploded directories;
2. удалить только текущие exploded `hrm` и `hrm-core` после backup;
3. установить новые `hrm.war`, `hrm-core.war`;
4. проверить owner/permissions;
5. проверить `hrm.xml`, `hrm-core.xml`;
6. убедиться, что JNDI указывает на `hunttech` и не содержит `itpearls`;
7. проверить `cuba.automaticDatabaseUpdate=false`;
8. оставить scheduling, email, Telegram и внешние callbacks отключёнными на restricted start.

Не переименовывать старые webapps внутри активного `/var/lib/tomcat9/webapps`: Tomcat может auto-deploy переименованный каталог.

## 14. Restricted start и smoke-test

```bash
MIGRATION_START=$(date --iso-8601=seconds)
systemctl start tomcat9
systemctl is-active tomcat9
```

Проверки:

```bash
curl -fsS -o /dev/null -w '%{http_code}\n' \
  http://hr.hunttech.ru:8080/hrm/

curl -sS -o /dev/null -w '%{http_code}\n' \
  http://hr.hunttech.ru:8080/app/
```

Ожидается:

- `/hrm/` → HTTP 200;
- `/app/` → HTTP 404.

Проверить connections:

```sql
SELECT datname, count(*)
FROM pg_stat_activity
WHERE datname IN ('hunttech', 'itpearls')
GROUP BY datname
ORDER BY datname;
```

Новый runtime должен подключаться только к `hunttech`.

Проверить journal текущего запуска:

```bash
journalctl -u tomcat9 --since "$MIGRATION_START" --no-pager \
  > "/tmp/tomcat-ai-prompts-${TS}.log"

grep -nE \
  'ClassCastException|String cannot be cast to class java.util.UUID|AiPromptTemplate|DatabaseException|PSQLException|ERROR|SEVERE' \
  "/tmp/tomcat-ai-prompts-${TS}.log" \
  || true
```

Критические признаки:

- `String cannot be cast to UUID`;
- relation/table not found;
- duplicate key по `CODE`;
- invalid UUID syntax;
- попытка automatic DB update;
- подключение к `itpearls`;
- ошибки security/login;
- ошибки загрузки системных промптов.

## 15. Ручной UI smoke-test

Под администратором:

1. открыть `/hrm`;
2. войти в систему;
3. открыть «Системные промпты AI»;
4. подтвердить загрузку таблицы без `ClassCastException`;
5. подтвердить наличие трёх системных шаблонов;
6. открыть каждый шаблон только для чтения;
7. не редактировать и не удалять production-записи;
8. не отправлять фактический запрос внешнему AI-провайдеру без отдельного разрешения.

## 16. GO / NO-GO

GO разрешён только при одновременном выполнении условий:

- exact SHA и checksum зафиксированы;
- финальный backup и test restore успешны;
- dry run успешен;
- production `updateDb` завершён с exit code 0;
- три scripts присутствуют в changelog;
- `ID` имеет тип `uuid`;
- обязательные промпты присутствуют без дубликатов;
- baseline existing data совпал;
- invalid constraints отсутствуют;
- WAR соответствует тому же SHA;
- `/hrm` отвечает HTTP 200;
- runtime подключён только к `hunttech`;
- критических ошибок в новых логах нет;
- UI smoke-test пройден;
- Алексей дал отдельное разрешение открыть доступ.

Фраза открытия для этого окна:

```text
РАЗРЕШАЮ ОТКРЫТЬ HRM ПОСЛЕ AI PROMPT MIGRATION
```

Без точной фразы доступ пользователям не открывать.

## 17. Rollback matrix

### UpdateDb завершился ошибкой, Tomcat не запускался

- не повторять updateDb вслепую;
- сохранить failed database и logs;
- восстановить backup в новую базу `hunttech_restore_<TS>`;
- проверить counts/schema;
- по отдельному approval переключить datasource на restored database;
- развернуть предыдущие WAR;
- выполнить restricted smoke-test.

Не использовать `pg_restore --clean` в действующей базе как первый вариант: failed state должен сохраниться для расследования.

### DB успешна, новый WAR не запускается, пользователей не открывали

- остановить Tomcat;
- восстановить предыдущие `hrm.war`, `hrm-core.war`;
- очистить exploded `hrm`, `hrm-core`;
- additive-таблицу можно оставить, поскольку старая версия её не использует;
- запустить предыдущую версию и выполнить smoke-test.

### Пользователи уже записали новые данные

Полный restore старого dump запрещён без reconciliation:

- немедленно остановить записи;
- зафиксировать timestamps;
- снять emergency dump;
- определить записи после cutover;
- подготовить отдельный data reconciliation plan;
- решение принимают Алексей, ChatGPT и DBA.

## 18. Итоговый отчёт Hermes

Отчёт должен содержать:

1. Полученные разрешения и окно работ.
2. Ветку, exact SHA и commit message.
3. SHA-256 SQL scripts и WAR.
4. Подтверждение host/service/path/database.
5. PostgreSQL version и free disk.
6. Pre-migration state S0–S4.
7. Состояние `SYS_DB_CHANGELOG`.
8. Active connections и prepared transactions.
9. Backup directory, размеры и checksum validation.
10. Test restore и dry-run result.
11. Production updateDb command без secrets и exit code.
12. Applied scripts.
13. Тип `ID` после миграции.
14. Список и количество prompts.
15. Baseline comparison.
16. Invalid constraints.
17. WAR deployment paths, owner, permissions.
18. Runtime safety flags.
19. HTTP `/hrm` и `/app`.
20. Connections после запуска.
21. Relevant runtime errors.
22. UI smoke-test.
23. GO/NO-GO решение.
24. Выполненный rollback, если был.
25. Финальный статус: `SUCCESS`, `ROLLED_BACK` или `FAILED_SAFE`.

Не коммитить production dumps, raw logs, `.pgpass`, passwords, tokens, API keys или персональные данные.

## 19. История изменений

| Дата | Изменение |
| --- | --- |
| 2026-07-22 | Создан runbook инкрементальной production-миграции системных промптов AI: preflight, backup, dry run, CUBA updateDb, deployment, smoke-test, GO/NO-GO и rollback. |

# Проверка CUBA updateDb и полной схемы UserAiProfile

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/fix-cuba-db-reconciliation-update`  
BASE: `master`  
STATUS: `WAITING_FOR_HERMES`  
MODE: проверка точного HEAD PR без изменения кода, документации, БД-источника и production.

Точный HEAD SHA указан в PR. До начала Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки равен `HEAD SHA для проверки` из PR;
3. PR открыт из этой ветки напрямую в `master`;
4. `base=master`;
5. HEAD PR совпадает с проверяемым SHA;
6. conflicts = NONE.

Несовпадение → `HEAD_MISMATCH`, проверку остановить.

## Область изменения

- добавлен CUBA update script `26/260727-2-reconcileProductionSchema.sql`;
- добавлен Liquibase follow-up `260727-2-completeUserAiProfileColumns.xml`;
- `db.changelog-master.xml` включает оба reconciliation-файла в правильном порядке;
- `updateDb` перед основным action регистрирует alias только для script, чей полный suffix после `/update/postgres/` уже присутствует в старой истории;
- добавлена статическая проверка всех 34 колонок `UserAiProfile`;
- бизнес-логика, UI, services, views, JPQL и production не изменялись.

## Обязательные команды

```bash
git diff --check

./gradlew :app-core:compileTestJava --no-daemon --stacktrace

./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.DatabaseSchemaReconciliationChangelogTest' \
  --no-daemon --stacktrace

./gradlew test \
  --tests '*ScreenViewIntegrityTest*' \
  --no-daemon --stacktrace

./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- `DatabaseSchemaReconciliationChangelogTest` — `7/7 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- compile — PASS;
- `BUILD SUCCESSFUL`;
- SCSS — N/A;
- Data View Integrity — PASS/N/A, controllers и views не менялись.

## Disposable PostgreSQL 11

Использовать только восстановленную disposable-копию актуальной БД. Production не подключать.

До миграции сохранить:

```sql
select count(*) from hunttech_job_candidate;
select count(*) from hunttech_open_position;
select count(*) from hunttech_user_settings;
select count(*) from hunttech_user_ai_configuration;
select count(*) from hunttech_vacancy_prompt_template;
select count(*) from hunttech_user_ai_profile;

select id, image_id, file_image_face
from hunttech_user_settings
order by id;
```

Если одна из image-колонок отсутствует, адаптировать только read-only запрос, структуру вручную не менять.

Проверить старые component paths и сохранить результат:

```sql
select script_name
from sys_db_changelog
where replace(lower(script_name), '\\', '/') like '%/update/postgres/%'
order by script_name;
```

Для каждого alias, добавленного baseline, должна существовать исходная запись с тем же полным suffix после `/update/postgres/`. Alias без такой исходной записи означает `FAILED_VERIFICATION`.

Запустить:

```bash
./gradlew updateDb --no-daemon --stacktrace
```

Обязательно подтвердить:

1. `19/191022-1-updateCountry.sql` не выполнялся;
2. alias добавлены только для scripts с доказанным точным suffix-совпадением в старой истории;
3. `260727-2-reconcileProductionSchema.sql` выполнен и зарегистрирован CUBA;
4. XML-файл не вставлялся в `SYS_DB_CHANGELOG`;
5. таблица `HUNTTECH_USER_AI_PROFILE` содержит все 34 entity-колонки;
6. FK и индексы существуют;
7. UUID фотографий сохранены;
8. broken references на `SYS_FILE` = 0;
9. counts бизнес-таблиц не уменьшились;
10. `HUNTTECH_USER_AI_PROFILE_PARAMETERS` не создана.

Повторить:

```bash
./gradlew updateDb --no-daemon --stacktrace
```

Ожидается: новых scripts нет, DDL/DML повторно не выполняются.

## Local deploy и smoke

После успешной DB-проверки:

1. выполнить стандартный local deploy точного HEAD;
2. проверить `http://localhost:8080/hrm/` → HTTP 200;
3. открыть `ExtSettingsWindow`;
4. открыть вкладки AI и «Обо мне»;
5. проверить загрузку и сохранение существующего профиля без потери заполненных полей;
6. проверить фотографию пользователя;
7. открыть вакансию с AI-полями;
8. проверить Tomcat logs: critical errors NONE.

Запрещено Hermes:

- менять Java, XML, Gradle, SQL, tests или docs;
- вручную добавлять недостающие колонки;
- делать commit, push, rebase или merge;
- изменять production;
- использовать production datasource;
- объявлять PASS при проверке другого SHA.

## Формат успешного отчёта

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
REPO: aananyev/it-pearls-recruiting
BRANCH: agent/fix-cuba-db-reconciliation-update
PR: <номер>
BASE: master
VERIFIED HEAD: <полный SHA>
HEAD MATCH: PASS
CONFLICTS: NONE
RECONCILIATION TEST: 7/7 PASS
SCREEN VIEW INTEGRITY: 8/8 PASS
COMPILE: PASS
CLEAN ASSEMBLE: PASS
DISPOSABLE DB RESTORE: PASS
LEGACY ALIAS BASELINE: PASS
OLD SCRIPT RE-EXECUTION: NONE
CUBA RECONCILIATION SCRIPT: REGISTERED
USER AI PROFILE COLUMNS: 34/34
SECOND UPDATE DB: NO PENDING SCRIPTS
ROW COUNTS: PRESERVED
IMAGE UUIDS: PRESERVED
BROKEN SYS_FILE REFERENCES: 0
LOCAL DEPLOY: PASS
HTTP /hrm/: 200
TOMCAT CRITICAL ERRORS: NONE
P1: 0
P2: 0
MERGE: NOT PERFORMED
PRODUCTION: NOT CHANGED
```

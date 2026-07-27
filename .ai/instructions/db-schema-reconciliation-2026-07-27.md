# Проверка reconciliation-миграции production-схемы PostgreSQL 11

PROJECT: HRM HuntTech  
STATUS: `WAITING_FOR_HERMES`  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/db-schema-reconciliation-liquibase`  
BASE: `master`  
MODE: проверка точного HEAD PR на disposable-копии production-БД без изменения production и без изменения кода.

## 1. Проверка Git-контекста

Перед любыми командами Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с `HEAD SHA для проверки` из PR;
3. PR открыт из `agent/db-schema-reconciliation-liquibase` напрямую в `master`;
4. HEAD PR совпадает с проверяемым SHA;
5. `base=master`;
6. conflicts = NONE.

Несовпадение → `HEAD_MISMATCH`, проверку остановить. Отчёт должен содержать формулировку:

```text
проверен HEAD: <полный SHA>
```

Hermes не меняет Java, XML, tests, docs, Liquibase или SQL; не делает commit, push, rebase или merge; не разрешает конфликты; production не изменяет.

## 2. Область проверки

Изменения PR:

- единый `260727-1-reconcileProductionSchema.xml`;
- старые пять Liquibase-файлов исключены из активного include, но сохранены в репозитории;
- `UserSettings.fileImageFace` переведён на `FILE_IMAGE_FACE`;
- добавлен статический регрессионный тест;
- обновлена документация сущностей и БД.

Отдельная таблица `HUNTTECH_USER_AI_PROFILE_PARAMETERS` не создаётся: в текущем HEAD отсутствует entity/DDL-контракт. Это ожидаемое поведение, а не дефект проверки.

Двойные таблицы `itpearls_*` и `hunttech_*` не объединяются, не переименовываются и не удаляются.

## 3. Статические проверки

```bash
git diff --check

xmllint --noout \
  modules/core/db/changelog/db.changelog-master.xml \
  modules/core/db/changelog/260727-1-reconcileProductionSchema.xml

./gradlew :app-global:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.DatabaseSchemaReconciliationChangelogTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- XML parse = PASS;
- `DatabaseSchemaReconciliationChangelogTest` = `4/4 PASS`;
- `ScreenViewIntegrityTest` = `8/8 PASS`;
- compile = PASS;
- `BUILD SUCCESSFUL`;
- SCSS = N/A, темы не изменены;
- Data View Integrity = PASS/N/A, views и controllers не изменены.

Дополнительно:

```bash
rg -n -i \
  'drop[[:space:]]+table|drop[[:space:]]+column|delete[[:space:]]+from|truncate' \
  modules/core/db/changelog/260727-1-reconcileProductionSchema.xml

rg -n \
  'HUNTTECH_USER_AI_PROFILE_PARAMETERS|UserAiProfileParameters' \
  modules/global/src modules/core/db/changelog
```

Ожидается: оба поиска не возвращают совпадений.

## 4. Подготовка disposable-БД

1. Использовать актуальный production dump, уже загруженный локально.
2. Зафиксировать имя и SHA-256 dump, PostgreSQL server version, имя исходной локальной БД и timestamp.
3. Создать отдельную disposable-БД. Исходную локальную копию не менять.
4. Выполнить restore и проверить отсутствие ошибок.
5. Снять pre-migration snapshot: counts, `IMAGE_ID`, FK, индексы, целевые колонки и `DATABASECHANGELOG`.

Production host, production БД и production Tomcat не использовать.

## 5. Liquibase validation и update-sql

Параметры передавать через environment variables; пароль не сохранять в отчёте:

```bash
liquibase \
  --url="$LIQUIBASE_URL" \
  --username="$LIQUIBASE_USERNAME" \
  --password="$LIQUIBASE_PASSWORD" \
  --changelog-file=modules/core/db/changelog/db.changelog-master.xml \
  validate

liquibase \
  --url="$LIQUIBASE_URL" \
  --username="$LIQUIBASE_USERNAME" \
  --password="$LIQUIBASE_PASSWORD" \
  --changelog-file=modules/core/db/changelog/db.changelog-master.xml \
  update-sql > /tmp/hrm-hunttech-reconciliation-update.sql
```

Проверить generated SQL:

```bash
rg -n -i \
  'drop[[:space:]]+table|drop[[:space:]]+column|delete[[:space:]]+from|truncate' \
  /tmp/hrm-hunttech-reconciliation-update.sql
```

Ожидается: совпадений нет. Любое DDL/DML по `itpearls_*` означает `FAILED_VERIFICATION`.

## 6. Применение на disposable-БД

```bash
liquibase \
  --url="$LIQUIBASE_URL" \
  --username="$LIQUIBASE_USERNAME" \
  --password="$LIQUIBASE_PASSWORD" \
  --changelog-file=modules/core/db/changelog/db.changelog-master.xml \
  update

liquibase \
  --url="$LIQUIBASE_URL" \
  --username="$LIQUIBASE_USERNAME" \
  --password="$LIQUIBASE_PASSWORD" \
  --changelog-file=modules/core/db/changelog/db.changelog-master.xml \
  status --verbose

liquibase \
  --url="$LIQUIBASE_URL" \
  --username="$LIQUIBASE_USERNAME" \
  --password="$LIQUIBASE_PASSWORD" \
  --changelog-file=modules/core/db/changelog/db.changelog-master.xml \
  update-sql > /tmp/hrm-hunttech-reconciliation-second-run.sql
```

Ожидается:

- pending changes = 0;
- второй `update-sql` не содержит DDL/DML;
- все changeSet зарегистрированы как `EXECUTED` или `MARK_RAN`.

## 7. Обязательные SQL-проверки

### 7.1. Сохранность строк

Для каждой существовавшей до миграции таблицы сравнить `count(*)` до/после. Ни один счётчик не должен уменьшиться.

Минимальный набор:

```text
HUNTTECH_USER_SETTINGS
HUNTTECH_OPEN_POSITION
HUNTTECH_USER_AI_CONFIGURATION
HUNTTECH_VACANCY_PROMPT_TEMPLATE
ITPEARLS_JOB_CANDIDATE
HUNTTECH_JOB_CANDIDATE
ITPEARLS_OPEN_POSITION
HUNTTECH_OPEN_POSITION
ITPEARLS_ITERACTION_LIST
HUNTTECH_ITERACTION_LIST
```

Если legacy-таблица отсутствует, зафиксировать `N/A`; создавать её запрещено.

### 7.2. Сохранность фотографии

До миграции:

```sql
select count(*) as image_rows,
       count(distinct image_id) as distinct_images
from hunttech_user_settings
where image_id is not null;
```

После миграции:

```sql
select count(*) as image_rows,
       count(distinct file_image_face) as distinct_images
from hunttech_user_settings
where file_image_face is not null;
```

Требуется полное совпадение обоих значений.

```sql
select count(*) as broken_file_refs
from hunttech_user_settings s
left join sys_file f on f.id = s.file_image_face
where s.file_image_face is not null
  and f.id is null;
```

Ожидается `0`.

### 7.3. Схема

Подтвердить наличие:

```text
HUNTTECH_USER_AI_PROFILE
FK_HUNTTECH_USER_AI_PROFILE_ON_USER
IDX_HUNTTECH_USER_AI_PROFILE_UNQ_USER
HUNTTECH_USER_SETTINGS.PREFER_PERSONAL_AI_API_SETTINGS
HUNTTECH_USER_SETTINGS.PREFER_PERSONAL_PROMPTS
HUNTTECH_USER_SETTINGS.FILE_IMAGE_FACE
FK_HUNTTECH_USER_SETTINGS_ON_FILE_IMAGE_FACE
IDX_HUNTTECH_USER_SETTINGS_ON_FILE_IMAGE_FACE
```

Подтвердить отсутствие нового объекта `HUNTTECH_USER_AI_PROFILE_PARAMETERS`.

### 7.4. Двойной namespace

Снять read-only count-сравнение для `job_candidate`, `open_position`, `iteraction`, `iteraction_list`, `person`, `company`. Различия зафиксировать. Не копировать, не merge-ить, не удалять и не переименовывать строки или таблицы.

## 8. Local deploy и smoke

Развернуть приложение на disposable-БД стандартным способом HRM HuntTech.

Проверить:

1. `http://localhost:8080/hrm/` → HTTP 200;
2. вход пользователя;
3. открытие `ExtSettingsWindow`;
4. вкладка AI показывает и сохраняет оба preference-флага;
5. вкладка «Обо мне» открывается без ошибки отсутствующей таблицы;
6. фотография пользователя отображается;
7. загрузка/замена фотографии на тестовом пользователе сохраняется;
8. открытие существующей вакансии читает AI-поля;
9. Tomcat logs: missing column/table, FK violation, unfetched, `IllegalStateException`, NPE и critical errors = NONE.

Тестовые изменения выполнять только в disposable-БД.

## 9. Формат отчёта

Отчёт сохранить в `.ai/reports/2026-07-27-db-schema-reconciliation.md` и добавить комментарием к PR.

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
REPO: aananyev/it-pearls-recruiting
BRANCH: agent/db-schema-reconciliation-liquibase
PR: <номер>
BASE: master
VERIFIED HEAD: <полный SHA>
HEAD MATCH: PASS
CONFLICTS: NONE
XML VALIDATION: PASS
LIQUIBASE VALIDATE: PASS
UPDATE-SQL SAFETY: PASS
RECONCILIATION TEST: 4/4 PASS
SCREEN VIEW INTEGRITY: 8/8 PASS
COMPILE: PASS
CLEAN ASSEMBLE: PASS
DISPOSABLE DB RESTORE: PASS
LIQUIBASE UPDATE: PASS
SECOND RUN IDEMPOTENT: PASS
ROW COUNTS: UNCHANGED
IMAGE UUIDS: PRESERVED
BROKEN FILE REFERENCES: 0
EXPECTED TABLES/COLUMNS/FK/INDEXES: PASS
USER_AI_PROFILE_PARAMETERS: NOT CREATED
DOUBLE NAMESPACE: AUDITED, NOT CHANGED
LOCAL DEPLOY: PASS
HTTP /hrm/: 200
TOMCAT CRITICAL ERRORS: NONE
P1: 0
P2: 0
MERGE: NOT PERFORMED
PRODUCTION: NOT CHANGED
```

При ошибке использовать `STATUS: FAILED_VERIFICATION`, указать FAILED STEP, ROOT CAUSE и проверенный SHA. Код, merge и production не изменять.

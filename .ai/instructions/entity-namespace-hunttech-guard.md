# Проверка единого namespace сущностей HRM HuntTech

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/entity-namespace-hunttech-guard`  
BASE: `master`  
BASE SHA: `1ebe4b26b992b8000d27c7678898923b4eb8dbb0`  
STATUS: `WAITING_FOR_HERMES`  
MODE: проверка точного HEAD PR без изменения функционального кода, документации, БД и production.

Точный полный HEAD SHA указан в PR. Перед проверкой Hermes обязан подтвердить:

1. ветка существует;
2. HEAD ветки совпадает с `HEAD SHA для проверки` из PR;
3. PR открыт из `agent/entity-namespace-hunttech-guard` напрямую в `master`;
4. HEAD PR совпадает с проверяемым SHA;
5. `base=master`;
6. conflicts = NONE.

Несовпадение → `HEAD_MISMATCH`, проверку остановить. Отчёт должен содержать формулировку `проверен HEAD: <полный SHA>`.

## Область изменения

- из `metadata.xml` удалена регистрация legacy metadata model `com.company.itpearls` / `itpearls`;
- `ScreenViewIntegrityTest` проверяет `hunttech_ExtUser` и `hunttech_JobCandidate`, сохраняя обязательные `8/8 PASS`;
- добавлен `EntityNamespaceIntegrityTest` с тремя контрактами:
  1. runtime project entities используют namespace `hunttech_*`;
  2. metadata descriptor регистрирует только модель `hunttech`;
  3. рабочие Java/XML/properties-файлы не содержат активных legacy entity/table references;
- исторические migration-, backup- и rollback-файлы не изменены;
- DDL, DML, Liquibase, entity fields, views, JPQL, loaders, services, controllers и UI не изменены.

## Обязательные команды

```bash
git diff --check

./gradlew :app-core:compileTestJava \
          :app-web:compileJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.EntityNamespaceIntegrityTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- `EntityNamespaceIntegrityTest` — `3/3 PASS`;
- `ScreenViewIntegrityTest` — `8/8 PASS`;
- compile — PASS;
- `BUILD SUCCESSFUL`;
- Data View Integrity — PASS/N/A, поскольку views и controllers не изменены;
- SCSS — N/A, поскольку темы не изменены.

## Дополнительный статический аудит

Hermes должен сохранить полный вывод поиска, но не менять найденные исторические файлы:

```bash
rg -n --hidden \
  --glob '!modules/core/db/**' \
  --glob '!deployment/database-migration/**' \
  --glob '!scripts/db-migration/**' \
  --glob '!docs/**' \
  --glob '!.git/**' \
  'itpearls_|ITPEARLS_' \
  modules/global/src modules/core/src modules/web/src modules/core/test
```

Допустимы только строки теста, которые конструируют legacy-префикс для отрицательной проверки. Любая активная `@Entity`, `@Table`, JPQL, XML entity/class binding, metadata lookup или runtime SQL-ссылка на `itpearls_*`/`ITPEARLS_*` означает `FAILED_VERIFICATION`.

## Local deploy и smoke

1. Развернуть точный HEAD локально стандартным способом HRM HuntTech.
2. Проверить `http://localhost:8080/hrm/` → HTTP `200`.
3. Проверить startup Tomcat log:
   - unknown entity `itpearls_*` — NONE;
   - SQL к `ITPEARLS_*` — NONE;
   - metadata duplication/namespace errors — NONE;
   - critical errors — NONE.
4. Выполнить smoke без изменения данных production:
   - вход в HRM HuntTech;
   - открытие `ExtSettingsWindow`;
   - открытие списка кандидатов;
   - открытие существующей карточки `JobCandidateEdit`;
   - создание новой карточки кандидата без сохранения и отмена;
   - открытие lookup кандидата в связанном экране.
5. Подтвердить отсутствие `IllegalStateException`, `UnknownEntityNameException`, `Cannot get unfetched attribute`, XML loader/binding errors и ошибок десериализации пользовательских настроек.
6. P1 = 0, P2 = 0.

## Read-only DB check локальной базы

Без DDL/DML проверить:

```sql
select count(*) as legacy_relations
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public'
  and lower(c.relname) like 'itpearls\_%' escape '\\';
```

Ожидается `0`. Если результат отличается, статус `FAILED_VERIFICATION`; таблицы не переименовывать и не удалять.

## Запреты Hermes

Hermes не меняет Java, XML, tests, docs, Liquibase или SQL; не делает commit, push, rebase или merge; не разрешает конфликты; не изменяет production; не запускает production deploy или migration.

Отчёт сохранить в `.ai/reports/2026-07-26-entity-namespace-hunttech-guard.md` и добавить комментарием к PR.

## Формат успешного отчёта

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
REPO: aananyev/it-pearls-recruiting
BRANCH: agent/entity-namespace-hunttech-guard
PR: <номер>
BASE: master
VERIFIED HEAD: <полный SHA>
HEAD MATCH: PASS
CONFLICTS: NONE
EntityNamespaceIntegrityTest: 3/3 PASS
ScreenViewIntegrityTest: 8/8 PASS
COMPILE: PASS
CLEAN ASSEMBLE: PASS
LOCAL DEPLOY: PASS
HTTP /hrm/: 200
LEGACY RELATIONS: 0
TOMCAT CRITICAL ERRORS: NONE
P1: 0
P2: 0
MERGE: NOT PERFORMED
PRODUCTION: NOT CHANGED
```

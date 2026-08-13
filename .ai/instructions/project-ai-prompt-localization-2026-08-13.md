# Hermes — локальная проверка русских Project AI prompt HRM HuntTech

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/project-ai-prompt-localization`  
BASE: `master`  
STATUS: `WAITING_FOR_HERMES`  
MODE: проверка точного HEAD и применение миграции только к локальной БД; функциональный код/docs не менять

## Exact HEAD gate

Полный `Verified HEAD to check` взять из актуального описания PR этой ветки. До любых действий подтвердить:

- ветка существует;
- branch HEAD = PR HEAD = `Verified HEAD to check`;
- `base=master`;
- conflicts=`NONE`;
- working tree clean.

Несовпадение → `HEAD_MISMATCH`, проверку остановить. Новый commit аннулирует предыдущий отчёт.

## 1. Scope audit

Проверить diff:

```bash
git diff --check origin/master...HEAD
git diff --name-only origin/master...HEAD
```

Допустимый scope:

- новый Liquibase changelog `260813-1-localizeProjectDescriptionAiPrompts.xml`;
- локальный PostgreSQL script с тем же поведением;
- include в `db.changelog-master.xml`;
- контрактный тест;
- DB documentation/README;
- эта инструкция.

Не должно быть изменений Project entity, ProjectBrowse/ProjectEdit Java/XML, AI services, credentials, models, controllers, SCSS или других бизнес-экранов.

## 2. DB migration contract

Подтвердить статически:

- target table: `HUNTTECH_AI_FUNCTION_CONFIGURATION`;
- target code: `PROJECT_DESCRIPTION_GENERATE`;
- `SYSTEM_PROMPT` и `PROMPT_TEMPLATE` на русском языке;
- template содержит `${projectName}`, `${sourceFileName}`, `${sourceText}`;
- UPDATE разрешён только для пустого/нерусского prompt либо исходного migration-seed версии 1;
- уже изменённый администратором русский prompt не перезаписывается;
- UPDATE не меняет `ADMIN_CONFIGURATION_ID`, `ADMIN_MODEL_NAME`, credentials/API key, `EXECUTION_POLICY`, `FALLBACK_POLICY`;
- отсутствуют `DELETE`, `DROP`, `TRUNCATE`;
- повторное применение direct SQL не должно повторно менять уже локализованную запись.

## 3. Профильные тесты

```bash
./gradlew :app-core:test \
  --tests '*ProjectDescriptionAiPromptMigrationTest*' \
  --tests '*ProjectDescriptionAiUploadContractTest*' \
  --tests '*ProjectAiServiceTest*' \
  --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
```

Ожидается:

- профильные tests PASS;
- `ScreenViewIntegrityTest 8/8 PASS`;
- Data View Integrity PASS.

## 4. Применить только к локальной БД

Использовать штатный локальный механизм CUBA updateDb/Liquibase, которым проект применяет `db.changelog-master.xml`. Production не трогать.

До миграции сохранить вывод:

```sql
SELECT id, code, system_prompt, prompt_template,
       configuration_version, created_by, updated_by
FROM hunttech_ai_function_configuration
WHERE code = 'PROJECT_DESCRIPTION_GENERATE';
```

После применения сохранить вывод:

```sql
SELECT code, capability, system_prompt, prompt_template,
       execution_policy, fallback_policy,
       configuration_version, updated_by
FROM hunttech_ai_function_configuration
WHERE code = 'PROJECT_DESCRIPTION_GENERATE';
```

Ожидается:

- ровно одна строка;
- оба prompt на русском языке;
- присутствуют `projectName`, `sourceFileName`, `sourceText` placeholders;
- для исходного seed `configuration_version >= 2`;
- provider/model/credential/policy не изменены миграцией.

Отдельно проверить `DATABASECHANGELOG`: changeSet `260813-1-localizeProjectDescriptionAiPrompts` применён ровно один раз.

## 5. Build

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- SCSS PASS;
- `BUILD SUCCESSFUL`.

## 6. Local deploy/runtime smoke

После чистого локального deploy:

```text
http://localhost:8080/hrm/ = HTTP 200
Tomcat critical errors = NONE
```

Smoke:

1. `ProjectBrowse` → открыть Project → «Описание проекта».
2. Открыть `Управление AI → Функции AI → PROJECT_DESCRIPTION_GENERATE` и подтвердить русские `systemPrompt`/`promptTemplate`.
3. Загрузить TXT; при настроенном локальном credential результат должен быть сформирован по русской инструкции.
4. По возможности повторить DOCX/PDF.
5. При недоступном AI raw extracted text остаётся fallback и UI не падает.
6. Сохранить и повторно открыть Project — описание сохраняется.

## 7. Ограничение image/vision

Текущий `ProjectBrowse.java` только отображает `projectLogo`; AI image transformation там не реализован. Текущий `AiExecutionService` не исполняет `VISION`/`IMAGE_GENERATION`. Не добавлять вручную image/vision seed и не менять функциональный код в рамках проверки этой миграции.

Если обнаружится отдельный новый function code image/vision в фактическом проверяемом HEAD, остановить проверку и вернуть `SCOPE_MISMATCH` ChatGPT — миграцию должен исправлять ChatGPT.

## 8. Запреты

Hermes без прямого разрешения владельца:

- не меняет функциональный код или docs;
- не делает commit/push/rebase/merge;
- не разрешает конфликты;
- не применяет migration к production;
- не меняет production DB/Tomcat/config.

## 9. Итоговый отчёт

Успех:

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/project-ai-prompt-localization
PR: <номер>
Base: master
Verified HEAD: <полный SHA>
HEAD match: PASS
Conflicts: NONE
Prompt migration test: PASS
Project AI tests: PASS
ScreenViewIntegrityTest: 8/8 PASS
Local DB migration: PASS
Russian prompts: PASS
DATABASECHANGELOG: PASS
SCSS: PASS
Build: PASS
Local deploy: PASS
HTTP /hrm/: 200
Tomcat errors: NONE
Smoke: PASS
Docs/history: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
```

Ошибка: `STATUS: FAILED_VERIFICATION` с FAILED STEP, ROOT CAUSE, логом, выполненными/неисполненными проверками и рекомендацией. Код не менять.

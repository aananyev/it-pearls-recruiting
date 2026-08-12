# Hermes — проверка Project description AI upload HRM HuntTech

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/project-description-ai-upload`  
PR: `#131`  
BASE: `master`  
STATUS: `WAITING_FOR_HERMES`  
MODE: проверка точного HEAD без изменения функционального кода/docs

## Exact HEAD gate

Полный `Verified HEAD to check` взять из актуального описания PR #131. До любых проверок подтвердить: branch HEAD = PR HEAD = указанному SHA; base=`master`; conflicts=`NONE`; branch существует; working tree clean. Несовпадение → `HEAD_MISMATCH`, остановить проверку. Новый commit аннулирует результаты.

## Maker/checker

- A MAKER — AI routing/service/migration; CHECKER B.
- B MAKER — ProjectEdit/CUBA upload/security/fallback; CHECKER A.
- C MAKER — tests/docs/scope/Data View Integrity; CHECKER D.
- D — integration final gate; не отменяет FAIL A/B/C.

Ни один агент не подтверждает собственный домен.

## A — service, AI routing, DB

Проверить:

- `ProjectAiService` использует только `PROJECT_DESCRIPTION_GENERATE` + context;
- `ProjectAiServiceBean` делегирует в `AiExecutionService`;
- `ProjectEdit` не импортирует `AIProviderRegistry`, `UserAiConfiguration`, `VacancyPromptTemplate` и не хранит prompt/provider/model/API key;
- context = `projectName`, `sourceFileName`, `sourceText`;
- Liquibase/production SQL создают максимум одну функцию, не перезаписывают существующую admin configuration;
- миграция не меняет `Project`, `SkillTree`, другие справочники и не содержит secret.

## B — CUBA UI/upload/security

Проверить путь `ProjectBrowse → ProjectEdit → Описание проекта`.

- XML layout существующей карточки не перестроен;
- upload программно добавляется в `projectDescriptionCard`;
- PDF/DOCX/TXT, limit 10 MiB;
- raw extracted text устанавливается до AI call;
- AI выполняется через `BackgroundWorker`;
- AI failure сохраняет raw fallback;
- temporary upload удаляется после extraction;
- RichTextArea получает escaped text;
- DOCX parser отключает DTD/external entities;
- существующая lazy load логика `projectDescription`/`templateLetter`/vacancies не сломана.

## C — tests/docs/scope

Запустить:

```bash
./gradlew :app-core:test \
  --tests '*ProjectAiServiceTest*' \
  --tests '*ProjectDescriptionTextExtractorTest*' \
  --tests '*ProjectDescriptionAiUploadContractTest*' \
  --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
```

Ожидается: профильные tests PASS; `ScreenViewIntegrityTest 8/8 PASS`; Data View Integrity PASS.

Проверить docs: `ProjectEdit_Spec.md`, `ProjectAiService.md`, architecture doc, production migration runbook и профильные README. Scope diff не должен менять другие бизнес entity/screens.

## D — build/runtime final gate

```bash
git diff --check origin/master...HEAD
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается SCSS PASS, `BUILD SUCCESSFUL`.

После clean local deploy:

```text
http://localhost:8080/hrm/ = HTTP 200
Tomcat critical errors = NONE
```

Runtime smoke:

1. ProjectBrowse → открыть сохранённый Project → «Описание проекта».
2. TXT: raw появляется, configured AI заменяет его обработанным текстом.
3. DOCX и PDF: extraction + AI аналогично.
4. Отключить/не настроить `PROJECT_DESCRIPTION_GENERATE`: raw остаётся, UI не падает.
5. Изменить admin prompt функции, повторить upload: следующий результат следует новой admin configuration без изменения экрана.
6. Сохранить/переоткрыть Project: description сохранён.
7. Новый Project: upload не нарушает lifecycle.
8. Проверить, что временный upload не становится persistent attachment Project.
9. Проверить production seed на тестовой/локальной БД: одна строка, повторное применение не меняет admin-configured row.

## Consensus

`READY_TO_MERGE` только при A=PASS + B checker PASS, B=PASS + A checker PASS, C=PASS + D checker PASS, D integration PASS, P1=0, P2=0, exact HEAD match, conflicts NONE, docs synchronized, local deploy PASS, HTTP 200, smoke PASS.

Hermes не меняет функциональный код/docs, не делает commit/push/rebase/merge, не выполняет production migration и не меняет production.

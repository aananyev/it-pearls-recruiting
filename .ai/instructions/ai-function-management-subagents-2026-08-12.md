# Hermes — мультиагентная проверка AI Control Plane HRM HuntTech

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/ai-function-management-architecture`  
PR: `#128`  
BASE: `master`  
STATUS: `WAITING_FOR_HERMES`  
MODE: независимая maker/checker-проверка точного HEAD без изменения функционального кода

## 1. Exact HEAD gate

`Verified HEAD to check` взять только из актуального описания PR #128. До запуска подтвердить branch HEAD = PR HEAD = указанному SHA, base=`master`, conflicts=`NONE`, рабочее дерево чистое. Несовпадение → `HEAD_MISMATCH`, проверку остановить. Новый commit аннулирует все результаты.

## 2. Maker/checker

- A — Architecture & Data Model; checker C.
- B — Security & Credentials; checker D.
- C — CUBA UI / Data View Integrity; checker A.
- D — Services / Resolver / Tests; checker B.
- E — Integration / Build / Runtime final gate.

Ни один субагент не подтверждает собственный домен. Любое расхождение maker/checker → `FAILED_VERIFICATION`.

## 3. A — Architecture & Data Model

Проверить:

- `AdminAiConfiguration`, `AiFunctionConfiguration`, `UserAiFunctionOverride`;
- JPA ↔ Liquibase, FK/index/unique;
- legacy migration `260812-4-migrateLegacyVacancyPrompts`;
- сохранение `VacancyPromptTemplate` как legacy, но отсутствие runtime-чтения из `HrmAiServiceBean`;
- `AiProviderCatalog` как global UI contract;
- persistence/views/docs/history;
- отсутствие non-AI entity/screen changes.

Checker C подтверждает views/loaders/XML/data integrity.

## 4. B — Security & Credentials

Проверить:

- corporate secret отсутствует в browse-view/DataGrid/filter;
- Admin Edit использует unbound secret input;
- `web-permissions.xml` регистрирует `hunttech.ai.manageCorporateCredentials`;
- `AiCredentialServiceBean` проверяет `Security.isSpecificPermitted(...)`, а не только screen permission;
- AES-GCM random IV/roundtrip/wrong-key;
- decrypt только core-side;
- service API не экспортирует decrypt/get-secret;
- логи/errors не раскрывают secret;
- real production keys отсутствуют.

Checker D проверяет middleware bypass paths.

## 5. C — CUBA UI / Data View Integrity

Проверить шесть новых AI Browse/Edit screens и AI-only изменение `UserAiConfigurationEdit`:

- StandardLookup/StandardEditor, DataContext, containers/loaders/views;
- shared Edit contract и label-navigation;
- current-user row scope override;
- ADMIN_ONLY нельзя сохранить как personal override;
- user credential picker только текущего пользователя;
- `UserAiConfigurationEdit` и `AdminAiConfigurationEdit` используют один `AiProviderCatalog`, нет локальных дублирующих карт провайдеров;
- ScreenViewIntegrityTest ожидаемо 8/8;
- отсутствуют unfetched/detached reads.

Checker A подтверждает entity/view consistency.

## 6. D — Services / Resolver / Tests

Проверить resolver:

- ADMIN_ONLY → admin;
- USER_OVERRIDE_ALLOWED + valid override → personal;
- override отсутствует → admin;
- personal failure + FALLBACK_TO_ADMIN → admin fallback;
- USER_REQUIRED без override → controlled error;
- ownership current user;
- disabled/unsupported function → provider не вызывается;
- prompt/system context только из `AiFunctionConfiguration`;
- provider через `AIProviderRegistry`.

Отдельно проверить cut-over vacancy AI:

- `HrmAiServiceBean` inject `AiExecutionService`;
- рабочие методы не содержат DataManager query `UserAiConfiguration`/`VacancyPromptTemplate`;
- новый provider-independent API присутствует;
- legacy overloads сохраняются и игнорируют `providerCode`;
- `templateCode` legacy совместим с `functionCode` благодаря Liquibase migration;
- прямой provider call остаётся только в `testConnection(UserAiConfiguration)` как диагностика credential;
- `AIProviderCatalogTest` подтверждает соответствие global catalog фактическим core providers.

Запустить:

```bash
./gradlew :app-core:test \
  --tests '*AiSecretCipherTest*' \
  --tests '*AiControlPlaneServiceTest*' \
  --tests '*AiControlPlaneScreenContractTest*' \
  --tests '*AIProviderCatalogTest*' \
  --tests '*HrmAiServiceTest*' \
  --no-daemon --stacktrace
```

Checker B повторно проверяет security-sensitive branches.

## 7. E — Integration final gate

E допускается только после PASS/PASS A–D.

```bash
git diff --check origin/master...HEAD
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается `ScreenViewIntegrityTest 8/8 PASS`, SCSS PASS, `BUILD SUCCESSFUL`.

После clean local deploy:

- `http://localhost:8080/hrm/` = HTTP 200;
- Tomcat critical errors = NONE;
- smoke AI Administration = PASS;
- P1=0, P2=0.

## 8. Runtime smoke

Под full-access/admin role:

1. открыть AI Administration;
2. проверить функции/corporate connections/user overrides/legacy entries;
3. без `hunttech.ai.encryptionKey` corporate secret save → controlled error;
4. с test encryption key сохранить НЕ-production secret → reopen не показывает plaintext;
5. specific permission отсутствует → middleware corporate credential operation denied;
6. AI function code после сохранения read-only;
7. обычный пользователь видит только свои overrides/credentials;
8. ADMIN_ONLY нельзя назначить personal override;
9. model override policy соблюдается;
10. light/dark UI не ломается.

## 9. Scope audit

FAIL при изменениях `Project/ProjectEdit`, `OpenPosition/OpenPositionEdit`, `JobCandidate/JobCandidateEdit`, `CandidateCV/CandidateCVEdit` и других non-AI entity/screens.

Разрешены AI entities/services/core/config/views/Liquibase/tests/docs/screens, AI-specific `web-permissions.xml`, provider catalog и `web-menu.xml` только внутри `aiAdministration`.

## 10. Отчёты

Сохранять при необходимости в `docs/performance-archive/2026-08-12/ai-control-plane/`:

- `subagent-a-architecture-data.md`;
- `subagent-b-security.md`;
- `subagent-c-cuba-ui.md`;
- `subagent-d-services-tests.md`;
- `subagent-e-integration.md`;
- `final-cross-review.md`.

Формат каждого: verified HEAD, maker/checker role, PASS/FAIL, evidence, P1/P2/P3, code changed NO, production changed NO.

## 11. Consensus gate

`READY_TO_MERGE` только если Exact HEAD PASS, conflicts NONE, A–D maker/checker PASS/PASS, E PASS, AI tests PASS, ScreenViewIntegrityTest 8/8, SCSS PASS, clean assemble SUCCESS, deploy PASS, HTTP 200, logs clean, smoke PASS, docs synchronized, P1=0, P2=0.

Hermes не меняет функциональный код, не делает rebase/merge и не меняет production.

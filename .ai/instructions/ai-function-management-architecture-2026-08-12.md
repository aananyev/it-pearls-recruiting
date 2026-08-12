# Hermes — проверка реализации AI Control Plane HRM HuntTech

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/ai-function-management-architecture`  
BASE: `master`  
STATUS: `WAITING_FOR_HERMES`  
MODE: верификация точного HEAD без изменения кода и документации

## 1. Exact HEAD gate

Полный `Verified HEAD to check` взять из описания PR #128. Перед любыми действиями подтвердить:

1. ветка существует;
2. её HEAD равен `Verified HEAD to check`;
3. PR #128 открыт из этой ветки напрямую в `master`;
4. HEAD PR равен тому же SHA;
5. base=`master`;
6. conflicts=`NONE`;
7. рабочее дерево чистое.

Любое несовпадение → `HEAD_MISMATCH`, проверку остановить. Новый commit аннулирует этот отчёт.

## 2. Scope audit

Разрешены только AI Control Plane и его документация/tests:

- новые `entity/ai/*`, `AiCredentialService`, `AiExecutionService`, core AI secret/resolver implementation;
- `persistence.xml`, `app-component.xml`, новый AI views file;
- AI Liquibase changelog + master include;
- новые AI Browse/Edit screens и существующий `web-menu.xml` только внутри `aiAdministration`;
- AI tests/docs;
- архитектурный документ и его README index.

**FAIL**, если изменены `Project`, `ProjectEdit`, `OpenPosition`, `OpenPositionEdit`, `JobCandidate`, `JobCandidateEdit`, `CandidateCV`, `CandidateCVEdit` либо другие не-AI entity/screens.

## 3. Static diff checks

```bash
git diff --check origin/master...HEAD
git diff --name-only origin/master...HEAD
```

Проверить, что изменения menu ограничены `aiAdministration` и старые `VacancyPromptTemplate`/`UserAiConfiguration` entries сохранены.

## 4. AI entity / Liquibase integrity

Проверить соответствие JPA ↔ DB:

- `hunttech_AdminAiConfiguration` ↔ `HUNTTECH_ADMIN_AI_CONFIGURATION`;
- `hunttech_AiFunctionConfiguration` ↔ `HUNTTECH_AI_FUNCTION_CONFIGURATION`;
- `hunttech_UserAiFunctionOverride` ↔ `HUNTTECH_USER_AI_FUNCTION_OVERRIDE`;
- unique `AiFunctionConfiguration.code`;
- unique `(USER_ID, AI_FUNCTION_ID)`;
- FK override → `SEC_USER`, function, `HUNTTECH_USER_AI_CONFIGURATION`;
- FK function → corporate configuration;
- changelog `260812-1-addAiFunctionControlPlane.xml` включён один раз в master changelog.

## 5. Credential security audit

Обязательно подтвердить:

- `AdminAiConfiguration` = `@SystemLevel`;
- safe browse-view не содержит `apiKeyEncrypted`;
- Admin Browse DataGrid/filter не содержит secret;
- Admin Edit `apiKeyInput` unbound, нет `property="apiKeyEncrypted"`;
- service API не содержит decrypt/get-secret;
- AES-GCM roundtrip/random-IV/wrong-key tests PASS;
- `hunttech.ai.encryptionKey` не имеет реального значения в Git;
- middleware test принимает только UUID, decrypt выполняется core-only;
- logs/errors не печатают plaintext key;
- ordinary-user role без screen/entity grant не видит corporate screens/entity.

## 6. Resolver functional contract

Проверить код/tests:

- `ADMIN_ONLY` → только admin;
- `USER_OVERRIDE_ALLOWED` → valid per-function user override → personal credential;
- override отсутствует → admin;
- user provider failure + `FALLBACK_TO_ADMIN` → admin fallback;
- `USER_REQUIRED` без valid override → controlled error;
- override проверяет current-user ownership;
- disabled function → API не вызывается;
- unsupported capability → API не вызывается;
- prompt обрабатывается из `AiFunctionConfiguration`, не из business screen;
- provider выбирается существующим `AIProviderRegistry`.

Реальные платные внешние API не использовать, кроме явного manual smoke при наличии выделенного тестового credential.

## 7. Profile tests

```bash
./gradlew :app-core:test \
  --tests '*AiSecretCipherTest*' \
  --tests '*AiControlPlaneServiceTest*' \
  --tests '*AiControlPlaneScreenContractTest*' \
  --no-daemon --stacktrace
```

Ожидается PASS.

## 8. Screen / Data View Integrity

```bash
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
```

Ожидается `8/8 PASS`.

Дополнительно вручную сопоставить controller getters / XML bindings с:

- `ai-function-configuration-*` views;
- `admin-ai-configuration-*` views;
- `user-ai-function-override-*` views;
- `user-ai-configuration-override-picker-view`;
- core execution views.

Не должно быть unfetched getter доступа.

## 9. Browse/Edit contract smoke

Для трёх новых Edit-форм подтвердить:

- `edit-screen-layout`;
- sidebar 312px;
- identity → `label-navigation`;
- `label-nav-title`, `label-nav-item`, active-state;
- `edit-workspace`, toolbar, cards, footer;
- навигация только presentation/focus;
- local root namespace;
- стандартные DataContext save/close actions.

Для трёх Browse-форм подтвердить StandardLookup/container/loader/actions/filter/table lifecycle и отсутствие секретов.

Проверить XML semantic comments согласно `XML_Screen_Documentation_Standard.md`.

## 10. SCSS/build/runtime

Новый SCSS не добавлялся: формы используют существующий shared Edit API. Тем не менее собрать темы для защиты shared contract:

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
```

Затем:

```bash
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается `BUILD SUCCESSFUL`.

После clean local deploy:

```text
http://localhost:8080/hrm/ = HTTP 200
```

Проверить Tomcat logs: critical errors `NONE`.

## 11. Manual smoke

Под admin/full-access ролью:

1. открыть «Управление AI»;
2. создать corporate connection с тестовым placeholder запрещено сохранять без server `hunttech.ai.encryptionKey` — controlled error, UI жив;
3. при настроенном server encryption key создать/сохранить запись → повторное Edit не показывает прежний plaintext;
4. создать AI-функцию → code после сохранения read-only;
5. проверить policy/capability/fallback enum fields;
6. обычным пользователем открыть «Мои замещения AI-функций» → видны только свои записи/credentials;
7. `ADMIN_ONLY` функция отсутствует среди options и не сохраняется через обход UI;
8. model override read-only, если function запрещает его;
9. light/dark themes: sidebar/workspace/nav/cards/footer читаемы и не ломают layout.

## 12. Documentation

Проверить синхронизацию:

- `docs/architecture/HRM_HuntTech_AI_Function_Management_Architecture.md` + architecture README;
- entity docs + `docs/entities/ai-control-plane/README.md`;
- `docs/services/AiExecutionService.md`, `AiCredentialService.md`, services README;
- шесть `docs/ui/*_Spec.md` + `docs/ui/ai-control-plane/README.md`;
- history rows date `2026-08-12` newest first.

## 13. Запреты Hermes

Не менять функциональный код/docs, не делать commit/push/rebase/merge, не разрешать конфликты, не менять production, не вводить реальные production AI keys.

## 14. Успешный отчёт

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE
Repo: aananyev/it-pearls-recruiting
Branch: agent/ai-function-management-architecture
PR: 128
Base: master
Verified HEAD: <SHA из PR>
HEAD match: PASS
Conflicts: NONE
Scope audit: PASS
Entity/Liquibase integrity: PASS
Credential security: PASS
AI profile tests: PASS
ScreenViewIntegrityTest: 8/8 PASS
Data View Integrity: PASS
Browse/Edit contracts: PASS
buildScssThemes: PASS
Clean assemble: BUILD SUCCESSFUL
Local deploy: PASS
HTTP /hrm/: 200
Tomcat critical errors: NONE
Smoke: PASS
Docs/history synchronized: PASS
P1: 0
P2: 0
Merge: NOT PERFORMED
Production: NOT CHANGED
проверен HEAD: <SHA из PR>
```

При любой ошибке: `STATUS: FAILED_VERIFICATION`, `FAILED STEP`, `ROOT CAUSE`, stack/log, выполненные/невыполненные проверки. Код не менять.

# Hermes — мультиагентная проверка AI Control Plane HRM HuntTech

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/ai-function-management-architecture`  
PR: `#128`  
BASE: `master`  
STATUS: `WAITING_FOR_HERMES`  
MODE: независимая проверка точного HEAD без изменения функционального кода

## 1. Обязательный принцип maker/checker

Проверка выполняется пятью независимыми субагентами Hermes. Один субагент не имеет права единолично подтвердить собственный результат.

Для каждого домена назначаются:

- `MAKER` — выполняет первичную проверку и приводит доказательства;
- `CHECKER` — повторно проверяет критические выводы MAKER по исходному коду, diff, тестам или runtime-доказательствам;
- `FINAL GATE` — принимает домен только если MAKER=PASS и CHECKER=PASS.

Если MAKER и CHECKER расходятся, статус домена = `FAILED_VERIFICATION` до устранения причины. Нельзя усреднять или игнорировать более строгий вывод.

Субагенты не меняют Java/XML/SCSS/entity/views/JPQL/Liquibase/функциональную документацию и не выполняют commit/push/rebase/merge. Разрешено создавать только отчёты проверки в `docs/performance-archive/2026-08-12/ai-control-plane/`, если это предусмотрено текущими правилами Hermes.

## 2. Exact HEAD gate — общий для всех

Перед запуском субагентов координатор Hermes обязан получить `Verified HEAD to check` из актуального описания PR #128 и подтвердить:

1. ветка существует;
2. branch HEAD = PR HEAD = `Verified HEAD to check`;
3. PR открыт `agent/ai-function-management-architecture → master`;
4. base = `master`;
5. conflicts = `NONE`;
6. локальное рабочее дерево чистое;
7. `git diff --check origin/master...HEAD` = PASS.

Любое несовпадение → `HEAD_MISMATCH`; все субагенты прекращают проверку. Новый commit аннулирует все ранее полученные результаты.

## 3. Субагент A — Architecture & Data Model

### Роль MAKER

Проверить архитектуру AI Control Plane и JPA/Liquibase/Data View Integrity:

- `AdminAiConfiguration`;
- `AiFunctionConfiguration`;
- `UserAiFunctionOverride`;
- существующий `UserAiConfiguration` как personal credential;
- separation of concerns с `UserAiProfile` и `VacancyPromptTemplate`;
- enum-политики capability/execution/fallback;
- JPA ↔ таблицы/колонки/FK/index/unique constraints;
- `persistence.xml`, `app-component.xml`, `ai-control-plane-views.xml`;
- отсутствие изменений не-AI entity/screens;
- документацию entity/architecture и history/README.

Обязательные инварианты:

- unique `AiFunctionConfiguration.code`;
- unique `(USER_ID, AI_FUNCTION_ID)`;
- override связан только с существующим personal credential;
- function ссылается на corporate configuration, но не хранит plaintext secret;
- execution views содержат ровно необходимый graph;
- browse views не содержат secret attributes.

### CHECKER для A: субагент C

Субагент C независимо проверяет views/data containers/XML bindings и подтверждает, что модель A реально пригодна для CUBA screens без unfetched/detached ошибок.

### Выход A

`A_ARCH_DATA: PASS|FAIL` + список доказательств и замечаний P1/P2/P3.

## 4. Субагент B — Security & Credentials

### Роль MAKER

Проверить защиту корпоративных credentials:

- `AdminAiConfiguration` системного уровня;
- corporate secret не присутствует в browse-view/DataGrid/filter;
- edit использует unbound secret input;
- отсутствует UI-механизм чтения прежнего plaintext;
- encryption key не хранится реальным значением в Git;
- AES-GCM: random IV, roundtrip, wrong-key failure;
- decrypt выполняется только в core/middleware непосредственно перед provider call;
- service API не экспортирует decrypt/get-secret;
- логирование и exception text не раскрывают API key;
- specific permission `hunttech.ai.manageCorporateCredentials` используется для операций с corporate credential;
- ordinary user не получает screen/entity/specific permission corporate credentials;
- никакие реальные production AI keys не используются в тестах.

Запустить/проверить `AiSecretCipherTest` и security-oriented service tests.

### CHECKER для B: субагент D

Субагент D независимо просматривает service boundary и тесты: подтверждает отсутствие обходного пути к secret через service API/resolver и проверяет negative paths.

### Выход B

`B_SECURITY: PASS|FAIL` + threat findings с severity.

## 5. Субагент C — CUBA UI, Browse/Edit & Data View Integrity

### Роль MAKER

Проверить шесть новых AI Browse/Edit screens и изменённый существующий AI edit:

- `AiFunctionConfigurationBrowse/Edit`;
- `AdminAiConfigurationBrowse/Edit`;
- `UserAiFunctionOverrideBrowse/Edit`;
- `UserAiConfigurationEdit` только в разрешённом AI scope.

Проверить требования CUBA Platform:

- `StandardLookup` / `StandardEditor`;
- containers/loaders/DataContext lifecycle;
- `@LoadDataBeforeShow` и ручная load-логика без двойной загрузки;
- все getters/listeners ⊆ соответствующим views;
- `ScreenViewIntegrityTest` ожидаемо `8/8 PASS`;
- отсутствие unfetched getter detached entity;
- current-user filter ставится до загрузки override;
- `ADMIN_ONLY` нельзя выбрать/сохранить в user override;
- пользователь не может выбрать credential другого пользователя;
- code AI-function после сохранения защищён от случайного изменения;
- shared Edit contract: `edit-screen-layout`, sidebar 270px согласно текущему контракту, label-navigation, toolbar/cards/footer, `edit-form-control`;
- XML semantic comments;
- никакие не-AI экраны не изменены.

### CHECKER для C: субагент A

Субагент A проверяет обратную сторону: каждое поле/loader/view из UI соответствует entity/JPA контракту и не создаёт скрытый data-integrity риск.

### Выход C

`C_CUBA_UI: PASS|FAIL` + Data View Integrity matrix `controller/XML → view property`.

## 6. Субагент D — Services, Resolver & Automated Tests

### Роль MAKER

Проверить `AiCredentialService`, `AiExecutionService`, core secret layer и общий `AiProviderCatalog`.

Resolver-инварианты:

- `ADMIN_ONLY` → только corporate configuration;
- `USER_OVERRIDE_ALLOWED` + valid override → personal configuration;
- override отсутствует → corporate configuration;
- user provider failure + `FALLBACK_TO_ADMIN` → corporate fallback;
- `USER_REQUIRED` без valid override → controlled error;
- current-user ownership проверяется;
- disabled function → provider не вызывается;
- unsupported capability → provider не вызывается;
- prompt/system context берутся из `AiFunctionConfiguration`;
- provider выбирается существующим `AIProviderRegistry`;
- общий provider catalog не расходится с AI provider registry/legacy AI edit;
- legacy `HrmAiService` не ломается и остаётся совместимым до отдельной миграции потребителей.

Запустить:

```bash
./gradlew :app-core:test \
  --tests '*AiSecretCipherTest*' \
  --tests '*AiControlPlaneServiceTest*' \
  --tests '*AiControlPlaneScreenContractTest*' \
  --no-daemon --stacktrace
```

Если тестов недостаточно для какого-либо перечисленного инварианта, это не `PASS`: зафиксировать как gap/P2 и передать ChatGPT, не исправляя код самостоятельно.

### CHECKER для D: субагент B

Субагент B повторно проверяет security-sensitive ветви resolver: selection corporate/user credential, fallback, ошибки и отсутствие secret leakage.

### Выход D

`D_SERVICES_TESTS: PASS|FAIL` + таблица `инвариант → тест/код-доказательство`.

## 7. Субагент E — Integration, Build, Runtime & Final Gate

Субагент E не проверяет собственный кодовый домен и не может отменить FAIL A–D. Его задача — интеграционный gate.

До сборки получить четыре пары результатов:

- A MAKER + C CHECKER;
- B MAKER + D CHECKER;
- C MAKER + A CHECKER;
- D MAKER + B CHECKER.

Если хотя бы одна пара не `PASS/PASS`, E ставит `FAILED_VERIFICATION` и не объявляет READY_TO_MERGE.

После cross-review выполнить:

```bash
./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается:

- `ScreenViewIntegrityTest: 8/8 PASS`;
- `buildScssThemes: PASS`;
- `BUILD SUCCESSFUL`.

После clean local deploy проверить:

```text
http://localhost:8080/hrm/ = HTTP 200
```

Проверить Tomcat logs: critical errors `NONE`.

## 8. Runtime smoke — E с подтверждением C/B

Под admin/full-access ролью, имеющей corporate credential permission:

1. открыть «Управление AI»;
2. проверить пункты «Функции AI», «Корпоративные подключения», «Мои замещения AI-функций», legacy AI entries;
3. без `hunttech.ai.encryptionKey` сохранение нового corporate key должно завершаться controlled error без падения UI;
4. при server encryption key сохранить тестовый НЕ-production credential → повторный Edit не показывает старый plaintext;
5. создать AI-function → code после сохранения read-only;
6. проверить policy/capability/fallback;
7. обычным пользователем → только свои override/credentials;
8. `ADMIN_ONLY` нельзя назначить personal override;
9. запрет model override соблюдается;
10. light/dark theme визуально не ломают Edit layout.

Субагент C подтверждает UI/runtime пункты 1,2,5,6,7,8,9,10.  
Субагент B подтверждает security/runtime пункты 3,4,7,8.

## 9. Scope audit — каждый субагент обязан проверить

FAIL, если diff затрагивает неразрешённые бизнес-сущности/экраны, включая:

- `Project` / `ProjectEdit`;
- `OpenPosition` / `OpenPositionEdit`;
- `JobCandidate` / `JobCandidateEdit`;
- `CandidateCV` / `CandidateCVEdit`;
- другие не-AI entities/screens.

Разрешены только AI Control Plane, AI services, AI-specific screens, AI views/config, AI Liquibase/tests/docs и локальные изменения меню внутри `aiAdministration`.

## 10. Формат внутренних отчётов

Каждый субагент пишет краткий отчёт:

```text
PROJECT: HRM HuntTech
SUBAGENT: A|B|C|D|E
ROLE: MAKER|CHECKER|FINAL_GATE
VERIFIED HEAD: <full SHA>
SCOPE: <domain>
RESULT: PASS|FAIL
P1: <n>
P2: <n>
P3: <n>
EVIDENCE:
- ...
CROSS_CHECK_OF: <subagent/domain or N/A>
DISAGREEMENTS: NONE|...
CODE CHANGED: NO
PRODUCTION CHANGED: NO
```

Если Hermes сохраняет отчёты, использовать:

```text
docs/performance-archive/2026-08-12/ai-control-plane/
  subagent-a-architecture-data.md
  subagent-b-security.md
  subagent-c-cuba-ui.md
  subagent-d-services-tests.md
  subagent-e-integration.md
  final-cross-review.md
```

## 11. Финальный consensus gate

`READY_TO_MERGE` разрешён только если одновременно:

- Exact HEAD match PASS;
- conflicts NONE;
- A_ARCH_DATA PASS и подтверждён C;
- B_SECURITY PASS и подтверждён D;
- C_CUBA_UI PASS и подтверждён A;
- D_SERVICES_TESTS PASS и подтверждён B;
- E integration PASS;
- ScreenViewIntegrityTest 8/8 PASS;
- Data View Integrity PASS;
- buildScssThemes PASS;
- clean assemble BUILD SUCCESSFUL;
- local deploy PASS;
- HTTP `/hrm/` 200;
- Tomcat critical errors NONE;
- smoke PASS;
- docs/history synchronized;
- P1=0;
- P2=0.

P3 допускается только как явно зафиксированное некритичное замечание, не нарушающее требования задачи.

## 12. Финальный отчёт Hermes

```text
PROJECT: HRM HuntTech
STATUS: READY_TO_MERGE|FAILED_VERIFICATION
Repo: aananyev/it-pearls-recruiting
Branch: agent/ai-function-management-architecture
PR: 128
Base: master
Verified HEAD: <full SHA>
HEAD match: PASS|FAIL
Conflicts: NONE|...
A Architecture/Data: PASS|FAIL; checker C: PASS|FAIL
B Security: PASS|FAIL; checker D: PASS|FAIL
C CUBA UI/Data View: PASS|FAIL; checker A: PASS|FAIL
D Services/Tests: PASS|FAIL; checker B: PASS|FAIL
E Integration/Runtime: PASS|FAIL
ScreenViewIntegrityTest: 8/8 PASS|FAIL
Data View Integrity: PASS|FAIL
buildScssThemes: PASS|FAIL
Clean assemble: BUILD SUCCESSFUL|FAIL
Local deploy: PASS|FAIL
HTTP /hrm/: 200|FAIL
Tomcat critical errors: NONE|...
Smoke: PASS|FAIL
Docs/history synchronized: PASS|FAIL
P1: <n>
P2: <n>
P3: <n>
Merge: NOT PERFORMED
Production: NOT CHANGED
проверен HEAD: <full SHA>
```

При любой ошибке обязательно указать `FAILED STEP`, `ROOT CAUSE`, доказательство/лог и какие проверки не выполнялись. Функциональный код не менять.
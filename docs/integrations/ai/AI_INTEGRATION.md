# AI Integration — Architecture Passport

> **HRM HuntTech** · CUBA Platform 7.3  
> Актуальное состояние AI-подсистемы после внедрения AI Control Plane.

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

AI-интеграция HRM HuntTech строится вокруг стабильных AI-функций, а не вокруг выбора провайдера на каждом бизнес-экране. Код функции определяет prompt, capability, model policy, corporate credential, возможность персонального override и fallback. Такой контракт позволяет менять LLM-провайдера и модель без изменения бизнес-экранов.

### UI Context & Navigation

Административный контур расположен в меню «Управление AI»:

- «Функции AI» — `AiFunctionConfiguration`;
- «Корпоративные подключения» — `AdminAiConfiguration`;
- «Мои замещения AI-функций» — `UserAiFunctionOverride`;
- «Шаблоны промптов» — `VacancyPromptTemplate`, legacy compatibility;
- «Мониторинг ключей пользователей» — `UserAiConfiguration`.

Бизнес-экраны не должны читать ключи или выбирать HTTP endpoint. Они передают `functionCode + context` в middleware.

### Behavior Summary

- AI-действие → `AiExecutionService.executeText(functionCode, context)`;
- resolver → `ADMIN_ONLY`, `USER_OVERRIDE_ALLOWED` или `USER_REQUIRED`;
- допустимый personal override → `UserAiConfiguration` текущего пользователя;
- admin route → `AdminAiConfiguration`, секрет расшифровывается только в core;
- personal failure + `FALLBACK_TO_ADMIN` → повтор через corporate credential;
- disabled function/unsupported capability → controlled error до внешнего API;
- vacancy legacy facade → `HrmAiService` делегирует рабочую генерацию тому же Control Plane.

---

## 1. Архитектура

```text
WEB / business consumer
        │ functionCode + context
        ▼
HrmAiService (vacancy compatibility facade) / AiExecutionService
        │
        ▼
AiFunctionConfiguration
        │ executionPolicy + fallbackPolicy
        ├──────────────► UserAiFunctionOverride ► UserAiConfiguration
        │
        └──────────────► AdminAiConfiguration ► AiSecretService
        │
        ▼
AIProviderRegistry ► AIProvider ► external LLM API
```

`AIProviderRegistry` остаётся Strategy/adapter layer. Control Plane не дублирует vendor-specific HTTP/auth logic.

## 2. Основные сущности

### 2.1 `AiFunctionConfiguration`

Source of truth для бизнес-функции AI:

- `code` — стабильный уникальный код;
- `capability`;
- `systemPrompt` / `promptTemplate`;
- `temperature` / `maxTokens`;
- `adminConfiguration` / `adminModelName`;
- `executionPolicy`;
- `fallbackPolicy`;
- `allowModelOverride`;
- `active` / `configurationVersion`.

### 2.2 `AdminAiConfiguration`

Защищённое corporate connection. `apiKeyEncrypted` не входит в browse-view и не bind-ится на Edit-компонент. Plaintext вводится через unbound `PasswordField`, шифруется middleware-сервисом и не возвращается в UI.

Операции шифрования и проверки требуют specific permission `hunttech.ai.manageCorporateCredentials` в дополнение к обычным screen/entity grants.

### 2.3 `UserAiConfiguration`

Legacy entity персонального credential остаётся inventory пользователя: provider code, API key, default model, active flag. Рабочая функция использует её только через `UserAiFunctionOverride` и проверку ownership текущего пользователя.

### 2.4 `UserAiFunctionOverride`

Связь `user + aiFunction + userAiConfiguration`, уникальная по `(USER_ID, AI_FUNCTION_ID)`. UI ограничивает picker персональными активными credentials и блокирует override для `ADMIN_ONLY`.

### 2.5 `UserAiProfile`

Отдельный слой персонализации. Он не хранит credentials и не управляет provider routing.

## 3. Execution policies

### `ADMIN_ONLY`

Только корпоративное подключение функции.

### `USER_OVERRIDE_ALLOWED`

Personal override используется при наличии. Без override — corporate route. При ошибке personal route применяется `fallbackPolicy`.

### `USER_REQUIRED`

Функция требует personal override; corporate лимит не используется.

Fallback первого этапа:

- `NO_FALLBACK`;
- `FALLBACK_TO_ADMIN`.

## 4. Capability

`executeText` поддерживает:

- `TEXT_GENERATION`;
- `TEXT_ANALYSIS`;
- `TEXT_TRANSFORMATION`;
- `DOCUMENT_ANALYSIS`.

`VISION`, `IMAGE_GENERATION`, `EMBEDDING`, `AUDIO_TRANSCRIPTION` зарезервированы моделью и требуют отдельных typed adapters.

## 5. Vacancy compatibility facade

`HrmAiService` сохраняет старые overloads, чтобы не требовать одновременного изменения бизнес-экранов:

```java
String standardizeVacancyDescription(String rawText, String providerCode);
String generateVacancyArtifact(String description, String templateCode, String providerCode);
```

Но `providerCode` больше не является управляющим параметром. Методы делегируют provider-independent API:

```java
String standardizeVacancyDescription(String rawText);
String generateVacancyArtifact(String description, String functionCode);
```

Таким образом старый UI не может обойти policy, даже если продолжает передавать vendor code.

`testConnection(UserAiConfiguration)` остаётся прямой диагностикой конкретного personal credential. Он не используется для рабочих AI-функций.

## 6. Legacy prompt migration

ChangeSet `260812-4-migrateLegacyVacancyPrompts` копирует существующие активные `VacancyPromptTemplate` в `AiFunctionConfiguration` с тем же `CODE`.

Правила миграции:

- `STANDARDIZE_VACANCY` → `TEXT_TRANSFORMATION`;
- другие legacy codes → `TEXT_GENERATION`;
- `executionPolicy=USER_REQUIRED`;
- `fallbackPolicy=NO_FALLBACK`;
- corporate connection не назначается автоматически;
- существующая `VacancyPromptTemplate` не удаляется и остаётся legacy UI до отдельной cleanup-задачи.

Это обеспечивает безопасный cut-over без автоматического расхода корпоративных credentials.

## 7. Provider catalog

`AiProviderCatalog` в global-модуле является единым UI-каталогом caption/providerCode/defaultModel для personal и corporate Edit-форм. Контрактный `AIProviderCatalogTest` сравнивает его с фактическими core provider implementations, чтобы Web Client не расходился с `AIProviderRegistry`.

## 8. Credential security

Корпоративный secret:

1. вводится только в unbound `PasswordField`;
2. `AiCredentialService.encryptAdminSecret` проверяет specific permission;
3. `AiSecretService` использует AES-GCM;
4. ciphertext хранится в `API_KEY_ENCRYPTED`;
5. decrypt выполняется только в core непосредственно перед provider call;
6. browse-view и DataGrid не содержат secret;
7. provider error message не сохраняется целиком в `lastError`.

Реальный encryption key задаётся server property `hunttech.ai.encryptionKey` и не хранится в Git.

## 9. Data View Integrity

AI Control Plane использует отдельный `ai-control-plane-views.xml`:

- safe browse/edit views не раскрывают corporate ciphertext;
- execution view загружает function + admin configuration;
- override execution view загружает только нужный user credential graph;
- core secret view используется только middleware.

## 10. Что не изменено

В рамках Control Plane не меняются функциональные Java/XML бизнес-экраны `ProjectEdit`, `OpenPositionEdit`, `JobCandidateEdit`, `CandidateCVEdit` и другие non-AI screens. Их последующее подключение должно использовать стабильные function codes без доступа к credentials.

## 11. Проверки

Обязательный профиль:

```bash
./gradlew :app-core:test \
  --tests '*AiSecretCipherTest*' \
  --tests '*AiControlPlaneServiceTest*' \
  --tests '*AiControlPlaneScreenContractTest*' \
  --tests '*AIProviderCatalogTest*' \
  --tests '*HrmAiServiceTest*' \
  --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Сборка, deploy, HTTP 200, Tomcat logs и smoke выполняются Hermes по точному HEAD.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Vacancy runtime переведён на AI Control Plane; добавлены legacy migration, единый provider catalog и specific permission corporate credentials |
| 2026-08-12 | Создана модель AI Control Plane: функции, corporate connections, per-function user overrides, resolver и AI UI |

# AiExecutionService

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`AiExecutionService` — единая middleware-точка выполнения AI-функций HRM HuntTech. Потребитель не выбирает provider, модель и API key: он передаёт только стабильный `functionCode` и бизнес-контекст.

### UI Context & Navigation

Сервис не имеет собственного экрана. Его конфигурация управляется через «Управление AI» → «Функции AI», «Корпоративные AI-подключения» и «Мои замещения AI-функций». Legacy vacancy-фасад `HrmAiService` уже маршрутизирует рабочие генерации через этот сервис; изменение не-AI бизнес-экранов в текущем PR не выполняется.

### Behavior Summary

- `executeText(code, context)` → загружается активная функция → проверяется capability;
- `USER_REQUIRED` → требуется активный override текущего пользователя;
- `USER_OVERRIDE_ALLOWED` → при наличии валидного override вызывается personal provider;
- ошибка personal provider + `FALLBACK_TO_ADMIN` → выполняется corporate provider;
- override отсутствует / `ADMIN_ONLY` → используется corporate provider;
- secret корпоративного подключения → расшифровывается в core непосредственно перед `AIProvider.generateText`;
- `HrmAiService` legacy providerCode → не влияет на route и не может обойти policy.

## API

```java
AiExecutionResult executeText(String functionCode, Map<String, Object> context);
AiExecutionResult executeImage(String functionCode, Map<String, Object> context,
                               byte[] sourceImage, String sourceMimeType);
```

`AiExecutionResult` (`modules/global/.../service/AiExecutionResult.java`) — payload +
метаданные для пользовательской нотификации: `getText()`/`getImage()` (payload),
`getFunctionCode()`/`getFunctionName()` (что делала), `getModelName()`/`getProviderCode()`
(какая модель), `getCredentialOwner()` (`AiCredentialOwner.ADMIN | USER` — собственник
API: корпоративное подключение администратора или личное подключение пользователя).

Prompt формируется через `TemplateHelper.processTemplate`. Provider выбирается существующим `AIProviderRegistry`; vendor-specific HTTP/auth остаётся внутри `AIProvider` implementations.

## Data View Integrity

- функция: `ai-function-execution-view`;
- override: `user-ai-function-override-execution-view`;
- персональный API key появляется только в core execution view;
- корпоративный ciphertext появляется только в core secret/execution view.

## 4. Аудит и логирование вызовов (AiCallLog)

Каждый вызов через `AiExecutionService` автоматически сохраняется в сущность `hunttech_AiCallLog` (`HUNTTECH_AI_CALL_LOG`):
- **Инициатор**: системный пользователь `sec$User`, логин, ФИО.
- **Временные метрики**: дата/время вызова, длительность (`durationMs`).
- **Токены**: извлекаются из ответа API модели (`promptTokens`, `completionTokens`, `totalTokens`).
- **Стоимость**: вычисляется с помощью калькулятора тарифов `AiCostCalculator` (`estimatedCost`, `currency`).
- **Тексты**: сохраняются полный `promptText` и `responseText` (или `errorMessage` при статусе `ERROR`).
- **Контекст вызова**: `callerSource` (название вызывающего сервиса или экрана).

Журнал доступен для просмотра в интерфейсе через экран `AiCallLogBrowse` и агрегируется в дашбордах `UserAiDashboard` и `AdminAiDashboard`.

## Контракт пользовательской нотификации

Каждый успешный реальный вызов возвращает `AiExecutionResult` с метаданными: модель,
провайдер и собственник API (`ADMIN` — корпоративное подключение, `USER` — личное
подключение пользователя; пользовательский путь проставляет `USER`, административный —
`ADMIN`). Экраны обязаны показать исчезающую TRAY-нотификацию CUBA (web-утилита
`AiOperationNotifier`, 5 с) с этими данными. Полный текст контракта —
[HRM_HuntTech_AI_User_Notification_Contract](../architecture/HRM_HuntTech_AI_User_Notification_Contract.md).

## Интеграция legacy vacancy AI

`HrmAiServiceBean` больше не содержит JPQL к `UserAiConfiguration`/`VacancyPromptTemplate` для рабочих методов. `STANDARDIZE_VACANCY` и legacy template codes становятся function codes. Liquibase переносит существующие vacancy templates в `AiFunctionConfiguration`.

## Ограничения первого этапа

`executeText` поддерживает TEXT_GENERATION, TEXT_ANALYSIS, TEXT_TRANSFORMATION и DOCUMENT_ANALYSIS. `VISION`, `IMAGE_GENERATION`, `EMBEDDING`, `AUDIO_TRANSCRIPTION` присутствуют в модели, но требуют отдельных typed adapters.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-16 | Добавлено сквозное логирование всех обращений к AI в `AiCallLog`, парсинг токенов (OpenAI, DeepSeek, Anthropic) и автоматический расчет стоимости запросов `AiCostCalculator`. |
| 2026-08-16 | Контракт пользовательской нотификации: методы возвращают `AiExecutionResult` (payload + модель, провайдер, собственник API `AiCredentialOwner.ADMIN/USER`) — см. HRM_HuntTech_AI_User_Notification_Contract |
| 2026-08-12 | Подключён `HrmAiService` как совместимый vacancy-фасад; provider selection из legacy API исключён |
| 2026-08-12 | Реализован централизованный function resolver с per-function override и admin fallback |

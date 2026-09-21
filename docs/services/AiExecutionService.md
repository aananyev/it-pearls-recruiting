# AiExecutionService

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`AiExecutionService` — единая middleware-точка выполнения AI-функций HRM HuntTech. Потребитель не выбирает provider, модель и API key: он передаёт только стабильный `functionCode` и бизнес-контекст.

### UI Context & Navigation

Сервис не имеет собственного экрана. Его конфигурация управляется через «Управление AI» → «Функции AI», «Корпоративные AI-подключения» и «Мои замещения AI-функций». Vacancy-фасад `HrmAiService` маршрутизирует через него ручную генерацию в `OpenPositionEdit`; Smart Vacancy Creation всегда выполняет через тот же facade все три dedicated material-функции, сохраняя parsed/deterministic fallback на случай ошибки или пустого ответа.

Для материалов вакансии `HrmAiService.generateVacancyMaterial(VacancyMaterialType, context)` является единым business entry point. `CHECKLIST`, `SEARCH_MAP`, `INTERVIEW_PLAN` централизованно отображаются в `VACANCY_CHECKLIST`, `VACANCY_SEARCH_MAP`, `VACANCY_INTERVIEW_PLAN`; вызывающая сторона получает полный `AiExecutionResult`, включая модель, provider и владельца API.

### Behavior Summary

- `executeText(code, context)` → загружается активная функция → проверяется capability;
- `USER_REQUIRED` → требуется активный override текущего пользователя;
- `USER_OVERRIDE_ALLOWED` → при наличии валидного override вызывается personal provider;
- ошибка personal provider + `FALLBACK_TO_ADMIN` → выполняется corporate provider;
- override отсутствует / `ADMIN_ONLY` → используется corporate provider;
- secret корпоративного подключения → расшифровывается в core непосредственно перед `AIProvider.generateText`;
- `HrmAiService` legacy providerCode → не влияет на route и не может обойти policy.
- `generateVacancyMaterial(type, context)` → тип централизованно сопоставляется стабильному `VACANCY_*` code → используется тот же resolver/policy для Edit и Smart Vacancy Creation.

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

## Материалы вакансии и prompt migrations

| Бизнес-тип | Function code | Prompt migration | Policy |
|---|---|---|---|
| `CHECKLIST` | `VACANCY_CHECKLIST` | `260921-3-vacancy-checklist-prompt` | `USER_OVERRIDE_ALLOWED` / `FALLBACK_TO_ADMIN` |
| `SEARCH_MAP` | `VACANCY_SEARCH_MAP` | `260921-4-vacancy-search-map-prompt` | `USER_OVERRIDE_ALLOWED` / `FALLBACK_TO_ADMIN` |
| `INTERVIEW_PLAN` | `VACANCY_INTERVIEW_PLAN` | `260921-5-vacancy-interview-plan-prompt` | `USER_OVERRIDE_ALLOWED` / `FALLBACK_TO_ADMIN` |

Авторитетные системные prompt встроены в PostgreSQL migration; runtime не читает внешние или локальные filesystem paths. Фактическое описание вакансии передаётся отдельным context key `description` и подставляется в `PROMPT_TEMPLATE` `${description}`. Существующие административные правки защищены: migration обновляет только запись нужного `CODE`, созданную миграцией, не изменённую администратором (`CREATED_BY='migration'`, `UPDATED_BY` отсутствует либо `migration`) и с `CONFIGURATION_VERSION <= 1`; после штатного update версия становится не ниже 2. Если code отсутствует, выполняется idempotent `INSERT ... WHERE NOT EXISTS` со стабильным UUID.

### Production-safe runbook (без выполнения production в рамках задачи)

1. **Precheck:** сделать и проверить backup таблицы `HUNTECH_AI_FUNCTION_CONFIGURATION`; зафиксировать row count и полный снимок строк трёх `VACANCY_*` codes с `ID`, `CREATED_BY`, `UPDATED_BY`, `CONFIGURATION_VERSION`, policy, model/provider и prompt hashes.
2. **Apply:** применить Liquibase changelog `260921-3`, затем `260921-4`, затем `260921-5` штатным deployment-процессом. Ручной запуск SQL вне Liquibase не требуется.
3. **Verify:** для каждого code должна существовать ровно одна активная запись `TEXT_GENERATION`; проверить стабильный ID/code, `${description}`, `USER_OVERRIDE_ALLOWED`, `FALLBACK_TO_ADMIN`, `IS_ACTIVE=true`, а также отсутствие изменений во всех посторонних AI-функциях. Для ранее администраторской записи prompt/model/policy должны остаться прежними.
4. **Smoke:** выполнить по одному тестовому вызову каждого типа через AI Control Plane и убедиться, что `AiExecutionResult` содержит payload/model/provider/credential owner, а `AiCallLog` фиксирует правильный function code без secret.
5. **Rollback:** остановить дальнейшие вызовы, деактивировав только новые migration-owned записи, либо восстановить три строки из проверенного backup. Не удалять пользовательские override и не откатывать всю таблицу без отдельного решения. После rollback повторно проверить row count, уникальность codes и доступность остальных AI-функций.

## Ограничения первого этапа

`executeText` поддерживает TEXT_GENERATION, TEXT_ANALYSIS, TEXT_TRANSFORMATION и DOCUMENT_ANALYSIS. `VISION`, `IMAGE_GENERATION`, `EMBEDDING`, `AUDIO_TRANSCRIPTION` присутствуют в модели, но требуют отдельных typed adapters.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-09-21 | Добавлен типизированный единый контракт генерации материалов вакансии с возвратом `AiExecutionResult`; зарегистрированы три защищённые prompt migrations и production-safe precheck/backup/verification/rollback |
| 2026-08-16 | Добавлено сквозное логирование всех обращений к AI в `AiCallLog`, парсинг токенов (OpenAI, DeepSeek, Anthropic) и автоматический расчет стоимости запросов `AiCostCalculator`. |
| 2026-08-16 | Контракт пользовательской нотификации: методы возвращают `AiExecutionResult` (payload + модель, провайдер, собственник API `AiCredentialOwner.ADMIN/USER`) — см. HRM_HuntTech_AI_User_Notification_Contract |
| 2026-08-12 | Подключён `HrmAiService` как совместимый vacancy-фасад; provider selection из legacy API исключён |
| 2026-08-12 | Реализован централизованный function resolver с per-function override и admin fallback |

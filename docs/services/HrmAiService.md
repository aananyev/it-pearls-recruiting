# HrmAiService

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`HrmAiService` — совместимый vacancy AI-фасад HRM HuntTech. После внедрения AI Control Plane он больше не выбирает provider/API key и не читает `VacancyPromptTemplate` во время рабочей генерации. Его задача — перевести vacancy-контекст в стабильный `functionCode + context` и передать выполнение `AiExecutionService`.

Это сохраняет существующий middleware-контракт приложения, одновременно исключая обход централизованных execution/fallback policies.

### UI Context & Navigation

Сервис собственного UI не имеет. Конфигурация рабочих AI-вызовов выполняется в меню «Управление AI» через:

- «Функции AI» — function code, prompt, capability, policy, model;
- «Корпоративные подключения» — защищённые admin credentials;
- «Мои замещения AI-функций» — per-function personal override;
- `UserAiConfiguration` — персональные подключения пользователя;
- `VacancyPromptTemplate` — legacy-справочник для миграции, а не runtime source of truth.

Бизнес-экраны могут продолжить использовать legacy overloads до отдельного изменения их кода: `providerCode` в этих overloads намеренно игнорируется.

### Behavior Summary

- `standardizeVacancyDescription(rawText)` → `AiExecutionService.executeText("STANDARDIZE_VACANCY", {rawDescription})`;
- `generateVacancyArtifact(description, functionCode)` → `AiExecutionService.executeText(functionCode, {description})`;
- legacy overload с `providerCode` → делегирование provider-independent методу;
- `testConnection(UserAiConfiguration)` → диагностический прямой вызов конкретного personal credential, без выполнения бизнес-функции;
- отсутствие/отключение function configuration → controlled error из `AiExecutionService` до внешнего API.

## API

```java
String standardizeVacancyDescription(String rawText);
String generateVacancyArtifact(String standardizedDescription, String functionCode);

@Deprecated
String standardizeVacancyDescription(String rawText, String providerCode);

@Deprecated
String generateVacancyArtifact(String standardizedDescription,
                               String templateCode,
                               String providerCode);

void testConnection(UserAiConfiguration configuration);
```

## Legacy migration

Liquibase changeSet `260812-4-migrateLegacyVacancyPrompts` переносит существующие активные `HUNTTECH_VACANCY_PROMPT_TEMPLATE` в `HUNTTECH_AI_FUNCTION_CONFIGURATION` с тем же `CODE`.

Начальная политика мигрированных функций — `USER_REQUIRED` + `NO_FALLBACK`. Это намеренно не включает расход корпоративного API без явной административной настройки. `STANDARDIZE_VACANCY` получает capability `TEXT_TRANSFORMATION`, остальные legacy templates — `TEXT_GENERATION`.

## Ограничения

`testConnection` остаётся исключением из общего resolver path, потому что проверяет именно выбранный credential до его назначения на функцию. Рабочая генерация через этот метод невозможна.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Vacancy AI-фасад переведён на `AiExecutionService`; legacy providerCode оставлен только для совместимости |

# AiExecutionService

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`AiExecutionService` — единая middleware-точка выполнения AI-функций HRM HuntTech. Потребитель перестаёт выбирать provider, модель и API key: он передаёт только стабильный `functionCode` и бизнес-контекст.

### UI Context & Navigation

Сервис не имеет собственного экрана. Его конфигурация управляется через «Управление AI» → «Функции AI», «Корпоративные AI-подключения» и «Мои замещения AI-функций». Подключение существующих бизнес-экранов выполняется отдельными задачами; текущий этап их не изменяет.

### Behavior Summary

- `executeText(code, context)` → загружается активная функция → проверяется capability;
- `USER_REQUIRED` → требуется активный override текущего пользователя;
- `USER_OVERRIDE_ALLOWED` → при наличии валидного override вызывается personal provider;
- ошибка personal provider + `FALLBACK_TO_ADMIN` → выполняется corporate provider;
- override отсутствует / `ADMIN_ONLY` → используется corporate provider;
- secret корпоративного подключения → расшифровывается в core непосредственно перед `AIProvider.generateText`.

## API

```java
String executeText(String functionCode, Map<String, Object> context);
```

Prompt формируется через `TemplateHelper.processTemplate`. Provider выбирается существующим `AIProviderRegistry`; vendor-specific HTTP/auth остаётся внутри `AIProvider` implementations.

## Data View Integrity

- функция: `ai-function-execution-view`;
- override: `user-ai-function-override-execution-view`;
- персональный API key появляется только в core execution view;
- корпоративный ciphertext появляется только в core secret/execution view.

## Ошибки и логирование

Ошибки конфигурации преобразуются в `DevelopmentException`. При fallback логируется код функции и класс исключения, но не API key и не payload. Неподдержанная capability блокируется до внешнего вызова.

## Ограничения первого этапа

`executeText` поддерживает TEXT_GENERATION, TEXT_ANALYSIS, TEXT_TRANSFORMATION и DOCUMENT_ANALYSIS. `VISION`, `IMAGE_GENERATION`, `EMBEDDING`, `AUDIO_TRANSCRIPTION` присутствуют в модели, но требуют отдельных typed adapters.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Реализован централизованный function resolver с per-function override и admin fallback |

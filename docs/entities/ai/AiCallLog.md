# Сущность AiCallLog (Журнал вызовов AI)

## Назначение (Business Context)

`AiCallLog` (`hunttech_AiCallLog`, таблица `HUNTTECH_AI_CALL_LOG`) — централизованная сущность аудита и учета всех обращений к сервисам искусственного интеллекта в HRM HuntTech.

Каждый вызов к AI (успешный или завершившийся ошибкой) автоматически фиксируется в БД с сохранением контекста, пользователя, длительности, объема токенов, расчетной стоимости и текстов промпта/ответа.

---

## Структура полей сущности

| Поле | Тип | Описание |
| ---- | --- | -------- |
| `user` | `sec$User` (FK `USER_ID`) | Пользователь системы, инициировавший операцию |
| `userLogin` | `String(128)` | Логин пользователя (денормализация для быстрого поиска) |
| `userName` | `String(255)` | ФИО / имя пользователя |
| `callTime` | `Date` (timestamp) | Дата и время совершения вызова |
| `durationMs` | `Long` | Время выполнения запроса к AI API в миллисекундах |
| `functionCode` | `String(64)` | Код AI-функции (`FUNCTION_STANDARDIZE_VACANCY`, `FUNCTION_SKILL_ANALYSIS` и т.д.) |
| `functionName` | `String(255)` | Человекочитаемое наименование AI-функции |
| `capability` | `String(32)` | Тип возможности (`TEXT_GENERATION`, `TEXT_ANALYSIS`, `IMAGE_GENERATION`) |
| `providerCode` | `String(32)` | Провайдер API (`openai`, `deepseek`, `anthropic`, `gigachat`, `yandexgpt` и др.) |
| `modelName` | `String(128)` | Точное имя задействованной модели (`gpt-4o-mini`, `deepseek-chat`, `claude-3-5-sonnet` и т.д.) |
| `credentialOwner` | `String(32)` | Собственник API-ключа: `ADMIN` (корпоративный) или `USER` (личный) |
| `promptTokens` | `Integer` | Количество токенов во входном запросе (промпте) |
| `completionTokens` | `Integer` | Количество токенов в сгенерированном ответе модели |
| `totalTokens` | `Integer` | Суммарное число токенов (`promptTokens + completionTokens`) |
| `estimatedCost` | `BigDecimal(19, 6)` | Расчетная стоимость запроса в валюте провайдера |
| `currency` | `String(8)` | Валюта расчета стоимости (`USD`, `RUB`) |
| `promptText` | `String` (CLOB / `text`) | Полный текст отправленного в модель промпта (включая system prompt) |
| `responseText` | `String` (CLOB / `text`) | Полный текст ответа модели |
| `callerSource` | `String(255)` | Источник вызова (название сервиса, метод, экранная форма) |
| `status` | `String(32)` | Статус выполнения: `SUCCESS` или `ERROR` |
| `errorMessage` | `String` (CLOB / `text`) | Текст ошибки при сбое запроса к AI API |

---

## Индексы базы данных

- `IDX_HUNTTECH_AI_CALL_LOG_USER` — поиск по пользователю (`USER_ID`).
- `IDX_HUNTTECH_AI_CALL_LOG_CALL_TIME` — фильтрация по периодам дат (`CALL_TIME`).
- `IDX_HUNTTECH_AI_CALL_LOG_FUNCTION` — агрегация по функциям (`FUNCTION_CODE`).
- `IDX_HUNTTECH_AI_CALL_LOG_STATUS` — мониторинг ошибок (`STATUS`).

---

## Data Views (`ai-control-plane-views.xml`)

* `ai-call-log-browse-view`: лёгкий view для списков и таблиц без тяжелых CLOB-полей (`promptText`, `responseText`, `errorMessage`).
* `ai-call-log-edit-view`: полный view со всеми полями для экрана просмотра деталей вызова.

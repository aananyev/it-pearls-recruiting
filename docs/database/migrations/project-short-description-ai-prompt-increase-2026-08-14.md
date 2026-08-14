# Миграция увеличения AI-генерации «Кратко» ProjectEdit в 2 раза

## Назначение и бизнес-смысл (What & Why)

Миграция `260814-4-increaseProjectShortDescriptionPrompt` увеличивает объём AI-генерации краткого описания сути проекта (`PROJECT_SHORT_DESCRIPTION_GENERATE`) в 2 раза: промпт требует два предложения вместо одного, `MAX_TOKENS` поднимается с 125 до 250. Это продолжение цепочки редакций: исходный seed (260814-2, до 5 предложений, 500 токенов) → сокращение в 4 раза (260814-3, одно предложение, 125 токенов) → увеличение в 2 раза (260814-4, два предложения, 250 токенов) по указанию владельца.

Миграция изменяет только `SYSTEM_PROMPT`, `DESCRIPTION` и `MAX_TOKENS` функции. Она не выбирает provider/model/credential и не меняет AI routing. Административная настройка, изменённая администратором, не перезаписывается (контракт 260813-1).

## UI Context & Navigation

Фактически работающий пользовательский маршрут:

`ProjectBrowse → ProjectEdit → вкладка «Описание проекта» → кнопка «Кратко» → ProjectAiService.generateShortDescription → AiExecutionService.executeText("PROJECT_SHORT_DESCRIPTION_GENERATE")`.

Результат генерации выводится в sidebar-разделе «Коротко» (поле сущности `shortDescription`). При переполнении sidebar (длинный текст) включается вертикальная прокрутка.

## Behavior Summary

- запись от seed-миграции (v1) или результат 260814-3 (v2), не редактировалась администратором → миграция → промпт «не более 2 предложений», `MAX_TOKENS` 250, `CONFIGURATION_VERSION` 3;
- администратор уже кастомизировал промпт (версия > 2 или `UPDATED_BY` ≠ migration) → миграция → значения сохраняются без изменений;
- функция отсутствует → INSERT-fallback создаёт запись с новым промптом (защита `WHERE NOT EXISTS`);
- повторное применение прямого SQL → уже обновлённая запись не изменяется повторно.

## Артефакты

Liquibase:

`modules/core/db/changelog/260814-4-increaseProjectShortDescriptionPrompt.xml`

Локальный PostgreSQL-скрипт для ручной проверки:

`modules/core/db/update/postgres/26/260814-4-increaseProjectShortDescriptionPrompt.sql`

Контрактный тест:

`modules/core/test/com/company/hunttech/core/ProjectShortDescriptionAiContractTest.java` — метод `increasePromptMigrationDoublesGeneration`.

## Безопасность миграции

`UPDATE` ограничен `CODE = 'PROJECT_SHORT_DESCRIPTION_GENERATE'` и активной, не soft-deleted записью. Обновление разрешено только если промпт пустой/нерусский либо запись остаётся миграционной версии ≤ 2. В `UPDATE` отсутствуют `ADMIN_CONFIGURATION_ID`, `ADMIN_MODEL_NAME`, API-ключи, `EXECUTION_POLICY`, `FALLBACK_POLICY`. `DELETE`/`DROP`/`TRUNCATE` в миграции запрещены (проверяется тестом).

## Применение к локальной БД

Применяется штатным механизмом CUBA updateDb/Liquibase при локальном deploy. Контроль после применения:

```sql
SELECT code, max_tokens, configuration_version, updated_by
FROM hunttech_ai_function_configuration
WHERE code = 'PROJECT_SHORT_DESCRIPTION_GENERATE' AND delete_ts IS NULL;
```

Ожидается: `max_tokens` = 250, `configuration_version` ≥ 3, промпт содержит «не более 2 предложений».

## Production

**Production: НЕ ИЗМЕНЯЕТСЯ** без отдельной прямой команды владельца со словом `production`.

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-14 | Добавлена миграция увеличения AI-генерации «Кратко» в 2 раза (два предложения, `MAX_TOKENS` 250); админская кастомизация не перезаписывается |

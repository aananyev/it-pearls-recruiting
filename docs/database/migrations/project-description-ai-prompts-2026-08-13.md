# Локальная миграция русских административных prompt Project AI

## Назначение и бизнес-смысл (What & Why)

Миграция приводит административные `SYSTEM_PROMPT` и `PROMPT_TEMPLATE` функции `PROJECT_DESCRIPTION_GENERATE` к канонической русской редакции. Это нужно, чтобы локальная база HRM HuntTech содержала полноценную инструкцию для AI-обработки описания проекта, а не только технический function code.

Миграция не выбирает provider, model или credential и не изменяет AI routing. Уже настроенный администратором русский prompt не перезаписывается.

## UI Context & Navigation

Фактически работающий пользовательский маршрут на текущем `master`:

`ProjectBrowse → ProjectEdit → вкладка «Описание проекта» → «Загрузить описание»`.

`ProjectEdit` извлекает текст из PDF/DOCX/TXT и вызывает `ProjectAiService`, который использует стабильный код `PROJECT_DESCRIPTION_GENERATE`. Административные prompt этой функции редактируются в `Управление AI → Функции AI`.

Важно: на текущем `master` сам `ProjectBrowse` не выполняет AI-преобразование изображения/логотипа проекта. `ProjectBrowse.java` только отображает `projectLogo`. `AiExecutionService` сейчас выполняет текстовые capability и не исполняет `VISION`/`IMAGE_GENERATION`. Поэтому эта миграция намеренно не создаёт фиктивную image/vision-функцию, которую runtime ещё не способен вызвать.

## Behavior Summary

- функция отсутствует → миграция → создаётся `PROJECT_DESCRIPTION_GENERATE` с русскими prompt и безопасными исходными policy;
- функция создана прежней seed-миграцией и не редактировалась → миграция → prompt обновляются до канонической русской редакции;
- prompt пустой или не содержит кириллицы → миграция → prompt заменяются русской редакцией;
- администратор уже настроил собственный русский prompt → миграция → значения сохраняются без изменений;
- migration обновляет prompt → `ADMIN_CONFIGURATION_ID`, `ADMIN_MODEL_NAME`, credentials, execution/fallback policy не меняются;
- повторное применение прямого SQL → уже обновлённая запись не изменяется повторно.

## 1. Таблица и функция

Таблица:

`HUNTTECH_AI_FUNCTION_CONFIGURATION`

Стабильный код:

`PROJECT_DESCRIPTION_GENERATE`

Контекстные переменные prompt template:

- `${projectName}` — название проекта;
- `${sourceFileName}` — имя исходного файла;
- `${sourceText}` — извлечённый исходный текст.

## 2. Канонический system prompt

Русская инструкция требует:

- использовать только факты из исходника;
- не придумывать технологии, сроки, роли, заказчиков, географию или требования;
- сохранять названия технологий, продуктов, систем и организаций;
- удалять повторы, рекламные формулировки и служебный мусор;
- не анализировать чувствительные характеристики людей;
- возвращать результат на русском языке;
- возвращать только готовый текст без Markdown/HTML и пояснений.

## 3. Канонический prompt template

Шаблон передаёт название проекта, имя источника и исходный текст, а также задаёт семь явных требований к результату: назначение и предметная область, технологии/платформы/интеграции, роли и масштаб при наличии в исходнике, очистка мусора, запрет домыслов, русский нейтральный деловой стиль и отсутствие служебного оформления ответа.

## 4. Артефакты

Liquibase:

`modules/core/db/changelog/260813-1-localizeProjectDescriptionAiPrompts.xml`

Локальный PostgreSQL-скрипт для диагностики/ручной проверки:

`modules/core/db/update/postgres/26/260813-1-localizeProjectDescriptionAiPrompts.sql`

Контрактный тест:

`modules/core/test/com/company/hunttech/core/ProjectDescriptionAiPromptMigrationTest.java`

## 5. Безопасность миграции

`UPDATE` ограничен `CODE = 'PROJECT_DESCRIPTION_GENERATE'` и активной, не soft-deleted записью. Обновление разрешено только если prompt пустой/нерусский либо запись остаётся исходным migration-seed версии 1.

В `UPDATE` отсутствуют:

- `ADMIN_CONFIGURATION_ID`;
- `ADMIN_MODEL_NAME`;
- API keys / ciphertext;
- `EXECUTION_POLICY`;
- `FALLBACK_POLICY`.

`INSERT` выполняется только при полном отсутствии строки с таким `CODE`, включая soft-deleted строку, чтобы не нарушить unique constraint.

## 6. Применение Hermes к локальной БД

Hermes применяет миграцию только к локальной БД штатным механизмом CUBA updateDb/Liquibase при локальном deploy. Production в этой задаче не изменяется.

До применения:

```sql
SELECT id, code, system_prompt, prompt_template,
       configuration_version, created_by, updated_by
FROM hunttech_ai_function_configuration
WHERE code = 'PROJECT_DESCRIPTION_GENERATE';
```

После применения:

```sql
SELECT code, capability, system_prompt, prompt_template,
       execution_policy, fallback_policy,
       configuration_version, updated_by
FROM hunttech_ai_function_configuration
WHERE code = 'PROJECT_DESCRIPTION_GENERATE';
```

Ожидается ровно одна строка. Для seed/пустой/нерусской конфигурации оба prompt должны быть на русском языке, содержать `${projectName}`, `${sourceFileName}`, `${sourceText}`, а `configuration_version` — не менее 2.

## 7. Runtime smoke

После локального deploy:

1. Открыть `ProjectBrowse`.
2. Открыть существующий Project.
3. Перейти на «Описание проекта».
4. Загрузить TXT, затем при возможности DOCX/PDF.
5. При настроенном credential AI-результат должен формироваться по новой русской инструкции.
6. При недоступном AI исходный извлечённый текст должен остаться fallback-значением.
7. Открыть `Управление AI → Функции AI → PROJECT_DESCRIPTION_GENERATE` и визуально подтвердить русские prompt.

## 8. Ограничение image/vision

Эта миграция не реализует распознавание или преобразование изображения. Для будущего AI-преобразования `projectLogo`/другого изображения потребуется отдельная функциональная задача: стабильный function code, поддержка `VISION` или `IMAGE_GENERATION` в execution layer, передача бинарного/vision-контекста и профильные тесты. До появления такого runtime seed для image-функции создавать нельзя, иначе база будет содержать неисполняемую конфигурацию.

## 9. Production

**Production: НЕ ИЗМЕНЯЕТСЯ.**

Ни Liquibase, ни прямой SQL этой задачи не должны выполняться на production без отдельной прямой команды владельца со словом `production`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-13 | Добавлена безопасная локальная миграция канонических русских административных prompt `PROJECT_DESCRIPTION_GENERATE`; зафиксировано отсутствие image/vision runtime в текущем ProjectBrowse |

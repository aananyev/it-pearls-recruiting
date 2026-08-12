# Production migration: PROJECT_DESCRIPTION_GENERATE

## Назначение и бизнес-смысл (What & Why)

Миграция подготавливает production HRM HuntTech к upload-сценарию описания проекта. Она добавляет только конфигурацию AI-функции `PROJECT_DESCRIPTION_GENERATE`, чтобы ProjectEdit мог вызывать административно управляемый prompt через AI Control Plane.

## UI Context & Navigation

После применения администратор проверяет: `Управление AI → Функции AI → PROJECT_DESCRIPTION_GENERATE`. Пользовательский сценарий: `ProjectBrowse → ProjectEdit → Описание проекта → Загрузить описание`.

## Behavior Summary

- production уже содержит функцию с CODE → script выполняется → существующие prompt/model/policy не меняются;
- функции нет → script выполняется → создаётся одна базовая активная запись без credentials;
- таблицы AI Control Plane нет → preflight/script останавливается → никаких частичных изменений Project не выполняется;
- запись создана → администратор проверяет prompt и назначает допустимое corporate/personal routing → upload готов к runtime smoke.

## 1. Что изменяется

Таблица: `HUNTTECH_AI_FUNCTION_CONFIGURATION`.

Создаётся максимум одна запись:

```text
CODE = PROJECT_DESCRIPTION_GENERATE
CAPABILITY = DOCUMENT_ANALYSIS
EXECUTION_POLICY = USER_OVERRIDE_ALLOWED
FALLBACK_POLICY = FALLBACK_TO_ADMIN
TEMPERATURE = 0.2
MAX_TOKENS = 3000
ACTIVE = true
```

Не изменяются:

- `HUNTTECH_PROJECT`;
- `HUNTTECH_SKILL_TREE` и другие справочники;
- существующие проекты;
- корпоративные/пользовательские API keys;
- существующая запись `PROJECT_DESCRIPTION_GENERATE`, если администратор уже создал её вручную.

## 2. Артефакты

Liquibase:

`modules/core/db/changelog/260812-2-addProjectDescriptionAiFunction.xml`

Отдельный production-safe SQL:

`modules/core/db/update/postgres/26/260812-2-addProjectDescriptionAiFunction.sql`

Оба варианта INSERT-only/idempotent по `CODE`.

## 3. Preflight production

Production в рамках текущей задачи **не изменяется**. Перед будущим выполнением Hermes/DevOps должен работать только по отдельной прямой команде Алексея со словом `production`.

До выполнения:

```sql
SELECT to_regclass('public.hunttech_ai_function_configuration');
SELECT id, code, name, capability, execution_policy, fallback_policy, is_active
FROM hunttech_ai_function_configuration
WHERE code = 'PROJECT_DESCRIPTION_GENERATE';
```

Требование: AI Control Plane уже развернут. Рекомендуется backup или проверенная восстановимая копия БД по стандартной production-процедуре до применения любого DB change.

## 4. Выполнение в будущем

Один из штатных путей, выбранный DevOps в соответствии с deployment-процедурой:

```text
CUBA updateDb / Liquibase include 260812-2-addProjectDescriptionAiFunction.xml
```

или отдельно согласованный script:

```text
modules/core/db/update/postgres/26/260812-2-addProjectDescriptionAiFunction.sql
```

Не выполнять оба способа как независимые миграции без проверки `DATABASECHANGELOG`; SQL идемпотентен по CODE, но deployment history должен оставаться однозначным.

## 5. Post-migration admin steps

1. Открыть «Управление AI» → «Функции AI».
2. Найти `PROJECT_DESCRIPTION_GENERATE`.
3. Проверить/отредактировать `systemPrompt` и `promptTemplate`.
4. Выбрать corporate configuration, если функция должна иметь корпоративный fallback.
5. Проверить execution/fallback policy и model.
6. Не помещать credential в prompt, description или docs.

Template variables: `${projectName}`, `${sourceFileName}`, `${sourceText}`.

## 6. Проверка результата

```sql
SELECT code, capability, execution_policy, fallback_policy,
       allow_model_override, is_active, configuration_version
FROM hunttech_ai_function_configuration
WHERE code = 'PROJECT_DESCRIPTION_GENERATE';
```

Ожидается ровно одна строка.

Runtime smoke после deploy: TXT → DOCX → PDF; configured AI success; disabled/unconfigured AI fallback; сохранение и повторное открытие Project.

## 7. Rollback

Автоматический `DELETE` в rollback не закладывается: после эксплуатации запись может быть изменена администратором и стать рабочей конфигурацией. Если требуется откат, сначала отключить функцию (`IS_ACTIVE=false`) и подтвердить отсутствие зависимых вызовов. Физическое удаление допустимо только отдельным согласованным production-действием.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Подготовлен идемпотентный production-runbook для `PROJECT_DESCRIPTION_GENERATE`; production не изменён |

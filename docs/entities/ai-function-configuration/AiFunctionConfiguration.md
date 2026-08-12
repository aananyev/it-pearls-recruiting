# AiFunctionConfiguration

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`AiFunctionConfiguration` — центральная сущность AI Control Plane HRM HuntTech. Она переводит AI из набора отдельных provider-вызовов в каталог стабильных бизнес-функций: потребитель знает `code`, а prompt, модель, capability, корпоративное подключение и политика замещения управляются централизованно.

### UI Context & Navigation

Сущность администрируется через `hunttech_AiFunctionConfiguration.browse` → `hunttech_AiFunctionConfiguration.edit` в меню «Управление AI». Бизнес-экраны напрямую эту сущность не редактируют.

### Behavior Summary

- создание функции → задаются `code`, capability, prompt и routing policy → функция становится доступна execution layer после включения `active`;
- пользовательский вызов → передаётся `code` → `AiExecutionService` выбирает effective credential;
- `ADMIN_ONLY` → user override игнорируется → используется корпоративное подключение;
- изменение prompt/model → бизнес-экран не меняется → следующий вызов использует новую конфигурацию;
- отключение `active` → функция не находится resolver → внешний API не вызывается.

## Модель данных

Entity: `hunttech_AiFunctionConfiguration`  
Table: `HUNTTECH_AI_FUNCTION_CONFIGURATION`.

Ключевые поля: `code` (unique), `name`, `description`, `capability`, `systemPrompt`, `promptTemplate`, `temperature`, `maxTokens`, `adminConfiguration`, `adminModelName`, `executionPolicy`, `fallbackPolicy`, `allowModelOverride`, `active`, `configurationVersion`.

`systemPrompt` и `promptTemplate` — LOB и исключены из browse-view; они загружаются в edit/execution views. `adminConfiguration` ссылается на `AdminAiConfiguration`.

## Инварианты

- `code` уникален и после создания не редактируется UI;
- `executionPolicy` и `fallbackPolicy` обязательны;
- provider/API key не хранятся непосредственно в функции;
- capability отделена от provider, поэтому функция не зависит от vendor;
- текущее `AiExecutionService.executeText` поддерживает текстовые capability; неподдержанные capability возвращают контролируемую ошибку до вызова API.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Создана сущность AI-функции, views и routing contract |

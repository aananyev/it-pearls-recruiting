# AdminAiConfigurationBrowse

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Browse позволяет администратору управлять корпоративными AI-подключениями и видеть health status, не загружая API credential в Web Client.

### UI Context & Navigation

Открывается из «Управление AI» → «Корпоративные AI-подключения». Edit ведёт в защищённый editor.

### Behavior Summary

- открытие → safe browse-view → API_KEY_ENCRYPTED отсутствует;
- select → активируется «Проверить подключение»;
- test → UI передаёт UUID middleware → core делает provider request → safe loader reload;
- create/edit/remove → стандартные CUBA actions.

## 1. Invocation & Context

`hunttech_AdminAiConfiguration.browse`, `StandardLookup<AdminAiConfiguration>`, lookup component `adminConfigurationsTable`.

## 2. Data & Entity Binding

`adminConfigurationsDc`, `admin-ai-configuration-browse-view`, `adminConfigurationsDl`. Filter явно исключает `apiKeyEncrypted`.

## 3. Form Hierarchy

Parent: AI menu. Child: `hunttech_AdminAiConfiguration.edit`.

## 4. Behavior Model

Test выполняется `AiCredentialService.testAdminConnection(UUID)`; Web Client не выполняет reload secret-view.

## 5. Actions & Buttons Logic

CRUD + «Проверить подключение»; test button disabled без selection.

## 6. Visual Layout Schema

Filter → toolbar → DataGrid safe columns. Root `admin-ai-configuration-browse`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Создан безопасный Browse корпоративных AI-подключений |

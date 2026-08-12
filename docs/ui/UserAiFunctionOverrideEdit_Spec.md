# UserAiFunctionOverrideEdit

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Edit связывает одну разрешённую AI-функцию с одним собственным активным `UserAiConfiguration`, не раскрывая API key и не изменяя корпоративную конфигурацию.

### UI Context & Navigation

Открывается из персонального override browse. Sidebar: «AI-функция / Моё подключение».

### Behavior Summary

- новая запись → user=current session, enabled=true;
- options функций → только active и не `ADMIN_ONLY`;
- options credentials → только active текущего пользователя, safe view без key;
- before commit → ownership/policy/active guards → невалидный commit блокируется;
- model override → editable только если функция разрешает `allowModelOverride`.

## 1. Invocation & Context

`hunttech_UserAiFunctionOverride.edit`; `StandardEditor<UserAiFunctionOverride>`.

## 2. Data & Entity Binding

`overrideDc` view `user-ai-function-override-edit-view`; `functionsDc` safe function browse-view; `userConfigurationsDc` safe `user-ai-configuration-override-picker-view` с обязательным `:user`.

## 3. Form Hierarchy

Parent personal override browse. Relations: AiFunctionConfiguration, UserAiConfiguration текущего пользователя.

## 4. Behavior Model

`BeforeShow` устанавливает `:user` до automatic load. `BeforeCommitChanges` повторно валидирует policy и ownership независимо от options filtering.

## 5. Actions & Buttons Logic

Label navigation только focus/active-state; standard commit/close footer.

## 6. Visual Layout Schema

Shared Edit: 312px sidebar → workspace → two `edit-card` → footer. Root `user-ai-function-override-editor`.

## Data View Integrity

Контроллер не читает `apiKey`; safe picker view не содержит secret. Execution view с key используется только core resolver.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Создана Edit-форма персонального per-function override по общему Edit-контракту |

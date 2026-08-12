# UserAiFunctionOverrideBrowse

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Browse показывает только персональные per-function замещения текущего пользователя и позволяет вернуть функцию к корпоративной маршрутизации удалением/отключением override.

### UI Context & Navigation

Открывается из «Управление AI» → «Мои замещения AI-функций».

### Behavior Summary

- BeforeShow → устанавливается `:user` → loader выполняется вручную;
- таблица → показывает function/provider/model без API key;
- create/edit/remove → стандартные actions → изменяются только строки текущего пользователя.

## 1. Invocation & Context

`hunttech_UserAiFunctionOverride.browse`, `StandardLookup<UserAiFunctionOverride>`.

## 2. Data & Entity Binding

`overridesDc`, safe view `user-ai-function-override-browse-view`; JPQL `where e.user = :user`.

## 3. Form Hierarchy

Parent AI menu; child override edit.

## 4. Behavior Model

Нет `@LoadDataBeforeShow`: параметр текущего пользователя устанавливается до `load()`, что исключает ошибку отсутствующего query parameter и загрузку чужих строк.

## 5. Actions & Buttons Logic

Create/Edit/Remove + hidden lookup actions.

## 6. Visual Layout Schema

Toolbar → DataGrid; root `user-ai-function-override-browse`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Создан current-user scoped Browse персональных override |

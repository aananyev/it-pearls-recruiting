# UserAiFunctionOverride

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`UserAiFunctionOverride` позволяет пользователю заменить корпоративное подключение собственным API только для конкретной разрешённой AI-функции. Это предотвращает глобальную замену всех AI-сценариев одним личным ключом.

### UI Context & Navigation

Пользователь управляет своими записями через `hunttech_UserAiFunctionOverride.browse/edit`. Options loaders показывают только активные функции, не имеющие `ADMIN_ONLY`, и только активные `UserAiConfiguration` текущего пользователя.

### Behavior Summary

- создать override → выбрать функцию и собственное подключение → resolver начинает использовать personal API для этой функции;
- выключить `enabled` → override хранится, но resolver его не применяет;
- функция `ADMIN_ONLY` → сохранить override нельзя;
- `allowModelOverride=false` → модель персонально не задаётся;
- удалить override → функция возвращается к корпоративной маршрутизации согласно policy.

## Модель данных

Entity: `hunttech_UserAiFunctionOverride`  
Table: `HUNTTECH_USER_AI_FUNCTION_OVERRIDE`.

Поля: `user`, `aiFunction`, `userAiConfiguration`, `modelName`, `enabled`.

Уникальный DB-контракт: `(USER_ID, AI_FUNCTION_ID)`. Контроллер Edit дополнительно проверяет ownership текущего пользователя, активность credential и policy функции до commit.

## Data View Integrity

Browse/Edit views используют `user-ai-configuration-override-picker-view`, который не содержит `apiKey`. Core execution view добавляет `apiKey` только для middleware resolver.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Создано per-function пользовательское замещение с ownership/policy guards |

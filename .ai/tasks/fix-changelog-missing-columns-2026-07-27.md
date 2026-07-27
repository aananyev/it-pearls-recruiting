# Task: Проверить и доработать changelog 260727-1-reconcileProductionSchema.xml

## Проблема

Liquibase changelog `260727-1-reconcileProductionSchema.xml` при ручном применении через `updateDb` столкнулся с тем, что Gradle-таск `updateDb` запускает ВСЕ миграции, включая старые SQL-скрипты из `modules/core/db/update/`. Один из них (`19/191022-1-updateCountry.sql`) падает потому что пытается DROP INDEX, которого нет.

Поэтому changelog пришлось применять вручную через psql. При ручном создании таблицы `HUNTTECH_USER_AI_PROFILE` были пропущены ~20 колонок, т.к. ручной скрипт был короче, чем полное определение в changelog.

## Текущее состояние

- Таблицы и колонки созданы (я добавил недостающие вручную)
- `sys_db_changelog` НЕ содержит записи о `260727-1-reconcileProductionSchema.xml` (changelog не применялся через Liquibase)
- При повторном деплое на новую БД проблема проявится снова

## Что нужно сделать

1. **Проверить** что changelog `260727-1-reconcileProductionSchema.xml` содержит ВСЕ колонки из entity `UserAiProfile.java`
2. **Исправить** `updateDb` — либо:
   a. Отключить выполнение старых SQL-скриптов из `db/update/` (они уже применены на prod)
   b. Либо добавить preConditions/MARK_RAN на каждый старый скрипт
3. **Зарегистрировать** changelog в `sys_db_changelog` чтобы повторное выполнение не создавало дубликатов (опционально — можно при следующем deploy через `updateDb`)

## Файлы

- `modules/core/db/changelog/260727-1-reconcileProductionSchema.xml`
- `modules/global/src/com/company/hunttech/entity/UserAiProfile.java`

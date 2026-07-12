# Migration plan

Дата: 2026-07-10

## Вердикт этапа

`МИГРАЦИЮ НАЧИНАТЬ НЕЛЬЗЯ`

Этот документ описывает безопасную целевую стратегию. Он не является разрешением на запуск миграции.

## Предпочтительная архитектура

1. Старую production-базу не изменять.
2. Создать отдельную новую production/staging-базу для `hunttech`.
3. Восстановить или создать структуру новой базы из текущей версии приложения.
4. Мигрировать данные из копии старой базы или через read-only подключение к старой базе.
5. Проверить новую базу независимо.
6. Переключить приложение только после успешных проверок.
7. Сохранить старую базу без изменений до окончания периода наблюдения.

Эта архитектура применима, если на сервере можно создать новую базу и отдельное хранилище backup. Если создание новой базы невозможно, миграцию нужно остановить и отдельно согласовать альтернативу, потому что in-place миграция резко ухудшает rollback.

## Фазы

### Фаза 0. Запрет изменений

- Не выполнять DDL/DML на production.
- Не запускать `updateDb` на production.
- Не запускать существующие migration scripts.
- Не запускать FDW/dblink migration scripts на production.
- Не останавливать HRM.

### Фаза 1. Read-only production audit

Оператор должен выполнить read-only SQL-скрипты:

- `audit/sql/00-instance-architecture-readonly.sql`
- `audit/sql/01-schema-inventory-readonly.sql`
- `audit/sql/02-security-model-readonly.sql`

Вывод сохранить вне Git, например:

- `/secure/audit-output/hr-prod-00-instance-architecture-YYYYMMDD-HHMMSS.txt`
- `/secure/audit-output/hr-prod-01-schema-inventory-YYYYMMDD-HHMMSS.txt`
- `/secure/audit-output/hr-prod-02-security-model-YYYYMMDD-HHMMSS.txt`

### Фаза 2. Backup verification

До миграции обязательно:

- `pg_dump -Fc` production DB;
- `pg_dumpall --globals-only`;
- checksum всех backup-файлов;
- тестовое восстановление backup в отдельную базу;
- проверка, что приложение может читать восстановленную копию;
- сверка количества строк и ключевых FK chains.

### Фаза 3. Target database preparation

Только после отдельного задания:

- создать новую базу `hunttech` или согласованное production-имя;
- назначить owner и grants;
- применить структуру текущей версии приложения;
- зафиксировать `SYS_DB_CHANGELOG`;
- проверить, что новые таблицы пустые, кроме системных seed-данных.

### Фаза 4. Data mapping

Для каждой таблицы нужен explicit mapping:

- source table;
- target table;
- source columns;
- target columns;
- transform rule;
- default rule;
- handling of missing target column;
- handling of missing source column;
- FK validation rule;
- row count validation.

Переименования `itpearls_* -> hunttech_*` нельзя считать автоматическим правилом без подтверждения.

### Фаза 5. Test migration

Выполняется не на production source:

- либо из restored copy;
- либо из production read-only source в отдельную target DB.

Критерии успеха:

- совпали row counts по всем таблицам с approved mapping;
- нет orphan FK;
- sequence values выставлены выше `max(id)` там, где применимо;
- `sec_*` сохранены;
- `sys_file` и file storage согласованы;
- приложение запускается на новой базе;
- критичные сценарии рекрутеров работают.

### Фаза 6. Cutover

Разрешается только после отдельного задания и утверждения:

- maintenance mode;
- остановка записи в старую базу;
- финальный backup;
- финальная миграция;
- validation;
- переключение datasource;
- запуск приложения;
- smoke tests;
- начало периода наблюдения.

## Необходимое окно недоступности

Окно нельзя точно оценить без production row counts и размера backup.

Предварительно нужно разделить:

- окно read-only backup;
- окно freeze writes;
- окно финальной миграции;
- окно validation;
- окно переключения приложения;
- окно smoke tests.

До получения production sizes и тестового времени восстановления миграционное окно не утверждать.

## Backup strategy

Минимальный набор:

- `pg_dump -Fc` для application database;
- `pg_dumpall --globals-only` для ролей, membership, tablespaces definitions;
- checksum `sha256sum` или `shasum -a 256`;
- журнал команд;
- backup file names с timestamp;
- хранение вне каталога PostgreSQL data directory;
- запрет перезаписи предыдущих backup;
- тестовый restore.

Рекомендуется дополнительно:

- VM/disk snapshot перед cutover;
- WAL archiving/PITR, если допустима настройка заранее;
- filesystem-level backup только как дополнение, не как единственный backup.

## Validation

Минимальные проверки:

- databases, schemas, tablespaces;
- object counts;
- table row counts;
- column-level diff;
- FK orphan checks;
- CUBA `SYS_DB_CHANGELOG`;
- users and roles;
- `sec_user` active/blocked users;
- `sec_user_role`, `sec_permission`, `sec_constraint`;
- `sys_file` and file storage references;
- sequences;
- indexes;
- extension list;
- application login;
- search/browse/edit candidate;
- open position workflow;
- recruiter tasks;
- email-related screens without exposing passwords.

## Что не делать

- Не использовать `--clean` restore на production DB.
- Не запускать `updateDb` на старой production DB.
- Не запускать FDW migration script на production без отдельного approved plan.
- Не удалять legacy tables без проверки строк и смысла данных.
- Не переносить secrets в отчеты.

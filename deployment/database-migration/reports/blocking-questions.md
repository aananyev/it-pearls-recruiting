# Blocking questions

Дата: 2026-07-10

Миграцию нельзя начинать без ответов на эти вопросы.

## Confirmed facts

1. Фактическое имя production database: `itpearls` - подтверждено.
2. Production tablespace `itpearls`: отсутствует - подтверждено.
3. Production tablespace `hunttech`: отсутствует - подтверждено.
4. Physical paths: только cluster default для `pg_default` и `pg_global` - подтверждено.
5. Owner production database: `cuba` - подтверждено.
6. PostgreSQL version: `11.22` - подтверждено.
7. Encoding/collation/ctype: `UTF8`, `ru_RU.UTF-8`, `ru_RU.UTF-8` - подтверждено.
8. Schemas кроме `public`: не обнаружены.
9. Production datasource: Tomcat JNDI `jdbc/CubaDS` - подтверждено.
10. Production JDBC URL: `jdbc:postgresql://localhost/itpearls` - подтверждено.
11. Technical application user: `cuba` - подтверждено.
12. Production `local.app.properties`: найден внутри deployed Tomcat webapps - подтверждено.
13. Datasource переключается через Tomcat context/JNDI - подтверждено.
14. Backup storage вне data directory: `/var/backups/hunttech-hrm/20260710-100131` - подтверждено для созданного backup.

## Remaining access blockers

1. Нужна ли отдельная read-only роль для повторных production-аудитов, вместо операторского доступа?
2. Есть ли отдельная роль для production backup на постоянной основе?
3. Полный grants/default privileges snapshot должен быть повторно сохранен перед финальной миграцией.
4. Кто имеет право выполнять backup и restore в день cutover?

## Remaining application blockers

1. Есть ли утвержденный maintenance mode или процедура запрета записи пользователям?
2. Кто подтверждает smoke tests после будущего переключения?
3. Кто принимает решение о rollback?
4. Как именно будет отключен или проконтролирован `cuba.automaticDatabaseUpdate=true` перед будущим cutover?
5. Текущая локальная база `hunttech` отсутствует в проверенном локальном PostgreSQL instance; ее нельзя пересоздавать без отдельного approved preparation step.
6. После очистки диска локально доступно около `27 GiB`; нужно подтвердить, достаточно ли этого запаса для full test restore или подготовить более просторное окружение.
7. Нужно утвердить использование matching PostgreSQL client utilities `11.22` для restore либо явно принять запуск client utilities `14.20` против local server `11.22`.

## Data mapping

1. Нужно ли переносить legacy `*_link__u*` таблицы?
2. Что делать с `sec_remember_me` и активными сессиями?
3. Нужно ли переносить пользовательские SMTP/IMAP/POP3 настройки?
4. Нужно ли переносить AI API keys?
5. Как обрабатывать записи, созданные после cutover, если потребуется rollback?

## Backup and rollback

1. Где хранить production backups долгосрочно после `/var/backups/hunttech-hrm/20260710-100131`?
2. Какой retention period?
3. Есть ли VM/disk snapshots?
4. Есть ли WAL archiving/PITR?
5. Кто принимает решение об откате?
6. Какой максимальный период работы новой базы до irreversible decision?
7. Где выполнить повторный full test restore с минимум 20 GB свободного места?
8. Можно ли удалить частично восстановленную локальную базу `hunttech_prod_restore_test_20260710_1105` после review?
9. Можно ли копировать production dump в локальное окружение для test restore, если файл сейчас отсутствует в `/tmp`?

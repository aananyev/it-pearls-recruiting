# Architecture audit

Дата: 2026-07-10

## Область аудита

Исследованы:

- локальный проект `/Users/alekseyananyev/StudioProjects/hunttech_recruiting`;
- локальная PostgreSQL-инфраструктура на `127.0.0.1:5432`;
- доступность production PostgreSQL-порта `hr.hunttech.ru:5432`;
- CUBA/Liquibase/SQL/deployment-артефакты проекта.

Production-база не изменялась. На production выполнена только проверка доступности порта PostgreSQL через `pg_isready`.

## Проект и платформа

- Тип приложения: CUBA Platform, не Jmix.
- Версия CUBA: `7.3-SNAPSHOT`.
- Gradle-модули: `app-global`, `app-core`, `app-web`, `app-gui`, `app-web-toolkit`.
- Web context: `hrm`.
- Core context: `hrm-core`.
- Основной Java package: `com.company.hunttech`.
- CUBA namespace: `hunttech`.
- Persistence unit: `hunttech_recruiting`.
- JDBC driver: `org.postgresql:postgresql:42.2.9`.

## Конфигурация подключения

Локальная конфигурация `modules/core/src/app.properties`:

- JDBC URL: `jdbc:postgresql://localhost:5432/hunttech`
- username: `cuba`
- password: `***MASKED***`

Production/deploy конфиги:

- `hunttech.conf`
  - `DB_SERVER=hr.hunttech.ru`
  - `DB_NAME=HuntTech`
  - `DB_USER=replica`
  - `DB_DUMP_USER=postgres`
  - `DB_UPDATE_USER=cuba`
  - `REMOTE_DB_PORT=5432`
- `deploy_shared.conf`
  - `SERVER_HOST=hr.hunttech.ru`
  - `DB_SERVER=hr.hunttech.ru`
  - `DB_NAME=HuntTech`
  - `DB_USER=postgres`
  - `REMOTE_DB_PORT=5432`

Важно: в проекте есть противоречие имен локальной и production БД:

- локальная CUBA-конфигурация использует `hunttech`;
- Docker Compose по умолчанию создает `HuntTech`;
- deploy-конфиги используют `HuntTech`;
- локально база `HuntTech` отсутствует;
- локально существуют `hunttech`, `hunttech_jmix`, `itpearls`.

## Локальный PostgreSQL

- psql client: `14.20`.
- local server: `PostgreSQL 11.22`.
- host: `127.0.0.1`.
- port: `5432`.

Локальные базы:

| База | Владелец | Encoding | Collation | Ctype | Tablespace |
|---|---|---|---|---|---|
| `hunttech` | `cuba` | UTF8 | `ru_RU.UTF-8` | `ru_RU.UTF-8` | `pg_default` |
| `itpearls` | `cuba` | UTF8 | `ru_RU.UTF-8` | `ru_RU.UTF-8` | `pg_default` |
| `hunttech_jmix` | `cuba` | UTF8 | `ru_RU.UTF-8` | `ru_RU.UTF-8` | `pg_default` |
| `cuba` | `cuba` | UTF8 | `ru_RU.UTF-8` | `ru_RU.UTF-8` | `pg_default` |
| `postgres` | `postgres` | UTF8 | `ru_RU.UTF-8` | `ru_RU.UTF-8` | `pg_default` |

Локальные tablespaces:

| Tablespace | Owner | Physical path |
|---|---|---|
| `pg_default` | `postgres` | cluster default |
| `pg_global` | `postgres` | cluster default |

Локальные схемы в `hunttech` и `itpearls`:

- `public`, owner `postgres`.

## Чем являются `itpearls` и `hunttech`

Локально установлено фактически:

- `itpearls` - имя PostgreSQL database.
- `hunttech` - имя PostgreSQL database.
- `hunttech` - также CUBA namespace и префикс entity/table names.
- `itpearls` и `hunttech` локально не являются tablespace.
- `itpearls` и `hunttech` локально не являются schema.

Production:

- в конфигурациях указана база `HuntTech`;
- пользовательская постановка говорит о production tablespace `itpearls`;
- это противоречие нельзя разрешить без read-only production-аудита через SQL-скрипт `00-instance-architecture-readonly.sql`.

## Расширения

Локальная `hunttech`:

- `plpgsql`
- `dblink`
- `postgres_fdw`

Локальная `itpearls`:

- `plpgsql`

Риск: `dblink` и `postgres_fdw` могут быть следами предыдущих попыток миграции. На production их нельзя создавать на текущем этапе.

## Локальные роли PostgreSQL

Обнаружены роли:

- `postgres`: superuser, createdb, createrole, replication, bypassrls, login.
- `cuba`: superuser, login.
- `alan`: superuser, login.
- `replica`: replication, login.
- `wp_user`: login.
- системные роли PostgreSQL: `pg_monitor`, `pg_read_all_settings`, `pg_read_all_stats`, `pg_stat_scan_tables`, `pg_signal_backend`, `pg_read_server_files`, `pg_write_server_files`, `pg_execute_server_program`.

Риск: локальная роль приложения `cuba` является superuser. Для production это должно быть запрещено или отдельно подтверждено как временное наследие.

## Liquibase и CUBA DB scripts

Обнаружены два механизма изменений:

- CUBA DB scripts: `modules/core/db/update/postgres/`.
- Liquibase changelog: `modules/core/db/changelog/db.changelog-master.xml`.

`db.changelog-master.xml` включает `260627-1-addAiEntities.xml`.

Последние CUBA update scripts в локальных `sys_db_changelog` применены до `26/260701-*` и performance indexes `26/260704-*` присутствуют в файловой системе.

## Production-аудит

Выполнено только:

- проверка доступности `hr.hunttech.ru:5432` через `pg_isready`;
- результат: порт принимает подключения.

Не выполнено:

- `SELECT version()` на production;
- список production databases;
- список production tablespaces;
- production roles/grants;
- production schema inventory;
- production security inventory;
- production schema diff.

Причина: нет подтвержденных учетных данных и нельзя рисковать подключением с ролью, которая может иметь права изменения.

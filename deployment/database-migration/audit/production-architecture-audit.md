# Production architecture audit

Дата: 2026-07-10

## Production application datasource

Фактический datasource production HRM найден в Tomcat context:

- systemd service: `tomcat9`
- service user: `tomcat`
- app home: `/opt/app_home`
- WAR directory: `/var/lib/tomcat9/webapps`
- core app: `/var/lib/tomcat9/webapps/app-core`
- web app: `/var/lib/tomcat9/webapps/app`
- JNDI resource: `jdbc/CubaDS`
- JDBC URL: `jdbc:postgresql://localhost/itpearls`
- technical application user: `cuba`
- password: `***REDACTED***`
- `cuba.dataSourceProvider=jndi`
- `cuba.automaticDatabaseUpdate=true`

Важно: `cuba.automaticDatabaseUpdate=true` на production является отдельным риском перед любым будущим переключением или запуском новой версии.

## PostgreSQL

- PostgreSQL server: `PostgreSQL 11.22 (Ubuntu 11.22-1.pgdg20.04+1)`
- `psql`: `11.22`
- `pg_dump`: `11.22`
- `pg_restore`: `11.22`
- `pg_dumpall`: `11.22`
- production database: `itpearls`
- database OID: `111100`
- database owner: `cuba`
- encoding: `UTF8`
- collation: `ru_RU.UTF-8`
- ctype: `ru_RU.UTF-8`
- database size: `6342 MB`
- default database tablespace: `pg_default`
- schema: `public`
- schema owner: `postgres`
- search path: `"$user", public`

## Names `itpearls`, `hunttech`, `HuntTech`

| Name | Database | Tablespace | Schema |
|---|---:|---:|---:|
| `itpearls` | yes | no | no |
| `hunttech` | no | no | no |
| `HuntTech` | no | no | no |

Вывод: исходная формулировка про production tablespace `itpearls` фактически ошибочна. На production `itpearls` является именем PostgreSQL database и префиксом бизнес-таблиц, но не tablespace.

## Tablespaces

| Tablespace | Owner | Physical path |
|---|---|---|
| `pg_default` | `postgres` | cluster default |
| `pg_global` | `postgres` | cluster default |

Все проверенные объекты production-базы используют `pg_default`.

## Extensions and special features

- extensions: `plpgsql 1.0`
- large objects: `0`
- prepared transactions: `0`
- FDW wrappers: none
- foreign servers: none
- logical publications: none
- logical subscriptions: none
- replication slots: none reported by audit
- RLS policies: none reported by audit

## Active connections during audit

Во время read-only аудита:

- `itpearls / cuba / idle`: 2 sessions
- `itpearls / postgres / active`: 1 audit session

Долгих транзакций старше 5 минут аудит не зафиксировал.

## Disk check before backup

На сервере перед backup:

- filesystem: `/dev/vda2`
- size: `99G`
- used: `36G`
- available: `58G`
- use: `39%`

Этого было достаточно для custom dump `792M`.

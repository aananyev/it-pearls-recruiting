# Prompt 1 strict step status

Дата: 2026-07-10

## Режим работы

Активирован строгий режим первого промпта.

Разрешены только:

- исследование;
- read-only аудит;
- анализ уже собранных материалов;
- подготовка документации;
- подготовка безопасных read-only скриптов.

Запрещены:

- создание базы `hunttech` на production;
- создание tablespace `hunttech`;
- запуск миграции;
- запуск Liquibase/updateDb;
- изменение datasource;
- DDL/DML на production;
- автоматическое исправление найденных проблем;
- удаление backup;
- удаление частично восстановленной локальной базы без отдельного подтверждения.

## Что уже подтверждено

### Production architecture

- production host: `hr.hunttech.ru`;
- production database: `itpearls`;
- production PostgreSQL: `11.22`;
- production schema: `public`;
- production database owner: `cuba`;
- application technical user: `cuba`;
- datasource: Tomcat JNDI `jdbc/CubaDS`;
- JDBC URL: `jdbc:postgresql://localhost/itpearls`;
- password values are redacted and were not saved in Git;
- `cuba.automaticDatabaseUpdate=true` is present on production and is a future cutover risk.

### Meaning of `itpearls` and `hunttech`

On production:

| Name | Database | Tablespace | Schema |
|---|---:|---:|---:|
| `itpearls` | yes | no | no |
| `hunttech` | no | no | no |
| `HuntTech` | no | no | no |

Conclusion: the original wording about production tablespace `itpearls` is factually incorrect. On production `itpearls` is a PostgreSQL database name and business table prefix, not a tablespace.

### Production storage

- tablespaces: `pg_default`, `pg_global`;
- all audited production objects use `pg_default`;
- no production tablespace named `itpearls`;
- no production tablespace named `hunttech`;
- no non-default physical tablespace path was found.

### Production data and security inventory

- tables: 154;
- sequences: 2;
- views: 0;
- materialized views: 0;
- large objects: 0;
- database size: `6342 MB`;
- `sec_user`: 89;
- active users: 15;
- inactive or blocked users: 71;
- `sec_role`: 12;
- `sec_user_role`: 178;
- `sec_permission`: 3980;
- `sys_file`: 13458;
- `sys_db_changelog`: 995.

### Schema comparison already documented

Production `itpearls` matches local `itpearls` at audited column-metadata level.

Production differs from the previously audited local `hunttech` structure only in:

- two empty legacy link tables absent in `hunttech`;
- default `temperature=0.7` for `vacancy_prompt_template.temperature`.

## Current local-state note

The user reported that local database `hunttech` was deleted.

A local read-only verification attempt using the default local operating-system role failed with:

`FATAL: role "alekseyananyev" does not exist`

Then read-only checks with local PostgreSQL roles `postgres` and `cuba` succeeded and returned only:

- `itpearls`

Therefore local `hunttech` and local `HuntTech` are currently absent in the checked local PostgreSQL instance.

These checks did not modify any database.

After disk cleanup, the local PostgreSQL check returned:

- local PostgreSQL server: `11.22`;
- existing checked databases: `itpearls`;
- absent checked databases: `hunttech`, `HuntTech`, `hunttech_prod_restore_test_20260710_1105`;
- current local free space on `/System/Volumes/Data`: about `27 GiB`;
- production dump was not found under `/tmp` at the time of this check.

Local client tools currently found in the project shell:

- `psql`: `14.20`;
- `pg_restore`: `14.20`;
- `pg_dump`: `14.20`.

This differs from production PostgreSQL `11.22` and local server `11.22`, so the restore runbook must either use matching `11.22` client utilities or explicitly record that `14.20` client tools were used against an `11.22` server. For production backups already created on the server, production utilities `11.22` were used.

`27 GiB` is better than the previous local state, but still should be treated as a tight restore margin for a 6.3 GB production database with indexes.

## Backup status

Production backup was previously created and verified at file/list/checksum level:

- dump: `/var/backups/hunttech-hrm/20260710-100131/itpearls_20260710-100131.dump`;
- size: `829701541 bytes / 792 MB`;
- SHA-256: `43e78190d9e9a176f38d6d44384ff0a697d6742073afa355dd7c1a3f9ea1aa1f`;
- `pg_restore --list`: exit code 0, 1093 entries.

Full test restore is not successful yet. The prior local restore stopped because the local environment ran out of disk space.

## Current blockers before any migration design

1. A full test restore must be completed in an isolated environment with enough free disk space.
2. Target `hunttech` database must be recreated only after a separate approved preparation step.
3. It must be decided whether to preserve default `temperature=0.7` in the target model.
4. It must be decided whether the two empty legacy link tables are excluded from target migration.
5. Future cutover must explicitly control `cuba.automaticDatabaseUpdate=true`.
6. Rollback policy for user writes after cutover must be approved.
7. Long-term backup storage and retention must be approved.

## Safe next step

Do not create or migrate `hunttech` yet.

The next safe step is to complete Prompt 1 documentation hygiene:

1. update blocking questions to separate resolved facts from remaining blockers;
2. update risk register statuses for facts now confirmed;
3. explicitly approve copying or using the production dump in the local restore environment;
4. choose matching PostgreSQL client utilities for restore, preferably `11.22`;
5. repeat full test restore and validation before designing migration scripts.

## Verdict

`МИГРАЦИЮ НАЧИНАТЬ НЕЛЬЗЯ`

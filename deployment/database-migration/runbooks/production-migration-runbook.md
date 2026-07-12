# Production migration runbook

Дата: 2026-07-10

## Current status

Production migration is not approved.

## Hard gate

Do not start production migration until:

- full test restore succeeds without FK errors;
- all orphan records have an approved rule;
- test migration succeeds on an isolated copy;
- application smoke test succeeds;
- rollback simulation succeeds.
- no-cross-database-impact controls are implemented and reviewed.

## Safety requirement: other databases on `hr.hunttech.ru`

The production PostgreSQL server may host databases unrelated to HRM HuntTech. They are out of scope and must be treated as protected assets.

Production migration must not alter, drop, lock for a long time, overwrite, restore into, reassign ownership for, or otherwise affect any database except the explicitly approved HRM source and target:

- source database: `itpearls`;
- target database: `hunttech`;
- only explicitly approved temporary restore/test databases, if any.

Forbidden without a separate written approval:

- wildcard operations across all databases;
- `DROP DATABASE` except for an explicitly named disposable test database;
- restoring `pg_dumpall --globals-only` into the production cluster;
- `CREATE ROLE`, `ALTER ROLE`, `DROP ROLE`, `GRANT`, `REVOKE` for shared roles;
- `CREATE TABLESPACE`, `DROP TABLESPACE`, or tablespace reassignment;
- PostgreSQL cluster restart, `postgresql.conf` changes, or `pg_hba.conf` changes.

Every production script that can modify data or structure must preflight and stop unless all values match the approved plan:

- hostname is `hr.hunttech.ru`;
- port is the approved PostgreSQL port;
- `current_database()` is exactly the expected database for the current step;
- source database is exactly `itpearls`;
- target database is exactly `hunttech`;
- PostgreSQL user is the approved operator/technical user;
- production confirmation phrase is provided;
- no unapproved database name appears in the generated command plan.

## Intended production sequence

1. Announce maintenance window.
2. Stop user writes or enable maintenance mode.
3. Create final production backup.
4. Verify backup checksum and `pg_restore --list`.
5. Create separate target database/tablespace using parameterized DDL.
6. Restore or create target structure.
7. Run approved migration scripts with production confirmation variables.
8. Run post-migration validation.
9. Run application smoke test.
10. Switch datasource only after validation approval.
11. Keep old `itpearls` database unchanged for rollback period.

## Explicit production confirmation

Production scripts must require:

- expected hostname;
- expected source database;
- expected target database;
- `CONFIRM_PRODUCTION_MIGRATION`;
- migration ID;
- manual confirmation phrase.

## Current verdict

`ПРОМЫШЛЕННУЮ МИГРАЦИЮ ПРОВОДИТЬ НЕЛЬЗЯ`

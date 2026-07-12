# Test restore runbook

Дата: 2026-07-10

## Current status

The first local test restore failed because local disk had only about 1.7 GiB free.

## Required environment

Use a clean PostgreSQL 11-compatible restore environment with at least 20 GB free disk, preferably more.

Do not restore over:

- production database;
- local `hunttech`;
- local `itpearls`;
- any database with valuable data.

## Database name

Use unique name:

`hunttech_prod_restore_test_YYYYMMDD_HHMMSS`

## Restore command

```bash
RESTORE_DB=hunttech_prod_restore_test_YYYYMMDD_HHMMSS
createdb -h <restore_host> -p <restore_port> -U <restore_admin> "${RESTORE_DB}"

pg_restore \
  -h <restore_host> \
  -p <restore_port> \
  -U <restore_admin> \
  -d "${RESTORE_DB}" \
  --no-owner \
  --no-privileges \
  --verbose \
  /secure/backups/itpearls_YYYYMMDD-HHMMSS.dump \
  > /secure/restore-logs/pg_restore_${RESTORE_DB}.log 2>&1
```

## Validation

After successful restore:

```bash
psql -d "${RESTORE_DB}" -f deployment/database-migration/validation/validate-restored-database.sql
psql -d "${RESTORE_DB}" -f deployment/database-migration/validation/validate-security-data.sql
psql -d "${RESTORE_DB}" -f deployment/database-migration/validation/validate-foreign-keys.sql
psql -d "${RESTORE_DB}" -f deployment/database-migration/validation/validate-sequences.sql
```

## Failure handling

Any restore error is blocking until classified.

Do not ignore:

- role errors;
- extension errors;
- tablespace errors;
- primary key errors;
- foreign key errors;
- missing relation errors;
- disk full errors.

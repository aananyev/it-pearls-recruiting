# Test restore report

Дата: 2026-07-10

## Status

`BACKUP СОЗДАН, НО TEST RESTORE НЕ ПРОЙДЕН`

## Attempted restore environment

- Restore target: local PostgreSQL
- Restore database: `hunttech_prod_restore_test_20260710_1105`
- Restore method: `pg_restore --no-owner --no-privileges`
- Dump source: local copy in `/tmp/hunttech-hrm-restore-20260710-100131`
- Production database was not used as restore target.
- Local `hunttech` and local `itpearls` were not used as restore targets.

## Failure reason

Restore failed because local disk space was insufficient.

Local free space during investigation:

- `/`: about `1.7Gi` available
- `/System/Volumes/Data`: about `1.7Gi` available

Restore log primary error:

```text
COPY failed for table "sys_scheduled_execution": ERROR: could not extend file ... No space left on device
```

After the disk-space failure, many later primary key / foreign key creations failed because prerequisite indexes and constraints had not been restored.

## Partial restore state

Partial local restore database exists:

- database: `hunttech_prod_restore_test_20260710_1105`
- size: `2683 MB`
- tables: `154`
- sequences: `2`
- constraints: `322`
- indexes: `482`

This database contains production data and must be handled as sensitive. It should be removed only after explicit approval because the current stage forbids casual deletion and because the failed restore is evidence for the report.

## Interpretation

The failed test restore does not prove that the backup is invalid. It proves that the selected local restore environment is too small for a full restore.

The backup passed:

- file creation;
- checksum creation;
- `pg_restore --list`.

But it has not passed:

- full restore;
- full data validation;
- full FK validation;
- sequence validation;
- application-level smoke tests.

## Required next action

Prepare a restore environment with enough free disk space. Based on production size and index footprint, use at least 20 GB free for a clean test restore, preferably more.

## Repeat after disk cleanup

After local disk cleanup, production dump was copied to a protected local `/private/tmp` directory.

Checksum matched:

`43e78190d9e9a176f38d6d44384ff0a697d6742073afa355dd7c1a3f9ea1aa1f`

`pg_restore --list` succeeded with 1093 entries.

Full restore was retried into:

`hunttech_prod_restore_test_20260710_175404`

Restore failed during FK creation:

`fk_itpearls_job_candidate_on_current_company`

Error:

`itpearls_job_candidate.current_company_id` references a missing `itpearls_company.id`.

Production read-only confirmation:

- orphan rows: 17;
- distinct missing company IDs: 17;
- FK metadata reports `convalidated=true`;
- production reports 0 `NOT VALID` FK constraints.

The partial local restore must not be used as a valid migration source.

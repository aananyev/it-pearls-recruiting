# Test migration stage summary

Дата: 2026-07-10

## Result

`ПРОМЫШЛЕННУЮ МИГРАЦИЮ ПРОВОДИТЬ НЕЛЬЗЯ`

## What was prepared

- Parameterized DDL for target tablespace/database/schema/grants.
- Local-test preflight script.
- Local restored DB clone script.
- Target transform script for `itpearls_* -> hunttech_*`.
- Idempotent post-migration index wrapper.
- Target validation SQL.
- Source/target prefix comparison SQL.
- Application smoke-test checklist.
- Pre- and post-migration checklists.
- Production migration and rollback runbooks.
- Rollback simulation script.

## Test restore

Backup copied from protected server backup directory to local protected `/private/tmp` directory.

Checksum:

`43e78190d9e9a176f38d6d44384ff0a697d6742073afa355dd7c1a3f9ea1aa1f`

`pg_restore --list`: success, 1093 entries.

Restore target:

`hunttech_prod_restore_test_20260710_175404`

Restore result:

`FAILED`

Failure:

`fk_itpearls_job_candidate_on_current_company`

## Blocking production data issue

Read-only production confirmation:

- `itpearls_job_candidate.current_company_id` orphan rows: 17;
- distinct missing company IDs: 17;
- FK metadata: `convalidated=true`;
- production `NOT VALID` FK constraints: 0.

## Test migration

Not started.

Reason: migration scripts must not continue after integrity violation. The restored source copy is partial and must not be used as a valid migration source.

## Smoke test

Not executed because no migrated target DB exists.

## Rollback test

Not executed against target DB because test migration did not start. Production rollback is not needed because production was not changed.

## Next required decision

Approve one explicit data rule for 17 orphan `current_company_id` values:

- restore the missing company rows if they can be identified from history/backup;
- set `current_company_id` to `null` only if business owner approves;
- remap to approved existing company IDs;
- quarantine affected candidate rows and fail migration until manually resolved.

No silent repair is allowed.

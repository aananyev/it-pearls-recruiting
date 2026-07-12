# Test migration report

Дата: 2026-07-10

## Status

`TEST MIGRATION NOT STARTED`

## Reason

Full test restore from production backup failed during FK creation:

`fk_itpearls_job_candidate_on_current_company`

The failed FK maps:

`itpearls_job_candidate.current_company_id -> itpearls_company.id`

Production read-only confirmation found:

- orphan rows: 17;
- distinct missing company IDs: 17;
- constraint metadata on production: `convalidated=true`;
- not-valid FK constraints on production: 0.

This means production contains data that violates a declared validated FK. The likely historical cause is data written while FK enforcement was bypassed, for example during an import or maintenance session with triggers disabled. This is an inference from PostgreSQL behavior, not a proven event log.

## Actions completed

- Copied existing production backup from `/var/backups/hunttech-hrm/20260710-100131` to local protected `/private/tmp` directory.
- Verified SHA-256 of dump.
- Verified `pg_restore --list` with PostgreSQL 11.22 tools.
- Created local restore DB.
- Started full restore with `pg_restore --exit-on-error`.
- Stopped after FK violation.
- Did not start target migration.

## Local restore database

The partial local restore database remains available for inspection:

`hunttech_prod_restore_test_20260710_175404`

It must not be treated as a valid migration source.

## Verdict

`ПРОМЫШЛЕННУЮ МИГРАЦИЮ ПРОВОДИТЬ НЕЛЬЗЯ`

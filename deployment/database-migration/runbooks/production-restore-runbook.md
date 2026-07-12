# Production restore runbook

Дата: 2026-07-10

## Status

Production restore is not approved and was not executed.

## Principle

Never restore over production unless there is an explicit disaster-recovery command from the owner and a confirmed backup/restore rehearsal.

## Minimum prerequisites

- Verified backup.
- Successful test restore.
- Approved downtime.
- Confirmed rollback owner.
- Confirmed exact target.
- Confirmed command review.

## Forbidden without separate approval

- `pg_restore --clean` against production.
- Dropping production database.
- Recreating production database.
- Restoring globals into production.
- Changing datasource.

## Current verdict

Not ready for production restore or migration.

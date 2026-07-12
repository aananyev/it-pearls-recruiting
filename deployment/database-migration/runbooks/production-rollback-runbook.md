# Production rollback runbook

Дата: 2026-07-10

## Principle

Old production database `itpearls` must remain unchanged until the migration is accepted.

## Before cutover

If any validation fails before datasource switch:

1. Do not switch datasource.
2. Keep HRM connected to old database.
3. Preserve target DB and logs for investigation.
4. Do not delete or repair target DB without a separate task.

## After cutover

If cutover has happened and rollback is required:

1. Enable maintenance mode or stop application.
2. Confirm no simultaneous writes to old and new DB.
3. Record cutover and rollback timestamps.
4. Switch datasource back to old `itpearls`.
5. Start application.
6. Run login and critical recruiter smoke tests.
7. Decide how to handle records written to target DB after cutover.

## Current status

No production cutover happened. Production rollback is not required.

# Application smoke test

Дата: 2026-07-10

Smoke test must be executed only against an isolated migrated test database or approved production target after cutover.

## Login

- Start application with datasource pointing to target DB.
- Login as existing administrator.
- Login as ordinary recruiter.
- Confirm failed login behavior with a non-existent user.

## Core workflows

- Open vacancy list.
- Open vacancy card.
- Open candidate list.
- Open candidate card.
- Check candidate contacts.
- Check comments and history.
- Open files and attachments.
- Check dictionaries.
- Check search screens.
- Check AI prompt templates screen.
- Check user AI configuration screen without exposing API keys.

## Controlled write test

Use only clearly marked test records.

- Create a test entity with name prefix `MIGRATION_TEST_`.
- Edit that test entity.
- Delete only that test entity.
- Verify no production business record was modified as part of the smoke test.

## Access control

- Verify admin can access administration screens.
- Verify recruiter access matches expected role.
- Verify restricted screen remains inaccessible to ordinary recruiter.
- Review application logs for datasource, security and ORM errors.

## Current status

Not executed. Full test restore failed before application smoke test could start.

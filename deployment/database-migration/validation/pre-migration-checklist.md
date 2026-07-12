# Pre-migration checklist

Дата: 2026-07-10

До промышленной миграции все пункты должны быть закрыты.

- Production backup `pg_dump -Fc` создан и checksum совпадает.
- `pg_restore --list` успешно читает backup.
- Full test restore проходит без ошибок.
- Все FK constraints восстанавливаются без ошибок.
- Orphan checks по всем critical FK возвращают `0`.
- `sec_*`, `sys_file`, `sys_db_changelog` присутствуют и агрегаты совпадают.
- Target database создается отдельно от source database.
- Source production database остается неизменной.
- `cuba.automaticDatabaseUpdate=true` обработан отдельным утвержденным решением.
- Решено, переносить ли `sec_remember_me`.
- Решено, переносить ли пользовательские SMTP/IMAP/POP3/API-key поля.
- Решено, что делать с post-cutover user writes при rollback.
- Утверждено окно недоступности и ответственные за smoke test.

## Current blocker

Full test restore currently fails on FK:

`itpearls_job_candidate.current_company_id -> itpearls_company.id`

Production read-only check found 17 orphan rows. Migration must not proceed until this is resolved by an approved data rule.

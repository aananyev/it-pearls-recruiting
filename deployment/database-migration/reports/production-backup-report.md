# Production backup report

Дата: 2026-07-10

## Status

Production backup created and verified at file/list/checksum level.

## Backup location

Remote protected directory:

`/var/backups/hunttech-hrm/20260710-100131`

This directory is outside PostgreSQL data directory and outside Git.

## Backup files

| File | Size | Permissions | SHA-256 |
|---|---:|---|---|
| `itpearls_20260710-100131.dump` | 829701541 bytes / 792 MB | `-rw------- postgres:postgres` | `43e78190d9e9a176f38d6d44384ff0a697d6742073afa355dd7c1a3f9ea1aa1f` |
| `globals_20260710-100131.sql` | 1047 bytes | `-rw------- postgres:postgres` | `370f8dea6783b52d22c153180d766709caee62f746c006650e31c7cba32989e3` |

## Manifest

Remote manifest:

`/var/backups/hunttech-hrm/20260710-100131/backup_manifest_20260710-100131.txt`

Key values:

- hostname: `hr.hunttech.ru`
- database: `itpearls`
- PostgreSQL: `11.22`
- pg_dump: `11.22`
- format: custom
- duration: 100 seconds
- `pg_dump_exit_code=0`
- `pg_dumpall_globals_exit_code=0`
- `pg_restore_list_exit_code=0`
- `pg_restore --list` entries: 1093

## Verification

Completed:

- dump file exists;
- dump file is non-empty;
- globals file exists;
- globals file is non-empty;
- SHA-256 calculated;
- `pg_restore --list` completed with exit code 0;
- file permissions restricted to `600`.

Not completed:

- full successful test restore. See `test-restore-report.md`.

## Secret handling

No passwords or production credentials were written into Git.

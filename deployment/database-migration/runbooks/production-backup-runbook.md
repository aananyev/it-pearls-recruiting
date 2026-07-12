# Production backup runbook

Дата: 2026-07-10

## Completed backup

Backup completed on 2026-07-10:

- directory: `/var/backups/hunttech-hrm/20260710-100131`
- database: `itpearls`
- dump: `itpearls_20260710-100131.dump`
- size: `792 MB`
- SHA-256: `43e78190d9e9a176f38d6d44384ff0a697d6742073afa355dd7c1a3f9ea1aa1f`

## Repeat procedure

Use server-side PostgreSQL 11.22 utilities where possible.

```bash
sudo -iu postgres
TS=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR=/var/backups/hunttech-hrm/${TS}
mkdir -p "${BACKUP_DIR}"
chmod 700 "${BACKUP_DIR}"

pg_dump --format=custom --verbose --no-password \
  --file="${BACKUP_DIR}/itpearls_${TS}.dump" \
  itpearls

pg_dumpall --globals-only --no-password \
  --file="${BACKUP_DIR}/globals_${TS}.sql"

pg_restore --list "${BACKUP_DIR}/itpearls_${TS}.dump" \
  > "${BACKUP_DIR}/pg_restore_list_${TS}.txt"

sha256sum "${BACKUP_DIR}/itpearls_${TS}.dump" "${BACKUP_DIR}/globals_${TS}.sql" \
  > "${BACKUP_DIR}/SHA256SUMS"

chmod 600 "${BACKUP_DIR}"/*
```

## Git rule

Do not copy backup, globals, logs, raw output or manifest with sensitive paths into Git.

# Backup strategy

Дата: 2026-07-10

## Цель

Обеспечить технически исполнимое восстановление production HRM до любых миграционных действий.

## Обязательные backup-артефакты

1. Logical dump application database:
   - `pg_dump -Fc`
   - timestamp in file name
   - no overwrite

2. Global objects:
   - `pg_dumpall --globals-only`
   - roles
   - memberships
   - tablespace definitions

3. Checksums:
   - `sha256sum` или `shasum -a 256`

4. Backup log:
   - PostgreSQL server version
   - pg_dump version
   - command start/end time
   - file size
   - checksum
   - operator

5. Test restore:
   - restore to isolated DB
   - run metadata audit scripts
   - run row count validation
   - run application smoke test if possible

## Рекомендуемые команды

Команды ниже являются шаблонами. Не запускать без подтверждения production имен и путей.

```bash
TS=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR=/secure/backups/hrm/${TS}
mkdir -p "${BACKUP_DIR}"

pg_dump -h hr.hunttech.ru -p 5432 -U <backup_user> -d <prod_db_name> -Fc \
  -f "${BACKUP_DIR}/<prod_db_name>-${TS}.dump"

pg_dumpall -h hr.hunttech.ru -p 5432 -U <backup_user> --globals-only \
  -f "${BACKUP_DIR}/globals-${TS}.sql"

shasum -a 256 "${BACKUP_DIR}/<prod_db_name>-${TS}.dump" \
  > "${BACKUP_DIR}/checksums-${TS}.sha256"
shasum -a 256 "${BACKUP_DIR}/globals-${TS}.sql" \
  >> "${BACKUP_DIR}/checksums-${TS}.sha256"
```

## Restore rehearsal

До основной миграции обязательно восстановить backup в отдельную базу.

Шаблон:

```bash
createdb -h <restore_host> -p <port> -U <admin_user> <restore_db_name>
pg_restore -h <restore_host> -p <port> -U <admin_user> -d <restore_db_name> \
  --no-owner --no-privileges "${BACKUP_DIR}/<prod_db_name>-${TS}.dump"
```

Режим `--clean` не использовать против production database.

## PITR и snapshots

Рекомендуется:

- VM/disk snapshot перед cutover;
- WAL archiving/PITR, если это уже настроено или может быть безопасно подготовлено заранее;
- filesystem-level backup только как дополнение к logical backup.

## Запрещено

- Хранить backup в PostgreSQL data directory.
- Перезаписывать предыдущий backup.
- Коммитить backup в Git.
- Хранить backup рядом с исходным кодом.
- Передавать dump без шифрования.

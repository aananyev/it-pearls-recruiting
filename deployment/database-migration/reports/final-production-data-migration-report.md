# Final production data migration report: `itpearls` -> `hunttech`

Дата: 2026-07-12

## 1. Цель

Выполнить ЭТАП 2 финальной production-миграции данных HRM HuntTech из базы `itpearls` в новую базу `hunttech`.

Этап 2 не включает deployment приложения `/hrm` и не открывает доступ пользователям.

## 2. Production freeze

- Tomcat service: `tomcat9`.
- Status before migration: `inactive`.
- Остановка Tomcat выполнена вручную оператором, не Codex.
- Port `8080`: listener отсутствует.
- `/app`: не принимает HTTP-запросы.
- `/hrm`: не запущен.
- Active connections to `itpearls` before migration: `0`.
- Prepared transactions: `0`.

## 3. Pre-flight

Проверено:

- host: `hr.hunttech.ru`;
- PostgreSQL: `11.22 (Ubuntu 11.22-1.pgdg20.04+1)`;
- `pg_dump`: `11.22`;
- source database `itpearls`: exists;
- target database `hunttech`: absent before migration;
- source owner: `cuba`;
- encoding/collation/ctype: `UTF8`, `ru_RU.UTF-8`, `ru_RU.UTF-8`;
- source size before migration: `6346 MB`;
- free disk: `58G`;
- legacy link tables were empty after stop writes:
  - `itpearls_job_candidate_position_link__u59616`: `0`;
  - `itpearls_open_position_city_link__u70664`: `0`;
- orphan `itpearls_job_candidate.current_company_id`: `17`;
- critical counts:
  - `sec_user`: `89`;
  - `sec_role`: `12`;
  - `sec_user_role`: `178`;
  - `sec_permission`: `3980`;
  - `sys_file`: `13460`;
  - `sys_db_changelog`: `995`.

## 4. Backup

Backup directory:

`/var/backups/hunttech-hrm/20260712-175910-stage2-final`

Created files:

- `itpearls_20260712-175910.dump`: `792M`;
- `globals_20260712-175910.sql`;
- `pg_restore_list_20260712-175910.txt`: `1093` entries;
- `tomcat_deployment_20260712-175910.tar.gz`: `725M`;
- `fileStorage_20260712-175910.tar.gz`: `1.6G`;
- `SHA256SUMS`;
- `backup_manifest.txt`.

SHA-256:

```text
6aab296881cbf3553e8c8a9267922a70149d838471d25a61001c4ce87288c312  itpearls_20260712-175910.dump
370f8dea6783b52d22c153180d766709caee62f746c006650e31c7cba32989e3  globals_20260712-175910.sql
6678f0e9dc4f7754a52c27a498101c9e141959946be14a3939bd3b4fbb5666a0  pg_restore_list_20260712-175910.txt
a16ce44399c494812f9f4d30053d455d813b954702c0a2be9adddacd2e3241d0  tomcat_deployment_20260712-175910.tar.gz
a1cd45ce989d769acc5ed391521387443da7f3d6676f9e9031e12ecb8459a4d7  fileStorage_20260712-175910.tar.gz
```

Backup verification:

- `pg_dump`: completed with exit code `0`;
- `pg_restore --list`: completed;
- `sha256sum -c SHA256SUMS`: all files `OK`;
- dump contains `sec_user`, `sys_file`, `sys_db_changelog`, `itpearls_job_candidate`, `itpearls_company`, constraints and indexes.

Failed backup attempts:

- `/var/backups/hunttech-hrm/20260712-175659-stage2-final`: failed before dump due manifest quoting issue;
- `/var/backups/hunttech-hrm/20260712-175801-stage2-final`: failed before dump because `postgres` could not write to root-owned backup directory.

These failed directories were not deleted and do not contain completed dump files.

## 5. Target database creation

Created:

```sql
create database hunttech with template itpearls owner cuba;
```

Reason:

The previously tested local migration algorithm creates a full copy of `itpearls` and then transforms legacy object names to `hunttech`. This preserves all data, IDs, constraints, indexes, sequences and history before namespace transformation.

Result:

- `hunttech`: owner `cuba`, `UTF8`, `ru_RU.UTF-8`, `ru_RU.UTF-8`;
- create duration: `29` seconds;
- source `itpearls` was not altered.

## 6. Data transformation

Executed script:

`deployment/database-migration/migration/20-transform-restored-copy-to-hunttech.sql`

SHA-256:

`7acee22f29da580ff9188bceda847079dba3285033ca0a4e75baac7b715229e0`

Migration ID:

`prod-20260712-175910`

Transformation results:

- created `hunttech_migration_run_log`;
- created `hunttech_migration_quarantine`;
- recorded `17` orphan `current_company_id` rows;
- created `17` placeholder company rows with the original missing UUIDs;
- removed two empty legacy link tables in target;
- renamed `itpearls_*` tables/sequences/views/indexes to `hunttech_*`;
- renamed constraints containing `itpearls`;
- set `hunttech_vacancy_prompt_template.temperature` default to `0.7`;
- replaced legacy namespace references in security/UI/config tables;
- replaced `sec_user.dtype` from legacy namespace to `hunttech`.

## 7. Post-migration indexes

Executed:

`deployment/database-migration/migration/30-apply-post-migration-indexes.sql`

SHA-256:

`47c0b4fca920cd21b91e053080562fcc8692c31bccd2ded27d3318c131831eea`

Result:

- `24` `CREATE INDEX` statements completed;
- no error output.

## 8. Database validation

Basic validation:

- target database: `hunttech`;
- legacy `itpearls_*` tables remaining: `0`;
- `hunttech_*` tables: `59`;
- `sec_user`: `89`;
- `sec_role`: `12`;
- `sec_user_role`: `178`;
- `sec_permission`: `3980`;
- `sys_file`: `13460`;
- `sys_db_changelog`: `995`;
- invalid constraints: `0`;
- `hunttech_vacancy_prompt_template.temperature` default: `0.7`;
- quarantine rows: `17`;
- orphan `current_company_id` after migration: `0`;
- security references to `itpearls`: `0`.

Extended validation:

- table count comparison OK: `152`;
- excluded empty legacy tables: `2`;
- table count mismatches: `0`;
- target tables: `154`;
- constraints: `405`;
- foreign keys: `245`;
- indexes: `538`;
- sequences: `4`;
- relations containing `itpearls`: `0`;
- constraints containing `itpearls`: `0`;
- invalid constraints: `0`;
- target size after migration: `6382 MB`;
- source size after migration: `6346 MB`;
- Tomcat after migration: `inactive`.

Security source/target comparison:

| Metric | itpearls | hunttech |
|---|---:|---:|
| `sec_user` | 89 | 89 |
| active users | 18 | 18 |
| inactive users | 71 | 71 |
| `sec_role` | 12 | 12 |
| `sec_user_role` | 178 | 178 |
| `sec_group` | 7 | 7 |
| `sec_permission` | 3980 | 3980 |
| `sec_user_setting` | 1471 | 1471 |
| `sec_remember_me` | 88 | 88 |

Sequence checks:

- `hunttech_migration_quarantine_id_seq`: value `17`, max `17`;
- `hunttech_migration_run_log_id_seq`: value `2`, max `2`;
- `act_evt_log_log_nr__seq`: value `1`, table max `0`;
- `sys_query_result_seq`: value `49438`, table max `0`.

## 9. File storage validation

File storage backup was created successfully:

`fileStorage_20260712-175910.tar.gz`

Physical file validation result:

- `sys_file` rows: `13460`;
- expected paths checked: `13460`;
- found physical files: `12827`;
- missing physical files: `633`.

Example missing paths:

```text
2019/10/22/c0596ca9-8d31-cbf0-497c-53b969d419e2.csv
2019/10/22/dd3fa2dd-05f2-1988-fdbc-ce1f3eb50bcd.csv
2019/10/22/6757a0c0-de23-92fa-cf2c-f9c4d1954b3c.csv
```

This appears to be a pre-existing production file storage/data consistency issue because `sys_file` metadata was copied exactly from `itpearls` to `hunttech`. However, by the stage 2 stop criteria, missing physical files block deployment.

Detailed files on server:

- `/var/backups/hunttech-hrm/20260712-175910-stage2-final/sys_file_missing_paths_prod-20260712-175910.txt`;
- `/var/backups/hunttech-hrm/20260712-175910-stage2-final/file_existence_validation_prod-20260712-175910.txt`.

These files are not committed to Git.

## 10. Current production state

- `itpearls`: exists, owner `cuba`, size `6346 MB`;
- `hunttech`: exists, owner `cuba`, size `6382 MB`;
- `tomcat9`: `inactive`;
- `/app`: not serving users;
- `/hrm`: not deployed/started;
- other PostgreSQL databases: not touched;
- `itpearls`: not transformed or renamed.

## 11. Commands executed without secrets

```bash
ssh root@hr.hunttech.ru '<read-only preflight>'
pg_dump --format=custom --verbose --no-password --file=<backup>/itpearls_20260712-175910.dump itpearls
pg_dumpall --globals-only --no-password
pg_restore --list <backup>/itpearls_20260712-175910.dump
tar -czf <backup>/tomcat_deployment_20260712-175910.tar.gz <tomcat/app paths>
tar -czf <backup>/fileStorage_20260712-175910.tar.gz -C /opt/app_home fileStorage
sha256sum -c SHA256SUMS
create database hunttech with template itpearls owner cuba;
psql -d hunttech -v migration_id=prod-20260712-175910 -f 20-transform-restored-copy-to-hunttech.sql
psql -d hunttech -f 30-apply-post-migration-indexes.sql
psql -d hunttech -f validate-migration-target.sql
```

## 12. Errors and warnings

Errors:

- file storage validation failed: `633` `sys_file` rows do not have matching physical files at expected paths.

Warnings:

- old deployed `/app` configuration contains `cuba.automaticDatabaseUpdate=true`; Tomcat must not be started casually;
- failed backup attempt directories are preserved for audit;
- `hunttech` now exists and must be preserved for analysis, not dropped without explicit approval.

## 13. Production impact

Database changes performed:

- created new database `hunttech`;
- transformed only target database `hunttech`;
- source database `itpearls` was not altered.

Application changes performed:

- none by Codex;
- Tomcat was already stopped manually by operator and remains stopped.

## 14. Stop decision

Deployment `/hrm` is prohibited until the file storage discrepancy is resolved or explicitly accepted by the migration owner.

Do not proceed to ЭТАП 3.

Do not start `/hrm`.

Do not open users.

Do not delete `hunttech`.

## 15. Verdict

`МИГРАЦИЯ НЕ ПРОШЛА — ВЫПОЛНИТЬ ВОЗВРАТ К /app`

Reason: database migration itself completed and database validations passed, but mandatory file storage validation failed due `633` missing physical files.

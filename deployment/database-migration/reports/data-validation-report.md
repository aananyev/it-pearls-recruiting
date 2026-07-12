# Data validation report

Дата: 2026-07-10

## Restore validation

Backup file checksum matches expected production manifest:

`43e78190d9e9a176f38d6d44384ff0a697d6742073afa355dd7c1a3f9ea1aa1f`

`pg_restore --list` succeeded with 1093 entries.

Full restore failed on FK creation.

## Partial restore aggregates

Database: `hunttech_prod_restore_test_20260710_175404`

| Metric | Value |
|---|---:|
| restored size | 5300 MB |
| tables | 154 |
| constraints restored before failure | 272 |
| indexes | 514 |
| `sec_user` | 89 |
| `sec_role` | 12 |
| `sec_user_role` | 178 |
| `sys_file` | 13458 |
| `sys_db_changelog` | 995 |
| `itpearls_job_candidate.current_company_id` orphan rows | 17 |

## Blocking data issue

The source data violates:

`itpearls_job_candidate.current_company_id -> itpearls_company.id`

No approved migration rule exists for these 17 rows. The migration scripts must fail rather than silently nulling, deleting, or remapping these values.

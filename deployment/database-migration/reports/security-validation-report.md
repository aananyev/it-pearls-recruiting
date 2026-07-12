# Security validation report

Дата: 2026-07-10

## Status

Security aggregate data was restored before the FK failure:

| Metric | Count |
|---|---:|
| `sec_user` | 89 |
| `sec_role` | 12 |
| `sec_user_role` | 178 |
| `sys_file` | 13458 |
| `sys_db_changelog` | 995 |

## Limitation

Full security validation cannot be accepted because restore stopped before all post-data constraints were applied.

## Current verdict

Security data appears present in the partial restore, but the restored DB is not valid for cutover or test migration until full restore succeeds without FK errors.

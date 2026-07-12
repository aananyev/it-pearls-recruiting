# Production security inventory

Дата: 2026-07-10

## PostgreSQL level

- Production database owner: `cuba`
- Production app technical user: `cuba`
- Application connects through Tomcat JNDI `jdbc/CubaDS`.
- Passwords and role password hashes were not extracted into reports.

Detailed grants and default privileges should be collected with:

- `audit/sql/production-security-audit.sql`

## HRM/CUBA security aggregates

| Table / metric | Count |
|---|---:|
| `sec_user` | 89 |
| active users | 15 |
| inactive or blocked users | 71 |
| `sec_role` | 12 |
| `sec_user_role` | 178 |
| `sec_group` | 7 |
| `sec_permission` | 3980 |
| `sec_constraint` | 0 |
| `sec_user_setting` | 1471 |
| `sec_remember_me` | 88 |
| `sec_session_log` | 29537 |
| `sys_file` | 13458 |
| `sys_db_changelog` | 995 |

No logins, emails, phones, password hashes, tokens or SMTP/IMAP/POP3 values were written into this report.

## Critical security chains

Must remain valid after migration:

- `sec_user.group_id -> sec_group.id`
- `sec_user_role.user_id -> sec_user.id`
- `sec_user_role.role_id -> sec_role.id`
- `sec_permission.role_id -> sec_role.id`
- `sec_user_setting.user_id -> sec_user.id`
- `sec_remember_me.user_id -> sec_user.id`
- `sec_session_log.user_id -> sec_user.id`
- `sec_user.image_id -> sys_file.id`
- `sec_user.official_photo_id -> sys_file.id`
- `sec_user.user_avatar_id -> sys_file.id`

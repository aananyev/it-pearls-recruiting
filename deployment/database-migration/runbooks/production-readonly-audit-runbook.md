# Production read-only audit runbook

Дата: 2026-07-10

## Цель

Собрать production metadata без изменения базы.

## Preconditions

- Есть SSH/operator access.
- Есть PostgreSQL роль, способная читать catalog metadata.
- Raw output сохраняется вне Git.

## Commands

```bash
TS=$(date +%Y%m%d-%H%M%S)
OUT=/secure/audit-output/hr-prod-${TS}
mkdir -p "${OUT}"

psql -h hr.hunttech.ru -p 5432 -U <read_only_or_operator_user> -d itpearls \
  -f deployment/database-migration/audit/sql/production-readonly-audit.sql \
  -o "${OUT}/production-readonly-audit.txt"

psql -h hr.hunttech.ru -p 5432 -U <read_only_or_operator_user> -d itpearls \
  -f deployment/database-migration/audit/sql/production-object-inventory.sql \
  -o "${OUT}/production-object-inventory.txt"

psql -h hr.hunttech.ru -p 5432 -U <read_only_or_operator_user> -d itpearls \
  -f deployment/database-migration/audit/sql/production-security-audit.sql \
  -o "${OUT}/production-security-audit.txt"
```

## Rules

- Do not commit raw output.
- Do not extract row contents from user tables.
- Do not include secrets in reports.

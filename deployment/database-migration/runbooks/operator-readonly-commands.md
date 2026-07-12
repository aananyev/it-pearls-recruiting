# Operator read-only commands

Дата: 2026-07-10

## Назначение

Команды ниже предназначены для сбора метаданных. Они не создают и не изменяют объекты базы данных.

Перед запуском на production оператор должен убедиться, что используется роль без прав изменения данных, если такая роль доступна.

## Production architecture audit

```bash
TS=$(date +%Y%m%d-%H%M%S)
mkdir -p /secure/audit-output
psql -h hr.hunttech.ru -p 5432 -U <read_only_user> -d postgres \
  -f deployment/database-migration/audit/sql/00-instance-architecture-readonly.sql \
  -o /secure/audit-output/hr-prod-00-instance-architecture-${TS}.txt
```

## Production application schema audit

Заменить `<prod_db_name>` на фактическое имя production database после проверки.

```bash
TS=$(date +%Y%m%d-%H%M%S)
psql -h hr.hunttech.ru -p 5432 -U <read_only_user> -d <prod_db_name> \
  -f deployment/database-migration/audit/sql/01-schema-inventory-readonly.sql \
  -o /secure/audit-output/hr-prod-01-schema-inventory-${TS}.txt
```

## Production security audit

```bash
TS=$(date +%Y%m%d-%H%M%S)
psql -h hr.hunttech.ru -p 5432 -U <read_only_user> -d <prod_db_name> \
  -f deployment/database-migration/audit/sql/02-security-model-readonly.sql \
  -o /secure/audit-output/hr-prod-02-security-model-${TS}.txt
```

## Local audit

```bash
TS=$(date +%Y%m%d-%H%M%S)
mkdir -p /secure/audit-output
psql -h 127.0.0.1 -p 5432 -U cuba -d postgres \
  -f deployment/database-migration/audit/sql/00-instance-architecture-readonly.sql \
  -o /secure/audit-output/hr-local-00-instance-architecture-${TS}.txt
psql -h 127.0.0.1 -p 5432 -U cuba -d hunttech \
  -f deployment/database-migration/audit/sql/01-schema-inventory-readonly.sql \
  -o /secure/audit-output/hr-local-hunttech-01-schema-inventory-${TS}.txt
psql -h 127.0.0.1 -p 5432 -U cuba -d itpearls \
  -f deployment/database-migration/audit/sql/01-schema-inventory-readonly.sql \
  -o /secure/audit-output/hr-local-itpearls-01-schema-inventory-${TS}.txt
```

## Важно

Не коммитить `/secure/audit-output`.

Перед передачей отчетов проверить, что в них нет:

- паролей;
- API keys;
- токенов;
- персональных данных;
- production connection strings с секретами.

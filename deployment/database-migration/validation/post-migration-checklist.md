# Post-migration checklist

Дата: 2026-07-10

После test или production migration проверить:

- target database name matches approved name;
- no `itpearls_%` business tables remain in target;
- expected `hunttech_%` tables exist;
- row counts match approved mapping;
- excluded legacy tables have documented zero source rows;
- quarantine/error report is empty;
- all primary keys are unique;
- all foreign keys are valid;
- no invalid constraints;
- no duplicate unique keys;
- not-null checks pass;
- sequence values are greater than table max values where sequences are used;
- indexes required by current code exist;
- functions and triggers exist;
- ownership and grants match approved security model;
- CUBA security users, roles, groups, memberships and permissions are preserved;
- `sys_file` references are valid;
- `sys_db_changelog` is preserved and understood;
- application smoke test is complete;
- rollback simulation is complete.

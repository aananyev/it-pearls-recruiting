# Production audit report

Дата: 2026-07-10

## Result

Production read-only audit completed.

## Facts

- Server: `hr.hunttech.ru`
- PostgreSQL: `11.22`
- Application datasource: `jdbc:postgresql://localhost/itpearls`
- Production database: `itpearls`
- Database OID: `111100`
- Database owner: `cuba`
- Technical application user: `cuba`
- Encoding: `UTF8`
- Collation: `ru_RU.UTF-8`
- Ctype: `ru_RU.UTF-8`
- Size: `6342 MB`
- Schemas: `public`
- Tablespaces: `pg_default`, `pg_global`
- Tablespace `itpearls`: does not exist
- Database `itpearls`: exists
- Schema `itpearls`: does not exist
- Tablespace `hunttech`: does not exist
- Database `hunttech`: does not exist
- Schema `hunttech`: does not exist
- Database `HuntTech`: does not exist

## Interpretation

The production source is not a PostgreSQL tablespace named `itpearls`. It is a PostgreSQL database named `itpearls` with `itpearls_*` business table prefix.

## Production was not changed

Only read-only metadata queries, `pg_dump`, `pg_dumpall --globals-only`, and `pg_restore --list` were executed.

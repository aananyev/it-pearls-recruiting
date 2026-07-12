-- Validate foreign key metadata after restore.

\pset pager off
\pset tuples_only off
\pset format aligned

select
    con.conrelid::regclass::text as table_name,
    con.conname as foreign_key_name,
    con.confrelid::regclass::text as referenced_table,
    con.convalidated as validated,
    pg_get_constraintdef(con.oid) as definition
from pg_constraint con
where con.contype = 'f'
  and con.connamespace = 'public'::regnamespace
order by table_name, foreign_key_name;

select
    'invalid_fk_count' as metric,
    count(*) as value
from pg_constraint
where contype = 'f'
  and connamespace = 'public'::regnamespace
  and convalidated = false;

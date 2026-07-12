-- Production sequence audit. SELECT-only.

\pset pager off
\pset tuples_only off
\pset format aligned

BEGIN READ ONLY;
SET LOCAL statement_timeout = '5min';
SET LOCAL lock_timeout = '5s';
SET LOCAL idle_in_transaction_session_timeout = '5min';

select
    'sequences' as section,
    sequence_schema,
    sequence_name,
    data_type,
    start_value,
    minimum_value,
    maximum_value,
    increment
from information_schema.sequences
where sequence_schema not in ('pg_catalog', 'information_schema')
order by sequence_schema, sequence_name;

select
    'sequence_relations' as section,
    n.nspname as schema_name,
    c.relname as sequence_name,
    pg_get_userbyid(c.relowner) as owner,
    coalesce(nullif(ts.spcname, ''), 'pg_default') as tablespace
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
left join pg_tablespace ts on ts.oid = c.reltablespace
where c.relkind = 'S'
order by n.nspname, c.relname;

ROLLBACK;

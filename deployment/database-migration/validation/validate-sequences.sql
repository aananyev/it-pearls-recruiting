-- Validate restored sequences.

\pset pager off
\pset tuples_only off
\pset format aligned

select
    sequence_schema,
    sequence_name,
    data_type,
    start_value,
    minimum_value,
    maximum_value,
    increment
from information_schema.sequences
where sequence_schema = 'public'
order by sequence_name;

select
    n.nspname as schema_name,
    c.relname as sequence_name,
    pg_get_userbyid(c.relowner) as owner
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where c.relkind = 'S'
order by n.nspname, c.relname;

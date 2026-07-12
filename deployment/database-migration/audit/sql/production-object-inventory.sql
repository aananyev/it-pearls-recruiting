-- Production object inventory. SELECT-only.

\pset pager off
\pset tuples_only off
\pset format aligned

BEGIN READ ONLY;
SET LOCAL statement_timeout = '5min';
SET LOCAL lock_timeout = '5s';
SET LOCAL idle_in_transaction_session_timeout = '5min';

select
    'object_counts' as section,
    case c.relkind
        when 'r' then 'table'
        when 'p' then 'partitioned_table'
        when 'S' then 'sequence'
        when 'v' then 'view'
        when 'm' then 'materialized_view'
        when 'f' then 'foreign_table'
        else c.relkind::text
    end as object_type,
    count(*)
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname not like 'pg_%'
  and n.nspname <> 'information_schema'
  and c.relkind in ('r','p','S','v','m','f')
group by c.relkind
order by object_type;

select
    'table_inventory' as section,
    n.nspname as schema_name,
    c.relname as table_name,
    pg_get_userbyid(c.relowner) as owner,
    coalesce(nullif(ts.spcname, ''), 'pg_default') as tablespace,
    c.reltuples::bigint as estimated_rows,
    pg_size_pretty(pg_table_size(c.oid)) as table_size,
    pg_size_pretty(pg_indexes_size(c.oid)) as indexes_size,
    pg_size_pretty(pg_total_relation_size(c.oid)) as total_size
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
left join pg_tablespace ts on ts.oid = c.reltablespace
where n.nspname not like 'pg_%'
  and n.nspname <> 'information_schema'
  and c.relkind in ('r','p')
order by pg_total_relation_size(c.oid) desc, c.relname;

select
    'indexes' as section,
    schemaname,
    tablename,
    indexname,
    tablespace,
    indexdef
from pg_indexes
where schemaname not in ('pg_catalog', 'information_schema')
order by schemaname, tablename, indexname;

select
    'constraints' as section,
    con.conrelid::regclass::text as table_name,
    con.conname as constraint_name,
    case con.contype
        when 'p' then 'primary_key'
        when 'f' then 'foreign_key'
        when 'u' then 'unique'
        when 'c' then 'check'
        when 'x' then 'exclusion'
        else con.contype::text
    end as constraint_type,
    pg_get_constraintdef(con.oid) as constraint_definition
from pg_constraint con
where con.connamespace::regnamespace::text not in ('pg_catalog', 'information_schema')
order by con.conrelid::regclass::text, con.contype, con.conname;

select
    'columns' as section,
    table_schema,
    table_name,
    ordinal_position,
    column_name,
    data_type,
    udt_name,
    is_nullable,
    column_default,
    is_generated,
    generation_expression
from information_schema.columns
where table_schema not in ('pg_catalog', 'information_schema')
order by table_schema, table_name, ordinal_position;

select
    'triggers' as section,
    trigger_schema,
    event_object_table,
    trigger_name,
    event_manipulation,
    action_timing,
    action_statement
from information_schema.triggers
where trigger_schema not in ('pg_catalog', 'information_schema')
order by trigger_schema, event_object_table, trigger_name;

select
    'routines' as section,
    routine_schema,
    routine_name,
    routine_type,
    data_type,
    security_type,
    external_language
from information_schema.routines
where routine_schema not in ('pg_catalog', 'information_schema')
order by routine_schema, routine_type, routine_name;

select
    'custom_types' as section,
    n.nspname as schema_name,
    t.typname as type_name,
    case t.typtype
        when 'e' then 'enum'
        when 'd' then 'domain'
        when 'c' then 'composite'
        when 'r' then 'range'
        else t.typtype::text
    end as type_kind,
    pg_get_userbyid(t.typowner) as owner
from pg_type t
join pg_namespace n on n.oid = t.typnamespace
where n.nspname not like 'pg_%'
  and n.nspname <> 'information_schema'
  and t.typtype in ('e','d','c','r')
order by n.nspname, t.typname;

ROLLBACK;

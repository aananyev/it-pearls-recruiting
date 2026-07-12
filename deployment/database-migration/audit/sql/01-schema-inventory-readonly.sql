-- Read-only PostgreSQL schema inventory audit.
-- Safe for production metadata collection: SELECT-only.
-- Run against each application database that must be compared.

\pset pager off
\pset tuples_only off
\pset format aligned

select 'audit_started_at' as key, now()::text as value;
select 'database' as key, current_database() as value;
select 'current_user' as key, current_user as value;

select
    n.nspname as schema_name,
    pg_get_userbyid(n.nspowner) as owner,
    n.nspacl::text as acl
from pg_namespace n
where n.nspname not like 'pg_%'
  and n.nspname <> 'information_schema'
order by n.nspname;

select
    n.nspname as schema_name,
    c.relname as object_name,
    case c.relkind
        when 'r' then 'table'
        when 'p' then 'partitioned_table'
        when 'i' then 'index'
        when 'I' then 'partitioned_index'
        when 'S' then 'sequence'
        when 'v' then 'view'
        when 'm' then 'materialized_view'
        when 'f' then 'foreign_table'
        else c.relkind::text
    end as object_type,
    pg_get_userbyid(c.relowner) as owner,
    ts.spcname as tablespace,
    c.reltuples::bigint as estimated_rows,
    pg_size_pretty(pg_total_relation_size(c.oid)) as total_size,
    c.relacl::text as acl
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
left join pg_tablespace ts on ts.oid = c.reltablespace
where n.nspname not like 'pg_%'
  and n.nspname <> 'information_schema'
  and c.relkind in ('r','p','S','v','m','f')
order by n.nspname, object_type, c.relname;

select
    c.table_schema,
    c.table_name,
    c.ordinal_position,
    c.column_name,
    c.data_type,
    c.udt_name,
    c.character_maximum_length,
    c.numeric_precision,
    c.numeric_scale,
    c.is_nullable,
    c.column_default
from information_schema.columns c
where c.table_schema not in ('pg_catalog', 'information_schema')
order by c.table_schema, c.table_name, c.ordinal_position;

select
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
    schemaname,
    tablename,
    indexname,
    indexdef
from pg_indexes
where schemaname not in ('pg_catalog', 'information_schema')
order by schemaname, tablename, indexname;

select
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

select
    count(*) as large_object_count
from pg_largeobject_metadata;

select 'audit_finished_at' as key, now()::text as value;

-- Validate restored HRM database using metadata and aggregate checks only.

\pset pager off
\pset tuples_only off
\pset format aligned

select 'database' as metric, current_database() as value;
select 'size' as metric, pg_size_pretty(pg_database_size(current_database())) as value;

select 'tables' as metric, count(*)::text as value
from information_schema.tables
where table_schema = 'public'
  and table_type = 'BASE TABLE'
union all
select 'sequences', count(*)::text from information_schema.sequences where sequence_schema = 'public'
union all
select 'views', count(*)::text from information_schema.views where table_schema = 'public'
union all
select 'constraints', count(*)::text from pg_constraint where connamespace = 'public'::regnamespace
union all
select 'indexes', count(*)::text from pg_indexes where schemaname = 'public'
union all
select 'large_objects', count(*)::text from pg_largeobject_metadata
order by metric;

select
    'largest_tables' as section,
    relname as table_name,
    n_live_tup as estimated_rows,
    pg_size_pretty(pg_total_relation_size(relid)) as total_size
from pg_stat_user_tables
order by pg_total_relation_size(relid) desc
limit 30;

select
    'missing_expected_table' as section,
    expected.table_name
from (values
    ('sec_user'), ('sec_role'), ('sec_user_role'), ('sec_group'), ('sec_permission'),
    ('sys_file'), ('sys_db_changelog')
) as expected(table_name)
where to_regclass('public.' || expected.table_name) is null
order by expected.table_name;

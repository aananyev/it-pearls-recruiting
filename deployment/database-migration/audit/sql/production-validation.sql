-- Production pre-backup validation. SELECT-only.

\pset pager off
\pset tuples_only off
\pset format aligned

BEGIN READ ONLY;
SET LOCAL statement_timeout = '5min';
SET LOCAL lock_timeout = '5s';
SET LOCAL idle_in_transaction_session_timeout = '5min';

select 'database_size' as check_name, pg_size_pretty(pg_database_size(current_database())) as result;
select 'large_objects' as check_name, count(*)::text as result from pg_largeobject_metadata;
select 'prepared_transactions' as check_name, count(*)::text as result from pg_prepared_xacts;

select
    'missing_expected_security_table' as check_name,
    expected.table_name as result
from (values
    ('sec_user'), ('sec_role'), ('sec_user_role'), ('sec_group'), ('sec_group_hierarchy'),
    ('sec_permission'), ('sec_constraint'), ('sec_session_attr'), ('sec_user_setting'),
    ('sec_remember_me'), ('sys_file'), ('sys_db_changelog')
) as expected(table_name)
where to_regclass('public.' || expected.table_name) is null
order by expected.table_name;

select
    'legacy_link_status' as check_name,
    table_name || '=' || exists_flag || ', rows=' || coalesce(row_count::text, 'null') as result
from (
    select
        'itpearls_job_candidate_position_link__u59616' as table_name,
        (to_regclass('public.itpearls_job_candidate_position_link__u59616') is not null)::text as exists_flag,
        case when to_regclass('public.itpearls_job_candidate_position_link__u59616') is null then null else (select count(*) from public.itpearls_job_candidate_position_link__u59616) end as row_count
    union all
    select
        'itpearls_open_position_city_link__u70664',
        (to_regclass('public.itpearls_open_position_city_link__u70664') is not null)::text,
        case when to_regclass('public.itpearls_open_position_city_link__u70664') is null then null else (select count(*) from public.itpearls_open_position_city_link__u70664) end
) s;

ROLLBACK;

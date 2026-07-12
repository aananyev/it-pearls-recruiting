-- Safe row-count helpers.
-- Uses statistics for all tables and exact counts only for critical tables.

\pset pager off
\pset tuples_only off
\pset format aligned

BEGIN READ ONLY;
SET LOCAL statement_timeout = '5min';
SET LOCAL lock_timeout = '5s';
SET LOCAL idle_in_transaction_session_timeout = '5min';

select
    'estimated_rows' as section,
    schemaname,
    relname as table_name,
    n_live_tup,
    n_dead_tup,
    last_analyze,
    last_autoanalyze
from pg_stat_user_tables
order by n_live_tup desc, relname;

select 'critical_exact_count' as section, 'sec_user' as table_name, count(*) from sec_user
union all select 'critical_exact_count', 'sec_role', count(*) from sec_role
union all select 'critical_exact_count', 'sec_user_role', count(*) from sec_user_role
union all select 'critical_exact_count', 'sec_group', count(*) from sec_group
union all select 'critical_exact_count', 'sec_permission', count(*) from sec_permission
union all select 'critical_exact_count', 'sec_constraint', count(*) from sec_constraint
union all select 'critical_exact_count', 'sec_user_setting', count(*) from sec_user_setting
union all select 'critical_exact_count', 'sec_remember_me', count(*) from sec_remember_me
union all select 'critical_exact_count', 'sys_file', count(*) from sys_file
union all select 'critical_exact_count', 'sys_db_changelog', count(*) from sys_db_changelog
union all select 'critical_exact_count', 'itpearls_job_candidate_position_link__u59616', count(*) from itpearls_job_candidate_position_link__u59616
union all select 'critical_exact_count', 'itpearls_open_position_city_link__u70664', count(*) from itpearls_open_position_city_link__u70664
order by table_name;

ROLLBACK;

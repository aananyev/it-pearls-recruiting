-- Validation for migrated target database.
\set ON_ERROR_STOP on

select 'target_database' as metric, current_database() as value;

select 'legacy_itpearls_tables_remaining' as metric, count(*)::text as value
from information_schema.tables
where table_schema = 'public'
  and table_name like 'itpearls\_%' escape '\';

select 'hunttech_tables' as metric, count(*)::text as value
from information_schema.tables
where table_schema = 'public'
  and table_name like 'hunttech\_%' escape '\';

select 'security_users' as metric, count(*)::text as value from sec_user
union all select 'security_roles', count(*)::text from sec_role
union all select 'security_user_roles', count(*)::text from sec_user_role
union all select 'permissions', count(*)::text from sec_permission
union all select 'sys_file', count(*)::text from sys_file
union all select 'sys_db_changelog', count(*)::text from sys_db_changelog;

select 'invalid_constraints' as metric, count(*)::text as value
from pg_constraint
where convalidated = false;

select 'vacancy_prompt_template_temperature_default' as metric,
       coalesce(column_default, '<null>') as value
from information_schema.columns
where table_schema = 'public'
  and table_name = 'hunttech_vacancy_prompt_template'
  and column_name = 'temperature';

select 'quarantine_rows' as metric, count(*)::text as value
from hunttech_migration_quarantine;

select 'orphan_current_company_after_migration' as metric, count(*)::text as value
from hunttech_job_candidate jc
left join hunttech_company c on c.id = jc.current_company_id
where jc.current_company_id is not null
  and c.id is null;

select 'security_itpearls_references_remaining' as metric, (
    (select count(*) from sec_permission where target like '%itpearls%') +
    (select count(*) from sec_user_setting where name like '%itpearls%') +
    (select count(*) from sec_user where dtype like '%itpearls%') +
    (select count(*) from sec_filter where component like '%itpearls%' or xml like '%itpearls%') +
    (select count(*) from sec_presentation where component like '%itpearls%' or xml like '%itpearls%') +
    (select count(*) from dashboard_persistent_dashboard where dashboard_model like '%itpearls%') +
    (select count(*) from dashboard_widget_template where widget_model like '%itpearls%') +
    (select count(*) from sys_config where name like '%itpearls%' or value_ like '%itpearls%')
)::text as value;

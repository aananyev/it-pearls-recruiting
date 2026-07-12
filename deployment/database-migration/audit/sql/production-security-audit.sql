-- Production PostgreSQL and CUBA security audit. SELECT-only.

\pset pager off
\pset tuples_only off
\pset format aligned

BEGIN READ ONLY;
SET LOCAL statement_timeout = '5min';
SET LOCAL lock_timeout = '5s';
SET LOCAL idle_in_transaction_session_timeout = '5min';

select
    'roles' as section,
    rolname,
    rolsuper,
    rolcreatedb,
    rolcreaterole,
    rolreplication,
    rolbypassrls,
    rolcanlogin,
    rolconnlimit,
    rolvaliduntil
from pg_roles
order by rolname;

select
    'role_membership' as section,
    parent.rolname as granted_role,
    member.rolname as member_role,
    auth.admin_option
from pg_auth_members auth
join pg_roles parent on parent.oid = auth.roleid
join pg_roles member on member.oid = auth.member
order by parent.rolname, member.rolname;

select
    'database_acl' as section,
    datname,
    datacl::text
from pg_database
order by datname;

select
    'table_privileges' as section,
    grantee,
    table_schema,
    table_name,
    privilege_type,
    is_grantable
from information_schema.table_privileges
where table_schema not in ('pg_catalog', 'information_schema')
order by table_schema, table_name, grantee, privilege_type;

select
    'routine_privileges' as section,
    grantee,
    routine_schema,
    routine_name,
    privilege_type,
    is_grantable
from information_schema.routine_privileges
where routine_schema not in ('pg_catalog', 'information_schema')
order by routine_schema, routine_name, grantee, privilege_type;

select
    'default_privileges' as section,
    defaclrole::regrole::text as owner_role,
    defaclnamespace::regnamespace::text as schema_name,
    defaclobjtype as object_type,
    defaclacl::text as default_acl
from pg_default_acl
order by owner_role, schema_name, object_type;

select
    'rls_policies' as section,
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd
from pg_policies
order by schemaname, tablename, policyname;

select
    'security_counts' as section,
    'sec_user' as table_name,
    count(*) as row_count
from sec_user
union all select 'security_counts', 'sec_user_active', count(*) from sec_user where active = true and delete_ts is null
union all select 'security_counts', 'sec_user_inactive_or_blocked', count(*) from sec_user where coalesce(active, false) = false and delete_ts is null
union all select 'security_counts', 'sec_role', count(*) from sec_role
union all select 'security_counts', 'sec_user_role', count(*) from sec_user_role
union all select 'security_counts', 'sec_group', count(*) from sec_group
union all select 'security_counts', 'sec_permission', count(*) from sec_permission
union all select 'security_counts', 'sec_constraint', count(*) from sec_constraint
union all select 'security_counts', 'sec_user_setting', count(*) from sec_user_setting
union all select 'security_counts', 'sec_remember_me', count(*) from sec_remember_me
union all select 'security_counts', 'sec_session_log', count(*) from sec_session_log
union all select 'security_counts', 'sys_file', count(*) from sys_file
union all select 'security_counts', 'sys_db_changelog', count(*) from sys_db_changelog
order by table_name;

select
    'security_fk' as section,
    con.conrelid::regclass::text as table_name,
    con.conname as foreign_key_name,
    pg_get_constraintdef(con.oid) as foreign_key_definition
from pg_constraint con
where con.contype = 'f'
  and (
      con.conrelid::regclass::text like 'sec_%'
      or con.conrelid::regclass::text like 'sys_%'
      or con.conrelid::regclass::text like '%user%'
      or pg_get_constraintdef(con.oid) like '%sec_user%'
      or pg_get_constraintdef(con.oid) like '%sec_role%'
      or pg_get_constraintdef(con.oid) like '%sec_group%'
  )
order by con.conrelid::regclass::text, con.conname;

ROLLBACK;

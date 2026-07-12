-- Read-only PostgreSQL and CUBA/Jmix security model audit.
-- Safe for production metadata collection: SELECT-only.
-- Run against the application database.

\pset pager off
\pset tuples_only off
\pset format aligned

select 'audit_started_at' as key, now()::text as value;
select 'database' as key, current_database() as value;
select 'current_user' as key, current_user as value;

select
    grantee,
    table_schema,
    table_name,
    privilege_type,
    is_grantable
from information_schema.table_privileges
where table_schema not in ('pg_catalog', 'information_schema')
order by table_schema, table_name, grantee, privilege_type;

select
    grantee,
    object_schema,
    object_name,
    privilege_type,
    is_grantable
from information_schema.usage_privileges
where object_schema not in ('pg_catalog', 'information_schema')
order by object_schema, object_name, grantee, privilege_type;

select
    grantee,
    routine_schema,
    routine_name,
    privilege_type,
    is_grantable
from information_schema.routine_privileges
where routine_schema not in ('pg_catalog', 'information_schema')
order by routine_schema, routine_name, grantee, privilege_type;

select
    defaclrole::regrole::text as owner_role,
    defaclnamespace::regnamespace::text as schema_name,
    defaclobjtype as object_type,
    defaclacl::text as default_acl
from pg_default_acl
order by owner_role, schema_name, object_type;

select
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

select
    table_name
from information_schema.tables
where table_schema = 'public'
  and (
      table_name like 'sec_%'
      or table_name like 'sys_%'
      or table_name like '%user%'
      or table_name like '%permission%'
      or table_name like '%constraint%'
      or table_name like '%role%'
      or table_name like '%group%'
      or table_name like '%session%'
      or table_name like '%token%'
      or table_name like '%audit%'
      or table_name like '%entity_log%'
  )
order by table_name;

select
    con.conrelid::regclass::text as table_name,
    con.conname as foreign_key_name,
    pg_get_constraintdef(con.oid) as foreign_key_definition
from pg_constraint con
where con.contype = 'f'
  and (
      con.conrelid::regclass::text in (
          'sec_user',
          'sec_role',
          'sec_user_role',
          'sec_group',
          'sec_permission',
          'sec_constraint',
          'sec_user_setting',
          'sec_remember_me',
          'sec_session_log'
      )
      or pg_get_constraintdef(con.oid) like '%sec_user%'
  )
order by con.conrelid::regclass::text, con.conname;

select 'audit_finished_at' as key, now()::text as value;

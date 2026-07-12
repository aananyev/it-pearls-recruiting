-- Read-only PostgreSQL instance and database architecture audit.
-- Safe for production metadata collection: SELECT-only.
-- Run with psql against postgres or the target application database.
--
-- Recommended:
-- psql -h <host> -p <port> -U <read_only_user> -d postgres \
--   -f deployment/database-migration/audit/sql/00-instance-architecture-readonly.sql \
--   -o /secure/audit-output/00-instance-architecture-<host>-<timestamp>.txt

\pset pager off
\pset tuples_only off
\pset format aligned

select 'audit_started_at' as key, now()::text as value;
select 'current_database' as key, current_database() as value;
select 'current_user' as key, current_user as value;
select 'session_user' as key, session_user as value;
select 'server_version' as key, version() as value;
select 'server_version_num' as key, current_setting('server_version_num') as value;
select 'data_directory_visible_to_role' as key, current_setting('data_directory', true) as value;
select 'server_encoding' as key, current_setting('server_encoding') as value;
select 'lc_collate' as key, current_setting('lc_collate') as value;
select 'lc_ctype' as key, current_setting('lc_ctype') as value;
select 'timezone' as key, current_setting('TimeZone') as value;

select
    d.datname as database_name,
    pg_get_userbyid(d.datdba) as owner,
    pg_encoding_to_char(d.encoding) as encoding,
    d.datcollate as collation,
    d.datctype as ctype,
    t.spcname as tablespace,
    d.datallowconn as allow_connections,
    d.datistemplate as is_template
from pg_database d
join pg_tablespace t on t.oid = d.dattablespace
order by d.datname;

select
    spcname as tablespace_name,
    pg_get_userbyid(spcowner) as owner,
    coalesce(nullif(pg_tablespace_location(oid), ''), '[cluster default]') as physical_path,
    spcacl::text as acl
from pg_tablespace
order by spcname;

select
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
    parent.rolname as granted_role,
    member.rolname as member_role,
    auth.admin_option
from pg_auth_members auth
join pg_roles parent on parent.oid = auth.roleid
join pg_roles member on member.oid = auth.member
order by parent.rolname, member.rolname;

select
    datname as database_name,
    datacl::text as database_acl
from pg_database
order by datname;

select
    defaclrole::regrole::text as owner_role,
    defaclnamespace::regnamespace::text as schema_name,
    defaclobjtype as object_type,
    defaclacl::text as default_acl
from pg_default_acl
order by owner_role, schema_name, object_type;

select
    extname as extension_name,
    extversion as extension_version,
    extnamespace::regnamespace::text as schema_name,
    pg_get_userbyid(extowner) as owner
from pg_extension
order by extname;

select 'audit_finished_at' as key, now()::text as value;

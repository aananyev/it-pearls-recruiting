-- Production PostgreSQL read-only audit for HRM HuntTech.
-- PostgreSQL 11 compatible. SELECT-only, no DDL/DML.

\pset pager off
\pset tuples_only off
\pset format aligned

BEGIN READ ONLY;
SET LOCAL statement_timeout = '5min';
SET LOCAL lock_timeout = '5s';
SET LOCAL idle_in_transaction_session_timeout = '5min';

select 'audit_started_at' as key, now()::text as value;
select 'server_version' as key, version() as value;
select 'current_database' as key, current_database() as value;
select 'current_user' as key, current_user as value;
select 'search_path' as key, current_setting('search_path') as value;

select
    'db_identity' as section,
    d.datname,
    d.oid,
    pg_get_userbyid(d.datdba) as owner,
    pg_encoding_to_char(d.encoding) as encoding,
    d.datcollate,
    d.datctype,
    t.spcname as default_tablespace,
    pg_size_pretty(pg_database_size(d.datname)) as size
from pg_database d
join pg_tablespace t on t.oid = d.dattablespace
where d.datname = current_database();

select
    'name_kind' as section,
    names.name,
    exists(select 1 from pg_database where datname = names.name) as is_database,
    exists(select 1 from pg_tablespace where spcname = names.name) as is_tablespace,
    exists(select 1 from pg_namespace where nspname = names.name) as is_schema
from (values ('itpearls'), ('hunttech'), ('HuntTech')) as names(name)
order by names.name;

select
    'tablespaces' as section,
    spcname,
    pg_get_userbyid(spcowner) as owner,
    coalesce(nullif(pg_tablespace_location(oid), ''), '[cluster default]') as physical_path
from pg_tablespace
order by spcname;

select
    'schemas' as section,
    nspname,
    pg_get_userbyid(nspowner) as owner,
    nspacl::text as acl
from pg_namespace
where nspname not like 'pg_%'
  and nspname <> 'information_schema'
order by nspname;

select
    'extensions' as section,
    extname,
    extversion,
    extnamespace::regnamespace::text as schema_name,
    pg_get_userbyid(extowner) as owner
from pg_extension
order by extname;

select
    'active_connections' as section,
    datname,
    usename,
    state,
    count(*)
from pg_stat_activity
group by datname, usename, state
order by datname, usename, state;

select
    'long_transactions' as section,
    pid,
    datname,
    usename,
    state,
    now() - xact_start as xact_age,
    wait_event_type,
    wait_event
from pg_stat_activity
where xact_start is not null
  and now() - xact_start > interval '5 minutes'
order by xact_start;

select 'prepared_transactions' as section, count(*) from pg_prepared_xacts;

select
    'replication_slots' as section,
    slot_name,
    plugin,
    slot_type,
    database,
    active
from pg_replication_slots
order by slot_name;

select
    'publications' as section,
    pubname,
    puballtables,
    pubinsert,
    pubupdate,
    pubdelete
from pg_publication
order by pubname;

select
    'subscriptions' as section,
    s.subname,
    d.datname as database_name,
    s.subenabled
from pg_subscription s
left join pg_database d on d.oid = s.subdbid
order by s.subname;

select
    'fdw' as section,
    fdwname,
    pg_get_userbyid(fdwowner) as owner
from pg_foreign_data_wrapper
order by fdwname;

select
    'foreign_servers' as section,
    srvname,
    fdw.fdwname,
    pg_get_userbyid(srvowner) as owner
from pg_foreign_server fs
join pg_foreign_data_wrapper fdw on fdw.oid = fs.srvfdw
order by srvname;

select 'large_objects' as section, count(*) from pg_largeobject_metadata;
select 'audit_finished_at' as key, now()::text as value;

ROLLBACK;

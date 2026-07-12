-- Creates target database. Run from maintenance database, usually postgres.
-- Required psql variables:
--   expected_server_version, target_db, owner_role, tablespace_name
\set ON_ERROR_STOP on

\if :{?expected_server_version}
\else
  \echo 'ERROR: expected_server_version is required'
  \quit 2
\endif
\if :{?target_db}
\else
  \echo 'ERROR: target_db is required'
  \quit 2
\endif
\if :{?owner_role}
\else
  \echo 'ERROR: owner_role is required'
  \quit 2
\endif
\if :{?tablespace_name}
\else
  \echo 'ERROR: tablespace_name is required'
  \quit 2
\endif

select (current_setting('server_version') like :'expected_server_version' || '%') as version_ok \gset
\if :version_ok
\else
  \echo 'ERROR: server version does not match expected_server_version'
  \quit 3
\endif

select (count(*) = 0) as target_absent from pg_database where datname = :'target_db' \gset
\if :target_absent
\else
  \echo 'ERROR: target database already exists'
  \quit 4
\endif

create database :target_db
  owner :owner_role
  template template0
  encoding 'UTF8'
  lc_collate 'ru_RU.UTF-8'
  lc_ctype 'ru_RU.UTF-8'
  tablespace :tablespace_name;

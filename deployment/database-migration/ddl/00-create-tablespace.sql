-- Creates target tablespace. Run only in an approved maintenance window.
-- Required psql variables:
--   expected_server_version, tablespace_name, tablespace_path, owner_role
\set ON_ERROR_STOP on

\if :{?expected_server_version}
\else
  \echo 'ERROR: expected_server_version is required'
  \quit 2
\endif
\if :{?tablespace_name}
\else
  \echo 'ERROR: tablespace_name is required'
  \quit 2
\endif
\if :{?tablespace_path}
\else
  \echo 'ERROR: tablespace_path is required'
  \quit 2
\endif
\if :{?owner_role}
\else
  \echo 'ERROR: owner_role is required'
  \quit 2
\endif

select (current_setting('server_version') like :'expected_server_version' || '%') as version_ok \gset
\if :version_ok
\else
  \echo 'ERROR: server version does not match expected_server_version'
  \quit 3
\endif

create tablespace :tablespace_name owner :owner_role location :'tablespace_path';

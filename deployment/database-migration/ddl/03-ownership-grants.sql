-- Applies baseline ownership and grants for target DB.
-- Required psql variables:
--   target_db, owner_role, app_role
\set ON_ERROR_STOP on

alter database :target_db owner to :owner_role;
alter schema public owner to :owner_role;

grant connect, temporary on database :target_db to :app_role;
grant usage, create on schema public to :app_role;
grant select, insert, update, delete on all tables in schema public to :app_role;
grant usage, select, update on all sequences in schema public to :app_role;
grant execute on all functions in schema public to :app_role;

alter default privileges for role :owner_role in schema public
  grant select, insert, update, delete on tables to :app_role;
alter default privileges for role :owner_role in schema public
  grant usage, select, update on sequences to :app_role;
alter default privileges for role :owner_role in schema public
  grant execute on functions to :app_role;

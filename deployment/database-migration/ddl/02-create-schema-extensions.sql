-- Initializes schema and extensions for target DB.
-- Required psql variables:
--   owner_role
\set ON_ERROR_STOP on

create schema if not exists public;
alter schema public owner to :owner_role;

create extension if not exists plpgsql with schema pg_catalog;

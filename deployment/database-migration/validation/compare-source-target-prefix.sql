-- Compares restored source DB with migrated target DB.
-- Run from target DB with psql variables:
--   source_db
\set ON_ERROR_STOP on

create extension if not exists dblink;

with target_tables as (
  select table_name
  from information_schema.tables
  where table_schema = 'public'
    and table_type = 'BASE TABLE'
),
source_tables as (
  select *
  from dblink(
    'dbname=' || :'source_db',
    $q$
      select table_name
      from information_schema.tables
      where table_schema = 'public'
        and table_type = 'BASE TABLE'
    $q$
  ) as t(table_name text)
),
mapped as (
  select
    s.table_name as source_table,
    case
      when s.table_name like 'itpearls\_%' escape '\' then regexp_replace(s.table_name, '^itpearls_', 'hunttech_')
      else s.table_name
    end as expected_target_table
  from source_tables s
  where s.table_name not in (
    'itpearls_job_candidate_position_link__u59616',
    'itpearls_open_position_city_link__u70664'
  )
)
select
  'missing_target_table' as check_name,
  source_table,
  expected_target_table
from mapped m
where not exists (
  select 1 from target_tables t where t.table_name = m.expected_target_table
)
order by source_table;

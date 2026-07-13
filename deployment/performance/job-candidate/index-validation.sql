-- Read-only validation script for JobCandidateEdit performance audit.
-- Safe to run on local copy. Do not run EXPLAIN ANALYZE on production without approval.

select current_database(), current_user, inet_server_addr(), inet_server_port();

select c.relname as table_name,
       pg_size_pretty(pg_total_relation_size(c.oid)) as total_size,
       pg_size_pretty(pg_relation_size(c.oid)) as heap_size,
       pg_size_pretty(pg_indexes_size(c.oid)) as index_size
from pg_class c
join pg_namespace n on n.oid = c.relnamespace
where n.nspname = 'public'
  and c.relkind = 'r'
  and c.relname in (
      'hunttech_job_candidate',
      'hunttech_iteraction_list',
      'hunttech_candidate_cv',
      'hunttech_social_network_ur_ls',
      'hunttech_job_candidate_position_lists',
      'hunttech_company',
      'hunttech_city',
      'hunttech_position',
      'hunttech_open_position',
      'sys_file'
  )
order by pg_total_relation_size(c.oid) desc;

select relname, n_live_tup, n_dead_tup, last_analyze, last_autoanalyze, seq_scan, idx_scan
from pg_stat_user_tables
where relname in (
      'hunttech_job_candidate',
      'hunttech_iteraction_list',
      'hunttech_candidate_cv',
      'hunttech_social_network_ur_ls',
      'hunttech_job_candidate_position_lists',
      'hunttech_company',
      'hunttech_city',
      'hunttech_position',
      'hunttech_open_position',
      'sys_file'
)
order by relname;

select relname, indexrelname, idx_scan, idx_tup_read, idx_tup_fetch
from pg_stat_user_indexes
where relname in (
      'hunttech_job_candidate',
      'hunttech_iteraction_list',
      'hunttech_candidate_cv',
      'hunttech_social_network_ur_ls',
      'hunttech_job_candidate_position_lists',
      'hunttech_company',
      'hunttech_city',
      'hunttech_position',
      'hunttech_open_position',
      'sys_file'
)
order by relname, indexrelname;

-- Replace :candidate_id manually with an anonymized local UUID.
-- EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
-- select * from hunttech_job_candidate where id = :candidate_id and delete_ts is null;

-- EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
-- select * from hunttech_iteraction_list where candidate_id = :candidate_id and delete_ts is null;

-- EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
-- select * from hunttech_candidate_cv where candidate_id = :candidate_id and delete_ts is null;

-- EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
-- select * from hunttech_company where delete_ts is null order by comany_name;

-- EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
-- select distinct first_name
-- from hunttech_job_candidate
-- where delete_ts is null and lower(first_name) like lower('%sample%')
-- order by first_name;


-- Proposed only. Do not execute automatically.
-- Context: JobCandidateEdit performance audit, 2026-07-13.
-- Production rules:
--   * run only after separate approval;
--   * CREATE INDEX CONCURRENTLY must not run inside a transaction block;
--   * validate on a separate local copy first.

-- P1 if name suggestions remain "lower(name) LIKE '%text%'".
-- Requires explicit approval for extension.
-- create extension if not exists pg_trgm;

-- create index concurrently if not exists idx_hunttech_job_candidate_active_first_name_trgm
-- on hunttech_job_candidate using gin (lower(first_name) gin_trgm_ops)
-- where delete_ts is null;

-- create index concurrently if not exists idx_hunttech_job_candidate_active_second_name_trgm
-- on hunttech_job_candidate using gin (lower(second_name) gin_trgm_ops)
-- where delete_ts is null;

-- create index concurrently if not exists idx_hunttech_job_candidate_active_middle_name_trgm
-- on hunttech_job_candidate using gin (lower(middle_name) gin_trgm_ops)
-- where delete_ts is null and middle_name is not null;

-- P2: only if comments query does not use the existing index in a real trace.
-- Local DB already has idx_hunttech_iteraction_list_active_comments.
-- create index concurrently if not exists idx_hunttech_iteraction_list_active_candidate_comment_date
-- on hunttech_iteraction_list (candidate_id, date_iteraction desc, id)
-- where delete_ts is null and comment_ is not null and comment_ <> '';

-- P2: only if company lookup remains full-list and current idx_company_name is insufficient.
-- Prefer UI change to search lookup before adding duplicate indexes.
-- create index concurrently if not exists idx_hunttech_company_active_name_id
-- on hunttech_company (comany_name, id)
-- where delete_ts is null;


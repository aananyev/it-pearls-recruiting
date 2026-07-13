-- Rollback for proposed indexes only.
-- Do not execute automatically.
-- DROP INDEX CONCURRENTLY must not run inside a transaction block.

-- drop index concurrently if exists idx_hunttech_job_candidate_active_first_name_trgm;
-- drop index concurrently if exists idx_hunttech_job_candidate_active_second_name_trgm;
-- drop index concurrently if exists idx_hunttech_job_candidate_active_middle_name_trgm;
-- drop index concurrently if exists idx_hunttech_iteraction_list_active_candidate_comment_date;
-- drop index concurrently if exists idx_hunttech_company_active_name_id;


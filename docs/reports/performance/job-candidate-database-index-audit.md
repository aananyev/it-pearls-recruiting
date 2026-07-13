# JobCandidate Database Index Audit

Дата: 2026-07-13.

## JobCandidate Table

Table: `hunttech_job_candidate`.

Entity table: `HUNTTECH_JOB_CANDIDATE`.

Local size:

| Table | Total | Heap | Indexes | TOAST |
| --- | ---: | ---: | ---: | ---: |
| `hunttech_job_candidate` | 12 MB | 5008 kB | 6872 kB | 8192 bytes |

Rows: 11549 total, 11444 active.

## Related Table Sizes

| Table | Total size | Notes |
| --- | ---: | --- |
| `hunttech_candidate_cv` | 91 MB | largest; CV text/byte fields |
| `hunttech_iteraction_list` | 78 MB | largest index footprint |
| `hunttech_open_position` | 16 MB | lookup list for comments |
| `hunttech_job_candidate` | 12 MB | root |
| `hunttech_company` | 4104 kB | full lookup loaded on tab |
| `sys_file` | 2840 kB | file descriptors |

## Existing Useful Indexes

The following proposed/previous performance indexes already exist locally:

- `idx_hunttech_job_candidate_active_name`
- `idx_hunttech_job_candidate_active_created_name`
- `idx_hunttech_candidate_cv_active_candidate_date`
- `idx_hunttech_iteraction_list_active_candidate_number`
- `idx_hunttech_iteraction_list_active_comments`
- `idx_hunttech_job_candidate_sign_icon_active_sign_candidate`
- `idx_hunttech_employee_active_job_candidate_status`

## FK Coverage

Important FK columns used by `JobCandidateEdit` already have indexes in local DB:

- `hunttech_job_candidate.current_company_id`
- `hunttech_job_candidate.person_position_id`
- `hunttech_job_candidate.city_of_residence_id`
- `hunttech_job_candidate.file_image_face`
- `hunttech_iteraction_list.candidate_id`
- `hunttech_candidate_cv.candidate_id`
- `hunttech_social_network_ur_ls.job_candidate_id`
- `hunttech_job_candidate_position_lists.job_candidate_id`

## Suspicious / Review Later

Do not drop anything at this stage. Candidates for later review only:

- Several older `candidate_cv` indexes overlap with newer candidate/date indexes.
- `idx_hunttech_job_candidate_active_*` showed `idx_scan=0` in current local stats, but local stats can be reset and workload may not represent production.
- `idx_company_name` is heavily used and supports company lookup order.

## Missing Useful Indexes

No P0 missing FK index was proven for the measured local scenario. Potential candidates are in proposed SQL, but they require validation after creating on a separate local copy.

## Stats

`pg_stat_statements` is not installed locally, so historic SQL frequency is unavailable.


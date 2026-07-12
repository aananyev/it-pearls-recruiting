# Unresolved differences

Дата: 2026-07-10

## Blocking

| ID | Difference | Status | Required decision |
|---|---|---|---|
| UD-001 | 17 `itpearls_job_candidate.current_company_id` values reference missing `itpearls_company.id` rows | Blocking | Decide approved repair/mapping rule before migration |

## Pending manual decisions

| ID | Difference | Status | Required decision |
|---|---|---|---|
| UD-002 | Empty legacy link table `itpearls_job_candidate_position_link__u59616` absent in target model | Pending | Exclude only if repeated pre-cutover count is 0 |
| UD-003 | Empty legacy link table `itpearls_open_position_city_link__u70664` absent in target model | Pending | Exclude only if repeated pre-cutover count is 0 |
| UD-004 | `vacancy_prompt_template.temperature` default `0.7` | Pending | Preserve default in target DB |
| UD-005 | `sec_remember_me` sessions | Pending | Preserve or invalidate sessions during cutover |
| UD-006 | User SMTP/IMAP/POP3/API-key fields | Pending | Preserve values without exposing secrets |

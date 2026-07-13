# JobCandidateEdit EXPLAIN Analysis

Дата: 2026-07-13.

Candidate values are anonymized. Plans were run on local database `hunttech` only.

## Plans

| Query | Plan | Rows | Time | Buffers | Finding |
| --- | --- | ---: | ---: | ---: | --- |
| candidate by PK | Index Scan `hunttech_job_candidate_pkey` | 1 | 0.128 ms | hit=3 | OK |
| interactions by candidate | Bitmap Heap Scan via `idx_hunttech_iteraction_list_candidate_vacancy_date` | 70 | 0.557 ms | hit=51 | OK |
| CV by candidate | Bitmap Heap Scan via `idx_hunttech_candidate_c_v_candidate_date` | 14 | 1.205 ms | hit=12 read=1 | OK plan; wide rows |
| last projects aggregate | Index Only Scan `idx_hunttech_iteraction_list_candidate_vacancy_date` + sort | 23 | 0.280 ms | hit=8 | OK |
| company lookup | Index Scan `idx_company_name` | 5623 | 7.600 ms | hit=5414 | DB acceptable, UI/options heavy |
| city lookup | Seq Scan + sort | 292 | 3.189 ms | hit=10 | table small |
| first-name suggestion `%ив%` | Seq Scan + sort/unique | 272 | 17.371 ms | hit=626 | expected with leading wildcard |

## Slowest Observed SQL

1. `lower(first_name) like '%ив%'`: 17.371 ms.
2. full active company lookup: 7.600 ms.
3. full city lookup: 3.189 ms.
4. candidate CV collection: 1.205 ms.
5. interactions collection: 0.557 ms.

## Estimation Quality

The tested plans have acceptable row estimates for candidate-bound queries. The suggestion query estimates 92 rows and returns 272 rows, but the core issue is the leading wildcard expression, not stale stats alone.

## Sequential Scans

- `hunttech_city`: acceptable due small table.
- `hunttech_job_candidate` suggestion searches: potentially expensive on larger data because `lower(column) LIKE '%...%'` cannot use a normal btree index.

## Sorts

- last projects sort: small in-memory quicksort.
- city lookup sort: small in-memory quicksort.
- suggestion sort: small in-memory quicksort locally, but scales with matched rows and seq scan.


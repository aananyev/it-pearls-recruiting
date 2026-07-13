# JobCandidate Performance Audit Summary

Дата: 2026-07-13.

## Summary

Read-only audit completed on local DB `hunttech`. Production was not accessed, modified, analyzed, reindexed, vacuumed, or restarted. No index was created.

## Required Facts

| Item | Value |
| --- | --- |
| JobCandidate table | `HUNTTECH_JOB_CANDIDATE` / `hunttech_job_candidate` |
| Rows | 11549 total, 11444 active |
| Size | 12 MB total, 5008 kB heap, 6872 kB indexes |
| Existing web-tier open time | 110138 microseconds |
| Measured tabCandidate SQL | company/city/position lookup and suggestion plans captured |
| SQL count | web perf test: 11 `loadList`, 3 `loadValues`; full Hibernate SQL trace not enabled |
| Slowest observed SQL | name suggestion seq scan: 17.371 ms |
| N+1 found | no proven active N+1 in measured path; broad eager collection loading exists |
| Heaviest fetch plan | inline `jobCandidateDc` view with CVs, interactions, social networks, position list |
| Heaviest tabCandidate load | full company options: 5623 rows |

## Verdict

`ОСНОВНАЯ ПРОБЛЕМА НАЙДЕНА В ПРИКЛАДНОЙ ЗАГРУЗКЕ, А НЕ В ИНДЕКСАХ`

## Reports

- `docs/reports/performance/job-candidate-edit-baseline.md`
- `docs/reports/performance/job-candidate-edit-loading-map.md`
- `docs/reports/performance/job-candidate-edit-sql-inventory.md`
- `docs/reports/performance/job-candidate-edit-explain-analysis.md`
- `docs/reports/performance/job-candidate-database-index-audit.md`
- `docs/reports/performance/job-candidate-performance-recommendations.md`
- `docs/reports/performance/job-candidate-performance-audit-summary.md`

## Deployment Artifacts

- `deployment/performance/job-candidate/proposed-indexes.sql`
- `deployment/performance/job-candidate/rollback-indexes.sql`
- `deployment/performance/job-candidate/index-validation.sql`
- `deployment/performance/job-candidate/test-plan.md`


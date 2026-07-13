# JobCandidate Performance Test Plan

Дата: 2026-07-13.

## Index Validation

1. Restore a fresh local copy of DB and fileStorage.
2. Capture baseline SQL plans from `index-validation.sql`.
3. Create proposed indexes on a separate local copy only.
4. Run `ANALYZE` locally after index creation.
5. Repeat `EXPLAIN (ANALYZE, BUFFERS, VERBOSE)`.
6. Compare planning time, execution time, actual rows, buffers, and scan type.
7. Do not promote to production unless a concrete query changes plan or timing materially.

## UI Validation

Use three anonymized candidates:

| Scenario | Candidate type | Metric |
| --- | --- | --- |
| Small | few links, if present in dataset | open time, tabCandidate time |
| Typical | median links | open time, tabCandidate time |
| Heavy | many interactions/CVs/social rows | open time, tabCandidate time |

## Measurements

| Metric | How |
| --- | --- |
| screen build/open time | web perf test or temporary debug logging |
| `tabCandidate` first switch | controlled debug logging around selected-tab listener |
| SQL count | SQL logging or `pg_stat_statements` if enabled |
| N+1 | repeated SQL grouping |
| lookup row counts | loader post-load diagnostics or SQL counts |

## Regression Checks

- Existing candidate opens.
- New candidate opens.
- Save does not lose contacts/CV/interactions.
- Switching tabs does not reload dictionaries repeatedly.
- Company/city/position selection still works.
- Comments tab still loads comments once.

## Result Table

| Scenario | Before | After | Change | Result |
| --- | ---: | ---: | ---: | --- |
| open existing candidate | TBD | TBD | TBD | TBD |
| first switch to `tabCandidate` | TBD | TBD | TBD | TBD |
| company lookup SQL | 7.600 ms | TBD | TBD | TBD |
| first-name suggestion | 17.371 ms | TBD | TBD | TBD |


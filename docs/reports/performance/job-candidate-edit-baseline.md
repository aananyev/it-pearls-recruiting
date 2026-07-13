# JobCandidateEdit Performance Baseline

Дата: 2026-07-13.

## Scope

Read-only аудит локальной базы `hunttech` и текущей реализации `JobCandidateEdit`. Production не использовалась и не изменялась.

## Scenario

Entry point: `hunttech_JobCandidate.browse` -> edit existing `JobCandidate`.

Screen: `hunttech_JobCandidate.edit`.

Main `TabSheet`: `tabSheetSocialNetworks`, `lazy="true"`.

Initial tab: `jobCandidateCard`. Slow tab: `tabCandidate` ("Кандидат").

Lazy behavior: controller registers selected-tab listener in `onInit`; `tabCandidate` calls `ensureReferenceLoadersLoaded()` only on first selection.

## Local Dataset

| Table | Total | Active |
| --- | ---: | ---: |
| `hunttech_job_candidate` | 11549 | 11444 |
| `hunttech_iteraction_list` | 68688 | 67293 |
| `hunttech_candidate_cv` | 8148 | 8139 |
| `hunttech_social_network_ur_ls` | 78001 | 77416 |
| `hunttech_company` | 5709 | 5623 |
| `hunttech_open_position` | 4336 | 4253 |
| `hunttech_city` | 378 | 292 |
| `hunttech_position` | 226 | 216 |
| `sys_file` | 13458 | 13458 |

## Candidate Categories

Personal data was not exported. Candidate references are shortened UUIDs.

| Category | Candidate ref | Interactions | CVs | Social URLs | Positions | Total links |
| --- | --- | ---: | ---: | ---: | ---: | ---: |
| Heavy local candidate | `2ab70c2d…` | 70 | 14 | 15 | 0 | 99 |
| Typical high local candidate | `ee1cd239…` | 72 | 14 | 9 | 0 | 95 |
| Medium local candidate | `e675af0c…` | 66 | 12 | 9 | 0 | 87 |

The local dataset did not provide a truly small candidate; even the lowest linked candidates in the current sample have many interactions/social rows.

## Measured Baseline

Existing web-tier perf test:

```text
PERF_RESULT JobCandidateEdit openMicros=110138
load=0 loadList=11 getCount=0 loadValues=3
jobCandidateLoad=0 jobCandidateLoadList=1 jobCandidateGetCount=0
```

This test uses web-tier `DataService` mocks and is useful for regression shape, not for real SQL timing.

## SQL Baseline Highlights

| Operation | Rows | Execution time | Notes |
| --- | ---: | ---: | --- |
| Load `JobCandidate` by PK | 1 | 0.128 ms | PK index scan |
| Load interactions for heavy candidate | 70 | 0.557 ms | Bitmap index scan |
| Load CVs for heavy candidate | 14 | 1.205 ms | Bitmap index scan; wide LOB/TOAST risk |
| Last projects aggregate | 23 | 0.280 ms | Index-only scan + small sort |
| Load all companies for lookup | 5623 | 7.600 ms | Full active company list, wide rows |
| Load all cities for lookup | 292 | 3.189 ms | Seq scan + sort; small table |
| First-name suggestion `%ив%` | 272 | 17.371 ms | Seq scan; expected with leading wildcard |

## Baseline Conclusion

The most likely current bottleneck of tab `tabCandidate` is application loading/rendering, especially full lookup options. The DB query for `Company` is not slow by itself, but it returns 5623 wide rows and then Generic UI must build options for a `LookupPickerField`.


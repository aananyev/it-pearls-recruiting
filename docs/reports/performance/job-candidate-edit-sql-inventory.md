# JobCandidateEdit SQL Inventory

Дата: 2026-07-13.

## Method

Read-only local SQL inventory based on screen XML loaders, controller calls, current local DB, and existing perf test output. Full Hibernate SQL tracing was not enabled in code because this audit stage forbids durable diagnostic instrumentation.

## Query Groups

### 1. Root Candidate

```sql
select * from hunttech_job_candidate
where id = :candidate_id and delete_ts is null;
```

Rows: 1. Plan: PK index scan.

### 2. Root Collections From Inline View

```sql
select * from hunttech_iteraction_list
where candidate_id = :candidate_id and delete_ts is null;
```

Heavy local candidate: 70 rows.

```sql
select * from hunttech_candidate_cv
where candidate_id = :candidate_id and delete_ts is null;
```

Heavy local candidate: 14 rows. This table has large TOAST data (`text_cv`, byte arrays), and the screen view includes `_local`.

```sql
select * from hunttech_social_network_ur_ls
where job_candidate_id = :candidate_id and delete_ts is null;
```

Heavy local candidate: 15 rows.

### 3. First Card

```sql
select vacancy_id, max(date_iteraction)
from hunttech_iteraction_list
where candidate_id = :candidate_id and delete_ts is null and vacancy_id is not null
group by vacancy_id
order by max(date_iteraction) desc;
```

Heavy local candidate: 23 grouped rows.

### 4. Tab Candidate Lookups

```sql
select * from hunttech_company
where delete_ts is null
order by comany_name;
```

Local rows: 5623. This is the largest tab-candidate lookup.

```sql
select * from hunttech_city
where delete_ts is null
order by city_ru_name;
```

Local rows: 292.

```sql
select * from hunttech_position
where delete_ts is null and position_ru_name not like '%(не использовать)%'
order by position_ru_name;
```

Local rows: 216.

### 5. Suggestion Fields

```sql
select distinct first_name
from hunttech_job_candidate
where delete_ts is null
  and lower(first_name) like lower(:pattern)
order by first_name;
```

For `%ив%`: 272 rows, seq scan, 17.371 ms.

The same pattern exists for `second_name` and `middle_name`.

### 6. Comments Tab

```sql
select *
from hunttech_iteraction_list
where candidate_id = :candidate_id
  and delete_ts is null
  and comment_ is not null
  and comment_ <> ''
order by date_iteraction desc;
```

Covered by existing `idx_hunttech_iteraction_list_active_comments`.

## Repetition Risk

- `currentCompaniesLc`, `citiesDl`, and `personPositionsLc` are protected by `referenceLoadersInitialized`.
- `openPositionDl` is protected by `openPositionLoaderInitialized`.
- `interactionCommentDl` is protected by `commentsTabInitialized`.
- Root collection loading still happens as part of `jobCandidateDl` before tab-specific lazy loading can help.


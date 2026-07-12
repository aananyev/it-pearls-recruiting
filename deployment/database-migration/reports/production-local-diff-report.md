# Production/local diff report

Дата: 2026-07-10

## Summary

Production `itpearls` matches local `itpearls` at audited column-metadata level after normalization.

Production differs from local `hunttech` only in:

- two empty legacy link tables;
- `vacancy_prompt_template.temperature` default value.

## Column metadata counts

| Source | Normalized column metadata count |
|---|---:|
| production `itpearls` | 2129 |
| local `itpearls` | 2129 |
| local `hunttech` | 2125 |

## Production vs local `itpearls`

- production-only columns: 0
- local-only columns: 0

## Production vs local `hunttech`

Production-only after normalization:

- `hunttech_job_candidate_position_link__u59616.job_candidate_id`
- `hunttech_job_candidate_position_link__u59616.position_id`
- `hunttech_open_position_city_link__u70664.cities_list_id`
- `hunttech_open_position_city_link__u70664.city_id`
- `hunttech_vacancy_prompt_template.temperature default 0.7`

Local `hunttech` only:

- `hunttech_vacancy_prompt_template.temperature` without default

## Manual decisions

1. Confirm that empty legacy link tables must not be migrated to new `hunttech`.
2. Decide whether target `hunttech_vacancy_prompt_template.temperature` must keep default `0.7`.

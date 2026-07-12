# Production/local schema diff

Дата: 2026-07-10

## Compared sources

Compared by metadata only, no row contents:

- production `itpearls`;
- local `itpearls`;
- local `hunttech`.

Names were normalized only analytically:

- `itpearls_* -> hunttech_*`

This normalization is not migration proof by itself.

## Production vs local `itpearls`

After prefix normalization:

- production columns: 2129
- local `itpearls` columns: 2129
- production-only columns: 0
- local-only columns: 0

Conclusion: production `itpearls` matches local `itpearls` structurally at column level for audited metadata.

## Production vs local `hunttech`

After prefix normalization:

- production columns: 2129
- local `hunttech` columns: 2125
- production-only metadata differences: 5
- local-only metadata differences: 1

Differences:

| Production object | Local legacy object | New object | Type | Transfer approach | Risk | Manual decision |
|---|---|---|---|---|---|---|
| `itpearls_job_candidate_position_link__u59616.job_candidate_id` | same table in local `itpearls` | absent in local `hunttech` | legacy table column | do not migrate by default; table is empty | low, but structural | yes |
| `itpearls_job_candidate_position_link__u59616.position_id` | same table in local `itpearls` | absent in local `hunttech` | legacy table column | do not migrate by default; table is empty | low, but structural | yes |
| `itpearls_open_position_city_link__u70664.cities_list_id` | same table in local `itpearls` | absent in local `hunttech` | legacy table column | do not migrate by default; table is empty | low, but structural | yes |
| `itpearls_open_position_city_link__u70664.city_id` | same table in local `itpearls` | absent in local `hunttech` | legacy table column | do not migrate by default; table is empty | low, but structural | yes |
| `itpearls_vacancy_prompt_template.temperature default 0.7` | default `0.7` | default absent | changed default | either set default in target or ensure app writes value | medium | yes |

## Summary

Production is structurally aligned with local `itpearls`. Migration design can focus on controlled prefix/model transition to local `hunttech`, preserving security and system tables.

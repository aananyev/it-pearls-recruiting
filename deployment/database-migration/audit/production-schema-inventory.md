# Production schema inventory

Дата: 2026-07-10

## Object counts

| Object type | Count |
|---|---:|
| Tables | 154 |
| Sequences | 2 |
| Views | 0 |
| Materialized views | 0 |
| Large objects | 0 |

## Largest tables

| Table | Estimated rows | Total size |
|---|---:|---:|
| `sys_scheduled_execution` | 19,662,724 | 5972 MB |
| `itpearls_candidate_cv` | 7,752 | 89 MB |
| `sys_fts_queue` | 198,822 | 49 MB |
| `itpearls_iteraction_list` | 66,964 | 48 MB |
| `ddcdi_import_exec_detail` | 4,004 | 23 MB |
| `itpearls_social_network_ur_ls` | 71,899 | 23 MB |
| `itpearls_open_position_news` | 32,372 | 20 MB |
| `report_template` | 39 | 18 MB |
| `sec_session_log` | 28,160 | 16 MB |
| `itpearls_open_position` | 4,051 | 16 MB |

Главный объем базы создается таблицей `sys_scheduled_execution`. Для test restore нужно окружение с большим запасом свободного места под heap, indexes и WAL/temp files.

## Largest indexes

| Index | Table | Size |
|---|---|---:|
| `idx_sys_scheduled_execution_task_finish_time` | `sys_scheduled_execution` | 1221 MB |
| `idx_sys_scheduled_execution_task_start_time` | `sys_scheduled_execution` | 951 MB |
| `sys_scheduled_execution_pkey` | `sys_scheduled_execution` | 925 MB |
| `sys_fts_queue_pkey` | `sys_fts_queue` | 9904 kB |
| `idx_sys_fts_queue_idxhost_crts` | `sys_fts_queue` | 6616 kB |

## Legacy link tables

| Table | Exists | Row count |
|---|---:|---:|
| `itpearls_job_candidate_position_link__u59616` | yes | 0 |
| `itpearls_open_position_city_link__u70664` | yes | 0 |

Эти таблицы пустые на production, но решение об их исключении из будущей миграции все равно должно быть явно утверждено.

## AI changeset objects

| Object | Exists |
|---|---:|
| `itpearls_user_ai_configuration` | yes |
| `itpearls_vacancy_prompt_template` | yes |
| `itpearls_open_position.raw_description` | yes |

## `vacancy_prompt_template.temperature`

Production default:

- table: `itpearls_vacancy_prompt_template`
- column: `temperature`
- default: `0.7`

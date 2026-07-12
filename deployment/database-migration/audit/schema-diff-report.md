# Schema diff report

Дата: 2026-07-10

## Сравненные источники

Фактически сравнены:

- локальная legacy-база `itpearls`;
- локальная новая база `hunttech`;
- структура, ожидаемая текущим CUBA-кодом и update scripts.

Не сравнена:

- production-база на `hr.hunttech.ru`, так как production read-only SQL-аудит еще не выполнен.

## Локальные итоги

| Метрика | `itpearls` | `hunttech` |
|---|---:|---:|
| Base tables | 154 | 152 |
| Views | 0 | 0 |
| Materialized views | 0 | 0 |
| Sequences | 2 | 2 |
| Public functions | 1 | 44 |
| Procedures | 0 | 0 |
| Large objects | 0 | 0 |
| Schemas | `public` | `public` |

## Нормализация имен

Для локального сравнения применялась логическая нормализация:

- `itpearls_*` -> `hunttech_*`
- `ITPEARLS_*` -> `HUNTTECH_*`

Это не является правилом миграции. Это только метод аудита для поиска структурных отличий после брендового переименования.

## Объекты без изменений

После нормализации префиксов основная часть CUBA, BPM, Activiti, report, dashboard, emailtemplates и бизнес-таблиц совпадает по составу колонок.

Критичные CUBA security таблицы присутствуют в обеих локальных базах:

- `sec_user`
- `sec_role`
- `sec_user_role`
- `sec_group`
- `sec_group_hierarchy`
- `sec_permission`
- `sec_constraint`
- `sec_user_setting`
- `sec_remember_me`
- `sec_session_log`

## Новые объекты

В локальной `hunttech` по сравнению с нормализованной `itpearls` новых таблиц не выявлено.

При этом в коде и changelog присутствуют AI-изменения:

- `hunttech_user_ai_configuration`
- `hunttech_vacancy_prompt_template`
- AI-колонки в `hunttech_open_position`
- новые фото-колонки в `sec_user`: `official_photo_id`, `user_avatar_id`

В локальных базах эти объекты уже присутствуют.

## Удаленные или legacy-объекты

В локальной `itpearls` после нормализации есть две таблицы, которых нет в `hunttech`:

- `hunttech_job_candidate_position_link__u59616`
- `hunttech_open_position_city_link__u70664`

Это выглядят как CUBA DropScript/rename legacy tables. Их нельзя автоматически удалять или игнорировать без проверки строк в production.

Требуемые проверки перед миграцией:

- количество строк в этих таблицах на production;
- наличие бизнес-сценариев, которые все еще читают эти связи;
- подтверждение, что данные были перенесены в новые связи или больше не используются.

## Измененные колонки

Обнаружено одно локальное отличие default:

| Таблица | Колонка | `itpearls` | `hunttech` |
|---|---|---|---|
| `hunttech_vacancy_prompt_template` | `temperature` | default `0.7` | default отсутствует |

Риск: новые записи после миграции могут получать `NULL` вместо `0.7`, если приложение не задает значение явно.

## Измененные типы данных

В локальном сравнении после нормализации измененных типов данных не выявлено.

Production должен быть проверен отдельно.

## Измененные связи

В локальной `itpearls` присутствуют FK только у legacy tables:

- `hunttech_job_candidate_position_link__u59616.job_candidate_id -> hunttech_job_candidate.id`
- `hunttech_job_candidate_position_link__u59616.position_id -> hunttech_position.id`
- `hunttech_open_position_city_link__u70664.cities_list_id -> hunttech_open_position.id`
- `hunttech_open_position_city_link__u70664.city_id -> hunttech_city.id`

В `hunttech` этих таблиц и FK нет.

## Индексы

В `hunttech` добавлены performance indexes 2026 года, особенно для:

- `hunttech_candidate_cv`
- `hunttech_employee`
- `hunttech_internal_email_template`
- `hunttech_internal_emailer`
- `hunttech_iteraction`
- `hunttech_iteraction_list`
- `hunttech_job_candidate`
- `hunttech_open_position`
- `hunttech_project`
- `hunttech_user_ai_configuration`
- `hunttech_vacancy_prompt_template`

Часть старых индексов в `itpearls` имеет другое имя, но совпадает по смыслу с новыми индексами.

## Изменения Liquibase и CUBA scripts

Liquibase:

- `modules/core/db/changelog/db.changelog-master.xml`
- `modules/core/db/changelog/260627-1-addAiEntities.xml`

Изменения Liquibase:

- добавлены AI-колонки в `HUNTTECH_OPEN_POSITION`;
- создана `HUNTTECH_USER_AI_CONFIGURATION`;
- создана `HUNTTECH_VACANCY_PROMPT_TEMPLATE`.

CUBA scripts:

- основной поток изменений находится в `modules/core/db/update/postgres/`;
- последние группы скриптов: `24/240325-*`, `26/260627-*`, `26/260629-*`, `26/260701-*`, `26/260704-*`.

Важно: порядок и содержание CUBA scripts должны быть сопоставлены с `SYS_DB_CHANGELOG` production перед любым запуском.

## Классификация отличий

1. Объекты без изменений: большая часть CUBA/system/business tables после нормализации префикса.
2. Новые объекты: локально не выявлены относительно нормализованной `itpearls`, но в коде есть AI-объекты, которые должны быть проверены на production.
3. Удаленные объекты: две legacy link-таблицы с суффиксом `__u...`.
4. Переименованные объекты: большинство business tables фактически отличаются префиксом `itpearls_` vs `hunttech_`; подтверждение должно идти через entity/changelog/data semantics.
5. Измененные колонки: `vacancy_prompt_template.temperature` default.
6. Измененные типы данных: локально не выявлены.
7. Измененные связи: FK legacy link-таблиц.
8. Изменения security-модели: новые фото-колонки в `sec_user`, новые пользовательские AI-настройки.
9. Риски потери данных: legacy link-таблицы, `sec_user` extension columns, `sys_file`, user SMTP/IMAP/POP3 fields, security tables.
10. Невозможно применить автоматически: любые предполагаемые переименования без подтверждения production row counts, FK chains и правил трансформации.

## Блокер

Production schema diff отсутствует. Миграцию начинать нельзя до выполнения read-only production inventory и сравнения с локальной `hunttech`.

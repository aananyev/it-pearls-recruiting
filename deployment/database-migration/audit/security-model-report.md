# Security model report

Дата: 2026-07-10

## Два уровня security

Security HRM HuntTech разделяется на два независимых уровня:

1. PostgreSQL security:
   - роли PostgreSQL;
   - владельцы объектов;
   - database/schema/table/sequence/function privileges;
   - default privileges;
   - membership ролей.

2. CUBA application security:
   - пользователи;
   - группы;
   - роли;
   - permissions;
   - row-level constraints;
   - user settings;
   - sessions;
   - remember-me tokens;
   - audit/entity logs.

Оба уровня должны быть перенесены и проверены отдельно.

## PostgreSQL security - локальные факты

Локально обнаружены роли:

- `postgres`: superuser, createdb, createrole, replication, bypassrls, login.
- `cuba`: superuser, login.
- `alan`: superuser, login.
- `replica`: replication, login.
- `wp_user`: login.

Локальные базы `itpearls` и `hunttech` принадлежат роли `cuba`.

Риски:

- роль `cuba` локально superuser;
- production grants и ownership пока не подтверждены;
- production default privileges пока не подтверждены;
- роль `replica` существует локально и используется в deploy-конфиге, но ее production-права не подтверждены.

## CUBA security tables

Критичные таблицы, которые нельзя потерять:

- `sec_user`
- `sec_role`
- `sec_user_role`
- `sec_group`
- `sec_group_hierarchy`
- `sec_permission`
- `sec_constraint`
- `sec_localized_constraint_msg`
- `sec_user_setting`
- `sec_user_substitution`
- `sec_remember_me`
- `sec_session_log`
- `sec_session_attr`
- `sec_filter`
- `sec_presentation`
- `sec_search_folder`
- `sec_screen_history`
- `sec_entity_log`
- `sec_logged_entity`
- `sec_logged_attr`

Критичные system tables:

- `sys_file`
- `sys_attr_value`
- `sys_config`
- `sys_entity_snapshot`
- `sys_fts_queue`
- `sys_scheduled_task`
- `sys_scheduled_execution`
- `sys_sending_message`
- `sys_sending_attachment`
- `sys_db_changelog`

## ExtUser

`com.company.hunttech.entity.ExtUser` расширяет `com.haulmont.cuba.security.entity.User`.

Физическая таблица: `sec_user`.

Дополнительные поля:

- `image_id` - legacy photo;
- `official_photo_id`;
- `user_avatar_id`;
- `smtp_server`;
- `smtp_port`;
- `smtp_password_required`;
- `smtp_user`;
- `smtp_password`;
- `pop3_server`;
- `pop3_port`;
- `pop3_password_required`;
- `pop3_user`;
- `pop3password`;
- `imap_server`;
- `imap_port`;
- `imap_password_required`;
- `imap_user`;
- `imap_password`;
- `statistics_`;
- `dashboards`.

Эти поля содержат чувствительные пользовательские данные. Их нельзя логировать в открытом виде и нельзя сохранять в Git.

## Критичные FK chains

Security:

- `sec_user.group_id -> sec_group.id`
- `sec_group.parent_id -> sec_group.id`
- `sec_user_role.user_id -> sec_user.id`
- `sec_user_role.role_id -> sec_role.id`
- `sec_permission.role_id -> sec_role.id`
- `sec_constraint.group_id -> sec_group.id`
- `sec_user_setting.user_id -> sec_user.id`
- `sec_remember_me.user_id -> sec_user.id`
- `sec_session_log.user_id -> sec_user.id`
- `sec_session_log.substituted_user_id -> sec_user.id`

HuntTech business links to users:

- `hunttech_recruties_tasks.reacrutier_id -> sec_user.id`
- `hunttech_recruiting_recrutiers.recrutier_name_id -> sec_user.id`
- `hunttech_sign_icons.user_id -> sec_user.id`
- `hunttech_user_settings.user_id -> sec_user.id`
- `hunttech_user_ai_configuration.user_id -> sec_user.id`
- `hunttech_my_active_candidate_exclude.user_id -> sec_user.id`

File links:

- `sec_user.image_id -> sys_file.id`
- `sec_user.official_photo_id -> sys_file.id`
- `sec_user.user_avatar_id -> sys_file.id`
- `hunttech_user_settings.image_id -> sys_file.id`
- `hunttech_recruiting_recrutiers.recrutier_cv_id -> sys_file.id`

## Application roles in code

Код использует строковые роли и группы:

- `Manager`
- `Administrator`
- `Researcher`
- `Стажер`

Также есть `StandartRoles` и сервисы:

- `GetRoleService`
- `GetUserRoleService`

Перед миграцией нужно снять production-выгрузку `sec_role`, `sec_group`, `sec_user_role`, `sec_permission`, `sec_constraint` и проверить, что все строковые роли существуют.

## Sessions and remember-me

Таблицы:

- `sec_session_log`
- `sec_session_attr`
- `sec_remember_me`

Решение о переносе активных токенов нужно принять отдельно:

- переносить `sec_remember_me`, если пользователи не должны перелогиниваться;
- очищать/не переносить сессии только после отдельного решения и коммуникации пользователям.

## Запрещено раскрывать

Нельзя включать в отчеты:

- `sec_user.password`;
- пользовательские SMTP/IMAP/POP3 пароли;
- API keys из `hunttech_user_ai_configuration`;
- токены и remember-me secrets;
- raw dumps.

## Блокер

Production security model не подтверждена. Нужен read-only output скрипта `02-security-model-readonly.sql` с production и staging.

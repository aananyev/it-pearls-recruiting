# First-stage audit summary

Дата: 2026-07-10

## 1. Обнаруженная архитектура

Приложение CUBA Platform `7.3-SNAPSHOT`, Gradle, PostgreSQL. Основной package `com.company.hunttech`, CUBA namespace `hunttech`, persistence unit `hunttech_recruiting`.

Локально обнаружены базы `hunttech` и `itpearls` на PostgreSQL `11.22`, обе в `pg_default`, schema `public`.

Production host `hr.hunttech.ru:5432` принимает подключения, но production database metadata не собрана из-за отсутствия подтвержденного read-only доступа.

## 2. Что означают `itpearls` и `hunttech`

Локально:

- `itpearls` - PostgreSQL database;
- `hunttech` - PostgreSQL database;
- `hunttech` - CUBA namespace и table/entity prefix;
- локально не являются tablespace или schema.

Production: не подтверждено. Конфиги указывают `DB_NAME=HuntTech`, постановка говорит о tablespace `itpearls`. Это блокер до SQL-аудита production.

## 3. Версии PostgreSQL и совместимость

- Local client: `psql 14.20`.
- Local server: `PostgreSQL 11.22`.
- Docker Compose: `postgres:11`.
- Production server version: не установлена.

Совместимость утилит backup/restore должна проверяться: предпочтительно использовать `pg_dump` той же major-версии, что production server, или более новой совместимой версии после restore rehearsal.

## 4. Структурные отличия

Локально после нормализации `itpearls_* -> hunttech_*`:

- `itpearls`: 154 tables.
- `hunttech`: 152 tables.
- extra legacy tables в `itpearls`: `*_job_candidate_position_link__u59616`, `*_open_position_city_link__u70664`.
- одно отличие default: `vacancy_prompt_template.temperature` default `0.7` в `itpearls`, отсутствует в `hunttech`.
- в `hunttech` больше performance indexes.

Production diff не выполнен.

## 5. Изменения Liquibase

Liquibase master включает `260627-1-addAiEntities.xml`.

Изменения:

- AI columns в `HUNTTECH_OPEN_POSITION`;
- `HUNTTECH_USER_AI_CONFIGURATION`;
- `HUNTTECH_VACANCY_PROMPT_TEMPLATE`.

Основной исторический механизм изменений также включает CUBA scripts `modules/core/db/update/postgres/`.

## 6. Security-модель PostgreSQL

Локально роли: `postgres`, `cuba`, `alan`, `replica`, `wp_user`, системные PostgreSQL-роли.

Локально `cuba` является superuser. Это риск и не должно автоматически переноситься на production.

Production roles, grants, ownership и default privileges не подтверждены.

## 7. Security-модель HRM

Критичные CUBA tables:

- `sec_user`, `sec_role`, `sec_user_role`, `sec_group`, `sec_permission`, `sec_constraint`;
- `sec_user_setting`, `sec_remember_me`, `sec_session_log`;
- `sys_file`, `sys_db_changelog`, `sys_entity_snapshot`.

`ExtUser` расширяет `sec_user` и содержит фото, SMTP/IMAP/POP3 поля и пользовательские настройки. Значения секретов нельзя выводить в отчеты.

## 8. Основные риски

- production architecture не подтверждена;
- противоречие `HuntTech` vs `hunttech` vs `itpearls`;
- потеря `sec_*` данных;
- потеря file storage references;
- legacy link tables могут содержать данные;
- post-cutover writes усложняют rollback;
- backup может оказаться невосстановимым без restore rehearsal.

## 9. Предлагаемая архитектура миграции

Старую production-базу не менять. Создать отдельную новую target database, мигрировать туда данные, проверить независимо, затем переключить приложение.

## 10. Предлагаемая архитектура отката

Старая база сохраняется без изменений. До разрешения пользовательской записи в новую базу rollback - переключение datasource обратно на старую базу. После пользовательских записей нужен заранее утвержденный сценарий обработки post-cutover data.

## 11. План резервного копирования

- `pg_dump -Fc` application database;
- `pg_dumpall --globals-only`;
- checksums;
- backup logs;
- test restore в отдельную базу;
- row counts и FK validation;
- хранение вне data directory и вне Git.

## 12. Необходимое окно недоступности

Не может быть оценено без production row counts, размера backup и test migration duration.

До открытия доступа рекрутерам нужна пауза на final validation и smoke tests.

## 13. Список созданных файлов

См. `deployment/database-migration/README.md`. Созданы отчеты, runbooks, risk register, backup strategy и read-only SQL scripts.

## 14. Список команд, которые были выполнены

Выполнялись только read-only inspections и создание текстовых файлов:

- просмотр структуры проекта;
- поиск конфигураций;
- чтение CUBA/Gradle/Liquibase/deploy файлов;
- `pg_isready` для local и production host;
- read-only `SELECT` к локальным PostgreSQL базам;
- локальный синтаксический запуск read-only SQL scripts с выводом в `/tmp`;
- проверка отсутствия DDL/DML в audit SQL scripts;
- обновление `.gitignore` для запрета дампов и raw outputs.

## 15. Подтверждение, что production-база не изменялась

Подтверждаю: на production не выполнялись DDL, DML, migration, backup, restore, `updateDb`, `pg_dump`, `CREATE`, `ALTER`, `DROP`, `INSERT`, `UPDATE`, `DELETE`, `GRANT`, `REVOKE`.

Единственное обращение к production host: `pg_isready -h hr.hunttech.ru -p 5432`.

## 16. Блокирующие вопросы

См. `reports/blocking-questions.md`.

Главные блокеры:

- фактическое production DB name;
- является ли `itpearls` production tablespace;
- production PostgreSQL version;
- read-only credentials;
- production grants/ownership;
- backup location;
- rollback policy для post-cutover writes.

## 17. Вердикт

`МИГРАЦИЮ НАЧИНАТЬ НЕЛЬЗЯ`

Готовность только к следующему безопасному шагу: выполнить read-only production audit и restore rehearsal backup.

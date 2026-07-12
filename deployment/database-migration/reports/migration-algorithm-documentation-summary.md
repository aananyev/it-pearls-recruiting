# Итоговый отчет по фиксации воспроизводимого алгоритма production-миграции HRM HuntTech

Дата: 2026-07-12

## 1. Назначение

Подготовлена документация и конфигурационные шаблоны для воспроизводимой production-миграции HRM HuntTech из базы `itpearls` в базу `hunttech`.

Фактическая production-миграция в рамках этой задачи не выполнялась. Production-база, production-приложение, datasource и другие базы на сервере `hr.hunttech.ru` не изменялись.

## 2. Изученные материалы

Изучены и использованы материалы первого и второго этапов:

- `deployment/database-migration/reports/first-stage-audit-summary.md`;
- `deployment/database-migration/reports/blocking-questions.md`;
- `deployment/database-migration/audit/architecture-audit.md`;
- `deployment/database-migration/audit/schema-diff-report.md`;
- `deployment/database-migration/audit/security-model-report.md`;
- `deployment/database-migration/runbooks/migration-plan.md`;
- `deployment/database-migration/runbooks/rollback-plan.md`;
- `deployment/database-migration/reports/risk-register.md`;
- `deployment/database-migration/reports/second-stage-summary.md`;
- `deployment/database-migration/reports/production-backup-report.md`;
- `deployment/database-migration/reports/test-restore-report.md`;
- `deployment/database-migration/reports/local-itpearls-to-hunttech-migration-report-2026-07-11.md`;
- `deployment/database-migration/reports/local-dashboard-widgets-fix-2026-07-12.md`;
- `deployment/database-migration/validation/pre-migration-checklist.md`;
- `deployment/database-migration/validation/post-migration-checklist.md`;
- `deployment/database-migration/validation/application-smoke-test.md`;
- SQL-скрипты из `deployment/database-migration/audit/sql/`.

## 3. Зафиксированные факты

- Production PostgreSQL: `11.22`.
- Production database: `itpearls`.
- Target database для миграции: `hunttech`, требует финального письменного утверждения перед production.
- Production schema: `public`.
- Production tablespaces: `pg_default`, `pg_global`.
- Tablespace `itpearls` на production не обнаружен.
- Tablespace `hunttech` на production не обнаружен и не должен создаваться без отдельного решения.
- Production owner и technical user приложения: `cuba`.
- Количество production-таблиц: 154.
- Локальная таблица соответствия покрывает 154 source tables.
- Две legacy link-таблицы подтверждены пустыми на этапе аудита, но требуют повторной проверки после `STOP_WRITES`.
- Для `vacancy_prompt_template.temperature` зафиксировано правило: переносить существующие значения и `NULL` без изменения, target default установить `0.7`.
- Для `sec_user.dtype` зафиксировано правило `itpearls_ExtUser` -> `hunttech_ExtUser`.
- Для dashboard/widget models зафиксировано правило замены namespace `itpearls` -> `hunttech`.
- Для 17 orphan `job_candidate.current_company_id` локально проверен вариант placeholder-компаний с quarantine-журналом; production требует письменного бизнес-решения.

## 4. Что было реально проверено

- Read-only production audit по ранее подготовленным SQL-скриптам.
- Production backup на уровне создания файла, `pg_restore --list`, размера и SHA-256.
- Повторное тестовое восстановление выявило ошибку FK из-за missing `itpearls_company` для `job_candidate.current_company_id`.
- Локальная миграция `itpearls` -> `hunttech` с учетом измененной логики проекта.
- Локальное создание target database `hunttech`.
- Перенос таблиц и данных с заменой префиксов.
- Переименование constraints/indexes/sequences под `hunttech`.
- Локальное добавление 17 placeholder-компаний и запись в quarantine.
- Локальная проверка security tables, `sys_file`, `sys_db_changelog`, sequences и FK.
- Локальное исправление dashboard/widget models после миграции.
- Локальный старт приложения на базе `hunttech`.

## 5. Что не было проверено в production

- Полная production-миграция `itpearls` -> `hunttech`.
- Production cutover datasource.
- Production start новой версии приложения.
- Полное успешное восстановление production backup в чистом изолированном окружении без ошибок.
- Полная проверка физического файлового хранилища `sys_file`.
- Production smoke test с реальными пользователями.
- Rollback после появления пользовательских записей в target database.
- Replay delta после rollback-сценария.

## 6. Созданные файлы

- `deployment/database-migration/runbooks/HRM-HuntTech-production-migration-human-runbook.md`;
- `deployment/database-migration/runbooks/HRM-HuntTech-production-migration-AI-algorithm.md`;
- `deployment/database-migration/config/migration-manifest.yaml`;
- `deployment/database-migration/config/table-migration-mapping.yaml`;
- `deployment/database-migration/reports/migration-decisions-register.md`;
- `deployment/database-migration/reports/migration-algorithm-documentation-summary.md`.

Также сохранен структурированный пользовательский промпт в документации проекта:

- `/Users/alekseyananyev/Documents/Hunttech HRM/Рефакторинг/Промпты/База данных/Production документация/2026-07-12 Фиксация воспроизводимого алгоритма production миграции HRM HuntTech.md`.

## 7. Human runbook

Human runbook содержит:

- роли и зоны ответственности;
- предварительные условия;
- проверку места на диске;
- подготовку production;
- остановку записи;
- финальный backup;
- создание target database;
- создание новой схемы;
- перенос данных;
- правила table mapping;
- обработку legacy link-таблиц;
- обработку `vacancy_prompt_template.temperature`;
- обработку security tables;
- обработку `sys_file` и файлового хранилища;
- validation;
- application smoke test;
- cutover;
- post-cutover monitoring;
- rollback до пользовательских записей;
- rollback после пользовательских записей;
- критерии успеха;
- критерии остановки;
- формат итогового отчета;
- команды;
- checklists.

## 8. AI algorithm

AI algorithm описывает воспроизводимый state machine с 33 состояниями:

- `STATE 00` - `STATE 27` для основной миграции;
- `STATE R1` - `STATE R5` для rollback-сценариев.

Для каждого состояния указаны цель, предусловия, read-only проверки, разрешенные действия, запрещенные действия, ожидаемые артефакты, условия успеха, условия отказа и переход к следующему состоянию.

## 9. Migration manifest

`migration-manifest.yaml` фиксирует:

- source database и target database;
- запрет in-place migration;
- запрет изменения source database;
- запрет wildcard-операций по другим базам;
- backup requirements;
- file storage requirements;
- validation requirements;
- rollback rules;
- known data rules.

Manifest является шаблоном. Перед production-запуском должны быть заполнены значения, которые зависят от конкретного окна миграции: `migration.id`, `approved_git_commit`, `backup.directory`, `minimum_free_space_gb`, `application.service`, `datasource_config`, `file_storage.source_path`, `file_storage.target_path`.

## 10. Table migration mapping

`table-migration-mapping.yaml` содержит 154 source tables.

Для каждой таблицы зафиксированы:

- source table;
- target table;
- migration action;
- dependency handling;
- primary key;
- foreign key dependencies;
- column mapping;
- правила checksum/count validation;
- необходимость ручного решения.

Правила, требующие human approval перед production:

- исключение двух пустых legacy link-таблиц после повторной проверки;
- создание placeholder-компаний для 17 orphan `current_company_id`;
- применение default `temperature=0.7`;
- подтверждение file storage path;
- решение по rollback после пользовательских записей.

## 11. Security

Документы требуют переносить и валидировать:

- PostgreSQL owner/roles/grants без восстановления globals поверх существующего кластера без review;
- `sec_user`;
- `sec_role`;
- `sec_user_role`;
- `sec_group`;
- `sec_group_hierarchy`;
- `sec_permission`;
- `sec_constraint`;
- `sec_session_attr`;
- `sec_user_setting`;
- `sec_remember_me`;
- audit/session tables;
- `sys_db_changelog`;
- `sys_file`.

Секреты, пароли, токены, email и персональные данные в новые документы не добавлялись.

## 12. Защита других баз данных

Требование защиты других баз на `hr.hunttech.ru` внесено в:

- `migration-manifest.yaml`;
- `migration-decisions-register.md`;
- human runbook;
- AI algorithm.

Запрещены:

- wildcard database operations;
- cluster-wide `DROP`;
- неутвержденное восстановление globals;
- изменение shared roles/grants/tablespaces без review;
- PostgreSQL restart без отдельного runbook;
- действия с базами, отличными от exact source `itpearls` и exact target `hunttech`.

## 13. Rollback

Зафиксированы два принципиально разных rollback-сценария:

- до пользовательских записей в target database: возврат datasource на старую `itpearls` возможен после проверки отсутствия target writes;
- после пользовательских записей в target database: автоматический rollback запрещен, требуется freeze, delta analysis и письменное human decision.

Старая база `itpearls` должна сохраняться неизменной до отдельного решения о завершении retention period.

## 14. Блокирующие вопросы

До production-миграции нельзя продолжать без решений:

- подтвердить точное имя target database;
- подтвердить production file storage path и процедуру копирования;
- подтвердить правило для 17 orphan `current_company_id`;
- повторно проверить пустоту двух legacy link-таблиц после `STOP_WRITES`;
- утвердить обработку `vacancy_prompt_template.temperature`;
- утвердить режим `cuba.automaticDatabaseUpdate`;
- утвердить процедуру отключения scheduled jobs/outbound integrations на время smoke test;
- успешно выполнить full restore backup в чистом изолированном окружении;
- получить письменное решение по rollback после пользовательских записей.

## 15. Проверки перед коммитом

Плановые проверки:

- отсутствие dump/backup/raw production exports в Git;
- отсутствие `.env`, `.pgpass`, production credentials и логов с данными;
- проверка количества таблиц в mapping;
- проверка количества состояний AI algorithm;
- проверка `.gitignore`;
- `git diff --check`;
- просмотр `git status`;
- подготовка commit message `Document reproducible HRM database migration workflow`;
- `git push` не выполняется.

## 16. Production impact

В рамках этой задачи:

- production database не изменялась;
- production schema не изменялась;
- production datasource не изменялся;
- production application не останавливалось;
- production target database не создавалась;
- production migration не запускалась;
- другие базы на `hr.hunttech.ru` не затрагивались.

## 17. Вердикт

`АЛГОРИТМ ЗАФИКСИРОВАН, НО ТРЕБУЕТ УСТРАНЕНИЯ БЛОКЕРОВ`

Переход к production migration scripts и реальной production-миграции запрещен до закрытия блокирующих вопросов и отдельного задания.

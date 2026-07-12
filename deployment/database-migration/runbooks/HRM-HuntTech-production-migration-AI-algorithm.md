# Формализованный алгоритм автоматизированной миграции HRM HuntTech

Дата: 2026-07-12
Назначение: строгий конечный автомат для Codex/AI-агента перед production-миграцией.
Статус: production-миграция запрещена до закрытия блокеров и human approval.

Общие запреты для всех состояний:

- не импровизировать на production;
- не выполнять migration in place;
- не изменять source database `itpearls`;
- не удалять source database;
- не затрагивать другие базы на `hr.hunttech.ru`;
- не выполнять wildcard database operations;
- не восстанавливать globals в production cluster без отдельного review;
- не использовать `|| true`;
- не скрывать ошибки;
- не выводить секреты;
- не помещать dump/raw data/logs with secrets в Git;
- не выполнять `git push`;
- останавливать процесс при первой необъясненной ошибке.

## STATE 00 - LOAD_PROJECT_CONTEXT

PURPOSE: загрузить проект и migration-документы.
PRECONDITIONS: доступ к `/Users/alekseyananyev/StudioProjects/hunttech_recruiting`.
READ-ONLY CHECKS: определить repo root, branch, commit, migration files.
ALLOWED ACTIONS: читать Git metadata, `deployment/database-migration/`, manifest.
FORBIDDEN ACTIONS: изменять production, создавать DB.
COMMANDS OR SCRIPTS: `git rev-parse --show-toplevel`, `git status --short`, `find deployment/database-migration -type f`.
EXPECTED OUTPUT: root, branch, commit, file inventory.
VALIDATION: manifest и runbooks существуют.
SUCCESS CONDITION: контекст загружен.
FAILURE CONDITION: repo не найден или manifest отсутствует.
ROLLBACK OR SAFE STOP: остановиться.
ARTIFACTS: context log.
NEXT STATE: STATE 01.

## STATE 01 - VERIFY_EXECUTION_MODE

PURPOSE: определить режим.
PRECONDITIONS: manifest загружен.
READ-ONLY CHECKS: прочитать `migration.execution_mode`.
ALLOWED ACTIONS: проверить режим `LOCAL_TEST`, `TEST_RESTORE`, `PRODUCTION_DRY_RUN`, `PRODUCTION_BACKUP`, `PRODUCTION_MIGRATION`, `PRODUCTION_ROLLBACK`.
FORBIDDEN ACTIONS: любое production-действие без явного production mode.
COMMANDS OR SCRIPTS: parse `config/migration-manifest.yaml`.
EXPECTED OUTPUT: explicit mode.
VALIDATION: mode входит в разрешенный список.
SUCCESS CONDITION: mode валиден.
FAILURE CONDITION: mode пустой или неразрешенный.
ROLLBACK OR SAFE STOP: остановиться.
ARTIFACTS: mode log.
NEXT STATE: STATE 02.

## STATE 02 - VERIFY_HOST_IDENTITY

PURPOSE: исключить выполнение на неверном сервере/базе.
PRECONDITIONS: mode валиден.
READ-ONLY CHECKS: hostname, IP, PostgreSQL host/port, database, service path.
ALLOWED ACTIONS: read-only probes.
FORBIDDEN ACTIONS: DDL/DML.
COMMANDS OR SCRIPTS: `hostname`, `pg_isready`, `select current_database(), version()`.
EXPECTED OUTPUT: `hr.hunttech.ru`, PostgreSQL `11.22`, source `itpearls` или target `hunttech` по этапу.
VALIDATION: значения совпадают с manifest.
SUCCESS CONDITION: identity подтверждена.
FAILURE CONDITION: любое несовпадение.
ROLLBACK OR SAFE STOP: остановиться без изменений.
ARTIFACTS: host identity log.
NEXT STATE: STATE 03.

## STATE 03 - VERIFY_GIT_AND_SCRIPT_INTEGRITY

PURPOSE: доказать, что выполняются утвержденные скрипты.
PRECONDITIONS: host verified.
READ-ONLY CHECKS: branch, commit, working tree, script SHA-256, secrets scan, dump scan.
ALLOWED ACTIONS: read-only Git и filesystem checks.
FORBIDDEN ACTIONS: `git push`, auto-format unrelated files.
COMMANDS OR SCRIPTS: `git status`, `git diff --check`, `sha256sum deployment/database-migration/**/*`.
EXPECTED OUTPUT: clean/approved tree, no secrets/dumps.
VALIDATION: commit equals manifest approved commit.
SUCCESS CONDITION: scripts trusted.
FAILURE CONDITION: unapproved diff or secret.
ROLLBACK OR SAFE STOP: остановиться.
ARTIFACTS: integrity report.
NEXT STATE: STATE 04.

## STATE 04 - VERIFY_RESOURCES

PURPOSE: проверить ресурсы.
PRECONDITIONS: scripts trusted.
READ-ONLY CHECKS: disk, inodes, RAM, PostgreSQL data dir, WAL, backup FS, file storage FS.
ALLOWED ACTIONS: read-only system checks.
FORBIDDEN ACTIONS: cleanup/delete без отдельного approval.
COMMANDS OR SCRIPTS: `df -h`, `df -i`, PostgreSQL size queries.
EXPECTED OUTPUT: свободное место с запасом 30-50%.
VALIDATION: meets manifest minimum.
SUCCESS CONDITION: ресурсов достаточно.
FAILURE CONDITION: недостаточно места/inodes.
ROLLBACK OR SAFE STOP: остановиться.
ARTIFACTS: resource report.
NEXT STATE: STATE 05.

## STATE 05 - DISCOVER_SOURCE

PURPOSE: read-only discovery source DB.
PRECONDITIONS: ресурсы подтверждены.
READ-ONLY CHECKS: version, DB name, schema, tables, counts, roles, grants, connections, transactions, sequences, file storage, scheduled execution, automatic update.
ALLOWED ACTIONS: только read-only SQL.
FORBIDDEN ACTIONS: DDL/DML, lock-heavy queries без оценки.
COMMANDS OR SCRIPTS: `audit/sql/production-readonly-audit.sql`, `production-object-inventory.sql`, `production-security-audit.sql`.
EXPECTED OUTPUT: source inventory.
VALIDATION: source matches known architecture.
SUCCESS CONDITION: source discovered.
FAILURE CONDITION: mismatch or insufficient access.
ROLLBACK OR SAFE STOP: остановиться.
ARTIFACTS: source audit report.
NEXT STATE: STATE 06.

## STATE 06 - PRE_MIGRATION_BASELINE

PURPOSE: baseline до остановки записи.
PRECONDITIONS: source discovered.
READ-ONLY CHECKS: counts, hashes, PK ranges, sequence values, security aggregates, file manifest, schema, grants.
ALLOWED ACTIONS: read-only SQL/file manifest.
FORBIDDEN ACTIONS: считать baseline финальным при работающей записи.
COMMANDS OR SCRIPTS: validation SQL, file manifest commands.
EXPECTED OUTPUT: preliminary baseline.
VALIDATION: no unexpected anomalies.
SUCCESS CONDITION: baseline captured.
FAILURE CONDITION: critical mismatch.
ROLLBACK OR SAFE STOP: остановиться.
ARTIFACTS: preliminary baseline.
NEXT STATE: STATE 07.

## STATE 07 - STOP_WRITES

PURPOSE: остановить запись.
PRECONDITIONS: maintenance window approved.
READ-ONLY CHECKS: active users/connections.
ALLOWED ACTIONS: maintenance mode, stop application, stop jobs/integrations.
FORBIDDEN ACTIONS: оставлять пользователей с записью.
COMMANDS OR SCRIPTS: approved service commands only.
EXPECTED OUTPUT: no active writes.
VALIDATION: no write transactions.
SUCCESS CONDITION: writes stopped; timestamp recorded.
FAILURE CONDITION: записи продолжаются.
ROLLBACK OR SAFE STOP: вернуться к старому состоянию или продлить окно по human decision.
ARTIFACTS: stop-writes log.
NEXT STATE: STATE 08.

## STATE 08 - FINAL_SOURCE_BASELINE

PURPOSE: финальный baseline после остановки записи.
PRECONDITIONS: writes stopped.
READ-ONLY CHECKS: повторить counts/hashes/PK/security/file manifest.
ALLOWED ACTIONS: read-only SQL.
FORBIDDEN ACTIONS: DDL/DML.
COMMANDS OR SCRIPTS: validation SQL.
EXPECTED OUTPUT: final immutable baseline.
VALIDATION: baseline consistent.
SUCCESS CONDITION: final baseline approved.
FAILURE CONDITION: расхождение без объяснения.
ROLLBACK OR SAFE STOP: остановиться.
ARTIFACTS: final source baseline.
NEXT STATE: STATE 09.

## STATE 09 - CREATE_FINAL_BACKUP

PURPOSE: финальный backup.
PRECONDITIONS: final baseline captured.
READ-ONLY CHECKS: backup dir, permissions, disk.
ALLOWED ACTIONS: `pg_dump -Fc`, `pg_dumpall --globals-only`, app config backup, file storage backup, manifest, SHA-256.
FORBIDDEN ACTIONS: restore globals, overwrite backups, store in Git.
COMMANDS OR SCRIPTS: `backup/backup-production.sh`, `backup/backup-globals.sh`, `backup/create-backup-manifest.sh`.
EXPECTED OUTPUT: dump, globals backup, file backup, manifest.
VALIDATION: exit code 0 for each command.
SUCCESS CONDITION: backup files created.
FAILURE CONDITION: any backup error.
ROLLBACK OR SAFE STOP: остановиться; old app remains stopped or return by human decision.
ARTIFACTS: backup set.
NEXT STATE: STATE 10.

## STATE 10 - VERIFY_BACKUP

PURPOSE: доказать читаемость backup.
PRECONDITIONS: backup created.
READ-ONLY CHECKS: size, SHA-256, `pg_restore --list`, entries.
ALLOWED ACTIONS: verify only.
FORBIDDEN ACTIONS: считать файл успешным backup без restore-list.
COMMANDS OR SCRIPTS: `backup/verify-backup.sh`.
EXPECTED OUTPUT: verified backup manifest.
VALIDATION: `pg_restore --list` exit code 0; checksums match.
SUCCESS CONDITION: backup verified.
FAILURE CONDITION: checksum/list/size error.
ROLLBACK OR SAFE STOP: не продолжать.
ARTIFACTS: backup verification report.
NEXT STATE: STATE 11.

## STATE 11 - CREATE_TARGET_DATABASE

PURPOSE: создать новую `hunttech`.
PRECONDITIONS: backup verified.
READ-ONLY CHECKS: target DB absent, other DBs protected.
ALLOWED ACTIONS: create exact target database only.
FORBIDDEN ACTIONS: create tablespace `hunttech`, drop unrelated DB, reuse non-empty DB.
COMMANDS OR SCRIPTS: approved DDL with guards.
EXPECTED OUTPUT: empty `hunttech` on `pg_default`.
VALIDATION: current_database checks; DB empty.
SUCCESS CONDITION: target ready.
FAILURE CONDITION: DB exists unexpectedly or DDL error.
ROLLBACK OR SAFE STOP: preserve source, stop.
ARTIFACTS: target creation log.
NEXT STATE: STATE 12.

## STATE 12 - CREATE_TARGET_SCHEMA

PURPOSE: создать target schema.
PRECONDITIONS: empty target DB.
READ-ONLY CHECKS: automatic update disabled, scripts approved.
ALLOWED ACTIONS: apply approved schema scripts.
FORBIDDEN ACTIONS: uncontrolled `automaticDatabaseUpdate`.
COMMANDS OR SCRIPTS: approved CUBA/Liquibase/schema scripts.
EXPECTED OUTPUT: schema matching local verified `hunttech`.
VALIDATION: schema diff.
SUCCESS CONDITION: schema approved.
FAILURE CONDITION: schema mismatch.
ROLLBACK OR SAFE STOP: stop; target preserved.
ARTIFACTS: schema deployment report.
NEXT STATE: STATE 13.

## STATE 13 - EXECUTE_DATA_MIGRATION

PURPOSE: перенести данные по mapping/dependency graph.
PRECONDITIONS: target schema approved.
READ-ONLY CHECKS: source still write-frozen.
ALLOWED ACTIONS: run approved migration scripts.
FORBIDDEN ACTIONS: update source, skip unmapped table.
COMMANDS OR SCRIPTS: `migration/40-run-test-prefix-migration.sh` adapted for production only after approval; `table-migration-mapping.yaml`.
EXPECTED OUTPUT: all mapped tables loaded/transformed.
VALIDATION: after each group counts/PK/checksum/FK readiness.
SUCCESS CONDITION: all groups complete.
FAILURE CONDITION: unexplained mismatch or script error.
ROLLBACK OR SAFE STOP: STATE R1.
ARTIFACTS: migration logs, quarantine.
NEXT STATE: STATE 14.

## STATE 14 - RESTORE_SEQUENCE_STATE

PURPOSE: установить sequence values.
PRECONDITIONS: data loaded.
READ-ONLY CHECKS: source sequence values, target max IDs.
ALLOWED ACTIONS: set target sequence values only.
FORBIDDEN ACTIONS: source sequence changes.
COMMANDS OR SCRIPTS: `validation/validate-sequences.sql` and approved setval script.
EXPECTED OUTPUT: next value cannot conflict.
VALIDATION: nextval > max ID where applicable.
SUCCESS CONDITION: sequences valid.
FAILURE CONDITION: sequence lag.
ROLLBACK OR SAFE STOP: STATE R1.
ARTIFACTS: sequence report.
NEXT STATE: STATE 15.

## STATE 15 - CREATE_AND_VALIDATE_CONSTRAINTS

PURPOSE: constraints/indexes/triggers.
PRECONDITIONS: data and sequences ready.
READ-ONLY CHECKS: expected constraints list.
ALLOWED ACTIONS: create/validate target constraints/indexes/triggers.
FORBIDDEN ACTIONS: leave NOT VALID constraints unapproved.
COMMANDS OR SCRIPTS: `migration/30-apply-post-migration-indexes.sql`, validation SQL.
EXPECTED OUTPUT: all constraints valid.
VALIDATION: invalid constraints count = 0.
SUCCESS CONDITION: constraints valid.
FAILURE CONDITION: FK/unique/check error.
ROLLBACK OR SAFE STOP: STATE R1.
ARTIFACTS: constraints report.
NEXT STATE: STATE 16.

## STATE 16 - VALIDATE_ALL_DATA

PURPOSE: полная data validation.
PRECONDITIONS: constraints valid.
READ-ONLY CHECKS: counts, PK, checksums, null pattern, duplicate checks, exceptions.
ALLOWED ACTIONS: read-only validation.
FORBIDDEN ACTIONS: ignore count-only limitation.
COMMANDS OR SCRIPTS: `validation/compare-source-target-prefix.sql`, `validate-migration-target.sql`.
EXPECTED OUTPUT: no critical mismatches.
VALIDATION: every table accounted.
SUCCESS CONDITION: data accepted.
FAILURE CONDITION: unapproved mismatch.
ROLLBACK OR SAFE STOP: STATE R1.
ARTIFACTS: data validation report.
NEXT STATE: STATE 17.

## STATE 17 - VALIDATE_SECURITY

PURPOSE: HRM/CUBA security validation.
PRECONDITIONS: data validation passed.
READ-ONLY CHECKS: users, roles, groups, memberships, permissions, ExtUser, remember-me, settings.
ALLOWED ACTIONS: read-only validation; sensitive values redacted.
FORBIDDEN ACTIONS: print passwords/tokens.
COMMANDS OR SCRIPTS: `validation/validate-security-data.sql`.
EXPECTED OUTPUT: aggregates match approved baseline.
VALIDATION: security mismatch blocks cutover.
SUCCESS CONDITION: security valid.
FAILURE CONDITION: any critical security mismatch.
ROLLBACK OR SAFE STOP: STATE R1.
ARTIFACTS: security report.
NEXT STATE: STATE 18.

## STATE 18 - VALIDATE_FILE_STORAGE

PURPOSE: validate physical files.
PRECONDITIONS: security valid.
READ-ONLY CHECKS: `sys_file`, physical file count, checksums, missing/orphan, target isolation.
ALLOWED ACTIONS: read-only file checks.
FORBIDDEN ACTIONS: write test app to production storage.
COMMANDS OR SCRIPTS: file manifest procedure from human runbook.
EXPECTED OUTPUT: files available or documented exceptions.
VALIDATION: missing critical files block cutover.
SUCCESS CONDITION: storage valid.
FAILURE CONDITION: missing/unsafe storage.
ROLLBACK OR SAFE STOP: STATE R1.
ARTIFACTS: file storage report.
NEXT STATE: STATE 19.

## STATE 19 - VALIDATE_SCHEMA_AND_PRIVILEGES

PURPOSE: schema/privileges validation.
PRECONDITIONS: file validation passed.
READ-ONLY CHECKS: owners, grants, default privileges, functions, schemas, indexes, triggers, views, extensions.
ALLOWED ACTIONS: read-only validation.
FORBIDDEN ACTIONS: shared role/grant changes without approval.
COMMANDS OR SCRIPTS: production security/object inventory SQL.
EXPECTED OUTPUT: approved privilege model.
VALIDATION: app user least-privilege or approved owner model.
SUCCESS CONDITION: schema/privileges valid.
FAILURE CONDITION: missing grants or unsafe role plan.
ROLLBACK OR SAFE STOP: STATE R1.
ARTIFACTS: privileges report.
NEXT STATE: STATE 20.

## STATE 20 - START_ISOLATED_TEST_APPLICATION

PURPOSE: start isolated target app.
PRECONDITIONS: target DB valid.
READ-ONLY CHECKS: port/app home/config separated; integrations disabled.
ALLOWED ACTIONS: start test app against target DB.
FORBIDDEN ACTIONS: production user access, outbound email/Telegram/jobs.
COMMANDS OR SCRIPTS: approved service start.
EXPECTED OUTPUT: app starts.
VALIDATION: no unexpected schema update.
SUCCESS CONDITION: app running.
FAILURE CONDITION: startup failure/update attempt.
ROLLBACK OR SAFE STOP: STATE R1.
ARTIFACTS: app startup log.
NEXT STATE: STATE 21.

## STATE 21 - APPLICATION_SMOKE_TEST

PURPOSE: проверить приложение.
PRECONDITIONS: isolated app running.
READ-ONLY CHECKS: logs before tests.
ALLOWED ACTIONS: approved smoke tests and marked `MIGRATION_TEST_` record.
FORBIDDEN ACTIONS: modify real business data.
COMMANDS OR SCRIPTS: `validation/application-smoke-test.md`.
EXPECTED OUTPUT: admin/recruiter login, business screens, files, permissions pass.
VALIDATION: logs contain no critical datasource/security/ORM errors.
SUCCESS CONDITION: smoke test accepted.
FAILURE CONDITION: critical app failure.
ROLLBACK OR SAFE STOP: STATE R1.
ARTIFACTS: smoke test report.
NEXT STATE: STATE 22.

## STATE 22 - CUTOVER_DECISION_GATE

PURPOSE: human go/no-go.
PRECONDITIONS: all validations green.
READ-ONLY CHECKS: reports complete, rollback ready, backup verified.
ALLOWED ACTIONS: present decision package.
FORBIDDEN ACTIONS: AI self-approval.
COMMANDS OR SCRIPTS: no mutating commands.
EXPECTED OUTPUT: written approval or rejection.
VALIDATION: approval recorded.
SUCCESS CONDITION: human approval.
FAILURE CONDITION: no approval/critical warning.
ROLLBACK OR SAFE STOP: STATE R1.
ARTIFACTS: cutover decision record.
NEXT STATE: STATE 23.

## STATE 23 - SWITCH_APPLICATION

PURPOSE: switch datasource.
PRECONDITIONS: cutover approved.
READ-ONLY CHECKS: old config backed up.
ALLOWED ACTIONS: change datasource to `hunttech`, start app, keep users blocked.
FORBIDDEN ACTIONS: open user access now.
COMMANDS OR SCRIPTS: approved config deployment.
EXPECTED OUTPUT: app connected to `hunttech`.
VALIDATION: app startup, DB connections only to target.
SUCCESS CONDITION: post-switch app starts.
FAILURE CONDITION: app failure.
ROLLBACK OR SAFE STOP: STATE R2.
ARTIFACTS: switch log.
NEXT STATE: STATE 24.

## STATE 24 - POST_SWITCH_VALIDATION

PURPOSE: validate before users.
PRECONDITIONS: app switched.
READ-ONLY CHECKS: logs, connections, no old DB writes.
ALLOWED ACTIONS: admin smoke tests, marked test write if approved.
FORBIDDEN ACTIONS: user access.
COMMANDS OR SCRIPTS: smoke checklist.
EXPECTED OUTPUT: no critical errors.
VALIDATION: target writes only.
SUCCESS CONDITION: ready to open.
FAILURE CONDITION: critical issue.
ROLLBACK OR SAFE STOP: STATE R2.
ARTIFACTS: post-switch report.
NEXT STATE: STATE 25.

## STATE 25 - OPEN_USER_ACCESS

PURPOSE: open users.
PRECONDITIONS: human approval after post-switch validation.
READ-ONLY CHECKS: old DB still preserved, target healthy.
ALLOWED ACTIONS: disable maintenance mode/open access.
FORBIDDEN ACTIONS: open without human approval.
COMMANDS OR SCRIPTS: approved service action.
EXPECTED OUTPUT: users can log in.
VALIDATION: timestamp recorded.
SUCCESS CONDITION: access opened.
FAILURE CONDITION: access cannot open safely.
ROLLBACK OR SAFE STOP: STATE R2 if no writes, STATE R3 after writes.
ARTIFACTS: open access record.
NEXT STATE: STATE 26.

## STATE 26 - POST_CUTOVER_MONITORING

PURPOSE: monitor new system.
PRECONDITIONS: users opened.
READ-ONLY CHECKS: errors, SQL, auth, permissions, files, jobs, integrations, new records, performance.
ALLOWED ACTIONS: monitoring and approved forward-fixes only.
FORBIDDEN ACTIONS: automatic rollback with data loss.
COMMANDS OR SCRIPTS: logs, DB monitoring.
EXPECTED OUTPUT: stable operation.
VALIDATION: no critical errors.
SUCCESS CONDITION: observation window accepted.
FAILURE CONDITION: critical issue after user writes.
ROLLBACK OR SAFE STOP: STATE R3.
ARTIFACTS: monitoring report.
NEXT STATE: STATE 27.

## STATE 27 - SUCCESS_FINALIZATION

PURPOSE: finalize.
PRECONDITIONS: monitoring accepted.
READ-ONLY CHECKS: source preserved, backup preserved, reports complete.
ALLOWED ACTIONS: archive logs, prepare sanitized docs for Git, remove temporary secrets.
FORBIDDEN ACTIONS: delete old DB before retention approval, push without permission.
COMMANDS OR SCRIPTS: `git status`, secret scan.
EXPECTED OUTPUT: final report.
VALIDATION: sign-off recorded.
SUCCESS CONDITION: migration accepted.
FAILURE CONDITION: missing report/signoff.
ROLLBACK OR SAFE STOP: human decision.
ARTIFACTS: final migration report.
NEXT STATE: END.

## STATE R1 - SAFE_STOP_BEFORE_CUTOVER

PURPOSE: safe stop before datasource switch.
PRECONDITIONS: failure before cutover.
READ-ONLY CHECKS: source unchanged.
ALLOWED ACTIONS: preserve target/logs, keep old datasource.
FORBIDDEN ACTIONS: delete target without task, modify source.
SUCCESS CONDITION: old system can be resumed by human decision.
NEXT STATE: END.

## STATE R2 - ROLLBACK_BEFORE_USER_WRITES

PURPOSE: rollback after switch but before user writes.
PRECONDITIONS: no user writes on target.
READ-ONLY CHECKS: confirm no target writes after cutover timestamp.
ALLOWED ACTIONS: stop new app, restore old datasource, start old app.
FORBIDDEN ACTIONS: delete target.
SUCCESS CONDITION: old `itpearls` app healthy.
NEXT STATE: END.

## STATE R3 - FREEZE_AFTER_USER_WRITES

PURPOSE: freeze after issue with user writes.
PRECONDITIONS: target may contain new/changed rows.
READ-ONLY CHECKS: cutover timestamp, target changes.
ALLOWED ACTIONS: stop writes, preserve both DBs.
FORBIDDEN ACTIONS: automatic datasource rollback with data loss.
SUCCESS CONDITION: both DBs frozen for analysis.
NEXT STATE: STATE R4.

## STATE R4 - DELTA_ANALYSIS

PURPOSE: identify post-cutover delta.
PRECONDITIONS: writes frozen.
READ-ONLY CHECKS: new/updated/deleted rows after cutover timestamp.
ALLOWED ACTIONS: read-only diff and report.
FORBIDDEN ACTIONS: apply delta automatically.
SUCCESS CONDITION: delta report complete.
NEXT STATE: STATE R5.

## STATE R5 - HUMAN_ROLLBACK_DECISION

PURPOSE: human decision after user writes.
PRECONDITIONS: delta report complete.
READ-ONLY CHECKS: business impact.
ALLOWED ACTIONS: present options: rollback with delta replay, forward-fix, continue target.
FORBIDDEN ACTIONS: AI final decision.
SUCCESS CONDITION: written human decision.
NEXT STATE: END or approved recovery runbook.

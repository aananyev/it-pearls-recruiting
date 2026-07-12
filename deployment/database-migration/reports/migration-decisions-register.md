# Журнал решений по миграции HRM HuntTech

Дата создания: 2026-07-12

В этом журнале фиксируются только решения, которые влияют на production-миграцию `itpearls` -> `hunttech`. Если решение не утверждено для production, оно помечается как `ТРЕБУЕТ ПОДТВЕРЖДЕНИЯ ПЕРЕД PRODUCTION`.

| ID | Решение | Статус | Основание | Подтверждающий материал | Ответственный | Дата | Дополнительное утверждение |
|---|---|---|---|---|---|---|---|
| D-001 | Миграция выполняется как database-to-database: `itpearls` -> `hunttech`, не как tablespace migration | Подтверждено архитектурой | Production `itpearls` является database/table prefix, tablespace `itpearls` отсутствует | `reports/second-stage-summary.md`, `audit/production-architecture-audit.md` | Руководитель миграции / DBA | 2026-07-12 | Нет |
| D-002 | Source database `itpearls` не изменяется, не переименовывается, не очищается и не удаляется | Обязательное правило | Безопасный rollback требует неизменной старой базы | `runbooks/rollback-plan.md`, `reports/risk-register.md` | Руководитель миграции | 2026-07-12 | Нет |
| D-003 | Target database называется `hunttech` | Требует финального утверждения | Локальная миграция проверена на `hunttech`, но production target name должен быть подтвержден перед созданием | `reports/local-itpearls-to-hunttech-migration-report-2026-07-11.md` | Руководитель миграции / DBA | 2026-07-12 | Да |
| D-004 | Target tablespace: `pg_default`; отдельный tablespace `hunttech` не создается | Подтверждено как безопасный базовый вариант | На production используются `pg_default`, `pg_global`; tablespace `hunttech` отсутствует | `reports/second-stage-summary.md` | DBA | 2026-07-12 | Только если потребуется отдельный tablespace |
| D-005 | Две legacy link-таблицы `itpearls_job_candidate_position_link__u59616` и `itpearls_open_position_city_link__u70664` не создаются в target после повторной проверки 0 строк и отсутствия зависимостей | Проверено локально, требует production recheck | Обе таблицы подтвержденно пустые в production/local source, отсутствуют в target | `reports/second-stage-summary.md`, `reports/local-itpearls-to-hunttech-migration-report-2026-07-11.md` | DBA / ответственный за данные | 2026-07-12 | Да, после финального STOP_WRITES |
| D-006 | `vacancy_prompt_template.temperature`: существующие значения и `NULL` переносятся без изменения; target database default устанавливается `0.7` | Проверено локально, требует утверждения schema decision | Production default `0.7`; локальный target default отсутствовал; локальная миграция установила default без UPDATE строк | `reports/second-stage-summary.md`, `reports/local-itpearls-to-hunttech-migration-report-2026-07-11.md` | DBA / разработчик HRM | 2026-07-12 | Да |
| D-007 | 17 orphan-ссылок `job_candidate.current_company_id` сохраняются через placeholder-компании с теми же UUID и записью в quarantine | Проверено локально, production blocked до письменного решения | Production содержит 17 missing company ids; простое удаление или NULL запрещены | `reports/test-restore-report.md`, `reports/local-itpearls-to-hunttech-migration-report-2026-07-11.md` | Руководитель миграции / представитель пользователей | 2026-07-12 | Да, обязательно |
| D-008 | `sec_user.dtype` должен быть заменен `itpearls_ExtUser` -> `hunttech_ExtUser` | Проверено локально | Без этого CUBA ORM не видит `anonymous` и `admin` как актуальную сущность | `reports/local-itpearls-to-hunttech-migration-report-2026-07-11.md` | Разработчик HRM | 2026-07-12 | Нет |
| D-009 | Dashboard/widget models должны получить замену namespace `itpearls` -> `hunttech` | Проверено локально | Иначе главный dashboard открывает несуществующие legacy `frameId` | `reports/local-dashboard-widgets-fix-2026-07-12.md` | Разработчик HRM | 2026-07-12 | Нет |
| D-010 | `sys_scheduled_execution` переносится полностью; очистка/retention запрещены в рамках миграции без отдельного задания | Требует production решения по объему | Таблица занимает значительную часть базы, но удаление истории не является миграцией | `reports/second-stage-summary.md`, `reports/risk-register.md` | Руководитель миграции / владелец данных | 2026-07-12 | Да, если предлагается чистка |
| D-011 | Physical file storage должен быть найден, остановлен для записи, скопирован/проверен отдельно от PostgreSQL dump | Требует подтверждения production path | `sys_file` содержит 13458 записей; физические файлы могут находиться вне PostgreSQL | `reports/second-stage-summary.md`, `reports/local-itpearls-to-hunttech-migration-report-2026-07-11.md` | Системный администратор | 2026-07-12 | Да |
| D-012 | `cuba.automaticDatabaseUpdate=true` на production должен быть отключен или переведен в контролируемый режим до запуска новой версии | Open blocker | Неконтролируемый update при старте может изменить target schema вне runbook | `audit/production-architecture-audit.md`, `reports/risk-register.md` | Разработчик HRM / DBA | 2026-07-12 | Да |
| D-013 | Rollback после открытия записи пользователям запрещено выполнять автоматически без delta analysis | Обязательное правило | Простое переключение datasource после новых записей может потерять данные | `runbooks/rollback-plan.md`, `reports/risk-register.md` | Лицо, принимающее rollback | 2026-07-12 | Да |
| D-014 | Другие базы PostgreSQL на `hr.hunttech.ru` являются неприкосновенными | Обязательное правило | На сервере есть другие базы; cluster-wide операции могут повлиять на них | `README.md`, `runbooks/production-migration-runbook.md`, `reports/risk-register.md` | DBA / руководитель миграции | 2026-07-12 | Нет |

## Текущие блокирующие решения

- D-003: финально подтвердить имя target database.
- D-005: повторно подтвердить 0 строк в legacy link-таблицах после STOP_WRITES.
- D-006: письменно утвердить schema default `temperature=0.7`.
- D-007: письменно утвердить правило placeholder-компаний для 17 orphan `current_company_id`.
- D-011: подтвердить production file storage path и процедуру копирования.
- D-012: утвердить способ контроля `cuba.automaticDatabaseUpdate`.
- D-013: утвердить бизнес-правило rollback после пользовательских записей.

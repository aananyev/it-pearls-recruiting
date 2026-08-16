# Карта документации для AI-агента

| Задача | Где искать | Основные файлы |
| ------ | ---------- | -------------- |
| Изменить сущность JobCandidate | `docs/entities/job-candidate/` | [JobCandidate.md](../entities/job-candidate/JobCandidate.md), [current-company.md](../entities/job-candidate/current-company.md) |
| Изменить сущность Company | `docs/entities/company/` | [Company.md](../entities/company/Company.md) |
| Изменить вакансию OpenPosition | `docs/entities/open-position/` | [OpenPosition.md](../entities/open-position/OpenPosition.md), [OpenPosition_Spec.md](../entities/open-position/OpenPosition_Spec.md) |
| Изменить JobCandidateEdit | `docs/screens/job-candidate/` | [hunttech_JobCandidate.edit_Spec.md](../screens/job-candidate/hunttech_JobCandidate.edit_Spec.md), [job-candidate-edit.md](../screens/job-candidate/job-candidate-edit.md) |
| Изменить CompanyEdit | `docs/screens/company/` | [hunttech_Company.edit_Spec.md](../screens/company/hunttech_Company.edit_Spec.md), [company-edit.md](../screens/company/company-edit.md) |
| Найти UI-компоненты | `docs/screens/components/` | [FallbackImage.md](../screens/components/FallbackImage.md), [OvalImage.md](../screens/components/OvalImage.md), [OvaFallbackImage.md](../screens/components/OvaFallbackImage.md) |
| Проверить бизнес-правила | `docs/business-rules/` | [job-candidate-company-selection.md](../business-rules/job-candidate-company-selection.md), [user-settings-photo-sync.md](../business-rules/user-settings-photo-sync.md) |
| Спроектировать или изменить Telegram-бота | `docs/bots/` | [AccountingDocumentsTelegramBot.md](../bots/AccountingDocumentsTelegramBot.md), [AccountingDocumentsTelegramBot_MVP_Plan.md](../bots/AccountingDocumentsTelegramBot_MVP_Plan.md), [bots README](../bots/README.md) |
| Подготовить миграцию бухгалтерского Telegram-бота | `docs/database/migrations/` | [accounting-bot-preseed-migration-2026-07-29.md](../database/migrations/accounting-bot-preseed-migration-2026-07-29.md), [AccountingDocumentsTelegramBot.md](../bots/AccountingDocumentsTelegramBot.md) |
| Проверить структуру БД | `docs/database/` | [schema/db-schema-diff-report.md](../database/schema/db-schema-diff-report.md), [database README](../database/README.md) |
| Поднять локальную среду | `docs/operations/local-development/` | [local-database.md](../operations/local-development/local-database.md) |
| Выполнить production deployment | `deployment/production-deployment/` и `docs/operations/` | [production-deployment-runbook.md](../../deployment/production-deployment/runbooks/production-deployment-runbook.md), [operations README](../operations/README.md) |
| Выполнить migration/backup/restore | `deployment/database-migration/` и `docs/operations/` | [production-migration-runbook.md](../../deployment/database-migration/runbooks/production-migration-runbook.md), [production-backup-runbook.md](../../deployment/database-migration/runbooks/production-backup-runbook.md), [production-restore-runbook.md](../../deployment/database-migration/runbooks/production-restore-runbook.md) |
| Проверить тесты и smoke validation | `deployment/database-migration/validation/` | [application-smoke-test.md](../../deployment/database-migration/validation/application-smoke-test.md), [post-migration-checklist.md](../../deployment/database-migration/validation/post-migration-checklist.md) |
| Найти отчёты аудита | `docs/reports/` и `deployment/*/reports/` | [performance summary](../reports/performance/job-candidate-performance-audit-summary.md), [database migration reports](../../deployment/database-migration/reports/) |
| Настроить или сопровождать AI-подключения | `docs/integrations/ai/` | [USER_AI_CONNECTION_GUIDE.md](../integrations/ai/USER_AI_CONNECTION_GUIDE.md), [USER_AI_SETTINGS_IMPLEMENTATION.md](../integrations/ai/USER_AI_SETTINGS_IMPLEMENTATION.md), [AI_INTEGRATION.md](../integrations/ai/AI_INTEGRATION.md) |
| Журнал вызовов AI и аналитика | `docs/entities/ai/`, `docs/screens/ai/` | [AiCallLog.md](../entities/ai/AiCallLog.md), [AiCallLogBrowse.md](../screens/ai/AiCallLogBrowse.md), [AiDashboards_Spec.md](../screens/ai/AiDashboards_Spec.md) |

# database

Долговременная документация по PostgreSQL, схеме и проверкам данных.

| Документ | Краткое описание |
| -------- | ---------------- |
| [project-description-ai-prompts-2026-08-13.md](migrations/project-description-ai-prompts-2026-08-13.md) | Локальная безопасная миграция канонических русских административных prompt `PROJECT_DESCRIPTION_GENERATE` |
| [project-short-description-ai-prompt-increase-2026-08-14.md](migrations/project-short-description-ai-prompt-increase-2026-08-14.md) | Миграция увеличения AI-генерации «Кратко» ProjectEdit в 2 раза (`PROJECT_SHORT_DESCRIPTION_GENERATE`, два предложения, `MAX_TOKENS` 250) |
| [project-description-ai-function-2026-08-12.md](migrations/project-description-ai-function-2026-08-12.md) | Production-safe seed и runbook AI-функции `PROJECT_DESCRIPTION_GENERATE` для ProjectEdit upload |
| [db-schema-diff-report.md](schema/db-schema-diff-report.md) | Отчёт: расхождения схемы PostgreSQL и модели приложения HRM HuntTech |
| [accounting-bot-preseed-migration-2026-07-29.md](migrations/accounting-bot-preseed-migration-2026-07-29.md) | Обязательный регламент будущей миграции бухгалтерского Telegram-бота и предзаполнения справочников |
| [production-schema-reconciliation-2026-07-27.md](migrations/production-schema-reconciliation-2026-07-27.md) | Первоначальный Liquibase-план сверки частично применённых AI-изменений |
| [cuba-update-db-reconciliation-2026-07-27.md](migrations/cuba-update-db-reconciliation-2026-07-27.md) | Фактическое выполнение через CUBA updateDb, baseline legacy aliases и полнота UserAiProfile |

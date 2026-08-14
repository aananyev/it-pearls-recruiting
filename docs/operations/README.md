# operations

Локальный запуск, эксплуатация, Tomcat, backup/restore и ссылки на production runbook-и HRM HuntTech.

| Документ | Краткое описание |
| -------- | ---------------- |
| [local-database.md](local-development/local-database.md) | Локальная PostgreSQL для HRM HuntTech. |

## Рабочие runbook-и в deployment

Production-материалы оставлены на исходных путях, потому что скрипты и отчёты ссылаются на них по фиксированной структуре:

- [database-migration](../../deployment/database-migration/)
- [production-deployment/runbooks](../../deployment/production-deployment/runbooks/)
- [production-deployment/reports](../../deployment/production-deployment/reports/)
- [Безопасная миграция системных промптов AI](../../deployment/production-deployment/runbooks/ai-system-prompts-production-migration-runbook.md) — инкрементальное обновление действующей базы `hunttech`, CUBA `updateDb`, backup, dry run, smoke-test и rollback.

## История изменений

| Дата | Изменение |
| --- | --- |
| 2026-07-22 | Добавлена ссылка на production runbook миграции системных промптов AI. |

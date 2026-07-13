# Проверка навигации

## Сценарий человека

| Что найти | Результат |
| --------- | --------- |
| JobCandidate | [docs/entities/job-candidate/JobCandidate.md](../docs/entities/job-candidate/JobCandidate.md) найден через [docs/entities/README.md](../docs/entities/README.md). |
| Company | [docs/entities/company/Company.md](../docs/entities/company/Company.md) найден через [docs/entities/README.md](../docs/entities/README.md). |
| JobCandidateEdit | [docs/screens/job-candidate/hunttech_JobCandidate.edit_Spec.md](../docs/screens/job-candidate/hunttech_JobCandidate.edit_Spec.md) найден через [docs/screens/README.md](../docs/screens/README.md). |
| CompanyEdit | [docs/screens/company/hunttech_Company.edit_Spec.md](../docs/screens/company/hunttech_Company.edit_Spec.md) найден через [docs/screens/README.md](../docs/screens/README.md). |
| Сервисы кандидатов | Специального candidate-service документа не найдено; сервисная навигация ведёт в [docs/services/README.md](../docs/services/README.md). |
| Структура базы | [docs/database/README.md](../docs/database/README.md) и [schema/db-schema-diff-report.md](../docs/database/schema/db-schema-diff-report.md). |
| Deployment | [docs/operations/README.md](../docs/operations/README.md) с ссылками на `deployment/production-deployment`. |
| Testing | Основные validation/smoke материалы находятся в `deployment/database-migration/validation/`; отдельный `docs/testing/` не создан, потому что долговременных testing-документов в `docs` не найдено. |
| Backup/restore | [deployment/database-migration/runbooks/production-backup-runbook.md](../deployment/database-migration/runbooks/production-backup-runbook.md) и [production-restore-runbook.md](../deployment/database-migration/runbooks/production-restore-runbook.md). |

## Сценарий AI

| Проверка | Результат |
| -------- | --------- |
| Найти сущность | `docs/ai/documentation-map.md` указывает на `docs/entities/<entity>/`. |
| Найти экран | `docs/ai/documentation-map.md` указывает на `docs/screens/<area>/`. |
| Найти бизнес-правило | `docs/ai/documentation-map.md` указывает на `docs/business-rules/`. |
| Найти сервисы | `docs/ai/documentation-map.md` и `docs/services/README.md`. |
| Найти database mapping | `docs/database/README.md` и deployment migration navigation. |
| Найти тесты | AI-карта указывает на `deployment/database-migration/validation/`. |
| Найти production runbook | AI-карта указывает на `deployment/production-deployment/runbooks/`. |

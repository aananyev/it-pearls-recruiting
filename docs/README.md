# Документация HRM HuntTech

Навигационный индекс документации проекта. Документы разложены так, чтобы разработчик, администратор, DBA, тестировщик и AI-агент могли быстро найти материалы по сущности, экрану, сервису, базе данных или эксплуатации.

## Карта разделов

| Раздел | Назначение |
| ------ | ---------- |
| [project/](project/) | Общая информация, шаблоны и правила ведения документации. |
| [architecture/](architecture/) | Архитектурные заметки и решения верхнего уровня. |
| [entities/](entities/) | Living-документация сущностей CUBA и связанных таблиц. |
| [screens/](screens/) | Материалы редизайна, исследования и спецификации экранов по функциональным областям. |
| [ui/](ui/) | Канонические технические спецификации экранов и фрагментов CUBA. |
| [services/](services/) | Документация сервисов и сервисных подсистем. |
| [business-rules/](business-rules/) | Сквозные бизнес-правила и сценарии. |
| [database/](database/) | Схема, проверки, расхождения и навигация по БД. |
| [integrations/](integrations/) | Внешние интеграции, AI, файлы, API и провайдеры. |
| [operations/](operations/) | Локальный запуск, Tomcat, deployment, backup/restore и troubleshooting. |
| [reports/](reports/) | Аудиты, отчёты внедрения, performance и исторические исследования. |
| [ai/](ai/) | Карта документации для AI-агентов. |
| [review-needed/](review-needed/) | Документы, требующие ручной классификации. |

## Быстрые входы

| Роль | Начать здесь |
| ---- | ------------ |
| Разработчик | [entities/](entities/), [ui/](ui/), [screens/](screens/), [services/](services/) |
| Экран JobCandidateEdit | [ui/JobCandidateEdit_Spec.md](ui/JobCandidateEdit_Spec.md) · [architecture/JobCandidateEdit_Performance_Architecture.md](architecture/JobCandidateEdit_Performance_Architecture.md) · [screens/job-candidate/JobCandidateEdit_Design_Fix_2026-07-14.md](screens/job-candidate/JobCandidateEdit_Design_Fix_2026-07-14.md) |
| Диагностика разбора резюме и OOM | [services/PdfParserService.md](services/PdfParserService.md) · [operations/JobCandidateEdit_OOM_Runbook.md](operations/JobCandidateEdit_OOM_Runbook.md) |
| DBA | [database/](database/) и [deployment/database-migration/](../deployment/database-migration/) |
| Тестировщик | [operations/](operations/), [reports/](reports/), [deployment/database-migration/validation/](../deployment/database-migration/validation/) |
| Администратор | [operations/](operations/) и [deployment/production-deployment/runbooks/](../deployment/production-deployment/runbooks/) |
| AI-агент | [ai/documentation-map.md](ai/documentation-map.md) · [ai/ChatGPT_Project_Instructions.md](ai/ChatGPT_Project_Instructions.md) |

## Куда добавлять новый документ

| Тип документа | Каталог |
| ------------- | ------- |
| Новая сущность или описание таблицы сущности | `docs/entities/<entity>/` |
| Каноническая спецификация экрана или фрагмента | `docs/ui/<FormName>_Spec.md` |
| Материалы редизайна и исследования UI | `docs/screens/<area>/` |
| Сервис или фоновые задачи | `docs/services/<service-area>/` |
| Сквозное бизнес-правило | `docs/business-rules/` |
| Схема, индексы, проверки БД | `docs/database/` |
| Рабочий production runbook или скриптовая инструкция | `deployment/` с навигационной ссылкой из `docs/operations/` |
| Аудит, investigation, implementation report | `docs/reports/` |
| Инструкция или карта для AI-агента | `docs/ai/` |

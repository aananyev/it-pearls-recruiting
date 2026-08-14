# Документация HRM HuntTech

Навигационный индекс документации проекта. Документы разложены так, чтобы разработчик, администратор, DBA, тестировщик и AI-агент могли быстро найти материалы по сущности, экрану, сервису, базе данных или эксплуатации.

## Карта разделов

| Раздел | Назначение |
| ------ | ---------- |
| [project/](project/) | Общая информация, обязательные правила, роли и шаблоны документации. |
| [architecture/](architecture/) | Архитектурные заметки и решения верхнего уровня. |
| [entities/](entities/) | Living-документация сущностей CUBA и связанных таблиц. |
| [screens/](screens/) | Материалы редизайна, исследования и спецификации экранов по функциональным областям. |
| [ui/](ui/) | Канонические технические спецификации экранов и фрагментов CUBA. |
| [services/](services/) | Документация сервисов и сервисных подсистем. |
| [bots/](bots/) | Служебные Telegram-боты HRM HuntTech, сценарии подтверждения и интеграции. |
| [business-rules/](business-rules/) | Сквозные бизнес-правила и сценарии. |
| [database/](database/) | Схема, проверки, расхождения и навигация по БД. |
| [integrations/](integrations/) | Внешние интеграции, AI, файлы, API и провайдеры. |
| [operations/](operations/) | Локальный запуск, Tomcat, deployment, backup/restore и troubleshooting. |
| [reports/](reports/) | Аудиты, отчёты внедрения, performance и исторические исследования. |
| [ai/](ai/) | Карта документации и рабочие prompts для AI-агентов. |
| [review-needed/](review-needed/) | Документы, требующие ручной классификации. |

## Быстрые входы

| Роль | Начать здесь |
| ---- | ------------ |
| Правила проекта | [project/HRM_HuntTech_Project_Working_Rules.md](project/HRM_HuntTech_Project_Working_Rules.md) |
| Hermes / DevOps | [ai/Hermes_DevOps_Operating_Prompt.md](ai/Hermes_DevOps_Operating_Prompt.md) · [operations/README.md](operations/README.md) |
| Разработчик | [entities/](entities/), [ui/](ui/), [screens/](screens/), [services/](services/) |
| Экран JobCandidateEdit | [ui/JobCandidateEdit_Spec.md](ui/JobCandidateEdit_Spec.md) · [architecture/JobCandidateEdit_Performance_Architecture.md](architecture/JobCandidateEdit_Performance_Architecture.md) · [screens/job-candidate/JobCandidateEdit_Design_Fix_2026-07-14.md](screens/job-candidate/JobCandidateEdit_Design_Fix_2026-07-14.md) |
| Stage 6 JobCandidateEdit | [review узкого picker-view должностей](performance-archive/2026-07-15/job-candidate-position-picker-stage-6/stage-6-chatgpt-review.md) |
| Stage 7 JobCandidateEdit | [review baseline загрузки городов](performance-archive/2026-07-15/job-candidate-city-loader-stage-7-baseline/stage-7-chatgpt-review.md) |
| Stage 8 JobCandidateEdit | [review baseline последнего взаимодействия](performance-archive/2026-07-15/job-candidate-last-interaction-stage-8-baseline/stage-8-chatgpt-review.md) |
| Stage 9 JobCandidateEdit | [review ленивой загрузки последнего взаимодействия](performance-archive/2026-07-15/job-candidate-last-interaction-stage-9-lazy-load/stage-9-chatgpt-review.md) |
| Stage 10 JobCandidateEdit | [review скалярной проверки наличия резюме](performance-archive/2026-07-15/job-candidate-cv-indicator-stage-10-scalar-existence/stage-10-chatgpt-review.md) |
| Stage 11 JobCandidateEdit | [review фоновой загрузки рейтинга](performance-archive/2026-07-15/job-candidate-rating-stage-11-background-load/stage-11-chatgpt-review.md) |
| Stage 12 JobCandidateEdit | [review фоновой загрузки фотографии](performance-archive/2026-07-15/job-candidate-photo-stage-12-background-load/stage-12-chatgpt-review.md) |
| Stage 13 JobCandidateEdit | [review дублирующего CV COUNT](performance-archive/2026-07-15/job-candidate-cv-indicator-stage-13-background-load/stage-13-chatgpt-review.md) |
| Stage 14 JobCandidateEdit | [review единственного background-источника CV indicator](performance-archive/2026-07-15/job-candidate-cv-indicator-stage-14-single-background-source/stage-14-chatgpt-review.md) |
| Stage 15 JobCandidateEdit | [контракт guard скрытого Skillsbar](performance-archive/2026-07-15/job-candidate-skills-stage-15-hidden-component-guard/stage-15-hidden-skills-background-guard-contract.md) |
| Диагностика разбора резюме и OOM | [services/PdfParserService.md](services/PdfParserService.md) · [operations/JobCandidateEdit_OOM_Runbook.md](operations/JobCandidateEdit_OOM_Runbook.md) |
| Подключение и выбор AI | [integrations/ai/USER_AI_CONNECTION_GUIDE.md](integrations/ai/USER_AI_CONNECTION_GUIDE.md) · [services/HrmAiService.md](services/HrmAiService.md) · [ui/UserAiConfigurationBrowse_Spec.md](ui/UserAiConfigurationBrowse_Spec.md) |
| Системный AI-анализ сущностей | [services/AiAnalysisService.md](services/AiAnalysisService.md) · [services/AiAnalysisHelper.md](services/AiAnalysisHelper.md) |
| Системные промпты AI | [ui/AiPromptTemplateBrowse_Spec.md](ui/AiPromptTemplateBrowse_Spec.md) · [ui/AiPromptTemplateEdit_Spec.md](ui/AiPromptTemplateEdit_Spec.md) |
| Production migration системных промптов AI | [ai-system-prompts-production-migration-runbook.md](../deployment/production-deployment/runbooks/ai-system-prompts-production-migration-runbook.md) |
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
| Служебный Telegram-бот или сценарий работы бота | `docs/bots/` |
| Сквозное бизнес-правило | `docs/business-rules/` |
| Схема, индексы, проверки БД | `docs/database/` |
| Рабочий production runbook или скриптовая инструкция | `deployment/` с навигационной ссылкой из `docs/operations/` |
| Аудит, investigation, implementation report | `docs/reports/` |
| Инструкция или карта для AI-агента | `docs/ai/` |

## История изменений

| Дата | Изменение |
| --- | --- |
| 2026-07-22 | Добавлена документация AiAnalysisService и явного вложенного view вакансии для системного AI-анализа. |
| 2026-07-22 | Добавлены спецификации системных промптов, выбор текущей нейросети и документация HrmAiService. |
| 2026-07-22 | Добавлены проектные правила, канонический DevOps-промпт Hermes и production runbook миграции системных промптов AI. |

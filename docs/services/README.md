# services

Сервисы и сервисные подсистемы приложения.

| Документ | Краткое описание |
| -------- | ---------------- |
| [AiAnalysisHelper.md](AiAnalysisHelper.md) | UI-мост системного AI-анализа и корректное получение Dialogs/Notifications из ScreenContext |
| [AiAnalysisService.md](AiAnalysisService.md) | Core-перезагрузка detached-сущностей и специализированные views для системного AI-анализа |
| [HrmAiService.md](HrmAiService.md) | Выбор AI-провайдера, текущая конфигурация и безопасная отправка промптов |
| [ImageProcessingService.md](file-storage/ImageProcessingService.md) | ImageProcessingService (`hunttech_ImageProcessingService`) |

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Добавлен `ProjectAiService` для административно управляемой AI-обработки описания проекта |
| 2026-08-12 | Добавлен `HrmAiService` как compatibility facade над `AiExecutionService` |
| 2026-08-12 | Добавлены `AiExecutionService` и `AiCredentialService` AI Control Plane |

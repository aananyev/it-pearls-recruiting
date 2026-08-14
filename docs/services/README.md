# services

Сервисы и сервисные подсистемы приложения.

| Документ | Краткое описание |
| -------- | ---------------- |
| [AccountingDocumentIngestService.md](AccountingDocumentIngestService.md) | AccountingDocumentIngestService — прием фото/PDF бухгалтерского Telegram-бота |
| [ImageProcessingService.md](file-storage/ImageProcessingService.md) | ImageProcessingService (`hunttech_ImageProcessingService`) |

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Добавлен `ProjectAiService` для административно управляемой AI-обработки описания проекта |
| 2026-08-12 | Добавлен `HrmAiService` как compatibility facade над `AiExecutionService` |
| 2026-08-12 | Добавлены `AiExecutionService` и `AiCredentialService` AI Control Plane |

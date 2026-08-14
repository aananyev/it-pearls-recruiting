# services

Сервисы и сервисные подсистемы приложения.

| Документ | Краткое описание |
| -------- | ---------------- |
| [AccountingDocumentIngestService.md](AccountingDocumentIngestService.md) | AccountingDocumentIngestService — учетный прием новых файлов от внешнего Hermes-бота |
| [OpenPositionRestApi.md](OpenPositionRestApi.md) | OpenPosition REST API — контракт публикации открытых вакансий на сайт hunttech.ru (CUBA REST API v2, OAuth2) |
| [ImageProcessingService.md](file-storage/ImageProcessingService.md) | ImageProcessingService (`hunttech_ImageProcessingService`) |

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Добавлен `ProjectAiService` для административно управляемой AI-обработки описания проекта |
| 2026-08-12 | Добавлен `HrmAiService` как compatibility facade над `AiExecutionService` |
| 2026-08-12 | Добавлены `AiExecutionService` и `AiCredentialService` AI Control Plane |

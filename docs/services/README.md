# services

Сервисы и сервисные подсистемы приложения.

| Документ | Краткое описание |
| -------- | ---------------- |
| [ProjectAiService.md](ProjectAiService.md) | Domain facade AI-обработки описания проекта через AI Control Plane |
| [AiExecutionService.md](AiExecutionService.md) | Единый resolver/execution layer AI-функций HRM HuntTech |
| [AiCredentialService.md](AiCredentialService.md) | Защищённое управление корпоративными AI credentials |
| [HrmAiService.md](HrmAiService.md) | Совместимый vacancy AI-фасад поверх AI Control Plane |
| [UserAiContextService.md](UserAiContextService.md) | Пользовательский профессиональный контекст и предпочтения AI |
| [AccountingDocumentIngestService.md](AccountingDocumentIngestService.md) | AccountingDocumentIngestService — учетный прием новых файлов от внешнего Hermes-бота |
| [OpenPositionRestApi.md](OpenPositionRestApi.md) | OpenPosition REST API — контракт публикации открытых вакансий на сайт hunttech.ru (CUBA REST API v2, OAuth2) |
| [ImageProcessingService.md](file-storage/ImageProcessingService.md) | ImageProcessingService (`hunttech_ImageProcessingService`) |

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Добавлен `ProjectAiService` для административно управляемой AI-обработки описания проекта |
| 2026-08-12 | Добавлен `HrmAiService` как compatibility facade над `AiExecutionService` |
| 2026-08-12 | Добавлены `AiExecutionService` и `AiCredentialService` AI Control Plane |

# CandidateSkillEnrichmentService

Сервис фонового анализа навыков сохраняет фактические `providerCode` и `modelName` из `AiExecutionResult` выбранного AI-подключения.

`executionSource` фиксирует результат:

- `AI` — выполнен реальный AI-вызов с полными metadata;
- `AI_METADATA_INCOMPLETE` — AI вернул неполные metadata;
- `DICTIONARY_FALLBACK` — использован словарный fallback без AI-вызова.

Колонка «Провайдер / Модель» читает эти поля из `candidateCvSkillAnalysis-browse-view`. Миграция `260922-1-addExecutionSourceToCandidateCvSkillAnalysis` добавляет `EXECUTION_SOURCE`.

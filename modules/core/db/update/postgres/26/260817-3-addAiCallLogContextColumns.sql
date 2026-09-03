-- 260817-3-addAiCallLogContextColumns.sql
-- Персонализация AI-ответов данными «Обо мне» (UserAiProfile), этап E:
-- колонки аудита передачи контекста в HUNTTECH_AI_CALL_LOG.
-- contextIncluded — флаг фактического добавления блока в system prompt;
-- contextCodePoints — размер добавленного блока (для диагностики стоимости).
-- Контент блока в лог не пишется (приватность).
-- Идемпотентно: ADD COLUMN IF NOT EXISTS.
ALTER TABLE HUNTTECH_AI_CALL_LOG
    ADD COLUMN IF NOT EXISTS CONTEXT_INCLUDED boolean;

ALTER TABLE HUNTTECH_AI_CALL_LOG
    ADD COLUMN IF NOT EXISTS CONTEXT_CODE_POINTS integer;

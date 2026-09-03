-- 260817-1-addIncludeUserContextFlag.sql
-- Персонализация AI-ответов данными «Обо мне» (UserAiProfile), этап B:
-- колонка-флаг INCLUDE_USER_CONTEXT таблицы HUNTTECH_AI_FUNCTION_CONFIGURATION.
-- Флаг управляет передачей контекста пользователя в system prompt при executeText.
-- NULL трактуется execution layer'ом по capability (текстовые — true, IMAGE — false).
-- Идемпотентно: ADD COLUMN IF NOT EXISTS (прод: cuba.automaticDatabaseUpdate=false,
-- применяется DbUpdaterEngine по db/update/**.sql; Liquibase-версия 260817-1 в db/changelog — для dev).
ALTER TABLE HUNTTECH_AI_FUNCTION_CONFIGURATION
    ADD COLUMN IF NOT EXISTS INCLUDE_USER_CONTEXT boolean;

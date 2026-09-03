-- 260817-2-setIncludeUserContextDefaults.sql
-- Персонализация AI-ответов данными «Обо мне» (UserAiProfile), этап B:
-- UPDATE-дефолты флага INCLUDE_USER_CONTEXT по матрице уместности персонализации
-- (план 2026-08-17 §4.2). Явные FALSE — детерминированные/объективные функции и
-- корпоративные артефакты; TRUE — генеративные vacancy-артефакты (legacy VACANCY_*);
-- остальным текстовым NULL-записям — TRUE; IMAGE и прочим — FALSE.
-- INSERT-only-паттерн проекта: существующие ручные настройки (не-NULL) не перезаписываются.
-- Идемпотентно: все UPDATE с guard'ом IS NULL.
UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION SET INCLUDE_USER_CONTEXT = FALSE
 WHERE INCLUDE_USER_CONTEXT IS NULL
   AND CODE IN ('STANDARDIZE_VACANCY','SKILLS_EXTRACT','TEXT_SMART_FORMAT_HTML',
                'TEXT_SMART_FORMAT_PLAIN','PROJECT_DESCRIPTION_GENERATE',
                'PROJECT_SHORT_DESCRIPTION_GENERATE','PROJECT_LOGO_IMAGE_GENERATE',
                'TEST_CONNECTION');

UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION SET INCLUDE_USER_CONTEXT = TRUE
 WHERE INCLUDE_USER_CONTEXT IS NULL
   AND CAPABILITY IN ('TEXT_GENERATION','TEXT_ANALYSIS','TEXT_TRANSFORMATION','DOCUMENT_ANALYSIS');

UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION SET INCLUDE_USER_CONTEXT = FALSE
 WHERE INCLUDE_USER_CONTEXT IS NULL;

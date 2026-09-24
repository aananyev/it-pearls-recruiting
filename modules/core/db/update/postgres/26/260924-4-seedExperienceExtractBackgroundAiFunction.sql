-- 260924-4-seedExperienceExtractBackgroundAiFunction.sql
-- Регистрация AI-функции EXPERIENCE_EXTRACT_BACKGROUND для фонового извлечения мест работы (JobHistory)

DO $$
DECLARE
    v_admin_config_id UUID;
    v_admin_model TEXT := 'glm-5.2';
BEGIN
    IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
        RAISE EXCEPTION 'AI Control Plane не мигрирован: отсутствует HUNTTECH_AI_FUNCTION_CONFIGURATION';
    END IF;

    SELECT c.ID, COALESCE(c.DEFAULT_MODEL_NAME, 'deepseek-v4-flash')
      INTO v_admin_config_id, v_admin_model
      FROM HUNTTECH_ADMIN_AI_CONFIGURATION c
     WHERE c.DELETE_TS IS NULL
       AND c.IS_ACTIVE = TRUE
     ORDER BY c.PRIORITY_ DESC NULLS LAST
     LIMIT 1;

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, INCLUDE_USER_CONTEXT, CONFIGURATION_VERSION,
        ADMIN_CONFIGURATION_ID, ADMIN_MODEL_NAME
    )
    SELECT
        'd5e7f9a1-2b3c-4d5e-af0a-8b1c2d3e4f5a'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'EXPERIENCE_EXTRACT_BACKGROUND',
        'Извлечение мест работы из резюме (фоновое)',
        'Фоновое извлечение опыта и мест работы (JobHistory) из резюме с форматированием HTML для проектов, роли и достижений.',
        'TEXT_GENERATION',
        'Ты — профессиональный специализированный модуль извлечения опыта и мест работы из резюме для HRM HuntTech.
Твоя задача — извлечь из переданного текста резюме ВСЕ места работы кандидата (от самых ранних к текущим) и вернуть СТРОГИЙ валидный JSON без markdown-блоков, без ```json и без пояснений следующей структуры:
{
  "workExperience": [
    {
      "companyName": "Название компании без форм собственности и кавычек (например: Сбер, Яндекс, Эволв, Рога и Копыта)",
      "companyDescription": "Сфера деятельности компании или null",
      "companyWebsite": "Сайт компании или null",
      "positionName": "Название должности в резюме (например: Python-разработчик, Team Lead Data Engineer)",
      "startDate": "YYYY-MM-01 или YYYY-01-01 (например: 2024-12-01)",
      "endDate": "YYYY-MM-01 (null если текущее место работы)",
      "isCurrent": false,
      "city": "Город места работы или null",
      "projectDescription": "HTML-описание проектов кандидата (кратко, связно, структурированно, используя теги <p>, <ul>, <li>)",
      "roleDescription": "HTML-описание роли и ключевых обязанностей кандидата (<p>, <ul>, <li>)",
      "achievements": "HTML-описание конкретных результатов и достижений (<p>, <ul>, <li>)",
      "formattedDutiesHtml": "<p><strong>Проекты:</strong> ...</p><p><strong>Роль и задачи:</strong> ...</p><p><strong>Достижения:</strong> ...</p>"
    }
  ]
}

ПРАВИЛА ИЗВЛЕЧЕНИЯ:
1. Выдели ВСЕ места работы из резюме без пропусков.
2. Даты: переводи текстовые даты в формат ISO (YYYY-MM-DD или YYYY-MM-01). Если год/месяц не указан, делай null. Если место работы продолжается по настоящее время: endDate = null, isCurrent = true.
3. Описания:
   - Обязательно обработай AI для максимальной легкости восприятия и профессионального вида.
   - Оформи с использованием базовых HTML-тегов: <p>, <strong>, <ul>, <li>. Запрещены теги <script>, <style>, <h1>, <h2>.
   - В поле formattedDutiesHtml объедини проекты, роль и достижения в единый презентабельный HTML-блок.
4. Верни ТОЛЬКО чистый валидный JSON-объект.',
        E'Текст резюме для извлечения мест работы:\n${sourceText}',
        0.1,
        4000,
        'ADMIN_ONLY',
        'FALLBACK_TO_ADMIN',
        TRUE,
        TRUE,
        FALSE,
        1,
        v_admin_config_id,
        v_admin_model
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'EXPERIENCE_EXTRACT_BACKGROUND'
    );
END
$$;

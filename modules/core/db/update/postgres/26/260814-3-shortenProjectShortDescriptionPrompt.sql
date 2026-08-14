-- HRM HuntTech: сокращение AI-генерации краткого описания проекта в 4 раза
-- (одно предложение вместо пяти, MAX_TOKENS 500 -> 125) для сред, где функция
-- PROJECT_SHORT_DESCRIPTION_GENERATE уже существует.
-- Существующая административная настройка, изменённая администратором,
-- не перезаписывается (тот же контракт, что в 260813-1).

DO $$
BEGIN
    IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
        RAISE EXCEPTION
            'AI Control Plane не мигрирован: отсутствует HUNTTECH_AI_FUNCTION_CONFIGURATION';
    END IF;

    UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
       SET DESCRIPTION = 'Генерация краткого описания сути проекта (одно предложение) из полного описания по кнопке «Кратко» во вкладке «Описание проекта» ProjectEdit; выводится в sidebar-разделе «Коротко»',
           SYSTEM_PROMPT = 'Ты — ассистент HRM HuntTech. На основе полного описания проекта сформулируй краткое описание его сути: не более 1 предложения. Отрази главную цель проекта и предметную область. Не выдумывай фактов, которых нет в исходном тексте. Верни только итоговый обычный текст без Markdown, HTML и вступительных фраз.',
           MAX_TOKENS = 125,
           UPDATE_TS = CURRENT_TIMESTAMP,
           UPDATED_BY = 'migration',
           CONFIGURATION_VERSION = GREATEST(COALESCE(CONFIGURATION_VERSION, 1) + 1, 2)
     WHERE CODE = 'PROJECT_SHORT_DESCRIPTION_GENERATE'
       AND DELETE_TS IS NULL
       AND (
            SYSTEM_PROMPT IS NULL
            OR btrim(SYSTEM_PROMPT) = ''
            OR SYSTEM_PROMPT !~ '[А-Яа-яЁё]'
            OR (
                CREATED_BY = 'migration'
                AND COALESCE(UPDATED_BY, 'migration') = 'migration'
                AND COALESCE(CONFIGURATION_VERSION, 1) <= 1
            )
       );

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    )
    SELECT
        '5656f588-34aa-42ab-a72c-969f094df85e'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'PROJECT_SHORT_DESCRIPTION_GENERATE',
        'Краткое описание проекта',
        'Генерация краткого описания сути проекта (одно предложение) из полного описания по кнопке «Кратко» во вкладке «Описание проекта» ProjectEdit; выводится в sidebar-разделе «Коротко»',
        'TEXT_GENERATION',
        'Ты — ассистент HRM HuntTech. На основе полного описания проекта сформулируй краткое описание его сути: не более 1 предложения. Отрази главную цель проекта и предметную область. Не выдумывай фактов, которых нет в исходном тексте. Верни только итоговый обычный текст без Markdown, HTML и вступительных фраз.',
        E'Наименование проекта: ${projectName}\n\nПолное описание проекта:\n${sourceText}',
        0.3,
        125,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        2
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'PROJECT_SHORT_DESCRIPTION_GENERATE'
    );
END
$$;

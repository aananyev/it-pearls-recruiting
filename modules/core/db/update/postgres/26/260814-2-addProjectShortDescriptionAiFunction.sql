-- HRM HuntTech: production-safe seed AI-функции генерации краткого описания проекта.
-- Скрипт INSERT-only и идемпотентный: существующая административная настройка
-- PROJECT_SHORT_DESCRIPTION_GENERATE не изменяется и не перезаписывает prompt/model/policy.

DO $$
BEGIN
    IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
        RAISE EXCEPTION
            'AI Control Plane не мигрирован: отсутствует HUNTTECH_AI_FUNCTION_CONFIGURATION';
    END IF;

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    ) VALUES (
        '5656f588-34aa-42ab-a72c-969f094df85e'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'PROJECT_SHORT_DESCRIPTION_GENERATE',
        'Краткое описание проекта',
        'Генерация краткого описания сути проекта (не более 5 предложений) из полного описания по кнопке «Кратко» во вкладке «Описание проекта» ProjectEdit; выводится в sidebar-разделе «Коротко»',
        'TEXT_GENERATION',
        'Ты — ассистент HRM HuntTech. На основе полного описания проекта сформулируй краткое описание его сути: не более 5 предложений. Отрази главную цель проекта, предметную область и ключевые особенности. Не выдумывай фактов, которых нет в исходном тексте. Верни только итоговый обычный текст без Markdown, HTML и вступительных фраз.',
        E'Наименование проекта: ${projectName}\n\nПолное описание проекта:\n${sourceText}',
        0.3,
        500,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        1
    )
    ON CONFLICT (CODE) DO NOTHING;
END
$$;

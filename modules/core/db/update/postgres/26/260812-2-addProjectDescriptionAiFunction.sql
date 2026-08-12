-- HRM HuntTech: production-safe seed AI-функции ProjectEdit upload.
-- Скрипт INSERT-only и идемпотентный: существующая административная настройка
-- PROJECT_DESCRIPTION_GENERATE не изменяется и не перезаписывает prompt/model/policy.

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
        '2f9176ea-6e73-4bca-91ae-9a6f73ca2a81'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'PROJECT_DESCRIPTION_GENERATE',
        'Обработка загруженного описания проекта',
        'Автоматическая обработка текста, загруженного во вкладке «Описание проекта» ProjectEdit',
        'DOCUMENT_ANALYSIS',
        'Ты анализируешь исходное описание проекта для HRM HuntTech. Сформируй точное структурированное описание без выдуманных фактов. Сохрани технологии, роли, ограничения и предметную область. Не используй чувствительные характеристики людей. Верни только итоговый обычный текст без Markdown и HTML.',
        E'Наименование проекта: ${projectName}\nИмя загруженного файла: ${sourceFileName}\n\nИсходный текст:\n${sourceText}',
        0.2,
        3000,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        1
    )
    ON CONFLICT (CODE) DO NOTHING;
END
$$;

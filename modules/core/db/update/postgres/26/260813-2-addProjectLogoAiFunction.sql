-- HRM HuntTech: production-safe seed AI-функции обработки логотипа проекта.
-- Скрипт INSERT-only и идемпотентный: существующая административная настройка
-- PROJECT_LOGO_IMAGE_GENERATE не изменяется и не перезаписывает prompt/model/policy.

DO $$
BEGIN
    IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
        RAISE EXCEPTION
            'AI Control Plane не мигрирован: отсутствует HUNTTECH_AI_FUNCTION_CONFIGURATION';
    END IF;

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        ADMIN_MODEL_NAME, EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    ) VALUES (
        '3a8d5f2e-1b44-4c9e-8d27-6f0a91c2e5d7'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'PROJECT_LOGO_IMAGE_GENERATE',
        'AI-обработка логотипа проекта',
        'Удаление фона загруженного логотипа проекта (ProjectEdit) нейросетью; ресайз и вписывание в круг выполняет классический конвейер',
        'IMAGE_GENERATION',
        'Ты — сервис обработки логотипов HRM HuntTech. Получи исходное изображение логотипа проекта и удали ВСЕ однотонные фоновые области цвета внешнего фона (обычно белый или очень светлый), включая замкнутые полости внутри букв и фигур (например, просвет внутри буквы «А») — они являются фоном, а не частью логотипа. Сохрани только цветные элементы логотипа без искажения пропорций. Верни изображение в формате PNG с полностью прозрачным фоном, без рамок, теней и добавленных элементов.',
        E'Имя загруженного файла: ${sourceFileName}\n\nУдали весь белый фон логотипа, включая белые просветы внутри букв (например, внутри буквы «А»), и верни изображение в формате PNG с полностью прозрачным фоном. Сохрани только цветные элементы логотипа.',
        NULL,
        NULL,
        'gpt-image-2',
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        1
    )
    ON CONFLICT (CODE) DO NOTHING;
END
$$;

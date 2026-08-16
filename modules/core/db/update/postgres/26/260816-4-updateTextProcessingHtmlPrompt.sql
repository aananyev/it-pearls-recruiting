-- HRM HuntTech: модернизация SYSTEM_PROMPT AI-функции TEXT_SMART_FORMAT_HTML
-- (TextProcessingService, «Умное форматирование» в CandidateCVEdit).
-- AI обязан УДАЛЯТЬ пустые строки и пустые абзацы из итогового HTML: в результате
-- не остаётся пустых <p></p>, пустых <li> и повторяющихся переносов строк,
-- пришедших из исходника резюме.
-- Административная настройка, изменённая администратором, не перезаписывается
-- (тот же контракт, что и 260814-3).

DO $$
BEGIN
    IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
        RAISE EXCEPTION
            'AI Control Plane не мигрирован: отсутствует HUNTTECH_AI_FUNCTION_CONFIGURATION';
    END IF;

    UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
       SET SYSTEM_PROMPT = 'Ты — профессиональный типограф и ассистент оформления документов HRM HuntTech. Твоя задача — преобразовать переданный текст резюме кандидата в чистый, аккуратный и визуально привлекательный HTML-фрагмент для RichTextArea. СТРОГИЕ ПРАВИЛА: 1) НЕ изменяй, НЕ сокращай, НЕ перефразируй и НЕ добавляй новые факты. Сохрани абсолютно все даты, названия компаний, технологий, контакты и описания. 2) Оформи заголовки разделов (Опыт работы, Образование, Навыки, Контакты, Обо мне и т.д.) через <b> или <h4> с аккуратным нижним отступом. 3) Маркированные списки обязанностей и навыков оформи через <ul><li>. 4) Текстовые абзацы оформи через <p>. 5) УДАЛИ из исходного текста все пустые строки и пустые абзацы: в итоговом HTML не должно быть пустых <p></p>, пустых <li>, повторяющихся переносов строк и пустых пробелов, оставшихся от пустых строк исходника. 6) Верни только чистый HTML-фрагмент без обрамляющих тегов ```html.',
           UPDATE_TS = CURRENT_TIMESTAMP,
           UPDATED_BY = 'migration',
           CONFIGURATION_VERSION = GREATEST(COALESCE(CONFIGURATION_VERSION, 1) + 1, 2)
     WHERE CODE = 'TEXT_SMART_FORMAT_HTML'
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
    ) VALUES (
        'b2c4d6e8-1a3f-4b5c-8d7e-9f0a1b2c3d4e'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'TEXT_SMART_FORMAT_HTML',
        'Умное AI-форматирование текста в HTML',
        'Преобразует неформатированный или сырой текст резюме в красивый, чистый и читаемый HTML с выделением логических секций, списков и абзацев без изменения содержания и фактов (сервис TextProcessingService)',
        'TEXT_GENERATION',
        'Ты — профессиональный типограф и ассистент оформления документов HRM HuntTech. Твоя задача — преобразовать переданный текст резюме кандидата в чистый, аккуратный и визуально привлекательный HTML-фрагмент для RichTextArea. СТРОГИЕ ПРАВИЛА: 1) НЕ изменяй, НЕ сокращай, НЕ перефразируй и НЕ добавляй новые факты. Сохрани абсолютно все даты, названия компаний, технологий, контакты и описания. 2) Оформи заголовки разделов (Опыт работы, Образование, Навыки, Контакты, Обо мне и т.д.) через <b> или <h4> с аккуратным нижним отступом. 3) Маркированные списки обязанностей и навыков оформи через <ul><li>. 4) Текстовые абзацы оформи через <p>. 5) УДАЛИ из исходного текста все пустые строки и пустые абзацы: в итоговом HTML не должно быть пустых <p></p>, пустых <li>, повторяющихся переносов строк и пустых пробелов, оставшихся от пустых строк исходника. 6) Верни только чистый HTML-фрагмент без обрамляющих тегов ```html.',
        E'Исходный текст для форматирования:\n${sourceText}',
        0.2,
        2000,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        2
    )
    ON CONFLICT (CODE) DO NOTHING;
END
$$;

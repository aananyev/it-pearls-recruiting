-- HRM HuntTech: Добавление AI-функции CONTACTS_EXTRACT_BACKGROUND для службы фонового определения контактов кандидатов (BL-2026-004).

DO $$
DECLARE
    v_admin_config_id UUID;
    v_admin_model TEXT := 'glm-5.2';
BEGIN
    IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
        RAISE EXCEPTION 'AI Control Plane не мигрирован: отсутствует HUNTTECH_AI_FUNCTION_CONFIGURATION';
    END IF;

    -- Ищем активное корпоративное подключение с бесплатной моделью (B.AI GLM 5.2 или другое)
    SELECT c.ID, COALESCE(c.DEFAULT_MODEL_NAME, 'glm-5.2')
      INTO v_admin_config_id, v_admin_model
      FROM HUNTTECH_ADMIN_AI_CONFIGURATION c
     WHERE c.DELETE_TS IS NULL
       AND c.IS_ACTIVE = TRUE
       AND (c.IS_FREE_MODEL = TRUE OR c.ID = 'e1a2b3c4-0001-4000-8000-000000000001'::uuid)
     ORDER BY (CASE WHEN c.ID = 'e1a2b3c4-0001-4000-8000-000000000001'::uuid THEN 0 ELSE 1 END),
              c.PRIORITY_ DESC NULLS LAST
     LIMIT 1;

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, INCLUDE_USER_CONTEXT, CONFIGURATION_VERSION,
        ADMIN_CONFIGURATION_ID, ADMIN_MODEL_NAME
    )
    SELECT
        'c4d6e8f0-1a2b-4c3d-9e5f-7a9b1c3d5e7f'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'CONTACTS_EXTRACT_BACKGROUND',
        'Извлечение контактов из резюме (фоновое)',
        'Фоновый анализ резюме кандидатов (Candidate Contact Enrichment): нейросеть возвращает строгий JSON с контактными данными кандидата (телефон, email, telegram, skype, whatsapp, viber, город проживания).',
        'TEXT_GENERATION',
        'Ты — специализированный интеллектуальный экстрактор контактных данных кандидатов HRM HuntTech. Твоя задача: найти в переданном тексте резюме контактную информацию и вернуть строгий JSON без пояснений и без markdown-разметки: {"phone": "+7 999 123-45-67", "mobilePhone": "+7 999 123-45-67", "email": "candidate@example.com", "telegramName": "username", "skypeName": "skype.login", "whatsupName": "+79991234567", "wiberName": "+79991234567", "city": "Москва"}. Правила: 1. Номера телефонов форматируй с международным кодом. 2. Telegram: укажи никнейм без символа @ и без ссылки t.me/. 3. Город: укажи город проживания кандидата без страны. 4. Если контакт не указан в тексте, верни для этого поля null. 5. Не придумывай контакты, которых нет в тексте.',
        E'Текст резюме для извлечения контактов:\n${sourceText}',
        0.1,
        500,
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
         WHERE CODE = 'CONTACTS_EXTRACT_BACKGROUND'
    );
END
$$;

-- Настройка OpenRouter Nemotron 3 Ultra (free) в качестве основной административной AI-модели
DO $$
DECLARE
    v_config_id UUID;
    v_enc_key TEXT := 'v1:mtPESKyKL4NtOZxB:h3/5/6ADh1lmD4qR0D+WQQrHzotj1UELHdQ6JKYHYnlRjwir/ZHvK75L1P15UK54RxjsF4pTFV9KYeRt/MC2udFZF+CJEs/yT9DqbscuwgbtxFLWAr7aL8w=';
BEGIN
    SELECT id INTO v_config_id FROM hunttech_admin_ai_configuration
     WHERE provider_code = 'openrouter' AND delete_ts IS NULL
     ORDER BY priority_ DESC, create_ts DESC LIMIT 1;

    IF v_config_id IS NULL THEN
        v_config_id := '1b603f1f-96b7-4704-421f-2ab05f9ce8c3'::UUID;
        INSERT INTO hunttech_admin_ai_configuration (
            id, version, create_ts, created_by, name, provider_code, api_key_encrypted,
            default_model_name, base_api_url, is_active, priority_, max_context_tokens
        ) VALUES (
            v_config_id, 1, now(), 'admin', 'OpenRouter Nemotron 3 Ultra (free)', 'openrouter',
            v_enc_key, 'nvidia/nemotron-3-ultra-550b-a55b:free', 'https://openrouter.ai/api/v1/chat/completions',
            true, 100, 10000
        );
    ELSE
        UPDATE hunttech_admin_ai_configuration
           SET name = 'OpenRouter Nemotron 3 Ultra (free)',
               provider_code = 'openrouter',
               api_key_encrypted = v_enc_key,
               default_model_name = 'nvidia/nemotron-3-ultra-550b-a55b:free',
               base_api_url = 'https://openrouter.ai/api/v1/chat/completions',
               is_active = true,
               priority_ = 100,
               update_ts = now(),
               updated_by = 'admin'
         WHERE id = v_config_id;
    END IF;

    -- Привязываем AI-функции (кроме генерации изображений) к основной административной конфигурации OpenRouter Nemotron
    UPDATE hunttech_ai_function_configuration
       SET admin_configuration_id = v_config_id,
           admin_model_name = 'nvidia/nemotron-3-ultra-550b-a55b:free',
           update_ts = now(),
           updated_by = 'admin'
     WHERE delete_ts IS NULL
       AND code <> 'PROJECT_LOGO_IMAGE_GENERATE';
END $$;

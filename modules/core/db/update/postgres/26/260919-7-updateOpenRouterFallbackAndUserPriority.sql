-- Актуализация приоритета подключения OpenRouter и оптимизация доступности AI
DO $$
DECLARE
    v_alan_id UUID;
BEGIN
    -- Находим активного пользователя alan
    SELECT id INTO v_alan_id FROM sec_user WHERE login = 'alan' AND delete_ts IS NULL LIMIT 1;

    IF v_alan_id IS NOT NULL THEN
        -- Делаем OpenRouter приоритетной и основной нейросетью пользователя alan
        UPDATE hunttech_user_ai_configuration
           SET is_primary = true,
               priority_ = 100,
               update_ts = now(),
               updated_by = 'system'
         WHERE user_id = v_alan_id
           AND provider_code = 'openrouter'
           AND delete_ts IS NULL;

        -- Для DeepSeek понижаем приоритет (т.к. баланс исчерпан)
        UPDATE hunttech_user_ai_configuration
           SET is_primary = false,
               priority_ = 10,
               update_ts = now(),
               updated_by = 'system'
         WHERE user_id = v_alan_id
           AND provider_code = 'deepseek'
           AND delete_ts IS NULL;
    END IF;
END $$;

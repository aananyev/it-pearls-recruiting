-- Дополнительное поле UserSettings для сохранения местоположения плавающей кнопки LLM-чата.
-- Идемпотентно для CUBA updateDb.

DO $$
BEGIN
    IF to_regclass('public.hunttech_user_settings') IS NULL THEN
        RAISE EXCEPTION 'HUNTTECH_USER_SETTINGS must exist before 260907-1-addLlmChatButtonPosition';
    END IF;

    ALTER TABLE HUNTTECH_USER_SETTINGS
        ADD COLUMN IF NOT EXISTS LLM_CHAT_BUTTON_POSITION varchar(255);
END
$$;


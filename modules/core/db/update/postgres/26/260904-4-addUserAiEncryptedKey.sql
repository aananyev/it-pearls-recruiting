-- Additive personal credential hardening.
-- API_KEY remains transitional; no plaintext-to-ciphertext copy is performed in SQL.

DO $$
BEGIN
    IF to_regclass('public.hunttech_user_ai_configuration') IS NULL THEN
        RAISE EXCEPTION 'HUNTTECH_USER_AI_CONFIGURATION must exist before 260904-4-addUserAiEncryptedKey';
    END IF;

    ALTER TABLE HUNTTECH_USER_AI_CONFIGURATION
        ADD COLUMN IF NOT EXISTS API_KEY_ENCRYPTED varchar(4096);
END
$$;

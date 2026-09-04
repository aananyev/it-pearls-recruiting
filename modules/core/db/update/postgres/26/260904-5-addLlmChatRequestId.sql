-- Idempotency key for chat retries; pre-existing messages remain nullable.

DO $$
BEGIN
    IF to_regclass('public.hunttech_llm_chat_message') IS NULL THEN
        RAISE EXCEPTION 'HUNTTECH_LLM_CHAT_MESSAGE must exist before 260904-5-addLlmChatRequestId';
    END IF;

    ALTER TABLE HUNTTECH_LLM_CHAT_MESSAGE
        ADD COLUMN IF NOT EXISTS REQUEST_ID varchar(64);
    CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_LLM_CHAT_MESSAGE_REQUEST
        ON HUNTTECH_LLM_CHAT_MESSAGE (REQUEST_ID);
END
$$;

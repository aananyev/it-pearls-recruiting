-- Provider request ID and manual UNKNOWN_PENDING reconciliation audit.

DO $$
BEGIN
    IF to_regclass('public.hunttech_llm_chat_message') IS NULL
       OR to_regclass('public.hunttech_llm_chat_quota_reservation') IS NULL THEN
        RAISE EXCEPTION 'LLM chat tables must exist before 260904-6-addLlmChatReconciliationAudit';
    END IF;

    ALTER TABLE HUNTTECH_LLM_CHAT_MESSAGE
        ADD COLUMN IF NOT EXISTS PROVIDER_REQUEST_ID varchar(128);
    ALTER TABLE HUNTTECH_LLM_CHAT_QUOTA_RESERVATION
        ADD COLUMN IF NOT EXISTS PROVIDER_REQUEST_ID varchar(128),
        ADD COLUMN IF NOT EXISTS RECONCILED_BY varchar(50),
        ADD COLUMN IF NOT EXISTS RECONCILED_AT timestamp;
    CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_LLM_CHAT_QUOTA_PROVIDER_REQUEST
        ON HUNTTECH_LLM_CHAT_QUOTA_RESERVATION (PROVIDER_REQUEST_ID);
END
$$;

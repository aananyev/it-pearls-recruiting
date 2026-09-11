-- Установка дефолтной месячной квоты 10 000 токенов для LLM-чата
-- и добавление индивидуальных квот до 10 000 для всех активных пользователей системы.
-- Идемпотентно для CUBA updateDb.

UPDATE hunttech_ai_function_configuration
SET default_monthly_token_quota = 10000
WHERE code = 'LLM_CHAT' AND (default_monthly_token_quota IS NULL OR default_monthly_token_quota < 10000);

INSERT INTO hunttech_llm_user_quota_override (
    id, version, create_ts, created_by, user_id, monthly_quota_tokens, effective_from, reason
)
SELECT
    gen_random_uuid(), 1, CURRENT_TIMESTAMP, 'admin', u.id, 10000, CURRENT_DATE,
    'Лимит LLM-чата для активного пользователя: 10 000 токенов в календарный месяц'
FROM sec_user u
WHERE u.active = true AND u.delete_ts IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM hunttech_llm_user_quota_override o
      WHERE o.user_id = u.id AND o.delete_ts IS NULL
        AND o.monthly_quota_tokens >= 10000
        AND o.effective_from <= CURRENT_DATE
        AND (o.effective_to IS NULL OR o.effective_to >= CURRENT_DATE)
  );

UPDATE hunttech_llm_chat_quota_period
SET quota_tokens = 10000
WHERE quota_tokens < 10000 AND delete_ts IS NULL;

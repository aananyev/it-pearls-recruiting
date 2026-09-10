-- Добавление дополнительных 100 000 токенов для пользователя alan
-- и установка месячной индивидуальной квоты 110 000 токенов.
-- Идемпотентно для CUBA updateDb.

DELETE FROM hunttech_llm_user_quota_override
WHERE user_id IN (SELECT id FROM sec_user WHERE login = 'alan' AND delete_ts IS NULL);

INSERT INTO hunttech_llm_user_quota_override (
    id, version, create_ts, created_by, user_id, monthly_quota_tokens, effective_from, reason
)
SELECT
    gen_random_uuid(), 1, CURRENT_TIMESTAMP, 'admin', u.id, 110000, CURRENT_DATE,
    'Дополнительно 100 000 токенов в месяц для пользователя alan (итого 110 000)'
FROM sec_user u
WHERE u.login = 'alan' AND u.delete_ts IS NULL
ORDER BY u.create_ts DESC
LIMIT 1;

UPDATE hunttech_llm_chat_quota_period
SET quota_tokens = 110000
WHERE user_id IN (SELECT id FROM sec_user WHERE login = 'alan' AND delete_ts IS NULL)
  AND period_start = DATE_TRUNC('month', CURRENT_DATE)
  AND delete_ts IS NULL;

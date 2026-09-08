-- Установка безлимитной месячной квоты пользования административным API для пользователя alan
-- Идемпотентно для CUBA updateDb

DELETE FROM hunttech_llm_user_quota_override
WHERE user_id IN (SELECT id FROM sec_user WHERE login = 'alan' AND delete_ts IS NULL);

INSERT INTO hunttech_llm_user_quota_override (id, version, create_ts, created_by, user_id, quota_tokens, effective_from, note)
SELECT gen_random_uuid(), 1, current_timestamp, 'admin', u.id, 2147483647, '2026-01-01', 'Безлимитная месячная квота административного API для пользователя alan'
FROM sec_user u
WHERE u.login = 'alan' AND u.delete_ts IS NULL
ORDER BY u.create_ts DESC
LIMIT 1;

UPDATE hunttech_llm_chat_quota_period
SET quota_tokens = 2147483647
WHERE user_id IN (SELECT id FROM sec_user WHERE login = 'alan' AND delete_ts IS NULL);

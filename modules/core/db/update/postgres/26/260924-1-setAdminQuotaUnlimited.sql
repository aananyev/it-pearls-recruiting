-- Установка безлимитной квоты (-1) для пользователя admin
-- Идемпотентно для CUBA updateDb

DELETE FROM hunttech_llm_user_quota_override
WHERE user_id IN (SELECT id FROM sec_user WHERE login = 'admin' AND delete_ts IS NULL);

INSERT INTO hunttech_llm_user_quota_override (id, version, create_ts, created_by, user_id, monthly_quota_tokens, effective_from, reason)
SELECT gen_random_uuid(), 1, current_timestamp, 'admin', u.id, -1, '2026-01-01', 'Безлимитная квота администратора системы'
FROM sec_user u
WHERE u.login = 'admin' AND u.delete_ts IS NULL
ORDER BY u.create_ts DESC
LIMIT 1;

UPDATE hunttech_llm_chat_quota_period
SET quota_tokens = -1
WHERE user_id IN (SELECT id FROM sec_user WHERE login = 'admin' AND delete_ts IS NULL);

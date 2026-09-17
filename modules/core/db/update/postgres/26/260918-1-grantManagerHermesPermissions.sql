-- Выдача специфических прав hunttech.ai.useLocalChat и hunttech.ai.useManagerHermesWrite
-- для роли Manager (в UI отображается как «Директор»).
-- Обычным ролям (Researcher, Recruiter, Headhunter и др.) эти права не выдаются.

INSERT INTO sec_permission (id, create_ts, version, permission_type, target, value_, role_id)
SELECT 
    gen_random_uuid(), 
    CURRENT_TIMESTAMP, 
    1, 
    40, 
    'hunttech.ai.useLocalChat', 
    1, 
    r.id
FROM sec_role r
WHERE r.name = 'Manager'
  AND NOT EXISTS (
      SELECT 1 FROM sec_permission p 
      WHERE p.role_id = r.id 
        AND p.permission_type = 40 
        AND p.target = 'hunttech.ai.useLocalChat'
  );

INSERT INTO sec_permission (id, create_ts, version, permission_type, target, value_, role_id)
SELECT 
    gen_random_uuid(), 
    CURRENT_TIMESTAMP, 
    1, 
    40, 
    'hunttech.ai.useManagerHermesWrite', 
    1, 
    r.id
FROM sec_role r
WHERE r.name = 'Manager'
  AND NOT EXISTS (
      SELECT 1 FROM sec_permission p 
      WHERE p.role_id = r.id 
        AND p.permission_type = 40 
        AND p.target = 'hunttech.ai.useManagerHermesWrite'
  );

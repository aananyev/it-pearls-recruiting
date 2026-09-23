-- Локальная настройка роли и технического пользователя для External Integration API (BL-2026-030, BL-2026-029).
-- ТОЛЬКО ДЛЯ ЛОКАЛЬНОЙ/ТЕСТОВОЙ БАЗЫ. На проде пользователи создаются администратором с секретными паролями.
-- Применение: PGPASSWORD=cuba psql -h 127.0.0.1 -U cuba -d hunttech -v ON_ERROR_STOP=1 -f scripts/rest_integration_setup.sql
-- Идемпотентен: повторный запуск безопасен.

-- 1. Роль "REST Внешняя интеграция" (security scope GENERIC_UI, совпадает с cuba.rest.securityScope)
INSERT INTO sec_role (id, create_ts, version, name, role_type, security_scope, is_default_role)
VALUES ('b2849201-9a7c-48be-9fae-d4c391748201', now(), 1, 'REST Внешняя интеграция', 20, 'GENERIC_UI', false)
ON CONFLICT (id) DO NOTHING;

-- 2. Техпользователь ext-integration-client (пароль rest-local-pass-2026, хэш bcrypt)
INSERT INTO sec_user (id, create_ts, version, login, login_lc, password, password_encryption, name, active, group_id, dtype, language_)
VALUES
 ('c3958402-9e8a-49cf-8bcd-f5d492859302', now(), 1, 'ext-integration-client', 'ext-integration-client',
  '$2a$10$0Spa3K5y7BC31MWnA2KcouNxO5XJpCewKloAVEwFnWxbQHprLUgPO', 'bcrypt',
  'Внешняя интеграция: клиент API', true, '0fa2b1a5-1d68-4d69-9fbd-dff348347f93', 'hunttech_ExtUser', 'ru')
ON CONFLICT (id) DO NOTHING;

-- 3. Назначение роли пользователю с гарантией идемпотентности
INSERT INTO sec_user_role (id, create_ts, version, user_id, role_id, role_name)
SELECT (md5(random()::text || clock_timestamp()::text))::uuid, now(), 1, u.id, r.id, r.name
FROM sec_user u, sec_role r
WHERE u.login = 'ext-integration-client' AND r.name = 'REST Внешняя интеграция'
  AND NOT EXISTS (
      SELECT 1 FROM sec_user_role sur
      WHERE sur.user_id = u.id AND sur.role_id = r.id AND sur.delete_ts IS NULL
  );

-- 4. Права роли (permission_type: 20 = entity read, 40 = specific/service)
INSERT INTO sec_permission (id, create_ts, version, permission_type, target, value_, role_id)
SELECT (md5(random()::text || clock_timestamp()::text))::uuid, now(), 1, p.permission_type, p.target, p.value_, r.id
FROM sec_role r, (VALUES
    (40, 'cuba.restApi.enabled', 1),
    (40, 'cuba.rest.service.hunttech_ExternalReferenceDataService.getCities', 1),
    (40, 'cuba.rest.service.hunttech_ExternalReferenceDataService.getPositions', 1),
    (40, 'cuba.rest.service.hunttech_ExternalReferenceDataService.getGrades', 1),
    (40, 'cuba.rest.service.hunttech_ExternalReferenceDataService.getInteractionTypes', 1),
    (40, 'cuba.rest.service.hunttech_ExternalReferenceDataService.getSkills', 1),
    (40, 'cuba.rest.service.hunttech_ExternalReferenceDataService.getCountries', 1),
    (20, 'hunttech_City:read', 1),
    (20, 'hunttech_Position:read', 1),
    (20, 'hunttech_Grade:read', 1),
    (20, 'hunttech_Country:read', 1),
    (20, 'hunttech_SkillTree:read', 1),
    (20, 'hunttech_Iteraction:read', 1)
) AS p(permission_type, target, value_)
WHERE r.name = 'REST Внешняя интеграция'
  AND NOT EXISTS (
      SELECT 1 FROM sec_permission sp
      WHERE sp.role_id = r.id AND sp.permission_type = p.permission_type AND sp.target = p.target AND sp.value_ = p.value_ AND sp.delete_ts IS NULL
  );

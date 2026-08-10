-- Локальная настройка REST API v2 HRM HuntTech (роль + техпользователи).
-- ТОЛЬКО ДЛЯ ЛОКАЛЬНОЙ БАЗЫ. На проде создавать через UI администрирования и с реальными паролями.
-- Применение: PGPASSWORD=cuba psql -h 127.0.0.1 -U cuba -d hunttech -v ON_ERROR_STOP=1 -f scripts/rest_local_setup.sql
-- Идемпотентен: повторный запуск ничего не меняет.
-- Пароль техпользователей (локальный): rest-local-pass-2026 (bcrypt-хэш ниже).
-- OAuth2-клиент (web-app.properties): hrm-rest / local-hrm-rest-secret-2026.

-- 1. Роль (STANDARD=20, security scope GENERIC_UI — совпадает с cuba.rest.securityScope)
INSERT INTO sec_role (id, create_ts, version, name, role_type, security_scope, is_default_role)
VALUES ('9b1770a5-ce6a-432a-9239-0c62222a5c89', now(), 1, 'REST чтение', 20, 'GENERIC_UI', false)
ON CONFLICT (id) DO NOTHING;

-- 2. Техпользователи (group_id = группа alan; dtype = hunttech_ExtUser; пароль rest-local-pass-2026)
INSERT INTO sec_user (id, create_ts, version, login, login_lc, password, password_encryption, name, active, group_id, dtype, language_)
VALUES
 ('8528a6f3-ebd6-4c17-9f44-f74d3a7f6303', now(), 1, 'site-reader', 'site-reader', '$2a$10$0Spa3K5y7BC31MWnA2KcouNxO5XJpCewKloAVEwFnWxbQHprLUgPO', 'bcrypt', 'Сайт: чтение вакансий', true, '0fa2b1a5-1d68-4d69-9fbd-dff348347f93', 'hunttech_ExtUser', 'ru'),
 ('7aac6683-b455-46af-ac80-54965bc9a94d', now(), 1, 'rest-checker', 'rest-checker', '$2a$10$0Spa3K5y7BC31MWnA2KcouNxO5XJpCewKloAVEwFnWxbQHprLUgPO', 'bcrypt', 'REST: проверки и отчёты', true, '0fa2b1a5-1d68-4d69-9fbd-dff348347f93', 'hunttech_ExtUser', 'ru')
ON CONFLICT (id) DO NOTHING;

-- 3. Назначение роли пользователям (уникальность по (user_id, role_id))
INSERT INTO sec_user_role (id, create_ts, version, user_id, role_id, role_name)
SELECT (md5(random()::text || clock_timestamp()::text))::uuid, now(), 1, u.id, r.id, r.name
FROM sec_user u, sec_role r
WHERE u.login IN ('site-reader', 'rest-checker') AND r.name = 'REST чтение'
ON CONFLICT DO NOTHING;

-- 4. Права роли (типы: 20 = entity read, 30 = entity attr deny, 40 = specific)
INSERT INTO sec_permission (id, create_ts, version, permission_type, target, value_, role_id)
SELECT (md5(random()::text || clock_timestamp()::text))::uuid, now(), 1, p.permission_type, p.target, p.value_, r.id
FROM sec_role r, (VALUES
    (40, 'cuba.restApi.enabled', 1),
    (20, 'hunttech_OpenPosition:read', 1),
    (20, 'hunttech_City:read', 1),
    (20, 'hunttech_Position:read', 1),
    (20, 'hunttech_Project:read', 1),
    (20, 'hunttech_Grade:read', 1),
    (20, 'hunttech_Country:read', 1),
    (20, 'hunttech_Company:read', 1),
    (20, 'hunttech_SkillTree:read', 1),
    -- Финансовые/внутренние атрибуты OpenPosition: запрет (второй контур защиты помимо view)
    (30, 'hunttech_OpenPosition:outstaffingCost', 0),
    (30, 'hunttech_OpenPosition:percentComissionOfCompany', 0),
    (30, 'hunttech_OpenPosition:percentSalaryOfResearcher', 0),
    (30, 'hunttech_OpenPosition:percentSalaryOfRecrutier', 0),
    (30, 'hunttech_OpenPosition:typeCompanyComission', 0),
    (30, 'hunttech_OpenPosition:useTaxNDFL', 0),
    (30, 'hunttech_OpenPosition:salaryIE', 0),
    (30, 'hunttech_OpenPosition:memoForInterview', 0),
    (30, 'hunttech_OpenPosition:rawDescription', 0),
    (30, 'hunttech_OpenPosition:searchMap', 0),
    (30, 'hunttech_OpenPosition:interviewPlan', 0),
    (30, 'hunttech_OpenPosition:interviewChecklist', 0),
    (30, 'hunttech_OpenPosition:openPositionComments', 0),
    (30, 'hunttech_OpenPosition:someFiles', 0)
) AS p(permission_type, target, value_)
WHERE r.name = 'REST чтение'
ON CONFLICT DO NOTHING;

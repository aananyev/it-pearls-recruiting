-- Pre-seed корпоративных Яндекс-календарей для PostgreSQL

-- 1. Основной корпоративный календарь собеседований с заказчиками «Hunttech у заказчика»
INSERT INTO HUNTTECH_CORP_YANDEX_CAL (
    ID, VERSION, CREATE_TS, CREATED_BY, UPDATE_TS, UPDATED_BY, DELETE_TS, DELETED_BY,
    NAME, CALENDAR_PATH, ACCOUNT_EMAIL, CALENDAR_BASE_URL, IS_DEFAULT, ACTIVE, DESCRIPTION, OAUTH_TOKEN_ENCRYPTED
)
SELECT 
    '26091400-0000-4000-a000-000000000001'::uuid,
    1,
    now(),
    'admin',
    now(),
    'admin',
    null,
    null,
    'Hunttech у заказчика',
    '/calendars/alan%40hunttech.ru/events-34179601/',
    'alan@hunttech.ru',
    'https://caldav.yandex.ru',
    true,
    true,
    'Основной корпоративный календарь для проведения собеседований с заказчиками',
    (SELECT c.OAUTH_TOKEN_ENCRYPTED FROM HUNTTECH_USER_YANDEX_CONFIG c JOIN SEC_USER u ON u.ID = c.USER_ID WHERE lower(u.LOGIN) = 'alan' AND c.DELETE_TS IS NULL AND u.DELETE_TS IS NULL LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM HUNTTECH_CORP_YANDEX_CAL WHERE CALENDAR_PATH = '/calendars/alan%40hunttech.ru/events-34179601/' AND DELETE_TS IS NULL
);

-- 2. Корпоративный календарь общих встреч и событий аккаунта alan@hunttech.ru
INSERT INTO HUNTTECH_CORP_YANDEX_CAL (
    ID, VERSION, CREATE_TS, CREATED_BY, UPDATE_TS, UPDATED_BY, DELETE_TS, DELETED_BY,
    NAME, CALENDAR_PATH, ACCOUNT_EMAIL, CALENDAR_BASE_URL, IS_DEFAULT, ACTIVE, DESCRIPTION, OAUTH_TOKEN_ENCRYPTED
)
SELECT 
    '26091400-0000-4000-a000-000000000002'::uuid,
    1,
    now(),
    'admin',
    now(),
    'admin',
    null,
    null,
    'Мои события (alan@hunttech.ru)',
    '/calendars/alan%40hunttech.ru/events-32367521/',
    'alan@hunttech.ru',
    'https://caldav.yandex.ru',
    false,
    true,
    'Корпоративный календарь общих встреч и событий аккаунта alan@hunttech.ru',
    (SELECT c.OAUTH_TOKEN_ENCRYPTED FROM HUNTTECH_USER_YANDEX_CONFIG c JOIN SEC_USER u ON u.ID = c.USER_ID WHERE lower(u.LOGIN) = 'alan' AND c.DELETE_TS IS NULL AND u.DELETE_TS IS NULL LIMIT 1)
WHERE NOT EXISTS (
    SELECT 1 FROM HUNTTECH_CORP_YANDEX_CAL WHERE CALENDAR_PATH = '/calendars/alan%40hunttech.ru/events-32367521/' AND DELETE_TS IS NULL
);

-- Синхронизация токена, если запись уже была создана без токена
UPDATE HUNTTECH_CORP_YANDEX_CAL
SET OAUTH_TOKEN_ENCRYPTED = (SELECT c.OAUTH_TOKEN_ENCRYPTED FROM HUNTTECH_USER_YANDEX_CONFIG c JOIN SEC_USER u ON u.ID = c.USER_ID WHERE lower(u.LOGIN) = 'alan' AND c.DELETE_TS IS NULL AND u.DELETE_TS IS NULL LIMIT 1)
WHERE DELETE_TS IS NULL
  AND (OAUTH_TOKEN_ENCRYPTED IS NULL OR OAUTH_TOKEN_ENCRYPTED = '')
  AND ACCOUNT_EMAIL = 'alan@hunttech.ru'
  AND EXISTS (SELECT 1 FROM HUNTTECH_USER_YANDEX_CONFIG c JOIN SEC_USER u ON u.ID = c.USER_ID WHERE lower(u.LOGIN) = 'alan' AND c.DELETE_TS IS NULL AND u.DELETE_TS IS NULL AND c.OAUTH_TOKEN_ENCRYPTED IS NOT NULL);

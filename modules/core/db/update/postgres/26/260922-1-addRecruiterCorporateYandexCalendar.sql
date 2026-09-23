-- Подключение корпоративного календаря Hunttech у рекрутеров для пользователя alan.
-- Токен не хранится в миграции: копируется только уже зашифрованное значение.

DO $$
DECLARE
    v_token varchar(4096);
BEGIN
    SELECT c.OAUTH_TOKEN_ENCRYPTED
      INTO v_token
      FROM HUNTTECH_USER_YANDEX_CONFIG c
      JOIN SEC_USER u ON u.ID = c.USER_ID
     WHERE lower(u.LOGIN) = 'alan'
       AND c.DELETE_TS IS NULL
       AND u.DELETE_TS IS NULL
       AND nullif(trim(c.OAUTH_TOKEN_ENCRYPTED), '') IS NOT NULL
     LIMIT 1;

    IF v_token IS NULL THEN
        RAISE EXCEPTION 'Cannot seed recruiter Yandex calendar: active OAuth token for user alan is missing';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM HUNTTECH_CORP_YANDEX_CAL
         WHERE DELETE_TS IS NULL
           AND NAME = 'Hunttech у рекрутеров'
           AND CALENDAR_PATH <> '/calendars/alan%40hunttech.ru/events-34179663/'
    ) THEN
        RAISE EXCEPTION 'Cannot seed recruiter Yandex calendar: name is already used with another calendar path';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM HUNTTECH_CORP_YANDEX_CAL
         WHERE DELETE_TS IS NULL
           AND CALENDAR_PATH = '/calendars/alan%40hunttech.ru/events-34179663/'
           AND ACCOUNT_EMAIL IS NOT NULL
           AND lower(ACCOUNT_EMAIL) <> 'alan@hunttech.ru'
    ) THEN
        RAISE EXCEPTION 'Cannot seed recruiter Yandex calendar: target path belongs to another account';
    END IF;

    IF EXISTS (
        SELECT 1
          FROM HUNTTECH_CORP_YANDEX_CAL
         WHERE ID = '26092200-0000-4000-a000-000000000003'::uuid
           AND (DELETE_TS IS NOT NULL OR CALENDAR_PATH <> '/calendars/alan%40hunttech.ru/events-34179663/')
    ) THEN
        RAISE EXCEPTION 'Cannot seed recruiter Yandex calendar: managed id is already used by another row';
    END IF;
END $$;

INSERT INTO HUNTTECH_CORP_YANDEX_CAL (
    ID, VERSION, CREATE_TS, CREATED_BY, UPDATE_TS, UPDATED_BY, DELETE_TS, DELETED_BY,
    NAME, CALENDAR_PATH, ACCOUNT_EMAIL, CALENDAR_BASE_URL, IS_DEFAULT, ACTIVE, DESCRIPTION, OAUTH_TOKEN_ENCRYPTED
)
SELECT
    '26092200-0000-4000-a000-000000000003'::uuid,
    1,
    now(),
    'admin',
    now(),
    'admin',
    null,
    null,
    'Hunttech у рекрутеров',
    '/calendars/alan%40hunttech.ru/events-34179663/',
    'alan@hunttech.ru',
    'https://caldav.yandex.ru',
    false,
    true,
    'Корпоративный календарь внутренних встреч и собеседований с рекрутерами HuntTech',
    c.OAUTH_TOKEN_ENCRYPTED
FROM HUNTTECH_USER_YANDEX_CONFIG c
JOIN SEC_USER u ON u.ID = c.USER_ID
WHERE lower(u.LOGIN) = 'alan'
  AND c.DELETE_TS IS NULL
  AND u.DELETE_TS IS NULL
  AND nullif(trim(c.OAUTH_TOKEN_ENCRYPTED), '') IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
        FROM HUNTTECH_CORP_YANDEX_CAL
       WHERE DELETE_TS IS NULL
         AND CALENDAR_PATH = '/calendars/alan%40hunttech.ru/events-34179663/'
  )
LIMIT 1;

-- Если запись уже была создана без токена, синхронизируем только зашифрованное значение.
UPDATE HUNTTECH_CORP_YANDEX_CAL target
   SET OAUTH_TOKEN_ENCRYPTED = source.OAUTH_TOKEN_ENCRYPTED
  FROM (
      SELECT c.OAUTH_TOKEN_ENCRYPTED
        FROM HUNTTECH_USER_YANDEX_CONFIG c
        JOIN SEC_USER u ON u.ID = c.USER_ID
       WHERE lower(u.LOGIN) = 'alan'
         AND c.DELETE_TS IS NULL
         AND u.DELETE_TS IS NULL
         AND nullif(trim(c.OAUTH_TOKEN_ENCRYPTED), '') IS NOT NULL
       LIMIT 1
  ) source
 WHERE target.DELETE_TS IS NULL
   AND target.CALENDAR_PATH = '/calendars/alan%40hunttech.ru/events-34179663/'
   AND (target.OAUTH_TOKEN_ENCRYPTED IS NULL OR target.OAUTH_TOKEN_ENCRYPTED = '');

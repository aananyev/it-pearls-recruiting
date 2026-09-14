-- Создание таблицы справочника корпоративных Яндекс Календарей HUNTTECH_CORP_YANDEX_CAL
CREATE TABLE IF NOT EXISTS HUNTTECH_CORP_YANDEX_CAL (
    ID uuid NOT NULL,
    VERSION integer NOT NULL,
    CREATE_TS timestamp without time zone,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp without time zone,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp without time zone,
    DELETED_BY varchar(50),
    NAME varchar(255) NOT NULL,
    CALENDAR_PATH varchar(512) NOT NULL,
    ACCOUNT_EMAIL varchar(255),
    CALENDAR_BASE_URL varchar(512) DEFAULT 'https://caldav.yandex.ru',
    IS_DEFAULT boolean DEFAULT false,
    ACTIVE boolean DEFAULT true,
    DESCRIPTION varchar(1024),
    OAUTH_TOKEN_ENCRYPTED varchar(4096),
    PRIMARY KEY (ID)
);

-- Добавление колонок привязки к событию календаря в таблицу HUNTTECH_ITERACTION_LIST
ALTER TABLE HUNTTECH_ITERACTION_LIST ADD COLUMN IF NOT EXISTS CALENDAR_EVENT_ID varchar(255);
ALTER TABLE HUNTTECH_ITERACTION_LIST ADD COLUMN IF NOT EXISTS CALENDAR_ID varchar(512);
ALTER TABLE HUNTTECH_ITERACTION_LIST ADD COLUMN IF NOT EXISTS CALENDAR_SYNC_STATE varchar(50);
ALTER TABLE HUNTTECH_ITERACTION_LIST ADD COLUMN IF NOT EXISTS ADD_TO_CALENDAR boolean DEFAULT false;

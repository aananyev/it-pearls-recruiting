-- =============================================================================
-- HRM HuntTech: идемпотентная синхронизация схемы PostgreSQL с моделью entity
-- Дата: 2026-06-30
-- Назначение: закрыть отставание live БД от кода (пакеты 260627, 260629)
-- НЕ выполнять автоматически — только ручной запуск после бэкапа
--
-- Эквивалент (неидемпотентный): modules/core/db/update/postgres/26/260627-*.sql
--                               modules/core/db/update/postgres/26/260629-*.sql
-- =============================================================================
-- Обёртка транзакции — снаружи (psql -v ON_ERROR_STOP=1 -f ... в одной сессии
-- с BEGIN/COMMIT), не внутри файла: вложенный COMMIT фиксирует изменения
-- независимо от внешнего ROLLBACK.

-- -----------------------------------------------------------------------------
-- 1. ITPEARLS_OPEN_POSITION — AI / текстовые поля (OpenPosition entity)
-- -----------------------------------------------------------------------------
ALTER TABLE ITPEARLS_OPEN_POSITION ADD COLUMN IF NOT EXISTS RAW_DESCRIPTION text;
ALTER TABLE ITPEARLS_OPEN_POSITION ADD COLUMN IF NOT EXISTS INTERVIEW_CHECKLIST text;
ALTER TABLE ITPEARLS_OPEN_POSITION ADD COLUMN IF NOT EXISTS SEARCH_MAP text;
ALTER TABLE ITPEARLS_OPEN_POSITION ADD COLUMN IF NOT EXISTS INTERVIEW_PLAN text;

-- -----------------------------------------------------------------------------
-- 2. ITPEARLS_USER_AI_CONFIGURATION (UserAiConfiguration entity)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ITPEARLS_USER_AI_CONFIGURATION (
    ID uuid,
    VERSION integer NOT NULL,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    USER_ID uuid NOT NULL,
    PROVIDER_CODE varchar(64),
    API_KEY varchar(512),
    DEFAULT_MODEL_NAME varchar(128),
    IS_ACTIVE boolean,
    PRIMARY KEY (ID)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_itpearls_user_ai_configuration_on_user'
    ) THEN
        ALTER TABLE ITPEARLS_USER_AI_CONFIGURATION
            ADD CONSTRAINT FK_ITPEARLS_USER_AI_CONFIGURATION_ON_USER
            FOREIGN KEY (USER_ID) REFERENCES SEC_USER(ID);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS IDX_ITPEARLS_USER_AI_CONFIGURATION_USER
    ON ITPEARLS_USER_AI_CONFIGURATION (USER_ID);

-- -----------------------------------------------------------------------------
-- 3. ITPEARLS_VACANCY_PROMPT_TEMPLATE (VacancyPromptTemplate entity)
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ITPEARLS_VACANCY_PROMPT_TEMPLATE (
    ID uuid,
    VERSION integer NOT NULL,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    CODE varchar(64) NOT NULL,
    NAME varchar(255) NOT NULL,
    PROMPT_TEXT text,
    SYSTEM_CONTEXT varchar(1000),
    TEMPERATURE double precision DEFAULT 0.7,
    PRIMARY KEY (ID)
);

CREATE UNIQUE INDEX IF NOT EXISTS IDX_ITPEARLS_VACANCY_PROMPT_TEMPLATE_CODE
    ON ITPEARLS_VACANCY_PROMPT_TEMPLATE (CODE)
    WHERE DELETE_TS IS NULL;

-- -----------------------------------------------------------------------------
-- 4. SEC_USER / ExtUser — officialPhoto, userAvatar
-- -----------------------------------------------------------------------------
ALTER TABLE SEC_USER ADD COLUMN IF NOT EXISTS OFFICIAL_PHOTO_ID uuid;
ALTER TABLE SEC_USER ADD COLUMN IF NOT EXISTS USER_AVATAR_ID uuid;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sec_user_on_official_photo') THEN
        ALTER TABLE SEC_USER
            ADD CONSTRAINT FK_SEC_USER_ON_OFFICIAL_PHOTO
            FOREIGN KEY (OFFICIAL_PHOTO_ID) REFERENCES SYS_FILE(ID);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_sec_user_on_user_avatar') THEN
        ALTER TABLE SEC_USER
            ADD CONSTRAINT FK_SEC_USER_ON_USER_AVATAR
            FOREIGN KEY (USER_AVATAR_ID) REFERENCES SYS_FILE(ID);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS IDX_SEC_USER_ON_OFFICIAL_PHOTO ON SEC_USER (OFFICIAL_PHOTO_ID);
CREATE INDEX IF NOT EXISTS IDX_SEC_USER_ON_USER_AVATAR ON SEC_USER (USER_AVATAR_ID);

-- -----------------------------------------------------------------------------
-- 5. Миграция данных фото (однократно, безопасно при повторном запуске)
-- -----------------------------------------------------------------------------
UPDATE SEC_USER
SET OFFICIAL_PHOTO_ID = IMAGE_ID
WHERE IMAGE_ID IS NOT NULL
  AND OFFICIAL_PHOTO_ID IS NULL;

UPDATE SEC_USER u
SET USER_AVATAR_ID = us.IMAGE_ID
FROM ITPEARLS_USER_SETTINGS us
WHERE us.USER_ID = u.ID
  AND us.IMAGE_ID IS NOT NULL
  AND u.USER_AVATAR_ID IS NULL;

UPDATE SEC_USER
SET USER_AVATAR_ID = OFFICIAL_PHOTO_ID
WHERE USER_AVATAR_ID IS NULL
  AND OFFICIAL_PHOTO_ID IS NOT NULL;

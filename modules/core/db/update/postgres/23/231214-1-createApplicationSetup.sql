create table ITPEARLS_APPLICATION_SETUP (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    APPLICATION_LOGO_ID uuid,
    APPLICATION_ICON_ID uuid,
    TELEGRAM_TOKEN varchar(128),
    TELEGRAM_CHAT_OPEN_POSITION varchar(128),
    --
    primary key (ID)
);
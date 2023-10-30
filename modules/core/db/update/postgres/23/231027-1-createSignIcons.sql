create table ITPEARLS_SIGN_ICONS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    TITLE_END varchar(25),
    TITLE_RU varchar(25),
    ICON_NAME varchar(40),
    --
    primary key (ID)
);
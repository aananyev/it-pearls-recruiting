create table ITPEARLS_CURRENCY (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CURRENCY_LONG_NAME varchar(128) not null,
    CURRENCY_SHORT_NAME varchar(3) not null,
    --
    primary key (ID)
);
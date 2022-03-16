create table ITPEARLS_POSSIBLE_NAMES (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    POSITION_ID uuid not null,
    POSSIBLE_EN_NAME_POSIION varchar(128) not null,
    POSSIBLE_NAME varchar(128) not null,
    --
    primary key (ID)
);
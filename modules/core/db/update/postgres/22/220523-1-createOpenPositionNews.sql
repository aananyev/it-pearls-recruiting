create table ITPEARLS_OPEN_POSITION_NEWS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    OPEN_POSITION_ID uuid not null,
    DATE_NEWS timestamp not null,
    COMMENT_ text not null,
    AUTHOR_ID uuid not null,
    --
    primary key (ID)
);
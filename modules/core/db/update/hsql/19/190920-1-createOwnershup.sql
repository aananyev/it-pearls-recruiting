create table ITPEARLS_OWNERSHUP (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    SHORT_TYPE varchar(7) not null,
    LONG_TYPE varchar(30) not null,
    --
    primary key (ID)
);
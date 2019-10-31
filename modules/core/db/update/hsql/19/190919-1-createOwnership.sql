create table ITPEARLS_OWNERSHIP (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    OWNERSHIP_LONG_NAME varchar(50) not null,
    OWNERSHIP_SHORT_NAME varchar(7),
    --
    primary key (ID)
);
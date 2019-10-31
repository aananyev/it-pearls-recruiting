create table ITPEARLS_OPEN_POSITION (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    PROJECT_NAME_ID varchar(36),
    COMPANY_NAME_ID varchar(36),
    COMMENT_ longvarchar,
    --
    primary key (ID)
);
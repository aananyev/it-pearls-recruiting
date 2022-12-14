create table ITPEARLS_APPLICATION_RECRUITMENT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    STAFFING_TABLE_ID uuid not null,
    AMOUNT integer not null,
    EXIT_DATE date not null,
    COMMENT_ text,
    --
    primary key (ID)
);
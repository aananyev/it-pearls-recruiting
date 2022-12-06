create table ITPEARLS_STAFFING_TABLE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    POSITION_ID uuid,
    PROJECT_ID uuid,
    OPEN_POSITION_ID uuid,
    NUMBER_OF_STAFF integer,
    COMMENT_ text,
    --
    primary key (ID)
);
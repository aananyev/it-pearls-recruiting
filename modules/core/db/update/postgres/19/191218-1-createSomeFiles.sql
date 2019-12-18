create table ITPEARLS_SOME_FILES (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    FILE_DESCRIPTOR_ID uuid,
    FILE_DESCRIPTION varchar(80),
    FILE_OWNER_ID uuid,
    FILE_TYPE_ID uuid,
    --
    primary key (ID)
);
create table ITPEARLS_SOME_FILES (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    FILE_DESCRIPTION varchar(80) not null,
    FILE_DESCRIPTOR_ID varchar(36) not null,
    FILE_COMMENT longvarchar,
    FILE_OWNER_ID varchar(36) not null,
    FILE_TYPE_ID varchar(36) not null,
    CANDIDATE_CV_ID varchar(36),
    --
    primary key (ID)
);
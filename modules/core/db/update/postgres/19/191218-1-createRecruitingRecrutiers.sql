create table ITPEARLS_RECRUITING_RECRUTIERS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    RECRUTIER_NAME_ID uuid not null,
    PASSAGE text,
    SEND_PASSAGE boolean,
    CHECK_PASSAGE boolean,
    RECRUTIER_CV_ID uuid,
    --
    primary key (ID)
);
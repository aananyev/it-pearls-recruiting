create table ITPEARLS_JOB_CANDIDATE_SIGN_ICON (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    JOB_CANDIDATE_ID uuid,
    SIGN_ICON_ID uuid,
    USER_ID uuid,
    --
    primary key (ID)
);
create table ITPEARLS_MY_ACTIVE_CANDIDATE_EXCLUDE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    JOB_CANDIDATE_ID uuid not null,
    OPEN_POSITION_ID uuid not null,
    USER_ID uuid not null,
    --
    primary key (ID)
);
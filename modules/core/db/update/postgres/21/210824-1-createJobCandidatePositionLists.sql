create table ITPEARLS_JOB_CANDIDATE_POSITION_LISTS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    POSITION_LIST_ID uuid,
    JOB_CANDIDATE_ID uuid not null,
    --
    primary key (ID)
);
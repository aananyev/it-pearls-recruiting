create table ITPEARLS_JOB_HISTORY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CANDIDATE_ID varchar(36) not null,
    CURRENT_POSITION_ID varchar(36) not null,
    CURRENT_COMPANY_ID varchar(36) not null,
    --
    primary key (ID)
);
create table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    START_DATE date,
    END_DATE date,
    CANDIDATE_ID varchar(36),
    SUBSCRIBER_ID varchar(36),
    --
    primary key (ID)
);
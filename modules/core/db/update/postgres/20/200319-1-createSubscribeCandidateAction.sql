create table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    START_DATE timestamp,
    END_DATE timestamp,
    CANDIDATE_ID uuid,
    SUBSCRIBER_ID uuid,
    --
    primary key (ID)
);
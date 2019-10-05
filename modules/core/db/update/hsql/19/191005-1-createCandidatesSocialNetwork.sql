create table ITPEARLS_CANDIDATES_SOCIAL_NETWORK (
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
    SOCIAL_NETWORK_ID varchar(36) not null,
    SOCIAL_NETWORK_UPL varchar(80) not null,
    --
    primary key (ID)
);
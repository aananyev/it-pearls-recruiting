create table ITPEARLS_SOCIAL_NETWORK_UR_LS (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NETWORK_NAME varchar(80) not null,
    NETWORK_URLS varchar(80) not null,
    --
    primary key (ID)
);
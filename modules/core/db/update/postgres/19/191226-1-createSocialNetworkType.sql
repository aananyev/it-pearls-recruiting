create table ITPEARLS_SOCIAL_NETWORK_TYPE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    SOCIAL_NETWORK varchar(30) not null,
    SOCIAL_NETWORK_URL varchar(80),
    COMMENT_ varchar(255),
    --
    primary key (ID)
);
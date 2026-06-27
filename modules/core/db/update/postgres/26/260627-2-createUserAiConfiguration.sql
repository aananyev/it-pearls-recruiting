create table ITPEARLS_USER_AI_CONFIGURATION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    USER_ID uuid not null,
    PROVIDER_CODE varchar(64),
    API_KEY varchar(512),
    DEFAULT_MODEL_NAME varchar(128),
    IS_ACTIVE boolean,
    --
    primary key (ID)
);

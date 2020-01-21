create table ITPEARLS_EVENT_REGISTRATION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    EVENT_CLASS varchar(255),
    EVENT_PAYLOAD varchar(255),
    RECEIVER varchar(255),
    RECEIVED_AT varchar(255),
    --
    primary key (ID)
);
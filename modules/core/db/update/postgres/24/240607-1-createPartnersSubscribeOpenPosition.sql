create table ITPEARLS_PARTNERS_SUBSCRIBE_OPEN_POSITION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    PARTNER_ID uuid not null,
    OPEN_POSITION_ID uuid not null,
    START_DATE date,
    END_DATE date,
    --
    primary key (ID)
);
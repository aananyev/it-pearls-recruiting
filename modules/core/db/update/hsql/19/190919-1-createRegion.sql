create table ITPEARLS_REGION (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    REGION_RU_NAME varchar(50) not null,
    REGION_EN_NAME varchar(255),
    REGION_CODE integer,
    --
    primary key (ID)
);
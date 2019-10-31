create table ITPEARLS_CITY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CITY_RU_NAME varchar(50) not null,
    CITY_EN_NAME varchar(50),
    CITY_PHONE_CODE varchar(5),
    CITY_REGION_ID varchar(36),
    --
    primary key (ID)
);
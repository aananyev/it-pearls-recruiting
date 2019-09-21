create table ITPEARLS_COMPANY_GROUP (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    COMPANY_RU_GROUP_NAME varchar(80) not null,
    COMPANY_EN_GROUP_NAME varchar(80) not null,
    --
    primary key (ID)
);
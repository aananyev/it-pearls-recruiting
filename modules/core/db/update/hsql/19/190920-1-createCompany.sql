create table ITPEARLS_COMPANY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    COMPANY_OWNERSHIP_ID varchar(36),
    COMANY_NAME varchar(80) not null,
    COMPANY_SHORT_NAME varchar(30),
    COMPANY_EN_NAME varchar(80),
    COMPANY_EN_SHORT_NAME varchar(30),
    --
    primary key (ID)
);
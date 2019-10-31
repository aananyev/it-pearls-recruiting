create table ITPEARLS_PROJECT (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    PROJECT_NAME varchar(80),
    START_PROJECT_DATE date,
    END_PROJECT_DATE date,
    PROJECT_COMPANY_ID varchar(36) not null,
    PROJECT_DEPARTMENT_ID varchar(36) not null,
    --
    primary key (ID)
);
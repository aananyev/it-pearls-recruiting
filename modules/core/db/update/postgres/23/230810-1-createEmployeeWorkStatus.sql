create table ITPEARLS_EMPLOYEE_WORK_STATUS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    WORK_STATUS_NAME varchar(60) not null,
    --
    primary key (ID)
);
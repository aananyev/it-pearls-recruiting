create table ITPEARLS_STAFF_CURRENT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    EMPLOYEE_ID uuid not null,
    STAFFING_TABLE_ID uuid,
    SALARY integer not null,
    FORM_EMPLOYMENT varchar(50) not null,
    --
    primary key (ID)
);
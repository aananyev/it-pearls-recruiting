create table ITPEARLS_COMPANY_DEPARTAMENT (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    DEPARTAMENT_RU_NAME varchar(80) not null,
    DEPARTAMENT_EN_NAME varchar(80),
    COMPANY_NAME_ID varchar(36),
    DEPARTAMENT_HR_DIRECTOR_ID varchar(36),
    DEPARTAMENT_DIRECTOR_ID varchar(36),
    DEPARTAMENT_DESCRIPTION longvarchar,
    --
    primary key (ID)
);
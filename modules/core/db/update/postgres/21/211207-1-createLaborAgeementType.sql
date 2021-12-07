create table ITPEARLS_LABOR_AGEEMENT_TYPE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME_AGREEMENT varchar(255),
    LABOR_AGREEMENT_ID uuid not null,
    --
    primary key (ID)
);
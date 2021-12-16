create table ITPEARLS_LABOR_AGREEMENT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    PERHAPS boolean,
    COMPANY_ID uuid not null,
    LABOR_AGREEMENT_TYPE_ID uuid not null,
    COMMENT_ text,
    --
    primary key (ID)
);
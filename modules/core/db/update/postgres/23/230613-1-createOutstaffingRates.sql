create table ITPEARLS_OUTSTAFFING_RATES (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    RATE decimal(19, 2) not null,
    MIN_SALARY decimal(19, 2),
    MAX_SALARY decimal(19, 2),
    MAX_IE_SALARY decimal(19, 2),
    CURRENCY_ID uuid not null,
    COMMENT_ text,
    --
    primary key (ID)
);
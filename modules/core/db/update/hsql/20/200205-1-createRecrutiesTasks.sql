create table ITPEARLS_RECRUTIES_TASKS (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    START_DATE date not null,
    END_DATE date,
    REACRUTIER_ID varchar(36) not null,
    OPEN_POSITION_ID varchar(36),
    --
    primary key (ID)
);
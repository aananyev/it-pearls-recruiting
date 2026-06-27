create table ITPEARLS_VACANCY_PROMPT_TEMPLATE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CODE varchar(64) not null,
    NAME varchar(255) not null,
    PROMPT_TEXT clob,
    SYSTEM_CONTEXT varchar(1000),
    TEMPERATURE double default 0.7,
    --
    primary key (ID)
);

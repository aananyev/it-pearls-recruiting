create table HUNTTECH_AI_PROMPT_TEMPLATE (
    ID varchar(36) not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    VERSION integer not null default 1,
    NAME varchar(255) not null,
    CODE varchar(255) not null unique,
    ENTITY_CLASS varchar(255) not null,
    PROMPT_TEXT text not null,
    AVAILABLE_PLACEHOLDERS text,
    DESCRIPTION varchar(1000),
    ACTIVE boolean not null default true,
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    primary key (ID)
);

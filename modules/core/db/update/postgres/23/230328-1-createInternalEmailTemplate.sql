create table ITPEARLS_INTERNAL_EMAIL_TEMPLATE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    TEMPLATE_NAME varchar(128) not null,
    TEMPLATE_TEXT text not null,
    TEMPLATE_OPEN_POSITION_ID uuid,
    TEMPLATE_POSITION_ID uuid,
    TEMPLATE_COMMENT varchar(255),
    TEMPLATE_AUTHOR_ID uuid not null,
    --
    primary key (ID)
);
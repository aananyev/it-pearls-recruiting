create table ITPEARLS_INTERNAL_EMAILER (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    DTYPE varchar(31),
    --
    DRAFT_EMAIL boolean,
    FROM_EMAIL_ID uuid not null,
    TO_EMAIL_ID uuid not null,
    SUBJECT_EMAIL varchar(255) not null,
    BODY_EMAIL text not null,
    BODY_HTML boolean,
    DATE_CREATE_EMAIL timestamp not null,
    DATE_SEND_EMAIL timestamp,
    --
    -- from itpearls_InternalEmailerTemplate
    EMAIL_TEMPLATE_ID uuid,
    --
    primary key (ID)
);
create table ITPEARLS_EMAILER (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    FROM_EMAIL_ID uuid not null,
    TO_EMAIL varchar(80) not null,
    DRAFT_EMAIL boolean,
    SUBJECT_EMAIL varchar(255) not null,
    BODY_EMAIL text not null,
    DATE_CREATE_EMAIL timestamp not null,
    DATE_SEND_EMAIL timestamp not null,
    --
    primary key (ID)
);
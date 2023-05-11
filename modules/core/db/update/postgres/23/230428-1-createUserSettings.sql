create table ITPEARLS_USER_SETTINGS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    USER_ID uuid not null,
    IMAGE_ID uuid,
    SMTP_SERVER varchar(128),
    SMTP_PORT integer,
    SMTP_PASSWORD_REQUIRED boolean,
    SMTP_PASSWORD varchar(128),
    POP3_SERVER varchar(128),
    POP3_PORT integer,
    POP3_PASSWORD_REQUIRED boolean,
    POP3PASSWORD varchar(128),
    IMAP_SERVER varchar(128),
    IMAP_PORT integer,
    IMAP_PASSWORD_REQUIRED boolean,
    IMAP_PASSWORD varchar(128),
    --
    primary key (ID)
);
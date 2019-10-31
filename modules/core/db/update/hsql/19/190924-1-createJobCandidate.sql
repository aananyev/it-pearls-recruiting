create table ITPEARLS_JOB_CANDIDATE (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    FIRST_NAME varchar(80) not null,
    MIDDLE_NAME varchar(80) not null,
    SECOND_NAME varchar(80) not null,
    BIRDH_DATE date not null,
    EMAIL varchar(30),
    PHONE varchar(10),
    SKYPE_NAME varchar(30),
    TELEGRAM_NAME varchar(30),
    WIBER_NAME varchar(30),
    WHATSUP_NAME varchar(30),
    POSITION_COUNTRY_ID varchar(36),
    PERSON_POSITION_ID varchar(36),
    --
    primary key (ID)
);
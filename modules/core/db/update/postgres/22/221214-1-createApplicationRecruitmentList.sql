create table ITPEARLS_APPLICATION_RECRUITMENT_LIST (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ACTIVE boolean,
    OPEN_DATE date,
    CLOSE_DATE date,
    RECRUITER_ID uuid,
    COMMENT_ text,
    --
    primary key (ID)
);
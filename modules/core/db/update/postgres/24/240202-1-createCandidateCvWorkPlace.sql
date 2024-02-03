create table ITPEARLS_CANDIDATE_CV_WORK_PLACE (
    ID uuid,
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
    CURRENT_WORK boolean,
    POSITION_ID uuid not null,
    PROJECT_DESCRIPTION text,
    PERSONAL_CONTRIBUTION text,
    ACHIEVEMENT text,
    --
    primary key (ID)
);
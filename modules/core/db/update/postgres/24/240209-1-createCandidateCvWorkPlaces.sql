create table ITPEARLS_CANDIDATE_CV_WORK_PLACES (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CANDIDATE_CV_ID uuid not null,
    WORK_PLACE_ID uuid,
    WORK_PLACE_COMMENT varchar(128),
    START_DATE date not null,
    END_DATE date,
    WORK_TO_THIS_DAY boolean,
    FUNCTIONALITY_AT_WORK text,
    PERSONAL_ROLE text,
    ACHIEVEMENTS text,
    --
    primary key (ID)
);
create table ITPEARLS_SKILLS_FILTER_LAST_SELECTION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    POSITION_ID uuid,
    CITY_ID uuid,
    OPEN_POSITION_ID uuid,
    JOB_CANDIDATES_ID uuid,
    JOB_CANDIDATE_SELECTION boolean,
    USER_ID uuid,
    --
    primary key (ID)
);
create table ITPEARLS_INTERVIEW (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    DATE_INTERVIEW timestamp not null,
    CANDIDATE_ID uuid not null,
    CANDIDATE_CV_ID uuid,
    LETTER text,
    LETTER_REQUIREMENTS text,
    NOTE text,
    SALARY_MIN integer,
    SALARY_MAX integer,
    RECRUTIER_ID uuid not null,
    RESEARCHER_ID uuid,
    --
    primary key (ID)
);
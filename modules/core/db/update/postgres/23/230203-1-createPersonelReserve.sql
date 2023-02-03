create table ITPEARLS_PERSONEL_RESERVE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    JOB_CANDIDATE_ID uuid,
    REACTUTIER_ID uuid,
    DATE_ timestamp,
    TERM_OF_PLACEMENT integer,
    END_DATE date,
    PERSON_POSITION_ID uuid,
    OPEN_POSITION_ID uuid,
    --
    primary key (ID)
);
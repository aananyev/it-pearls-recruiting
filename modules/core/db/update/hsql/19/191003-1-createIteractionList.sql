create table ITPEARLS_ITERACTION_LIST (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NUMBER_ITERACTION integer,
    DATE_ITERACTION timestamp,
    CANDIDATE_ID varchar(36) not null,
    CURRENT_JOB_POSITION_ID varchar(36),
    PROJECT_ID varchar(36),
    COMPANY_DEPARTMENT_ID varchar(36) not null,
    ITERACTION_TYPE_ID varchar(36) not null,
    RECRUTIER_ID varchar(36) not null,
    COMMUNICATION_METHOD varchar(80),
    COMMENT_ longvarchar,
    --
    primary key (ID)
);
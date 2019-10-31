create table ITPEARLS_MAIN_LIST (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NUMBER_LIST integer not null,
    CONTACT_NAME_ID varchar(36) not null,
    PERSON_CURRENT_POSITION_ID varchar(36),
    PROJECT_NAME_ID varchar(36) not null,
    OPEN_POSITION_ID varchar(36),
    ITERACTION_NAME_ID varchar(36) not null,
    COMMUNICATION_METHOD varchar(20) not null,
    ITERACTION_COMMENT longvarchar,
    USER_FROM_COMPANY_ID varchar(36) not null,
    DATA_CONTACT timestamp,
    --
    primary key (ID)
);
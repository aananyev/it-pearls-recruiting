-- begin ITPEARLS_COUNTRY
create table ITPEARLS_COUNTRY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    COUNTRY_RU_NAME varchar(50) not null,
    COUNTRY_SHORT_NAME varchar(2),
    PHONE_CODE integer,
    --
    primary key (ID)
)^
-- end ITPEARLS_COUNTRY
-- begin ITPEARLS_POSITION
create table ITPEARLS_POSITION (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    POSITION_RU_NAME varchar(80) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_POSITION
-- begin ITPEARLS_PERSON
create table ITPEARLS_PERSON (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CANDIDATE boolean,
    FIRST_NAME varchar(80),
    MIDDLE_NAME varchar(80),
    SECOND_NAME varchar(80),
    BIRDH_DATE date,
    EMAIL varchar(40),
    PHONE varchar(10),
    SKYPE_NAME varchar(15),
    TELEGRAM_NAME varchar(15),
    WIBER_NAME varchar(15),
    WATSUP_NAME varchar(15),
    POSITION_COUNTRY_ID varchar(36),
    PERSON_POSITION_ID varchar(36),
    --
    primary key (ID)
)^
-- end ITPEARLS_PERSON

-- begin ITPEARLS_REGION
create table ITPEARLS_REGION (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    REGION_RU_NAME varchar(50) not null,
    REGION_COUNTRY_ID varchar(36),
    REGION_CODE integer,
    --
    primary key (ID)
)^
-- end ITPEARLS_REGION
-- begin ITPEARLS_CITY
create table ITPEARLS_CITY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CITY_RU_NAME varchar(50) not null,
    CITY_PHONE_CODE varchar(5),
    CITY_REGION_ID varchar(36),
    --
    primary key (ID)
)^
-- end ITPEARLS_CITY
-- begin ITPEARLS_OWNERSHUP
create table ITPEARLS_OWNERSHUP (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    SHORT_TYPE varchar(7) not null,
    LONG_TYPE varchar(50) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_OWNERSHUP
-- begin ITPEARLS_COMPANY
create table ITPEARLS_COMPANY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    COMPANY_OUR_CLIENT boolean,
    COMPANY_OWNERSHIP_ID varchar(36),
    COMANY_NAME varchar(80) not null,
    COMPANY_SHORT_NAME varchar(30),
    COMPANY_DIRECTOR_ID varchar(36),
    --
    primary key (ID)
)^
-- end ITPEARLS_COMPANY
-- begin ITPEARLS_COMPANY_DEPARTAMENT
create table ITPEARLS_COMPANY_DEPARTAMENT (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    DEPARTAMENT_RU_NAME varchar(80) not null,
    COMPANY_NAME_ID varchar(36),
    DEPARTAMENT_HR_DIRECTOR_ID varchar(36),
    DEPARTAMENT_DIRECTOR_ID varchar(36),
    DEPARTAMENT_DESCRIPTION longvarchar,
    DEPARTAMENT_NUMBER_OF_PROGRAMMERS integer,
    --
    primary key (ID)
)^
-- end ITPEARLS_COMPANY_DEPARTAMENT
-- begin ITPEARLS_SPECIALISATION
create table ITPEARLS_SPECIALISATION (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    SPEC_RU_NAME varchar(80),
    --
    primary key (ID)
)^
-- end ITPEARLS_SPECIALISATION
-- begin ITPEARLS_SKILL
create table ITPEARLS_SKILL (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    SKILL_NAME varchar(80) not null,
    SKILL_TYPE_ID varchar(36) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_SKILL
-- begin ITPEARLS_PROJECT
create table ITPEARLS_PROJECT (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    PROJECT_NAME varchar(80),
    START_PROJECT_DATE date,
    END_PROJECT_DATE date,
    PROJECT_DEPARTMENT_ID varchar(36) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_PROJECT
-- begin ITPEARLS_OPEN_POSITION
create table ITPEARLS_OPEN_POSITION (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    PROJECT_NAME_ID varchar(36) not null,
    COMPANY_NAME_ID varchar(36) not null,
    NUMBER_POSITION integer not null,
    COMMENT_ longvarchar,
    --
    primary key (ID)
)^
-- end ITPEARLS_OPEN_POSITION
-- begin ITPEARLS_COMPANY_GROUP
create table ITPEARLS_COMPANY_GROUP (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    COMPANY_RU_GROUP_NAME varchar(80) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_COMPANY_GROUP
-- begin ITPEARLS_MAIN_LIST
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
)^
-- end ITPEARLS_MAIN_LIST
-- begin ITPEARLS_ITERACTION
create table ITPEARLS_ITERACTION (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NUMBER_ integer not null,
    ITERATION_NAME varchar(80) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_ITERACTION
-- begin ITPEARLS_JOB_CANDIDATE
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
)^
-- end ITPEARLS_JOB_CANDIDATE
-- begin ITPEARLS_REQUIRED_PARAMETERS
create table ITPEARLS_REQUIRED_PARAMETERS (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    PARAMETER_NAME varchar(80) not null,
    NUMBER_ integer not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_REQUIRED_PARAMETERS

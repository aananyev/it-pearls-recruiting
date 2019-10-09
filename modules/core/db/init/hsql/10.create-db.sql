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
    FIRST_NAME varchar(80),
    MIDDLE_NAME varchar(80),
    SECOND_NAME varchar(80),
    BIRDH_DATE date,
    EMAIL varchar(40),
    PHONE varchar(20),
    MOB_PHONE varchar(20),
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
    COMPANY_OWNERSHIP_ID varchar(36),
    COMANY_NAME varchar(80) not null,
    COMPANY_SHORT_NAME varchar(30),
    COMPANY_DIRECTOR_ID varchar(36),
    CITY_OF_COMPANY_ID varchar(36),
    REGION_OF_COMPANY_ID varchar(36),
    COUNTRY_OF_COMPANY_ID varchar(36),
    ADDRESS_OF_COMPANY longvarchar,
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
    PROJECT_OWNER_ID varchar(36),
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
    VACANSY_NAME varchar(80) not null,
    POSITION_TYPE_ID varchar(36),
    PROJECT_NAME_ID varchar(36) not null,
    COMPANY_NAME_ID varchar(36) not null,
    COMPANY_DEPARTAMENT_ID varchar(36),
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
    ITERACTION_TREE_ID varchar(36),
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
    MIDDLE_NAME varchar(80),
    SECOND_NAME varchar(80) not null,
    PERSON_POSITION_ID varchar(36),
    CURRENT_COMPANY_ID varchar(36),
    BIRDH_DATE date,
    EMAIL varchar(30),
    PHONE varchar(10),
    SKYPE_NAME varchar(30),
    TELEGRAM_NAME varchar(30),
    WIBER_NAME varchar(30),
    WHATSUP_NAME varchar(30),
    POSITION_COUNTRY_ID varchar(36),
    CITY_OF_RESIDENCE_ID varchar(36),
    SPECIALISATION_ID varchar(36),
    --
    primary key (ID)
)^
-- end ITPEARLS_JOB_CANDIDATE
-- begin ITPEARLS_ITERACTION_LIST
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
    NUMBER_ITERACTION decimal(19, 2),
    DATE_ITERACTION timestamp,
    CANDIDATE_ID varchar(36) not null,
    CURRENT_JOB_POSITION_ID varchar(36),
    VACANCY_ID varchar(36),
    PROJECT_ID varchar(36),
    COMPANY_DEPARTMENT_ID varchar(36) not null,
    ITERACTION_TYPE_ID varchar(36) not null,
    RECRUTIER_ID varchar(36) not null,
    COMMUNICATION_METHOD varchar(80),
    COMMENT_ longvarchar,
    --
    primary key (ID)
)^
-- end ITPEARLS_ITERACTION_LIST
-- begin ITPEARLS_SOCIAL_NETWORK_UR_LS
create table ITPEARLS_SOCIAL_NETWORK_UR_LS (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NETWORK_NAME varchar(80) not null,
    NETWORK_URLS varchar(80) not null,
    JOB_CANDIDATE_ID varchar(36),
    --
    primary key (ID)
)^
-- end ITPEARLS_SOCIAL_NETWORK_UR_LS
-- begin ITPEARLS_SKILL_TREE
create table ITPEARLS_SKILL_TREE (
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
    SKILL_TREE_ID varchar(36),
    JOB_CANDIDATE_ID varchar(36),
    OPEN_POSITION_ID varchar(36),
    --
    primary key (ID)
)^
-- end ITPEARLS_SKILL_TREE
-- begin ITPEARLS_JOB_HISTORY
create table ITPEARLS_JOB_HISTORY (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CANDIDATE_ID varchar(36) not null,
    CURRENT_POSITION_ID varchar(36) not null,
    CURRENT_COMPANY_ID varchar(36) not null,
    DATE_NEWS_POSITION date,
    --
    primary key (ID)
)^
-- end ITPEARLS_JOB_HISTORY
-- begin ITPEARLS_CANDIDATES_SOCIAL_NETWORK
create table ITPEARLS_CANDIDATES_SOCIAL_NETWORK (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CANDIDATE_ID varchar(36) not null,
    SOCIAL_NETWORK_ID varchar(36) not null,
    SOCIAL_NETWORK_UPL varchar(80) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_CANDIDATES_SOCIAL_NETWORK
-- begin ITPEARLS_CANDIDATE_CV
create table ITPEARLS_CANDIDATE_CV (
    ID varchar(36) not null,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CANDIDATE_ID varchar(36) not null,
    RESUME_POSITION_ID varchar(36),
    TEXT_CV longvarchar,
    LETTER longvarchar,
    DATE_POST date not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_CANDIDATE_CV

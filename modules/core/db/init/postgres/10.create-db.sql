-- begin ITPEARLS_SOCIAL_NETWORK_UR_LS
create table ITPEARLS_SOCIAL_NETWORK_UR_LS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NETWORK_NAME varchar(80),
    NETWORK_URLS varchar(80),
    JOB_CANDIDATE_ID uuid,
    SOCIAL_NETWORK_URL_ID uuid,
    --
    primary key (ID)
)^
-- end ITPEARLS_SOCIAL_NETWORK_UR_LS
-- begin ITPEARLS_JOB_CANDIDATE
create table ITPEARLS_JOB_CANDIDATE (
    ID uuid,
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
    FULL_NAME varchar(160),
    CURRENT_COMPANY_ID uuid,
    PERSON_POSITION_ID uuid,
    CITY_OF_RESIDENCE_ID uuid,
    BIRDH_DATE date,
    EMAIL varchar(50),
    PHONE varchar(18),
    SKYPE_NAME varchar(30),
    TELEGRAM_NAME varchar(30),
    WIBER_NAME varchar(30),
    WHATSUP_NAME varchar(30),
    POSITION_COUNTRY_ID uuid,
    SPECIALISATION_ID uuid,
    SKILL_TREE_ID uuid,
    OPEN_POSITION_ID uuid,
    STATUS integer,
    --
    primary key (ID)
)^
-- end ITPEARLS_JOB_CANDIDATE
-- begin ITPEARLS_COMPANY_DEPARTAMENT
create table ITPEARLS_COMPANY_DEPARTAMENT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    DEPARTAMENT_RU_NAME varchar(80) not null,
    COMPANY_NAME_ID uuid,
    DEPARTAMENT_HR_DIRECTOR_ID uuid,
    DEPARTAMENT_DIRECTOR_ID uuid,
    DEPARTAMENT_DESCRIPTION text,
    DEPARTAMENT_NUMBER_OF_PROGRAMMERS integer,
    --
    primary key (ID)
)^
-- end ITPEARLS_COMPANY_DEPARTAMENT
-- begin ITPEARLS_ITERACTION_LIST
create table ITPEARLS_ITERACTION_LIST (
    ID uuid,
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
    CANDIDATE_ID uuid not null,
    CURRENT_JOB_POSITION_ID uuid,
    VACANCY_ID uuid,
    PROJECT_ID uuid,
    ITERACTION_TYPE_ID uuid not null,
    COMMUNICATION_METHOD varchar(80),
    COMMENT_ text,
    RECRUTIER_ID uuid,
    RECRUTIER_NAME varchar(80),
    ITERACTION_CHAIN_ID uuid,
    ADD_TYPE integer,
    ADD_DATE timestamp,
    ADD_STRING varchar(255),
    ADD_INTEGER integer,
    --
    primary key (ID)
)^
-- end ITPEARLS_ITERACTION_LIST
-- begin ITPEARLS_CANDIDATE_CV
create table ITPEARLS_CANDIDATE_CV (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CANDIDATE_ID uuid not null,
    RESUME_POSITION_ID uuid,
    TO_VACANCY_ID uuid,
    OWNER_ID uuid,
    TEXT_CV text,
    LETTER text,
    LINK_IT_PEARLS_CV varchar(255),
    LINK_ORIGINAL_CV varchar(255),
    FILE_CV_ID uuid,
    ORIGINAL_FILE_CV_ID uuid,
    DATE_POST date not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_CANDIDATE_CV
-- begin ITPEARLS_COMPANY
create table ITPEARLS_COMPANY (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    OUR_CLIENT boolean,
    COMPANY_OWNERSHIP_ID uuid,
    COMANY_NAME varchar(80) not null,
    COMPANY_SHORT_NAME varchar(80),
    COMPANY_DIRECTOR_ID uuid,
    CITY_OF_COMPANY_ID uuid,
    REGION_OF_COMPANY_ID uuid,
    COUNTRY_OF_COMPANY_ID uuid,
    ADDRESS_OF_COMPANY text,
    --
    primary key (ID)
)^
-- end ITPEARLS_COMPANY
-- begin ITPEARLS_REGION
create table ITPEARLS_REGION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    REGION_RU_NAME varchar(50) not null,
    REGION_COUNTRY_ID uuid,
    REGION_CODE integer,
    --
    primary key (ID)
)^
-- end ITPEARLS_REGION
-- begin ITPEARLS_ITERACTION
create table ITPEARLS_ITERACTION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NUMBER_ varchar(255),
    MANDATORY_ITERACTION boolean,
    ITERACTION_TREE_ID uuid,
    ITERATION_NAME varchar(80) not null,
    PIC varchar(80),
    CALL_BUTTON_TEXT varchar(30),
    CALL_CLASS varchar(30),
    CALL_FORM boolean,
    ADD_FLAG boolean,
    SET_DATE_TIME boolean,
    ADD_TYPE integer,
    ADD_FIELD varchar(40),
    ADD_CAPTION varchar(80),
    NOTIFICATION_TYPE integer,
    CALENDAR_ITEM boolean,
    CALENDAR_ITEM_STYLE varchar(255),
    CALENDAR_ITEM_DESCRIPTION varchar(80),
    FIND_TO_DIC boolean,
    --
    primary key (ID)
)^
-- end ITPEARLS_ITERACTION
-- begin ITPEARLS_COUNTRY
create table ITPEARLS_COUNTRY (
    ID uuid,
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
-- begin ITPEARLS_PROJECT
create table ITPEARLS_PROJECT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    PROJECT_NAME varchar(80),
    PROJECT_IS_CLOSED boolean,
    START_PROJECT_DATE date,
    END_PROJECT_DATE date,
    PROJECT_DEPARTMENT_ID uuid,
    PROJECT_OWNER_ID uuid,
    PROJECT_DESCRIPTION text,
    --
    primary key (ID)
)^
-- end ITPEARLS_PROJECT
-- begin ITPEARLS_POSITION
create table ITPEARLS_POSITION (
    ID uuid,
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
    ID uuid,
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
    POSITION_COUNTRY_ID uuid,
    PERSON_POSITION_ID uuid,
    COMPANY_DEPARTMENT_ID uuid,
    SEND_RESUME_TO_EMAIL boolean,
    --
    primary key (ID)
)^
-- end ITPEARLS_PERSON
-- begin ITPEARLS_CITY
create table ITPEARLS_CITY (
    ID uuid,
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
    CITY_REGION_ID uuid,
    --
    primary key (ID)
)^
-- end ITPEARLS_CITY
-- begin ITPEARLS_SKILL_TREE
create table ITPEARLS_SKILL_TREE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    SKILL_NAME varchar(80) not null,
    SKILL_TREE_ID uuid,
    JOB_CANDIDATE_ID uuid,
    OPEN_POSITION_ID uuid,
    --
    primary key (ID)
)^
-- end ITPEARLS_SKILL_TREE
-- begin ITPEARLS_OWNERSHUP
create table ITPEARLS_OWNERSHUP (
    ID uuid,
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
-- begin ITPEARLS_SPECIALISATION
create table ITPEARLS_SPECIALISATION (
    ID uuid,
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
-- begin ITPEARLS_COMPANY_GROUP
create table ITPEARLS_COMPANY_GROUP (
    ID uuid,
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
-- begin ITPEARLS_OPEN_POSITION
create table ITPEARLS_OPEN_POSITION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    OPEN_CLOSE boolean,
    VACANSY_NAME varchar(120) not null,
    REMOTE_WORK integer not null,
    SALARY_MIN decimal(19, 2),
    SALARY_MAX decimal(19, 2),
    CITY_POSITION_ID uuid,
    POSITION_TYPE_ID uuid,
    PROJECT_NAME_ID uuid not null,
    NUMBER_POSITION integer,
    WORK_EXPERIENCE integer,
    COMMAND_EXPERIENCE integer,
    COMMENT_ text,
    SHORT_DESCRIPTION varchar(160),
    TEMPLATE_LETTER text,
    EXERCISE text,
    NEED_EXERCISE boolean,
    PRIORITY integer,
    PAYMENTS_TYPE integer,
    TYPE_COMPANY_COMISSION integer,
    TYPE_SALARY_OF_RESEARCHER integer,
    TYPE_SALARY_OF_RECRUTIER integer,
    USE_TAX_NDFL boolean,
    INTERNAL_PROJECT boolean,
    PERCENT_COMISSION_OF_COMPANY varchar(5),
    PERCENT_SALARY_OF_RESEARCHER varchar(5),
    PERCENT_SALARY_OF_RECRUTIER varchar(5),
    --
    primary key (ID)
)^
-- end ITPEARLS_OPEN_POSITION
-- begin ITPEARLS_JOB_HISTORY
create table ITPEARLS_JOB_HISTORY (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CANDIDATE_ID uuid not null,
    CURRENT_POSITION_ID uuid not null,
    CURRENT_COMPANY_ID uuid not null,
    DATE_NEWS_POSITION date,
    --
    primary key (ID)
)^
-- end ITPEARLS_JOB_HISTORY
-- begin ITPEARLS_SETUP
create table ITPEARLS_SETUP (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    PERAM_NAME varchar(30) not null,
    PARAM_USER_ID uuid not null,
    PARAM_SET varchar(80),
    PARAM_SET_BOOL boolean,
    --
    primary key (ID)
)^
-- end ITPEARLS_SETUP
-- begin ITPEARLS_RECRUTIES_TASKS
create table ITPEARLS_RECRUTIES_TASKS (
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
    REACRUTIER_ID uuid not null,
    OPEN_POSITION_ID uuid,
    SUBSCRIBE boolean,
    RECRUTIER_NAME varchar(80),
    --
    primary key (ID)
)^
-- end ITPEARLS_RECRUTIES_TASKS
-- begin ITPEARLS_RECRUITING_RECRUTIERS
create table ITPEARLS_RECRUITING_RECRUTIERS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    RECRUTIER_NAME_ID uuid not null,
    PASSAGE text,
    SEND_PASSAGE boolean,
    CHECK_PASSAGE boolean,
    RECRUTIER_CV_ID uuid,
    --
    primary key (ID)
)^
-- end ITPEARLS_RECRUITING_RECRUTIERS
-- begin ITPEARLS_FILE_TYPE
create table ITPEARLS_FILE_TYPE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    NAME_FILE_TYPE varchar(30),
    DECRIPTION_FILE_TYPE varchar(80),
    --
    primary key (ID)
)^
-- end ITPEARLS_FILE_TYPE
-- begin ITPEARLS_SOME_FILES
create table ITPEARLS_SOME_FILES (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    FILE_DESCRIPTION varchar(80) not null,
    FILE_DESCRIPTOR_ID uuid not null,
    FILE_LINK varchar(255),
    FILE_COMMENT text,
    FILE_OWNER_ID uuid not null,
    FILE_TYPE_ID uuid not null,
    CANDIDATE_CV_ID uuid,
    --
    primary key (ID)
)^
-- end ITPEARLS_SOME_FILES
-- begin ITPEARLS_SOCIAL_NETWORK_TYPE
create table ITPEARLS_SOCIAL_NETWORK_TYPE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    SOCIAL_NETWORK varchar(30) not null,
    SOCIAL_NETWORK_URL varchar(80),
    COMMENT_ varchar(255),
    --
    primary key (ID)
)^
-- end ITPEARLS_SOCIAL_NETWORK_TYPE
-- begin ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION
create table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    START_DATE date,
    END_DATE date,
    CANDIDATE_ID uuid,
    SUBSCRIBER_ID uuid,
    --
    primary key (ID)
)^
-- end ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION
-- begin ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK
create table ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK (
    OPEN_POSITION_ID uuid,
    RECRUTIES_TASKS_ID uuid,
    primary key (OPEN_POSITION_ID, RECRUTIES_TASKS_ID)
)^
-- end ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK
-- begin ITPEARLS_ITEARCTION_REQUIREMENTS
create table ITPEARLS_ITEARCTION_REQUIREMENTS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ITERACTION_ID uuid,
    REQUIREMENT boolean,
    REQUIREMENT_ALL boolean,
    ITERACTION_REQUIREMEN_ID uuid,
    --
    primary key (ID)
)^
-- end ITPEARLS_ITEARCTION_REQUIREMENTS

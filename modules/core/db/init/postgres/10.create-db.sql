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
    BLOCK_CANDIDATE boolean,
    FULL_NAME varchar(160),
    BIRDH_DATE date,
    PERSON_POSITION_ID uuid,
    CURRENT_COMPANY_ID uuid,
    CITY_OF_RESIDENCE_ID uuid,
    EMAIL varchar(50),
    PHONE varchar(18),
    MOBILE_PHONE varchar(18),
    SKYPE_NAME varchar(30),
    TELEGRAM_NAME varchar(30),
    TELEGRAM_GROUP varchar(50),
    WIBER_NAME varchar(30),
    WHATSUP_NAME varchar(30),
    SPECIALISATION_ID uuid,
    SKILL_TREE_ID uuid,
    STATUS integer,
    FILE_IMAGE_FACE uuid,
    WORK_STATUS integer,
    PRIORITY_CONTACT integer,
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
    TEMPLATE_LETTER text,
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
    ITERACTION_TYPE_ID uuid,
    DATE_ITERACTION timestamp,
    CANDIDATE_ID uuid not null,
    VACANCY_ID uuid,
    COMMUNICATION_METHOD varchar(80),
    COMMENT_ text,
    RECRUTIER_ID uuid,
    RECRUTIER_NAME varchar(80),
    ADD_TYPE integer,
    ADD_DATE timestamp,
    ADD_STRING varchar(255),
    ADD_INTEGER integer,
    RATING integer,
    CURRENT_PRIORITY integer,
    CURRENT_OPEN_CLOSE boolean,
    CHAIN_INTERACTION_ID uuid,
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
    COMMENT_LETTER text,
    LINK_IT_PEARLS_CV varchar(255),
    LINK_ORIGINAL_CV varchar(255),
    FILE_CV_ID uuid,
    ORIGINAL_FILE_CV_ID uuid,
    DATE_POST date not null,
    FILE_IMAGE_FACE uuid,
    IMAGE_BYTE_ARRAY bytea,
    CONTACT_INFO_CHECKED boolean,
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
    OUR_LEGAL_ENTITY boolean,
    COMPANY_OWNERSHIP_ID uuid,
    COMANY_NAME varchar(80) not null,
    COMPANY_SHORT_NAME varchar(80),
    COMPANY_GROUP_ID uuid,
    COMPANY_DIRECTOR_ID uuid,
    CITY_OF_COMPANY_ID uuid,
    REGION_OF_COMPANY_ID uuid,
    COUNTRY_OF_COMPANY_ID uuid,
    FILE_COMPANY_LOGO uuid,
    ADDRESS_OF_COMPANY text,
    COMPANY_DESCRIPTION text,
    WORKING_CONDITIONS text,
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
    STAFF_INTERACTION_STATUS integer,
    PIC varchar(80),
    CALL_BUTTON_TEXT varchar(30),
    CALL_CLASS varchar(30),
    CALL_FORM boolean,
    ADD_FLAG boolean,
    SET_DATE_TIME boolean,
    ADD_TYPE integer,
    CHECK_TRACE integer,
    ADD_FIELD varchar(40),
    ADD_CAPTION varchar(80),
    CALENDAR_ITEM boolean,
    CALENDAR_ITEM_STYLE varchar(255),
    CALENDAR_ITEM_DESCRIPTION varchar(80),
    FIND_TO_DIC boolean,
    WIDGET_CHACK_JOB_CANDIDATES boolean,
    NEED_SEND_LETTER boolean,
    TEXT_EMAIL_TO_SEND text,
    NEED_SEND_MEMO boolean,
    SIGN_OUR_INTERVIEW_ASSIGNED boolean,
    SIGN_OUR_INTERVIEW boolean,
    SIGN_CLIENT_INTERVIEW boolean,
    SIGN_SEND_TO_CLIENT boolean,
    SIGN_PRIORITY_NEWS boolean,
    SIGN_VIEW_ONLY_MANAGER boolean,
    SIGN_COMMENT boolean,
    SIGN_PERSONAL_RESERVE boolean,
    SIGN_PUT_RESONAL boolean,
    SIGN_PERSONAL_RESERVE_REMOVE boolean,
    SIGN_END_CASE boolean,
    SIGN_FEEDBACK boolean,
    SIGN_START_PROJECT boolean,
    SIGN_END_PROJECT boolean,
    SIGN_END_PROCESS_VACANCY_CLOSED boolean,
    OUTSTAFFING_SIGN boolean not null,
    NOTIFICATION_NEED_SEND boolean,
    NOTIFICATION_TYPE integer,
    NOTIFICATION_PERIOD_TYPE integer,
    NOTIFICATION_BEFORE_AFTER_DAY integer,
    NOTIFICATION_WHEN_SEND integer,
    STATISTICS_ boolean,
    WORK_STATUS_ID uuid,
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
    PROJECT_NAME varchar(160),
    PROJECT_LOGO_ID uuid,
    PROJECT_TREE_ID uuid,
    PROJECT_IS_CLOSED boolean,
    DEFAULT_PROJECT boolean,
    START_PROJECT_DATE date,
    END_PROJECT_DATE date,
    PROJECT_DEPARTMENT_ID uuid,
    PROJECT_OWNER_ID uuid,
    PROJECT_DESCRIPTION text,
    TEMPLATE_LETTER text,
    GENERAL_CHAT varchar(255),
    CHAT_FOR_CV varchar(255),
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
    POSITION_EN_NAME varchar(80),
    STANDART_DECRIPTION text,
    WHO_IS_THIS_GUY text,
    JOB_CANDIDATE_ID uuid,
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
    SKYPE_NAME varchar(40),
    TELEGRAM_NAME varchar(40),
    WIBER_NAME varchar(40),
    WATSUP_NAME varchar(40),
    POSITION_COUNTRY_ID uuid,
    CITY_OF_RESIDENCE_ID uuid,
    PERSON_POSITION_ID uuid,
    COMPANY_DEPARTMENT_ID uuid,
    SEND_RESUME_TO_EMAIL boolean,
    FILE_IMAGE_FACE uuid,
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
    OPEN_POSITION_ID uuid,
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
    OPEN_POSITION_ID uuid,
    CANDIDATE_CV_ID uuid,
    SPECIALISATION_ID uuid,
    COMMENT text,
    WIKI_PAGE varchar(250),
    FILE_IMAGE_LOGO uuid,
    STYLE_HIGHLIGHTING varchar(128),
    NOT_PARSING boolean,
    PRIORITY_SKILL integer,
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
    RATING integer,
    SIGN_DRAFT boolean,
    LAST_OPEN_DATE date,
    VACANSY_NAME varchar(250) not null,
    VACANSY_ID varchar(16),
    GRADE_ID uuid,
    REMOTE_WORK integer not null,
    REGISTRATION_FOR_WORK integer,
    REMOTE_COMMENT varchar(40),
    COMMAND_CANDIDATE integer not null,
    SALARY_MIN decimal(19, 2),
    SALARY_MAX decimal(19, 2),
    SALARY_IE decimal(19, 2),
    SALARY_FIX_LIMIT boolean,
    SALARY_CANDIDATE_REQUEST boolean,
    SALARY_COMMENT varchar(255),
    OUTSTAFFING_COST decimal(19, 2),
    CITY_POSITION_ID uuid,
    POSITION_TYPE_ID uuid,
    PROJECT_NAME_ID uuid not null,
    NUMBER_POSITION integer,
    MORE10_NUMBER_POSITION boolean,
    WORK_EXPERIENCE integer not null,
    COMMAND_EXPERIENCE integer not null,
    COMMENT_ text,
    COMMENT_EN text,
    SHORT_DESCRIPTION varchar(250),
    TEMPLATE_LETTER text,
    NEED_LETTER boolean,
    EXERCISE text,
    NEED_EXERCISE boolean,
    PRIORITY integer,
    PRIORITY_COMMENT varchar(255),
    PAYMENTS_TYPE integer,
    TYPE_COMPANY_COMISSION integer,
    TYPE_SALARY_OF_RESEARCHER integer,
    TYPE_SALARY_OF_RECRUTIER integer,
    USE_TAX_NDFL boolean,
    INTERNAL_PROJECT boolean,
    PERCENT_COMISSION_OF_COMPANY varchar(5),
    PERCENT_SALARY_OF_RESEARCHER varchar(5),
    PERCENT_SALARY_OF_RECRUTIER varchar(5),
    PARENT_OPEN_POSITION_ID uuid,
    NEED_MEMO_FOR_INTERVIEW boolean,
    MEMO_FOR_INTERVIEW text,
    OWNER_ID uuid,
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
    CLOSED boolean,
    REACRUTIER_ID uuid not null,
    OPEN_POSITION_ID uuid,
    SUBSCRIBE boolean,
    RECRUTIER_NAME varchar(80),
    PLAN_FOR_PERIOD integer,
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
    DTYPE varchar(31),
    --
    FILE_DESCRIPTION varchar(80) not null,
    FILE_DESCRIPTOR_ID uuid,
    FILE_LINK varchar(255),
    FILE_COMMENT text,
    FILE_OWNER_ID uuid not null,
    FILE_TYPE_ID uuid not null,
    --
    -- from itpearls_SomeFilesOpenPosition
    OPEN_POSITION_ID uuid,
    --
    -- from itpearls_SomeFilesCandidateCV
    CANDIDATE_CV_ID uuid,
    --
    -- from itpearls_SomeFilesAgreement
    LABOR_AGREEMENT_ID uuid,
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
-- begin ITPEARLS_JOB_CANDIDATE_POSITION_LISTS
create table ITPEARLS_JOB_CANDIDATE_POSITION_LISTS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    POSITION_LIST_ID uuid,
    JOB_CANDIDATE_ID uuid not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_JOB_CANDIDATE_POSITION_LISTS

-- begin ITPEARLS_LABOR_AGEEMENT_TYPE
create table ITPEARLS_LABOR_AGEEMENT_TYPE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    EMPLOYEE_ORCOMPANY integer,
    NAME_AGREEMENT varchar(80) not null,
    COMMENT_ text,
    --
    primary key (ID)
)^
-- end ITPEARLS_LABOR_AGEEMENT_TYPE
-- begin ITPEARLS_LABOR_AGREEMENT
create table ITPEARLS_LABOR_AGREEMENT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    PERHAPS boolean,
    CLOSED boolean,
    EMPLOYEE_OR_CUSTOMER integer not null,
    ADDITIONAL_AGREEMENT boolean,
    JOB_CANDIDATE_ID uuid,
    LEGAL_ENTITY_EMPLOYEE_ID uuid,
    CONTRACTOR_COMPANY_ID uuid,
    ADDITIONAL_LABOR_AGREEMENT_ID uuid,
    LEGAL_ENTITY_FROM_ID uuid,
    AGREEMENT_NAME varchar(255) not null,
    AGREEMENT_NUMBER varchar(48) not null,
    AGREEMENT_DATE date not null,
    AGREEMENT_END_DATE date,
    RATE integer not null,
    PERPETUAL_AGREEMENT boolean,
    COMPANY_ID uuid,
    LABOR_AGREEMENT_TYPE_ID uuid not null,
    COMMENT_ text,
    AGREEMENT_TEXT text,
    FILE_AGREEMENT_ID uuid,
    --
    primary key (ID)
)^
-- end ITPEARLS_LABOR_AGREEMENT
-- begin ITPEARLS_OPEN_POSITION_LABOR_AGREEMENT_LINK
create table ITPEARLS_OPEN_POSITION_LABOR_AGREEMENT_LINK (
    OPEN_POSITION_ID uuid,
    LABOR_AGREEMENT_ID uuid,
    primary key (OPEN_POSITION_ID, LABOR_AGREEMENT_ID)
)^
-- end ITPEARLS_OPEN_POSITION_LABOR_AGREEMENT_LINK
-- begin ITPEARLS_POSSIBLE_NAMES
create table ITPEARLS_POSSIBLE_NAMES (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    POSITION_ID uuid not null,
    POSSIBLE_EN_NAME_POSIION varchar(128) not null,
    POSSIBLE_NAME varchar(128) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_POSSIBLE_NAMES
-- begin SEC_USER
alter table SEC_USER add column IMAGE_ID uuid ^
alter table SEC_USER add column SMTP_SERVER varchar(128) ^
alter table SEC_USER add column SMTP_PORT integer ^
alter table SEC_USER add column SMTP_PASSWORD_REQUIRED boolean ^
alter table SEC_USER add column SMTP_PASSWORD varchar(128) ^
alter table SEC_USER add column POP3_SERVER varchar(128) ^
alter table SEC_USER add column POP3_PORT integer ^
alter table SEC_USER add column POP3_PASSWORD_REQUIRED boolean ^
alter table SEC_USER add column POP3PASSWORD varchar(128) ^
alter table SEC_USER add column IMAP_SERVER varchar(128) ^
alter table SEC_USER add column IMAP_PORT integer ^
alter table SEC_USER add column IMAP_PASSWORD_REQUIRED boolean ^
alter table SEC_USER add column IMAP_PASSWORD varchar(128) ^
alter table SEC_USER add column DTYPE varchar(31) ^
update SEC_USER set DTYPE = 'itpearls_ExtUser' where DTYPE is null ^
-- end SEC_USER
-- begin ITPEARLS_OPEN_POSITION_NEWS
create table ITPEARLS_OPEN_POSITION_NEWS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    SUBJECT varchar(255) not null,
    OPEN_POSITION_ID uuid not null,
    DATE_NEWS timestamp not null,
    COMMENT_ text,
    CANDIDATES_ID uuid,
    AUTHOR_ID uuid not null,
    PRIORITY_NEWS boolean,
    --
    primary key (ID)
)^
-- end ITPEARLS_OPEN_POSITION_NEWS
-- begin ITPEARLS_INTERVIEW
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
)^
-- end ITPEARLS_INTERVIEW
-- begin ITPEARLS_INTERVIEW_OPEN_POSITION_LINK
create table ITPEARLS_INTERVIEW_OPEN_POSITION_LINK (
    INTERVIEW_ID uuid,
    OPEN_POSITION_ID uuid,
    primary key (INTERVIEW_ID, OPEN_POSITION_ID)
)^
-- end ITPEARLS_INTERVIEW_OPEN_POSITION_LINK

-- begin ITPEARLS_STAFFING_TABLE
create table ITPEARLS_STAFFING_TABLE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ACTIVE boolean,
    CODE varchar(32),
    OPEN_POSITION_ID uuid not null,
    GRADE_ID uuid not null,
    NUMBER_OF_STAFF integer not null,
    SALARY_MIN decimal(19, 2),
    SALARY_MAX decimal(19, 2),
    COMMENT_ text,
    --
    primary key (ID)
)^
-- end ITPEARLS_STAFFING_TABLE
-- begin ITPEARLS_GRADE
create table ITPEARLS_GRADE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    GRADE_NAME varchar(80),
    --
    primary key (ID)
)^
-- end ITPEARLS_GRADE
-- begin ITPEARLS_STAFF_CURRENT
create table ITPEARLS_STAFF_CURRENT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    EMPLOYEE_ID uuid not null,
    STAFFING_TABLE_ID uuid,
    SALARY integer not null,
    FORM_EMPLOYMENT varchar(50) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_STAFF_CURRENT
-- begin ITPEARLS_APPLICATION_RECRUITMENT
create table ITPEARLS_APPLICATION_RECRUITMENT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ACTIVE boolean,
    CODE varchar(80),
    STAFFING_TABLE_ID uuid,
    APPROVAL boolean,
    APPLICATION_DATE timestamp,
    AMOUNT integer,
    EXIT_DATE date,
    COMMENT_ text,
    APPLICATION_RECRUITMENT_LIST_ID uuid,
    --
    primary key (ID)
)^
-- end ITPEARLS_APPLICATION_RECRUITMENT
-- begin ITPEARLS_APPLICATION_RECRUITMENT_LIST
create table ITPEARLS_APPLICATION_RECRUITMENT_LIST (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    ACTIVE boolean,
    OPEN_DATE date,
    QUICK_DESCRIPTION varchar(80),
    PROJECT_ID uuid,
    PROJECT_DEPARTMENT_ID uuid,
    COMPANY_ID uuid,
    CLOSE_DATE date,
    RECRUITER_ID uuid,
    COMMENT_ text,
    --
    primary key (ID)
)^
-- end ITPEARLS_APPLICATION_RECRUITMENT_LIST
-- begin ITPEARLS_PERSONEL_RESERVE
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
    REMOVED_FROM_RESERVE boolean,
    IN_PROCESSED boolean,
    --
    primary key (ID)
)^
-- end ITPEARLS_PERSONEL_RESERVE
-- begin ITPEARLS_SKILLS_FILTER_LAST_SELECTION
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
)^
-- end ITPEARLS_SKILLS_FILTER_LAST_SELECTION
-- begin ITPEARLS_INTERNAL_EMAIL_TEMPLATE
create table ITPEARLS_INTERNAL_EMAIL_TEMPLATE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    TEMPLATE_NAME varchar(128) not null,
    TEMPLATE_SUBJ varchar(255),
    TEMPLATE_TEXT text not null,
    TEMPLATE_OPEN_POSITION_ID uuid,
    TEMPLATE_POSITION_ID uuid,
    TEMPLATE_COMMENT varchar(255),
    TEMPLATE_AUTHOR_ID uuid not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_INTERNAL_EMAIL_TEMPLATE
-- begin ITPEARLS_INTERNAL_EMAILER
create table ITPEARLS_INTERNAL_EMAILER (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    DTYPE varchar(31),
    --
    DRAFT_EMAIL boolean,
    FROM_EMAIL_ID uuid not null,
    TO_EMAIL_ID uuid not null,
    SUBJECT_EMAIL varchar(255) not null,
    BODY_EMAIL text not null,
    BODY_HTML boolean,
    DATE_CREATE_EMAIL timestamp not null,
    DATE_SEND_EMAIL timestamp,
    --
    -- from itpearls_InternalEmailerTemplate
    EMAIL_TEMPLATE_ID uuid,
    --
    primary key (ID)
)^
-- end ITPEARLS_INTERNAL_EMAILER
-- begin ITPEARLS_USER_SETTINGS
create table ITPEARLS_USER_SETTINGS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    USER_ID uuid not null,
    IMAGE_ID uuid,
    SMTP_SERVER varchar(128),
    SMTP_PORT integer,
    SMTP_PASSWORD_REQUIRED boolean,
    SMTP_PASSWORD varchar(128),
    POP3_SERVER varchar(128),
    POP3_PORT integer,
    POP3_PASSWORD_REQUIRED boolean,
    POP3PASSWORD varchar(128),
    IMAP_SERVER varchar(128),
    IMAP_PORT integer,
    IMAP_PASSWORD_REQUIRED boolean,
    IMAP_PASSWORD varchar(128),
    --
    primary key (ID)
)^
-- end ITPEARLS_USER_SETTINGS
-- begin ITPEARLS_OUTSTAFFING_RATES
create table ITPEARLS_OUTSTAFFING_RATES (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    RATE decimal(19, 2) not null,
    MIN_SALARY decimal(19, 2),
    MAX_SALARY decimal(19, 2),
    MAX_IE_SALARY decimal(19, 2),
    CURRENCY_ID uuid not null,
    COMMENT_ text,
    --
    primary key (ID)
)^
-- end ITPEARLS_OUTSTAFFING_RATES
-- begin ITPEARLS_CURRENCY
create table ITPEARLS_CURRENCY (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    CURRENCY_LONG_NAME varchar(128) not null,
    CURRENCY_SHORT_NAME varchar(3) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_CURRENCY
-- begin ITPEARLS_OPEN_POSITION_COMMENT
create table ITPEARLS_OPEN_POSITION_COMMENT (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    OPEN_POSITION_ID uuid not null,
    RATING integer,
    COMMENT_ text,
    USER_ID uuid not null,
    DATE_ timestamp,
    --
    primary key (ID)
)^
-- end ITPEARLS_OPEN_POSITION_COMMENT
-- begin ITPEARLS_EMPLOYEE
create table ITPEARLS_EMPLOYEE (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    OPEN_POSITION_ID uuid not null,
    EMPLOYEE_DATE date,
    DISSMISAL_DATE date,
    OUTSTAFFING_COST decimal(19, 2),
    SALARY decimal(19, 2),
    --
    primary key (ID)
)^
-- end ITPEARLS_EMPLOYEE
-- begin ITPEARLS_EMPLOYEE_WORK_STATUS
create table ITPEARLS_EMPLOYEE_WORK_STATUS (
    ID uuid,
    VERSION integer not null,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    --
    WORK_STATUS_NAME varchar(60) not null,
    --
    primary key (ID)
)^
-- end ITPEARLS_EMPLOYEE_WORK_STATUS

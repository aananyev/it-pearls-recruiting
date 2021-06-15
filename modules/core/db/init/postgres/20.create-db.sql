-- begin ITPEARLS_SOCIAL_NETWORK_UR_LS
alter table ITPEARLS_SOCIAL_NETWORK_UR_LS add constraint FK_ITPEARLS_SOCIAL_NETWORK_UR_LS_ON_JOB_CANDIDATE foreign key (JOB_CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID)^
alter table ITPEARLS_SOCIAL_NETWORK_UR_LS add constraint FK_ITPEARLS_SOCIAL_NETWORK_UR_LS_ON_SOCIAL_NETWORK_URL foreign key (SOCIAL_NETWORK_URL_ID) references ITPEARLS_SOCIAL_NETWORK_TYPE(ID)^
create index IDX_ITPEARLS_SOCIAL_NETWORK_UR_LS_ON_JOB_CANDIDATE on ITPEARLS_SOCIAL_NETWORK_UR_LS (JOB_CANDIDATE_ID)^
create index IDX_ITPEARLS_SOCIAL_NETWORK_UR_LS_ON_SOCIAL_NETWORK_URL on ITPEARLS_SOCIAL_NETWORK_UR_LS (SOCIAL_NETWORK_URL_ID)^
create index IDX_NETWORK_NAME on ITPEARLS_SOCIAL_NETWORK_UR_LS (NETWORK_NAME)^
-- end ITPEARLS_SOCIAL_NETWORK_UR_LS
-- begin ITPEARLS_JOB_CANDIDATE
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_CURRENT_COMPANY foreign key (CURRENT_COMPANY_ID) references ITPEARLS_COMPANY(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_PERSON_POSITION foreign key (PERSON_POSITION_ID) references ITPEARLS_POSITION(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_CITY_OF_RESIDENCE foreign key (CITY_OF_RESIDENCE_ID) references ITPEARLS_CITY(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_POSITION_COUNTRY foreign key (POSITION_COUNTRY_ID) references ITPEARLS_COUNTRY(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_SPECIALISATION foreign key (SPECIALISATION_ID) references ITPEARLS_SPECIALISATION(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_SKILL_TREE foreign key (SKILL_TREE_ID) references ITPEARLS_SKILL_TREE(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_OPEN_POSITION foreign key (OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_FILE_IMAGE_FACE foreign key (FILE_IMAGE_FACE) references SYS_FILE(ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_CURRENT_COMPANY on ITPEARLS_JOB_CANDIDATE (CURRENT_COMPANY_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_PERSON_POSITION on ITPEARLS_JOB_CANDIDATE (PERSON_POSITION_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_CITY_OF_RESIDENCE on ITPEARLS_JOB_CANDIDATE (CITY_OF_RESIDENCE_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_POSITION_COUNTRY on ITPEARLS_JOB_CANDIDATE (POSITION_COUNTRY_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_SPECIALISATION on ITPEARLS_JOB_CANDIDATE (SPECIALISATION_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_SKILL_TREE on ITPEARLS_JOB_CANDIDATE (SKILL_TREE_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_OPEN_POSITION on ITPEARLS_JOB_CANDIDATE (OPEN_POSITION_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_FILE_IMAGE_FACE on ITPEARLS_JOB_CANDIDATE (FILE_IMAGE_FACE)^
create index IDX_ITPEARLS_JOB_CANDIDATE_PERSON_POSITION_ID on ITPEARLS_JOB_CANDIDATE (PERSON_POSITION_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_FULL_NAME on ITPEARLS_JOB_CANDIDATE (FULL_NAME)^
create index IDX_ITPEARLS_JOB_CANDIDATE_OPEN_POSITION_ID on ITPEARLS_JOB_CANDIDATE (OPEN_POSITION_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_CURRENT_COMPANY_ID on ITPEARLS_JOB_CANDIDATE (CURRENT_COMPANY_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_CITY_OF_RESIDENCE_ID on ITPEARLS_JOB_CANDIDATE (CITY_OF_RESIDENCE_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ID on ITPEARLS_JOB_CANDIDATE (ID)^
-- end ITPEARLS_JOB_CANDIDATE
-- begin ITPEARLS_COMPANY_DEPARTAMENT
alter table ITPEARLS_COMPANY_DEPARTAMENT add constraint FK_ITPEARLS_COMPANY_DEPARTAMENT_ON_COMPANY_NAME foreign key (COMPANY_NAME_ID) references ITPEARLS_COMPANY(ID)^
alter table ITPEARLS_COMPANY_DEPARTAMENT add constraint FK_ITPEARLS_COMPANY_DEPARTAMENT_ON_DEPARTAMENT_HR_DIRECTOR foreign key (DEPARTAMENT_HR_DIRECTOR_ID) references ITPEARLS_PERSON(ID)^
alter table ITPEARLS_COMPANY_DEPARTAMENT add constraint FK_ITPEARLS_COMPANY_DEPARTAMENT_ON_DEPARTAMENT_DIRECTOR foreign key (DEPARTAMENT_DIRECTOR_ID) references ITPEARLS_PERSON(ID)^
create index IDX_ITPEARLS_COMPANY_DEPARTAMENT_ON_COMPANY_NAME on ITPEARLS_COMPANY_DEPARTAMENT (COMPANY_NAME_ID)^
create index IDX_ITPEARLS_COMPANY_DEPARTAMENT_ON_DEPARTAMENT_HR_DIRECTOR on ITPEARLS_COMPANY_DEPARTAMENT (DEPARTAMENT_HR_DIRECTOR_ID)^
create index IDX_ITPEARLS_COMPANY_DEPARTAMENT_ON_DEPARTAMENT_DIRECTOR on ITPEARLS_COMPANY_DEPARTAMENT (DEPARTAMENT_DIRECTOR_ID)^
create index IDX_ITPEARLS_COMPANY_DEPARTAMENT on ITPEARLS_COMPANY_DEPARTAMENT (DEPARTAMENT_RU_NAME)^
-- end ITPEARLS_COMPANY_DEPARTAMENT
-- begin ITPEARLS_ITERACTION_LIST
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_CANDIDATE foreign key (CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID)^
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_CURRENT_JOB_POSITION foreign key (CURRENT_JOB_POSITION_ID) references ITPEARLS_POSITION(ID)^
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_VACANCY foreign key (VACANCY_ID) references ITPEARLS_OPEN_POSITION(ID)^
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_PROJECT foreign key (PROJECT_ID) references ITPEARLS_PROJECT(ID)^
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_ITERACTION_TYPE foreign key (ITERACTION_TYPE_ID) references ITPEARLS_ITERACTION(ID)^
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_RECRUTIER foreign key (RECRUTIER_ID) references SEC_USER(ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_CANDIDATE on ITPEARLS_ITERACTION_LIST (CANDIDATE_ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_CURRENT_JOB_POSITION on ITPEARLS_ITERACTION_LIST (CURRENT_JOB_POSITION_ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_VACANCY on ITPEARLS_ITERACTION_LIST (VACANCY_ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_PROJECT on ITPEARLS_ITERACTION_LIST (PROJECT_ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_ITERACTION_TYPE on ITPEARLS_ITERACTION_LIST (ITERACTION_TYPE_ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_RECRUTIER on ITPEARLS_ITERACTION_LIST (RECRUTIER_ID)^
create index IDX_ITERACTION_NAME_NUMBER_ITERACTION on ITPEARLS_ITERACTION_LIST (NUMBER_ITERACTION)^
-- end ITPEARLS_ITERACTION_LIST
-- begin ITPEARLS_CANDIDATE_CV
alter table ITPEARLS_CANDIDATE_CV add constraint FK_ITPEARLS_CANDIDATE_CV_ON_CANDIDATE foreign key (CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID)^
alter table ITPEARLS_CANDIDATE_CV add constraint FK_ITPEARLS_CANDIDATE_CV_ON_RESUME_POSITION foreign key (RESUME_POSITION_ID) references ITPEARLS_POSITION(ID)^
alter table ITPEARLS_CANDIDATE_CV add constraint FK_ITPEARLS_CANDIDATE_CV_ON_TO_VACANCY foreign key (TO_VACANCY_ID) references ITPEARLS_OPEN_POSITION(ID)^
alter table ITPEARLS_CANDIDATE_CV add constraint FK_ITPEARLS_CANDIDATE_CV_ON_OWNER foreign key (OWNER_ID) references SEC_USER(ID)^
alter table ITPEARLS_CANDIDATE_CV add constraint FK_ITPEARLS_CANDIDATE_CV_ON_FILE_CV foreign key (FILE_CV_ID) references SYS_FILE(ID)^
alter table ITPEARLS_CANDIDATE_CV add constraint FK_ITPEARLS_CANDIDATE_CV_ON_ORIGINAL_FILE_CV foreign key (ORIGINAL_FILE_CV_ID) references SYS_FILE(ID)^
alter table ITPEARLS_CANDIDATE_CV add constraint FK_ITPEARLS_CANDIDATE_CV_ON_FILE_IMAGE_FACE foreign key (FILE_IMAGE_FACE) references SYS_FILE(ID)^
create index IDX_ITPEARLS_CANDIDATE_CV_ON_CANDIDATE on ITPEARLS_CANDIDATE_CV (CANDIDATE_ID)^
create index IDX_ITPEARLS_CANDIDATE_CV_ON_RESUME_POSITION on ITPEARLS_CANDIDATE_CV (RESUME_POSITION_ID)^
create index IDX_ITPEARLS_CANDIDATE_CV_ON_TO_VACANCY on ITPEARLS_CANDIDATE_CV (TO_VACANCY_ID)^
create index IDX_ITPEARLS_CANDIDATE_CV_ON_OWNER on ITPEARLS_CANDIDATE_CV (OWNER_ID)^
create index IDX_ITPEARLS_CANDIDATE_CV_ON_FILE_CV on ITPEARLS_CANDIDATE_CV (FILE_CV_ID)^
create index IDX_ITPEARLS_CANDIDATE_CV_ON_ORIGINAL_FILE_CV on ITPEARLS_CANDIDATE_CV (ORIGINAL_FILE_CV_ID)^
create index IDX_ITPEARLS_CANDIDATE_CV_ON_FILE_IMAGE_FACE on ITPEARLS_CANDIDATE_CV (FILE_IMAGE_FACE)^
-- end ITPEARLS_CANDIDATE_CV
-- begin ITPEARLS_COMPANY
alter table ITPEARLS_COMPANY add constraint FK_ITPEARLS_COMPANY_ON_COMPANY_OWNERSHIP foreign key (COMPANY_OWNERSHIP_ID) references ITPEARLS_OWNERSHUP(ID)^
alter table ITPEARLS_COMPANY add constraint FK_ITPEARLS_COMPANY_ON_COMPANY_DIRECTOR foreign key (COMPANY_DIRECTOR_ID) references ITPEARLS_PERSON(ID)^
alter table ITPEARLS_COMPANY add constraint FK_ITPEARLS_COMPANY_ON_CITY_OF_COMPANY foreign key (CITY_OF_COMPANY_ID) references ITPEARLS_CITY(ID)^
alter table ITPEARLS_COMPANY add constraint FK_ITPEARLS_COMPANY_ON_REGION_OF_COMPANY foreign key (REGION_OF_COMPANY_ID) references ITPEARLS_REGION(ID)^
alter table ITPEARLS_COMPANY add constraint FK_ITPEARLS_COMPANY_ON_COUNTRY_OF_COMPANY foreign key (COUNTRY_OF_COMPANY_ID) references ITPEARLS_COUNTRY(ID)^
alter table ITPEARLS_COMPANY add constraint FK_ITPEARLS_COMPANY_ON_FILE_COMPANY_LOGO foreign key (FILE_COMPANY_LOGO) references SYS_FILE(ID)^
create index IDX_ITPEARLS_COMPANY_ON_COMPANY_OWNERSHIP on ITPEARLS_COMPANY (COMPANY_OWNERSHIP_ID)^
create index IDX_ITPEARLS_COMPANY_ON_COMPANY_DIRECTOR on ITPEARLS_COMPANY (COMPANY_DIRECTOR_ID)^
create index IDX_ITPEARLS_COMPANY_ON_CITY_OF_COMPANY on ITPEARLS_COMPANY (CITY_OF_COMPANY_ID)^
create index IDX_ITPEARLS_COMPANY_ON_REGION_OF_COMPANY on ITPEARLS_COMPANY (REGION_OF_COMPANY_ID)^
create index IDX_ITPEARLS_COMPANY_ON_COUNTRY_OF_COMPANY on ITPEARLS_COMPANY (COUNTRY_OF_COMPANY_ID)^
create index IDX_ITPEARLS_COMPANY_ON_FILE_COMPANY_LOGO on ITPEARLS_COMPANY (FILE_COMPANY_LOGO)^
create index IDX_COMPANY_NAME on ITPEARLS_COMPANY (COMANY_NAME)^
-- end ITPEARLS_COMPANY
-- begin ITPEARLS_REGION
alter table ITPEARLS_REGION add constraint FK_ITPEARLS_REGION_ON_REGION_COUNTRY foreign key (REGION_COUNTRY_ID) references ITPEARLS_COUNTRY(ID)^
create unique index IDX_ITPEARLS_REGION_UK_REGION_RU_NAME on ITPEARLS_REGION (REGION_RU_NAME) where DELETE_TS is null ^
create unique index IDX_ITPEARLS_REGION_UK_REGION_CODE on ITPEARLS_REGION (REGION_CODE) where DELETE_TS is null ^
create index IDX_ITPEARLS_REGION_ON_REGION_COUNTRY on ITPEARLS_REGION (REGION_COUNTRY_ID)^
create index IDX_REGION_REGION_RU_NAME on ITPEARLS_REGION (REGION_RU_NAME)^
-- end ITPEARLS_REGION
-- begin ITPEARLS_ITERACTION
alter table ITPEARLS_ITERACTION add constraint FK_ITPEARLS_ITERACTION_ON_ITERACTION_TREE foreign key (ITERACTION_TREE_ID) references ITPEARLS_ITERACTION(ID)^
create unique index IDX_ITPEARLS_ITERACTION_UK_ITERATION_NAME on ITPEARLS_ITERACTION (ITERATION_NAME) where DELETE_TS is null ^
create index IDX_ITPEARLS_ITERACTION_ON_ITERACTION_TREE on ITPEARLS_ITERACTION (ITERACTION_TREE_ID)^
create index IDX_ITPEARLS_ITERACTION_NUMBER on ITPEARLS_ITERACTION (NUMBER_)^
create index IDX_ITPEARLS_ITERACTION_ID on ITPEARLS_ITERACTION (ID)^
create index IDX_ITPEARLS_ITERACTION_NAME on ITPEARLS_ITERACTION (ITERATION_NAME)^
-- end ITPEARLS_ITERACTION
-- begin ITPEARLS_COUNTRY
create unique index IDX_ITPEARLS_COUNTRY_UK_COUNTRY_RU_NAME on ITPEARLS_COUNTRY (COUNTRY_RU_NAME) where DELETE_TS is null ^
create index IDX_COUNTRY_COUNTRY_RU_NAME on ITPEARLS_COUNTRY (COUNTRY_RU_NAME)^
-- end ITPEARLS_COUNTRY
-- begin ITPEARLS_PROJECT
alter table ITPEARLS_PROJECT add constraint FK_ITPEARLS_PROJECT_ON_PROJECT_TREE foreign key (PROJECT_TREE_ID) references ITPEARLS_PROJECT(ID)^
alter table ITPEARLS_PROJECT add constraint FK_ITPEARLS_PROJECT_ON_PROJECT_DEPARTMENT foreign key (PROJECT_DEPARTMENT_ID) references ITPEARLS_COMPANY_DEPARTAMENT(ID)^
alter table ITPEARLS_PROJECT add constraint FK_ITPEARLS_PROJECT_ON_PROJECT_OWNER foreign key (PROJECT_OWNER_ID) references ITPEARLS_PERSON(ID)^
create index IDX_ITPEARLS_PROJECT_ON_PROJECT_TREE on ITPEARLS_PROJECT (PROJECT_TREE_ID)^
create index IDX_ITPEARLS_PROJECT_ON_PROJECT_DEPARTMENT on ITPEARLS_PROJECT (PROJECT_DEPARTMENT_ID)^
create index IDX_ITPEARLS_PROJECT_ON_PROJECT_OWNER on ITPEARLS_PROJECT (PROJECT_OWNER_ID)^
create index IDX_ITPEARLS_PROJECT on ITPEARLS_PROJECT (PROJECT_NAME)^
-- end ITPEARLS_PROJECT
-- begin ITPEARLS_POSITION
alter table ITPEARLS_POSITION add constraint FK_ITPEARLS_POSITION_ON_JOB_CANDIDATE foreign key (JOB_CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID)^
create unique index IDX_ITPEARLS_POSITION_UK_POSITION_RU_NAME on ITPEARLS_POSITION (POSITION_RU_NAME) where DELETE_TS is null ^
create unique index IDX_ITPEARLS_POSITION_UK_POSITION_EN_NAME on ITPEARLS_POSITION (POSITION_EN_NAME) where DELETE_TS is null ^
create index IDX_ITPEARLS_POSITION_ON_JOB_CANDIDATE on ITPEARLS_POSITION (JOB_CANDIDATE_ID)^
create index IDX_ITPEARLS_POSITION_ID on ITPEARLS_POSITION (ID)^
create index IDX_ITPEARLS_POSITION_EN_NAME on ITPEARLS_POSITION (POSITION_EN_NAME)^
create index IDX_ITPEARLS_POSITION_RU_NAME on ITPEARLS_POSITION (POSITION_RU_NAME)^
-- end ITPEARLS_POSITION
-- begin ITPEARLS_PERSON
alter table ITPEARLS_PERSON add constraint FK_ITPEARLS_PERSON_ON_POSITION_COUNTRY foreign key (POSITION_COUNTRY_ID) references ITPEARLS_COUNTRY(ID)^
alter table ITPEARLS_PERSON add constraint FK_ITPEARLS_PERSON_ON_CITY_OF_RESIDENCE foreign key (CITY_OF_RESIDENCE_ID) references ITPEARLS_CITY(ID)^
alter table ITPEARLS_PERSON add constraint FK_ITPEARLS_PERSON_ON_PERSON_POSITION foreign key (PERSON_POSITION_ID) references ITPEARLS_POSITION(ID)^
alter table ITPEARLS_PERSON add constraint FK_ITPEARLS_PERSON_ON_COMPANY_DEPARTMENT foreign key (COMPANY_DEPARTMENT_ID) references ITPEARLS_COMPANY_DEPARTAMENT(ID)^
alter table ITPEARLS_PERSON add constraint FK_ITPEARLS_PERSON_ON_FILE_IMAGE_FACE foreign key (FILE_IMAGE_FACE) references SYS_FILE(ID)^
create unique index IDX_ITPEARLS_PERSON_UK_MOB_PHONE on ITPEARLS_PERSON (MOB_PHONE) where DELETE_TS is null ^
create unique index IDX_ITPEARLS_PERSON_UK_SKYPE_NAME on ITPEARLS_PERSON (SKYPE_NAME) where DELETE_TS is null ^
create unique index IDX_ITPEARLS_PERSON_UK_WIBER_NAME on ITPEARLS_PERSON (WIBER_NAME) where DELETE_TS is null ^
create unique index IDX_ITPEARLS_PERSON_UK_WATSUP_NAME on ITPEARLS_PERSON (WATSUP_NAME) where DELETE_TS is null ^
create unique index IDX_ITPEARLS_PERSON_UK_TELEGRAM_NAME on ITPEARLS_PERSON (TELEGRAM_NAME) where DELETE_TS is null ^
create index IDX_ITPEARLS_PERSON_ON_POSITION_COUNTRY on ITPEARLS_PERSON (POSITION_COUNTRY_ID)^
create index IDX_ITPEARLS_PERSON_ON_CITY_OF_RESIDENCE on ITPEARLS_PERSON (CITY_OF_RESIDENCE_ID)^
create index IDX_ITPEARLS_PERSON_ON_PERSON_POSITION on ITPEARLS_PERSON (PERSON_POSITION_ID)^
create index IDX_ITPEARLS_PERSON_ON_COMPANY_DEPARTMENT on ITPEARLS_PERSON (COMPANY_DEPARTMENT_ID)^
create index IDX_ITPEARLS_PERSON_ON_FILE_IMAGE_FACE on ITPEARLS_PERSON (FILE_IMAGE_FACE)^
-- end ITPEARLS_PERSON
-- begin ITPEARLS_CITY
alter table ITPEARLS_CITY add constraint FK_ITPEARLS_CITY_ON_CITY_REGION foreign key (CITY_REGION_ID) references ITPEARLS_REGION(ID)^
alter table ITPEARLS_CITY add constraint FK_ITPEARLS_CITY_ON_OPEN_POSITION foreign key (OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID)^
create unique index IDX_ITPEARLS_CITY_UK_CITY_PHONE_CODE on ITPEARLS_CITY (CITY_PHONE_CODE) where DELETE_TS is null ^
create unique index IDX_ITPEARLS_CITY_UK_CITY_RU_NAME on ITPEARLS_CITY (CITY_RU_NAME) where DELETE_TS is null ^
create index IDX_ITPEARLS_CITY_ON_CITY_REGION on ITPEARLS_CITY (CITY_REGION_ID)^
create index IDX_ITPEARLS_CITY_ON_OPEN_POSITION on ITPEARLS_CITY (OPEN_POSITION_ID)^
create index IDX_CITY_CITY_RU_NAME on ITPEARLS_CITY (CITY_RU_NAME)^
-- end ITPEARLS_CITY
-- begin ITPEARLS_SKILL_TREE
alter table ITPEARLS_SKILL_TREE add constraint FK_ITPEARLS_SKILL_TREE_ON_SKILL_TREE foreign key (SKILL_TREE_ID) references ITPEARLS_SKILL_TREE(ID)^
alter table ITPEARLS_SKILL_TREE add constraint FK_ITPEARLS_SKILL_TREE_ON_JOB_CANDIDATE foreign key (JOB_CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID)^
alter table ITPEARLS_SKILL_TREE add constraint FK_ITPEARLS_SKILL_TREE_ON_OPEN_POSITION foreign key (OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID)^
alter table ITPEARLS_SKILL_TREE add constraint FK_ITPEARLS_SKILL_TREE_ON_CANDIDATE_CV foreign key (CANDIDATE_CV_ID) references ITPEARLS_CANDIDATE_CV(ID)^
alter table ITPEARLS_SKILL_TREE add constraint FK_ITPEARLS_SKILL_TREE_ON_SPECIALISATION foreign key (SPECIALISATION_ID) references ITPEARLS_SPECIALISATION(ID)^
alter table ITPEARLS_SKILL_TREE add constraint FK_ITPEARLS_SKILL_TREE_ON_FILE_IMAGE_LOGO foreign key (FILE_IMAGE_LOGO) references SYS_FILE(ID)^
create unique index IDX_ITPEARLS_SKILL_TREE_UK_SKILL_NAME on ITPEARLS_SKILL_TREE (SKILL_NAME) where DELETE_TS is null ^
create index IDX_ITPEARLS_SKILL_TREE_ON_SKILL_TREE on ITPEARLS_SKILL_TREE (SKILL_TREE_ID)^
create index IDX_ITPEARLS_SKILL_TREE_ON_JOB_CANDIDATE on ITPEARLS_SKILL_TREE (JOB_CANDIDATE_ID)^
create index IDX_ITPEARLS_SKILL_TREE_ON_OPEN_POSITION on ITPEARLS_SKILL_TREE (OPEN_POSITION_ID)^
create index IDX_ITPEARLS_SKILL_TREE_ON_CANDIDATE_CV on ITPEARLS_SKILL_TREE (CANDIDATE_CV_ID)^
create index IDX_ITPEARLS_SKILL_TREE_ON_SPECIALISATION on ITPEARLS_SKILL_TREE (SPECIALISATION_ID)^
create index IDX_ITPEARLS_SKILL_TREE_ON_FILE_IMAGE_LOGO on ITPEARLS_SKILL_TREE (FILE_IMAGE_LOGO)^
create index IDX_ITPEARLS_SKILL_TREE on ITPEARLS_SKILL_TREE (SKILL_NAME)^
-- end ITPEARLS_SKILL_TREE
-- begin ITPEARLS_OWNERSHUP
create unique index IDX_ITPEARLS_OWNERSHUP_UK_LONG_TYPE on ITPEARLS_OWNERSHUP (LONG_TYPE) where DELETE_TS is null ^
create unique index IDX_ITPEARLS_OWNERSHUP_UK_SHORT_TYPE on ITPEARLS_OWNERSHUP (SHORT_TYPE) where DELETE_TS is null ^
-- end ITPEARLS_OWNERSHUP
-- begin ITPEARLS_SPECIALISATION
create unique index IDX_ITPEARLS_SPECIALISATION_UK_SPEC_RU_NAME on ITPEARLS_SPECIALISATION (SPEC_RU_NAME) where DELETE_TS is null ^
-- end ITPEARLS_SPECIALISATION
-- begin ITPEARLS_COMPANY_GROUP
create unique index IDX_ITPEARLS_COMPANY_GROUP_UK_COMPANY_RU_GROUP_NAME on ITPEARLS_COMPANY_GROUP (COMPANY_RU_GROUP_NAME) where DELETE_TS is null ^
-- end ITPEARLS_COMPANY_GROUP
-- begin ITPEARLS_OPEN_POSITION
alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_CITY_POSITION foreign key (CITY_POSITION_ID) references ITPEARLS_CITY(ID)^
alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_POSITION_TYPE foreign key (POSITION_TYPE_ID) references ITPEARLS_POSITION(ID)^
alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_PROJECT_NAME foreign key (PROJECT_NAME_ID) references ITPEARLS_PROJECT(ID)^
alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_PARENT_OPEN_POSITION foreign key (PARENT_OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID)^
create index IDX_ITPEARLS_OPEN_POSITION_ON_CITY_POSITION on ITPEARLS_OPEN_POSITION (CITY_POSITION_ID)^
create index IDX_ITPEARLS_OPEN_POSITION_ON_POSITION_TYPE on ITPEARLS_OPEN_POSITION (POSITION_TYPE_ID)^
create index IDX_ITPEARLS_OPEN_POSITION_ON_PROJECT_NAME on ITPEARLS_OPEN_POSITION (PROJECT_NAME_ID)^
create index IDX_ITPEARLS_OPEN_POSITION_ON_PARENT_OPEN_POSITION on ITPEARLS_OPEN_POSITION (PARENT_OPEN_POSITION_ID)^
create index IDX_ITPEARLS_OPEN_POSITION_ID on ITPEARLS_OPEN_POSITION (ID)^
create index IDX_ITPEARLS_OPEN_POSITION_VACANCY_NAME on ITPEARLS_OPEN_POSITION (VACANSY_NAME)^
-- end ITPEARLS_OPEN_POSITION
-- begin ITPEARLS_JOB_HISTORY
alter table ITPEARLS_JOB_HISTORY add constraint FK_ITPEARLS_JOB_HISTORY_ON_CANDIDATE foreign key (CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID)^
alter table ITPEARLS_JOB_HISTORY add constraint FK_ITPEARLS_JOB_HISTORY_ON_CURRENT_POSITION foreign key (CURRENT_POSITION_ID) references ITPEARLS_POSITION(ID)^
alter table ITPEARLS_JOB_HISTORY add constraint FK_ITPEARLS_JOB_HISTORY_ON_CURRENT_COMPANY foreign key (CURRENT_COMPANY_ID) references ITPEARLS_COMPANY(ID)^
create index IDX_ITPEARLS_JOB_HISTORY_ON_CANDIDATE on ITPEARLS_JOB_HISTORY (CANDIDATE_ID)^
create index IDX_ITPEARLS_JOB_HISTORY_ON_CURRENT_POSITION on ITPEARLS_JOB_HISTORY (CURRENT_POSITION_ID)^
create index IDX_ITPEARLS_JOB_HISTORY_ON_CURRENT_COMPANY on ITPEARLS_JOB_HISTORY (CURRENT_COMPANY_ID)^
-- end ITPEARLS_JOB_HISTORY
-- begin ITPEARLS_SETUP
alter table ITPEARLS_SETUP add constraint FK_ITPEARLS_SETUP_ON_PARAM_USER foreign key (PARAM_USER_ID) references SEC_USER(ID)^
create unique index IDX_ITPEARLS_SETUP_UK_PERAM_NAME on ITPEARLS_SETUP (PERAM_NAME) where DELETE_TS is null ^
create index IDX_ITPEARLS_SETUP_ON_PARAM_USER on ITPEARLS_SETUP (PARAM_USER_ID)^
-- end ITPEARLS_SETUP

-- begin ITPEARLS_RECRUTIES_TASKS
alter table ITPEARLS_RECRUTIES_TASKS add constraint FK_ITPEARLS_RECRUTIES_TASKS_ON_REACRUTIER foreign key (REACRUTIER_ID) references SEC_USER(ID)^
alter table ITPEARLS_RECRUTIES_TASKS add constraint FK_ITPEARLS_RECRUTIES_TASKS_ON_OPEN_POSITION foreign key (OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID)^
create index IDX_ITPEARLS_RECRUTIES_TASKS_ON_REACRUTIER on ITPEARLS_RECRUTIES_TASKS (REACRUTIER_ID)^
create index IDX_ITPEARLS_RECRUTIES_TASKS_ON_OPEN_POSITION on ITPEARLS_RECRUTIES_TASKS (OPEN_POSITION_ID)^
-- end ITPEARLS_RECRUTIES_TASKS
-- begin ITPEARLS_RECRUITING_RECRUTIERS
alter table ITPEARLS_RECRUITING_RECRUTIERS add constraint FK_ITPEARLS_RECRUITING_RECRUTIERS_ON_RECRUTIER_NAME foreign key (RECRUTIER_NAME_ID) references SEC_USER(ID)^
alter table ITPEARLS_RECRUITING_RECRUTIERS add constraint FK_ITPEARLS_RECRUITING_RECRUTIERS_ON_RECRUTIER_CV foreign key (RECRUTIER_CV_ID) references SYS_FILE(ID)^
create index IDX_ITPEARLS_RECRUITING_RECRUTIERS_ON_RECRUTIER_NAME on ITPEARLS_RECRUITING_RECRUTIERS (RECRUTIER_NAME_ID)^
create index IDX_ITPEARLS_RECRUITING_RECRUTIERS_ON_RECRUTIER_CV on ITPEARLS_RECRUITING_RECRUTIERS (RECRUTIER_CV_ID)^
-- end ITPEARLS_RECRUITING_RECRUTIERS
-- begin ITPEARLS_SOME_FILES
alter table ITPEARLS_SOME_FILES add constraint FK_ITPEARLS_SOME_FILES_ON_FILE_DESCRIPTOR foreign key (FILE_DESCRIPTOR_ID) references SYS_FILE(ID)^
alter table ITPEARLS_SOME_FILES add constraint FK_ITPEARLS_SOME_FILES_ON_FILE_OWNER foreign key (FILE_OWNER_ID) references SEC_USER(ID)^
alter table ITPEARLS_SOME_FILES add constraint FK_ITPEARLS_SOME_FILES_ON_FILE_TYPE foreign key (FILE_TYPE_ID) references ITPEARLS_FILE_TYPE(ID)^
alter table ITPEARLS_SOME_FILES add constraint FK_ITPEARLS_SOME_FILES_ON_CANDIDATE_CV foreign key (CANDIDATE_CV_ID) references ITPEARLS_CANDIDATE_CV(ID)^
create index IDX_ITPEARLS_SOME_FILES_ON_FILE_DESCRIPTOR on ITPEARLS_SOME_FILES (FILE_DESCRIPTOR_ID)^
create index IDX_ITPEARLS_SOME_FILES_ON_FILE_OWNER on ITPEARLS_SOME_FILES (FILE_OWNER_ID)^
create index IDX_ITPEARLS_SOME_FILES_ON_FILE_TYPE on ITPEARLS_SOME_FILES (FILE_TYPE_ID)^
create index IDX_ITPEARLS_SOME_FILES_ON_CANDIDATE_CV on ITPEARLS_SOME_FILES (CANDIDATE_CV_ID)^
-- end ITPEARLS_SOME_FILES
-- begin ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION
alter table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION add constraint FK_ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION_ON_CANDIDATE foreign key (CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID)^
alter table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION add constraint FK_ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION_ON_SUBSCRIBER foreign key (SUBSCRIBER_ID) references SEC_USER(ID)^
create index IDX_ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION_ON_CANDIDATE on ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION (CANDIDATE_ID)^
create index IDX_ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION_ON_SUBSCRIBER on ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION (SUBSCRIBER_ID)^
-- end ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION
-- begin ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK
alter table ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK add constraint FK_OPEPOSRECTAS_ON_OPEN_POSITION foreign key (OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID)^
alter table ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK add constraint FK_OPEPOSRECTAS_ON_RECRUTIES_TASKS foreign key (RECRUTIES_TASKS_ID) references ITPEARLS_RECRUTIES_TASKS(ID)^
-- end ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK
-- begin ITPEARLS_ITEARCTION_REQUIREMENTS
alter table ITPEARLS_ITEARCTION_REQUIREMENTS add constraint FK_ITPEARLS_ITEARCTION_REQUIREMENTS_ON_ITERACTION foreign key (ITERACTION_ID) references ITPEARLS_ITERACTION(ID)^
alter table ITPEARLS_ITEARCTION_REQUIREMENTS add constraint FK_ITPEARLS_ITEARCTION_REQUIREMENTS_ON_ITERACTION_REQUIREMEN foreign key (ITERACTION_REQUIREMEN_ID) references ITPEARLS_ITERACTION(ID)^
create index IDX_ITPEARLS_ITEARCTION_REQUIREMENTS_ON_ITERACTION on ITPEARLS_ITEARCTION_REQUIREMENTS (ITERACTION_ID)^
create index IDX_ITPEARLS_ITEARCTION_REQUIREMENTS_ON_ITERACTION_REQUIREMEN on ITPEARLS_ITEARCTION_REQUIREMENTS (ITERACTION_REQUIREMEN_ID)^
-- end ITPEARLS_ITEARCTION_REQUIREMENTS

-- begin ITPEARLS_COUNTRY
create unique index IDX_ITPEARLS_COUNTRY_UNIQ_COUNTRY_RU_NAME on ITPEARLS_COUNTRY (COUNTRY_RU_NAME) ^
create unique index IDX_ITPEARLS_COUNTRY_UNIQ_PHONE_CODE on ITPEARLS_COUNTRY (PHONE_CODE) ^
create unique index IDX_ITPEARLS_COUNTRY_UNIQ_COUNTRY_SHORT_NAME on ITPEARLS_COUNTRY (COUNTRY_SHORT_NAME) ^
-- end ITPEARLS_COUNTRY
-- begin ITPEARLS_POSITION
create unique index IDX_ITPEARLS_POSITION_UNIQ_POSITION_RU_NAME on ITPEARLS_POSITION (POSITION_RU_NAME) ^
-- end ITPEARLS_POSITION
-- begin ITPEARLS_PERSON
alter table ITPEARLS_PERSON add constraint FK_ITPEARLS_PERSON_ON_POSITION_COUNTRY foreign key (POSITION_COUNTRY_ID) references ITPEARLS_COUNTRY(ID)^
alter table ITPEARLS_PERSON add constraint FK_ITPEARLS_PERSON_ON_PERSON_POSITION foreign key (PERSON_POSITION_ID) references ITPEARLS_POSITION(ID)^
create unique index IDX_ITPEARLS_PERSON_UNIQ_SKYPE_NAME on ITPEARLS_PERSON (SKYPE_NAME) ^
create unique index IDX_ITPEARLS_PERSON_UNIQ_TELEGRAM_NAME on ITPEARLS_PERSON (TELEGRAM_NAME) ^
create unique index IDX_ITPEARLS_PERSON_UNIQ_WATSUP_NAME on ITPEARLS_PERSON (WATSUP_NAME) ^
create unique index IDX_ITPEARLS_PERSON_UNIQ_WIBER_NAME on ITPEARLS_PERSON (WIBER_NAME) ^
create index IDX_ITPEARLS_PERSON_ON_POSITION_COUNTRY on ITPEARLS_PERSON (POSITION_COUNTRY_ID)^
create index IDX_ITPEARLS_PERSON_ON_PERSON_POSITION on ITPEARLS_PERSON (PERSON_POSITION_ID)^
-- end ITPEARLS_PERSON

-- begin ITPEARLS_REGION
alter table ITPEARLS_REGION add constraint FK_ITPEARLS_REGION_ON_REGION_COUNTRY foreign key (REGION_COUNTRY_ID) references ITPEARLS_COUNTRY(ID)^
create unique index IDX_ITPEARLS_REGION_UNIQ_REGION_RU_NAME on ITPEARLS_REGION (REGION_RU_NAME) ^
create unique index IDX_ITPEARLS_REGION_UNIQ_REGION_CODE on ITPEARLS_REGION (REGION_CODE) ^
create index IDX_ITPEARLS_REGION_ON_REGION_COUNTRY on ITPEARLS_REGION (REGION_COUNTRY_ID)^
-- end ITPEARLS_REGION
-- begin ITPEARLS_CITY
alter table ITPEARLS_CITY add constraint FK_ITPEARLS_CITY_ON_CITY_REGION foreign key (CITY_REGION_ID) references ITPEARLS_REGION(ID)^
create unique index IDX_ITPEARLS_CITY_UNIQ_CITY_RU_NAME on ITPEARLS_CITY (CITY_RU_NAME) ^
create unique index IDX_ITPEARLS_CITY_UNIQ_CITY_PHONE_CODE on ITPEARLS_CITY (CITY_PHONE_CODE) ^
create index IDX_ITPEARLS_CITY_ON_CITY_REGION on ITPEARLS_CITY (CITY_REGION_ID)^
-- end ITPEARLS_CITY
-- begin ITPEARLS_OWNERSHUP
create unique index IDX_ITPEARLS_OWNERSHUP_UNIQ_SHORT_TYPE on ITPEARLS_OWNERSHUP (SHORT_TYPE) ^
create unique index IDX_ITPEARLS_OWNERSHUP_UNIQ_LONG_TYPE on ITPEARLS_OWNERSHUP (LONG_TYPE) ^
-- end ITPEARLS_OWNERSHUP
-- begin ITPEARLS_COMPANY
alter table ITPEARLS_COMPANY add constraint FK_ITPEARLS_COMPANY_ON_COMPANY_OWNERSHIP foreign key (COMPANY_OWNERSHIP_ID) references ITPEARLS_OWNERSHUP(ID)^
alter table ITPEARLS_COMPANY add constraint FK_ITPEARLS_COMPANY_ON_COMPANY_DIRECTOR foreign key (COMPANY_DIRECTOR_ID) references ITPEARLS_PERSON(ID)^
create unique index IDX_ITPEARLS_COMPANY_UNIQ_COMPANY_SHORT_NAME on ITPEARLS_COMPANY (COMPANY_SHORT_NAME) ^
create unique index IDX_ITPEARLS_COMPANY_UNIQ_COMANY_NAME on ITPEARLS_COMPANY (COMANY_NAME) ^
create index IDX_ITPEARLS_COMPANY_ON_COMPANY_OWNERSHIP on ITPEARLS_COMPANY (COMPANY_OWNERSHIP_ID)^
create index IDX_ITPEARLS_COMPANY_ON_COMPANY_DIRECTOR on ITPEARLS_COMPANY (COMPANY_DIRECTOR_ID)^
-- end ITPEARLS_COMPANY
-- begin ITPEARLS_COMPANY_DEPARTAMENT
alter table ITPEARLS_COMPANY_DEPARTAMENT add constraint FK_ITPEARLS_COMPANY_DEPARTAMENT_ON_COMPANY_NAME foreign key (COMPANY_NAME_ID) references ITPEARLS_COMPANY(ID)^
alter table ITPEARLS_COMPANY_DEPARTAMENT add constraint FK_ITPEARLS_COMPANY_DEPARTAMENT_ON_DEPARTAMENT_HR_DIRECTOR foreign key (DEPARTAMENT_HR_DIRECTOR_ID) references ITPEARLS_PERSON(ID)^
alter table ITPEARLS_COMPANY_DEPARTAMENT add constraint FK_ITPEARLS_COMPANY_DEPARTAMENT_ON_DEPARTAMENT_DIRECTOR foreign key (DEPARTAMENT_DIRECTOR_ID) references ITPEARLS_PERSON(ID)^
create index IDX_ITPEARLS_COMPANY_DEPARTAMENT_ON_COMPANY_NAME on ITPEARLS_COMPANY_DEPARTAMENT (COMPANY_NAME_ID)^
create index IDX_ITPEARLS_COMPANY_DEPARTAMENT_ON_DEPARTAMENT_HR_DIRECTOR on ITPEARLS_COMPANY_DEPARTAMENT (DEPARTAMENT_HR_DIRECTOR_ID)^
create index IDX_ITPEARLS_COMPANY_DEPARTAMENT_ON_DEPARTAMENT_DIRECTOR on ITPEARLS_COMPANY_DEPARTAMENT (DEPARTAMENT_DIRECTOR_ID)^
-- end ITPEARLS_COMPANY_DEPARTAMENT
-- begin ITPEARLS_SPECIALISATION
create unique index IDX_ITPEARLS_SPECIALISATION_UNIQ_SPEC_RU_NAME on ITPEARLS_SPECIALISATION (SPEC_RU_NAME) ^
-- end ITPEARLS_SPECIALISATION

-- begin ITPEARLS_PROJECT
alter table ITPEARLS_PROJECT add constraint FK_ITPEARLS_PROJECT_ON_PROJECT_DEPARTMENT foreign key (PROJECT_DEPARTMENT_ID) references ITPEARLS_COMPANY_DEPARTAMENT(ID)^
alter table ITPEARLS_PROJECT add constraint FK_ITPEARLS_PROJECT_ON_PROJECT_OWNER foreign key (PROJECT_OWNER_ID) references ITPEARLS_PERSON(ID)^
create index IDX_ITPEARLS_PROJECT_ON_PROJECT_DEPARTMENT on ITPEARLS_PROJECT (PROJECT_DEPARTMENT_ID)^
create index IDX_ITPEARLS_PROJECT_ON_PROJECT_OWNER on ITPEARLS_PROJECT (PROJECT_OWNER_ID)^
-- end ITPEARLS_PROJECT
-- begin ITPEARLS_OPEN_POSITION
alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_POSITION_TYPE foreign key (POSITION_TYPE_ID) references ITPEARLS_POSITION(ID)^
alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_PROJECT_NAME foreign key (PROJECT_NAME_ID) references ITPEARLS_PROJECT(ID)^
alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_COMPANY_NAME foreign key (COMPANY_NAME_ID) references ITPEARLS_COMPANY(ID)^
alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_COMPANY_DEPARTAMENT foreign key (COMPANY_DEPARTAMENT_ID) references ITPEARLS_COMPANY_DEPARTAMENT(ID)^
create index IDX_ITPEARLS_OPEN_POSITION_ON_POSITION_TYPE on ITPEARLS_OPEN_POSITION (POSITION_TYPE_ID)^
create index IDX_ITPEARLS_OPEN_POSITION_ON_PROJECT_NAME on ITPEARLS_OPEN_POSITION (PROJECT_NAME_ID)^
create index IDX_ITPEARLS_OPEN_POSITION_ON_COMPANY_NAME on ITPEARLS_OPEN_POSITION (COMPANY_NAME_ID)^
create index IDX_ITPEARLS_OPEN_POSITION_ON_COMPANY_DEPARTAMENT on ITPEARLS_OPEN_POSITION (COMPANY_DEPARTAMENT_ID)^
-- end ITPEARLS_OPEN_POSITION
-- begin ITPEARLS_COMPANY_GROUP
create unique index IDX_ITPEARLS_COMPANY_GROUP_UNIQ_COMPANY_RU_GROUP_NAME on ITPEARLS_COMPANY_GROUP (COMPANY_RU_GROUP_NAME) ^
-- end ITPEARLS_COMPANY_GROUP

-- begin ITPEARLS_ITERACTION
create unique index IDX_ITPEARLS_ITERACTION_UNIQ_ITERATION_NAME on ITPEARLS_ITERACTION (ITERATION_NAME) ^
create unique index IDX_ITPEARLS_ITERACTION_UNIQ_NUMBER_ on ITPEARLS_ITERACTION (NUMBER_) ^
-- end ITPEARLS_ITERACTION
-- begin ITPEARLS_JOB_CANDIDATE
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_PERSON_POSITION foreign key (PERSON_POSITION_ID) references ITPEARLS_POSITION(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_CURRENT_COMPANY foreign key (CURRENT_COMPANY_ID) references ITPEARLS_COMPANY(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_POSITION_COUNTRY foreign key (POSITION_COUNTRY_ID) references ITPEARLS_COUNTRY(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_CITY_OF_RESIDENCE foreign key (CITY_OF_RESIDENCE_ID) references ITPEARLS_CITY(ID)^
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_SPECIALISATION foreign key (SPECIALISATION_ID) references ITPEARLS_SPECIALISATION(ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_PERSON_POSITION on ITPEARLS_JOB_CANDIDATE (PERSON_POSITION_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_CURRENT_COMPANY on ITPEARLS_JOB_CANDIDATE (CURRENT_COMPANY_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_POSITION_COUNTRY on ITPEARLS_JOB_CANDIDATE (POSITION_COUNTRY_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_CITY_OF_RESIDENCE on ITPEARLS_JOB_CANDIDATE (CITY_OF_RESIDENCE_ID)^
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_SPECIALISATION on ITPEARLS_JOB_CANDIDATE (SPECIALISATION_ID)^
-- end ITPEARLS_JOB_CANDIDATE
-- begin ITPEARLS_ITERACTION_LIST
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_CANDIDATE foreign key (CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID)^
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_CURRENT_JOB_POSITION foreign key (CURRENT_JOB_POSITION_ID) references ITPEARLS_POSITION(ID)^
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_PROJECT foreign key (PROJECT_ID) references ITPEARLS_PROJECT(ID)^
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_COMPANY_DEPARTMENT foreign key (COMPANY_DEPARTMENT_ID) references ITPEARLS_COMPANY_DEPARTAMENT(ID)^
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_ITERACTION_TYPE foreign key (ITERACTION_TYPE_ID) references ITPEARLS_ITERACTION(ID)^
alter table ITPEARLS_ITERACTION_LIST add constraint FK_ITPEARLS_ITERACTION_LIST_ON_RECRUTIER foreign key (RECRUTIER_ID) references SEC_USER(ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_CANDIDATE on ITPEARLS_ITERACTION_LIST (CANDIDATE_ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_CURRENT_JOB_POSITION on ITPEARLS_ITERACTION_LIST (CURRENT_JOB_POSITION_ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_PROJECT on ITPEARLS_ITERACTION_LIST (PROJECT_ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_COMPANY_DEPARTMENT on ITPEARLS_ITERACTION_LIST (COMPANY_DEPARTMENT_ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_ITERACTION_TYPE on ITPEARLS_ITERACTION_LIST (ITERACTION_TYPE_ID)^
create index IDX_ITPEARLS_ITERACTION_LIST_ON_RECRUTIER on ITPEARLS_ITERACTION_LIST (RECRUTIER_ID)^
-- end ITPEARLS_ITERACTION_LIST
-- begin ITPEARLS_SOCIAL_NETWORK_UR_LS
create unique index IDX_ITPEARLS_SOCIAL_NETWORK_UR_LS_UNIQ_NETWORK_NAME on ITPEARLS_SOCIAL_NETWORK_UR_LS (NETWORK_NAME) ^
create unique index IDX_ITPEARLS_SOCIAL_NETWORK_UR_LS_UNIQ_NETWORK_URLS on ITPEARLS_SOCIAL_NETWORK_UR_LS (NETWORK_URLS) ^
-- end ITPEARLS_SOCIAL_NETWORK_UR_LS
-- begin ITPEARLS_SKILL_TREE
alter table ITPEARLS_SKILL_TREE add constraint FK_ITPEARLS_SKILL_TREE_ON_SKILL_TREE foreign key (SKILL_TREE_ID) references ITPEARLS_SKILL_TREE(ID)^
alter table ITPEARLS_SKILL_TREE add constraint FK_ITPEARLS_SKILL_TREE_ON_JOB_CANDIDATE foreign key (JOB_CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID)^
alter table ITPEARLS_SKILL_TREE add constraint FK_ITPEARLS_SKILL_TREE_ON_OPEN_POSITION foreign key (OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID)^
create unique index IDX_ITPEARLS_SKILL_TREE_UNIQ_SKILL_NAME on ITPEARLS_SKILL_TREE (SKILL_NAME) ^
create index IDX_ITPEARLS_SKILL_TREE_ON_SKILL_TREE on ITPEARLS_SKILL_TREE (SKILL_TREE_ID)^
create index IDX_ITPEARLS_SKILL_TREE_ON_JOB_CANDIDATE on ITPEARLS_SKILL_TREE (JOB_CANDIDATE_ID)^
create index IDX_ITPEARLS_SKILL_TREE_ON_OPEN_POSITION on ITPEARLS_SKILL_TREE (OPEN_POSITION_ID)^
-- end ITPEARLS_SKILL_TREE
-- begin ITPEARLS_JOB_HISTORY
alter table ITPEARLS_JOB_HISTORY add constraint FK_ITPEARLS_JOB_HISTORY_ON_CANDIDATE foreign key (CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID)^
alter table ITPEARLS_JOB_HISTORY add constraint FK_ITPEARLS_JOB_HISTORY_ON_CURRENT_POSITION foreign key (CURRENT_POSITION_ID) references ITPEARLS_POSITION(ID)^
alter table ITPEARLS_JOB_HISTORY add constraint FK_ITPEARLS_JOB_HISTORY_ON_CURRENT_COMPANY foreign key (CURRENT_COMPANY_ID) references ITPEARLS_COMPANY(ID)^
create index IDX_ITPEARLS_JOB_HISTORY_ON_CANDIDATE on ITPEARLS_JOB_HISTORY (CANDIDATE_ID)^
create index IDX_ITPEARLS_JOB_HISTORY_ON_CURRENT_POSITION on ITPEARLS_JOB_HISTORY (CURRENT_POSITION_ID)^
create index IDX_ITPEARLS_JOB_HISTORY_ON_CURRENT_COMPANY on ITPEARLS_JOB_HISTORY (CURRENT_COMPANY_ID)^
-- end ITPEARLS_JOB_HISTORY

alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_POSITION_COUNTRY foreign key (POSITION_COUNTRY_ID) references ITPEARLS_COUNTRY(ID);
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_PERSON_POSITION foreign key (PERSON_POSITION_ID) references ITPEARLS_POSITION(ID);
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_POSITION_COUNTRY on ITPEARLS_JOB_CANDIDATE (POSITION_COUNTRY_ID);
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_PERSON_POSITION on ITPEARLS_JOB_CANDIDATE (PERSON_POSITION_ID);

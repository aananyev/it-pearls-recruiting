alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_CITY_OF_RESIDENCE foreign key (CITY_OF_RESIDENCE_ID) references ITPEARLS_CITY(ID);
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_CITY_OF_RESIDENCE on ITPEARLS_JOB_CANDIDATE (CITY_OF_RESIDENCE_ID);

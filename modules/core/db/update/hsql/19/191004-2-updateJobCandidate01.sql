alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_SPECIALISATION foreign key (SPECIALISATION_ID) references ITPEARLS_SPECIALISATION(ID);
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_SPECIALISATION on ITPEARLS_JOB_CANDIDATE (SPECIALISATION_ID);

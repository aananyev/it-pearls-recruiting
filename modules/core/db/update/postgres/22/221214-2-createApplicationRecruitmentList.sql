alter table ITPEARLS_APPLICATION_RECRUITMENT_LIST add constraint FK_ITPEARLS_APPLICATION_RECRUITMENT_LIST_ON_RECRUITER foreign key (RECRUITER_ID) references SEC_USER(ID);
create index IDX_ITPEARLS_APPLICATION_RECRUITMENT_LIST_ON_RECRUITER on ITPEARLS_APPLICATION_RECRUITMENT_LIST (RECRUITER_ID);

alter table ITPEARLS_APPLICATION_RECRUITMENT_LIST add constraint FK_ITPEARLS_APPLICATION_RECRUITMENT_LIST_ON_PROJECT foreign key (PROJECT_ID) references ITPEARLS_PROJECT(ID);
create index IDX_ITPEARLS_APPLICATION_RECRUITMENT_LIST_ON_PROJECT on ITPEARLS_APPLICATION_RECRUITMENT_LIST (PROJECT_ID);
alter table ITPEARLS_PROJECT add constraint FK_ITPEARLS_PROJECT_ON_PROJECT_GROUP foreign key (PROJECT_GROUP) references ITPEARLS_PROJECT(ID);
create index IDX_ITPEARLS_PROJECT_ON_PROJECT_GROUP on ITPEARLS_PROJECT (PROJECT_GROUP);

alter table ITPEARLS_PROJECT add constraint FK_ITPEARLS_PROJECT_ON_PROJECT_TREE foreign key (PROJECT_TREE_ID) references ITPEARLS_PROJECT(ID);
create index IDX_ITPEARLS_PROJECT_ON_PROJECT_TREE on ITPEARLS_PROJECT (PROJECT_TREE_ID);
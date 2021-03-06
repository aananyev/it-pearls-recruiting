alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_PROJECT_NAME foreign key (PROJECT_NAME_ID) references ITPEARLS_PROJECT(ID);
alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_COMPANY_NAME foreign key (COMPANY_NAME_ID) references ITPEARLS_COMPANY(ID);
create index IDX_ITPEARLS_OPEN_POSITION_ON_PROJECT_NAME on ITPEARLS_OPEN_POSITION (PROJECT_NAME_ID);
create index IDX_ITPEARLS_OPEN_POSITION_ON_COMPANY_NAME on ITPEARLS_OPEN_POSITION (COMPANY_NAME_ID);

alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_COMPANY_DEPARTAMENT foreign key (COMPANY_DEPARTAMENT_ID) references ITPEARLS_COMPANY_DEPARTAMENT(ID);
create index IDX_ITPEARLS_OPEN_POSITION_ON_COMPANY_DEPARTAMENT on ITPEARLS_OPEN_POSITION (COMPANY_DEPARTAMENT_ID);

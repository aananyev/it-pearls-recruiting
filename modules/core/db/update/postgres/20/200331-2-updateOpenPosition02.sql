alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_COMPANY_NAME foreign key (COMPANY_NAME_ID) references ITPEARLS_COMPANY(ID);
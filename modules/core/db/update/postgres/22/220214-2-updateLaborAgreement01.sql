alter table ITPEARLS_LABOR_AGREEMENT add constraint FK_ITPEARLS_LABOR_AGREEMENT_ON_LEGAL_ENTITY_EMPLOYEE foreign key (LEGAL_ENTITY_EMPLOYEE_ID) references ITPEARLS_COMPANY(ID);
create index IDX_ITPEARLS_LABOR_AGREEMENT_ON_LEGAL_ENTITY_EMPLOYEE on ITPEARLS_LABOR_AGREEMENT (LEGAL_ENTITY_EMPLOYEE_ID);

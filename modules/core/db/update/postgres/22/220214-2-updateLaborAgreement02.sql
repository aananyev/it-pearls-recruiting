alter table ITPEARLS_LABOR_AGREEMENT add constraint FK_ITPEARLS_LABOR_AGREEMENT_ON_CONTRACTOR_COMPANY foreign key (CONTRACTOR_COMPANY_ID) references ITPEARLS_COMPANY(ID);
create index IDX_ITPEARLS_LABOR_AGREEMENT_ON_CONTRACTOR_COMPANY on ITPEARLS_LABOR_AGREEMENT (CONTRACTOR_COMPANY_ID);

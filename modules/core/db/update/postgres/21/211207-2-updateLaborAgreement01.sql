alter table ITPEARLS_LABOR_AGREEMENT add constraint FK_ITPEARLS_LABOR_AGREEMENT_ON_LABOR_AGREEMENT_TYPE foreign key (LABOR_AGREEMENT_TYPE_ID) references ITPEARLS_LABOR_AGEEMENT_TYPE(ID);
create index IDX_ITPEARLS_LABOR_AGREEMENT_ON_LABOR_AGREEMENT_TYPE on ITPEARLS_LABOR_AGREEMENT (LABOR_AGREEMENT_TYPE_ID);

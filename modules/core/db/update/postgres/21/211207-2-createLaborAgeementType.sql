alter table ITPEARLS_LABOR_AGEEMENT_TYPE add constraint FK_ITPEARLS_LABOR_AGEEMENT_TYPE_ON_LABOR_AGREEMENT foreign key (LABOR_AGREEMENT_ID) references ITPEARLS_LABOR_AGREEMENT(ID);
create index IDX_ITPEARLS_LABOR_AGEEMENT_TYPE_ON_LABOR_AGREEMENT on ITPEARLS_LABOR_AGEEMENT_TYPE (LABOR_AGREEMENT_ID);

alter table ITPEARLS_LABOR_AGREEMENT add constraint FK_ITPEARLS_LABOR_AGREEMENT_ON_FILE_AGREEMENT foreign key (FILE_AGREEMENT_ID) references SYS_FILE(ID);
create index IDX_ITPEARLS_LABOR_AGREEMENT_ON_FILE_AGREEMENT on ITPEARLS_LABOR_AGREEMENT (FILE_AGREEMENT_ID);

alter table ITPEARLS_LABOR_AGREEMENT add column ADDITIONAL_LABOR_AGREEMENT_ID uuid ;
alter table ITPEARLS_LABOR_AGREEMENT add column RATE integer ^
update ITPEARLS_LABOR_AGREEMENT set RATE = 0 where RATE is null ;
alter table ITPEARLS_LABOR_AGREEMENT alter column RATE set not null ;

-- update ITPEARLS_LABOR_AGREEMENT set LABOR_AGREEMENT_TYPE_ID = <default_value> where LABOR_AGREEMENT_TYPE_ID is null ;
alter table ITPEARLS_LABOR_AGREEMENT alter column LABOR_AGREEMENT_TYPE_ID set not null ;
-- update ITPEARLS_LABOR_AGREEMENT set COMPANY_ID = <default_value> where COMPANY_ID is null ;
alter table ITPEARLS_LABOR_AGREEMENT alter column COMPANY_ID set not null ;

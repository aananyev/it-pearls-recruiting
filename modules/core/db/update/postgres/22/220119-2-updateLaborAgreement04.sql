update ITPEARLS_LABOR_AGREEMENT set AGREEMENT_NUMBER = '' where AGREEMENT_NUMBER is null ;
alter table ITPEARLS_LABOR_AGREEMENT alter column AGREEMENT_NUMBER set not null ;
update ITPEARLS_LABOR_AGREEMENT set AGREEMENT_NAME = '' where AGREEMENT_NAME is null ;
alter table ITPEARLS_LABOR_AGREEMENT alter column AGREEMENT_NAME set not null ;
update ITPEARLS_LABOR_AGREEMENT set AGREEMENT_DATE = current_date where AGREEMENT_DATE is null ;
alter table ITPEARLS_LABOR_AGREEMENT alter column AGREEMENT_DATE set not null ;
-- update ITPEARLS_LABOR_AGREEMENT set JOB_CANDIDATE_ID = <default_value> where JOB_CANDIDATE_ID is null ;
alter table ITPEARLS_LABOR_AGREEMENT alter column JOB_CANDIDATE_ID set not null ;

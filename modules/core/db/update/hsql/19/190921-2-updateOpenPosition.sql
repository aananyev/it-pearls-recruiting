alter table ITPEARLS_OPEN_POSITION add column NUMBER_POSITION integer ^
update ITPEARLS_OPEN_POSITION set NUMBER_POSITION = 0 where NUMBER_POSITION is null ;
alter table ITPEARLS_OPEN_POSITION alter column NUMBER_POSITION set not null ;
-- alter table ITPEARLS_OPEN_POSITION add column PROJECT_ID varchar(36) ^
-- update ITPEARLS_OPEN_POSITION set PROJECT_ID = <default_value> ;
-- alter table ITPEARLS_OPEN_POSITION alter column PROJECT_ID set not null ;
alter table ITPEARLS_OPEN_POSITION add column PROJECT_ID varchar(36) not null ;
-- update ITPEARLS_OPEN_POSITION set PROJECT_NAME_ID = <default_value> where PROJECT_NAME_ID is null ;
alter table ITPEARLS_OPEN_POSITION alter column PROJECT_NAME_ID set not null ;
-- update ITPEARLS_OPEN_POSITION set COMPANY_NAME_ID = <default_value> where COMPANY_NAME_ID is null ;
alter table ITPEARLS_OPEN_POSITION alter column COMPANY_NAME_ID set not null ;

alter table ITPEARLS_OPEN_POSITION add column POSITION_TYPE_ID varchar(36) ;
alter table ITPEARLS_OPEN_POSITION add column VACANSY_NAME varchar(80) ^
update ITPEARLS_OPEN_POSITION set VACANSY_NAME = '' where VACANSY_NAME is null ;
alter table ITPEARLS_OPEN_POSITION alter column VACANSY_NAME set not null ;
alter table ITPEARLS_OPEN_POSITION add column COMPANY_DEPARTAMENT_ID varchar(36) ;

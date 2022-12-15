-- update ITPEARLS_APPLICATION_RECRUITMENT set STAFFING_TABLE_ID = <default_value> where STAFFING_TABLE_ID is null ;
alter table ITPEARLS_APPLICATION_RECRUITMENT alter column STAFFING_TABLE_ID set not null ;
update ITPEARLS_APPLICATION_RECRUITMENT set AMOUNT = 0 where AMOUNT is null ;
alter table ITPEARLS_APPLICATION_RECRUITMENT alter column AMOUNT set not null ;
update ITPEARLS_APPLICATION_RECRUITMENT set APPLICATION_DATE = current_timestamp where APPLICATION_DATE is null ;
alter table ITPEARLS_APPLICATION_RECRUITMENT alter column APPLICATION_DATE set not null ;
-- update ITPEARLS_APPLICATION_RECRUITMENT set CODE = <default_value> where CODE is null ;
alter table ITPEARLS_APPLICATION_RECRUITMENT alter column CODE set not null ;

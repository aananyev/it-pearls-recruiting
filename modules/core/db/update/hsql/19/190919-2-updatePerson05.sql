-- update ITPEARLS_PERSON set WATSUP_NAME = <default_value> where WATSUP_NAME is null ;
alter table ITPEARLS_PERSON alter column WATSUP_NAME set not null ;

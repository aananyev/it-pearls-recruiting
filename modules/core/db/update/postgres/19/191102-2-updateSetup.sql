-- alter table ITPEARLS_SETUP add column PARAM_USER_ID uuid ^
-- update ITPEARLS_SETUP set PARAM_USER_ID = <default_value> ;
-- alter table ITPEARLS_SETUP alter column PARAM_USER_ID set not null ;
alter table ITPEARLS_SETUP add column PARAM_USER_ID uuid not null ;

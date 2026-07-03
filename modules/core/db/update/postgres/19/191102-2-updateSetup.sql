-- alter table HUNTTECH_SETUP add column PARAM_USER_ID uuid ^
-- update HUNTTECH_SETUP set PARAM_USER_ID = <default_value> ;
-- alter table HUNTTECH_SETUP alter column PARAM_USER_ID set not null ;
alter table HUNTTECH_SETUP add column PARAM_USER_ID uuid not null ;

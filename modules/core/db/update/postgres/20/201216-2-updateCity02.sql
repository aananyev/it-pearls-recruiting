alter table ITPEARLS_CITY rename column open_position_id to open_position_id__u31770 ;
alter table ITPEARLS_CITY drop constraint FK_ITPEARLS_CITY_ON_OPEN_POSITION ;
drop index IDX_ITPEARLS_CITY_ON_OPEN_POSITION ;
alter table ITPEARLS_CITY add column OPEN_POSITION_ID uuid ;

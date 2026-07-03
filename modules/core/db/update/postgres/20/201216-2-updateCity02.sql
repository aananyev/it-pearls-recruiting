alter table HUNTTECH_CITY rename column open_position_id to open_position_id__u31770 ;
alter table HUNTTECH_CITY drop constraint FK_HUNTTECH_CITY_ON_OPEN_POSITION ;
drop index IDX_HUNTTECH_CITY_ON_OPEN_POSITION ;
alter table HUNTTECH_CITY add column OPEN_POSITION_ID uuid ;

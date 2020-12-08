alter table ITPEARLS_CITY rename column open_position_id to open_position_id__u65883 ;
alter table ITPEARLS_CITY drop constraint FK_ITPEARLS_CITY_ON_OPEN_POSITION ;
drop index IDX_ITPEARLS_CITY_ON_OPEN_POSITION ;

alter table HUNTTECH_POSITION rename column logo_id to logo_id__u25898 ;
alter table HUNTTECH_POSITION drop constraint FK_HUNTTECH_POSITION_ON_LOGO ;
drop index IDX_HUNTTECH_POSITION_ON_LOGO ;

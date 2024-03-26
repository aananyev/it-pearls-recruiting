alter table ITPEARLS_POSITION rename column logo_id to logo_id__u25898 ;
alter table ITPEARLS_POSITION drop constraint FK_ITPEARLS_POSITION_ON_LOGO ;
drop index IDX_ITPEARLS_POSITION_ON_LOGO ;

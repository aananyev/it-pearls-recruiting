alter table ITPEARLS_APPLICATION_SETUP rename column coordinator_id to coordinator_id__u27801 ;
alter table ITPEARLS_APPLICATION_SETUP drop constraint FK_ITPEARLS_APPLICATION_SETUP_ON_COORDINATOR ;
drop index IDX_ITPEARLS_APPLICATION_SETUP_ON_COORDINATOR ;
alter table ITPEARLS_APPLICATION_SETUP rename column administrator_id to administrator_id__u80578 ;
alter table ITPEARLS_APPLICATION_SETUP drop constraint FK_ITPEARLS_APPLICATION_SETUP_ON_ADMINISTRATOR ;
drop index IDX_ITPEARLS_APPLICATION_SETUP_ON_ADMINISTRATOR ;
alter table ITPEARLS_APPLICATION_SETUP rename column telegram_bot_started to telegram_bot_started__u76883 ;
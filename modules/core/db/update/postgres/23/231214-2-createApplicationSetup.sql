alter table ITPEARLS_APPLICATION_SETUP add constraint FK_ITPEARLS_APPLICATION_SETUP_ON_APPLICATION_LOGO foreign key (APPLICATION_LOGO_ID) references SYS_FILE(ID);
alter table ITPEARLS_APPLICATION_SETUP add constraint FK_ITPEARLS_APPLICATION_SETUP_ON_APPLICATION_ICON foreign key (APPLICATION_ICON_ID) references SYS_FILE(ID);
create index IDX_ITPEARLS_APPLICATION_SETUP_ON_APPLICATION_LOGO on ITPEARLS_APPLICATION_SETUP (APPLICATION_LOGO_ID);
create index IDX_ITPEARLS_APPLICATION_SETUP_ON_APPLICATION_ICON on ITPEARLS_APPLICATION_SETUP (APPLICATION_ICON_ID);
alter table ITPEARLS_PROJECT add constraint FK_ITPEARLS_PROJECT_ON_PROJECT_LOGO foreign key (PROJECT_LOGO_ID) references SYS_FILE(ID);
create index IDX_ITPEARLS_PROJECT_ON_PROJECT_LOGO on ITPEARLS_PROJECT (PROJECT_LOGO_ID);

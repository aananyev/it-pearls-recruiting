alter table ITPEARLS_SIGN_ICONS add constraint FK_ITPEARLS_SIGN_ICONS_ON_USER foreign key (USER_ID) references SEC_USER(ID);
create index IDX_ITPEARLS_SIGN_ICONS_ON_USER on ITPEARLS_SIGN_ICONS (USER_ID);

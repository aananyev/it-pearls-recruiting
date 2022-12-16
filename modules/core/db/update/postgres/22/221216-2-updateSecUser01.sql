alter table SEC_USER add constraint FK_SEC_USER_ON_IMAGE foreign key (IMAGE_ID) references SYS_FILE(ID);
create index IDX_SEC_USER_ON_IMAGE on SEC_USER (IMAGE_ID);

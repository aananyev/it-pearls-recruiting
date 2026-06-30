alter table SEC_USER add constraint FK_SEC_USER_ON_OFFICIAL_PHOTO foreign key (OFFICIAL_PHOTO_ID) references SYS_FILE(ID);
alter table SEC_USER add constraint FK_SEC_USER_ON_USER_AVATAR foreign key (USER_AVATAR_ID) references SYS_FILE(ID);
create index IDX_SEC_USER_ON_OFFICIAL_PHOTO on SEC_USER (OFFICIAL_PHOTO_ID);
create index IDX_SEC_USER_ON_USER_AVATAR on SEC_USER (USER_AVATAR_ID);

alter table ITPEARLS_SOME_FILES add constraint FK_ITPEARLS_SOME_FILES_ON_OPEN_POSITION foreign key (OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID);
create index IDX_ITPEARLS_SOME_FILES_ON_OPEN_POSITION on ITPEARLS_SOME_FILES (OPEN_POSITION_ID);
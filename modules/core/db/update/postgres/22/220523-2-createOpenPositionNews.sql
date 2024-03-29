alter table ITPEARLS_OPEN_POSITION_NEWS add constraint FK_ITPEARLS_OPEN_POSITION_NEWS_ON_OPEN_POSITION foreign key (OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID);
alter table ITPEARLS_OPEN_POSITION_NEWS add constraint FK_ITPEARLS_OPEN_POSITION_NEWS_ON_AUTHOR foreign key (AUTHOR_ID) references SEC_USER(ID);
create index IDX_ITPEARLS_OPEN_POSITION_NEWS_ON_OPEN_POSITION on ITPEARLS_OPEN_POSITION_NEWS (OPEN_POSITION_ID);
create index IDX_ITPEARLS_OPEN_POSITION_NEWS_ON_AUTHOR on ITPEARLS_OPEN_POSITION_NEWS (AUTHOR_ID);

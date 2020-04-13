alter table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION add constraint FK_ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION_ON_CANDIDATE foreign key (CANDIDATE_ID) references ITPEARLS_JOB_CANDIDATE(ID);
alter table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION add constraint FK_ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION_ON_SUBSCRIBER foreign key (SUBSCRIBER_ID) references SEC_USER(ID);
create index IDX_ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION_ON_CANDIDATE on ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION (CANDIDATE_ID);
create index IDX_ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION_ON_SUBSCRIBER on ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION (SUBSCRIBER_ID);
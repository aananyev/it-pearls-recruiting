alter table ITPEARLS_CANDIDATE_CV_WORK_PLACE add constraint FK_ITPEARLS_CANDIDATE_CV_WORK_PLACE_ON_POSITION foreign key (POSITION_ID) references ITPEARLS_POSITION(ID);
create index IDX_ITPEARLS_CANDIDATE_CV_WORK_PLACE_ON_POSITION on ITPEARLS_CANDIDATE_CV_WORK_PLACE (POSITION_ID);

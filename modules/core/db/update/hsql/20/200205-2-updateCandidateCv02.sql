alter table ITPEARLS_CANDIDATE_CV add constraint FK_ITPEARLS_CANDIDATE_CV_ON_RESUME_POSITION foreign key (RESUME_POSITION_ID) references ITPEARLS_POSITION(ID);
create index IDX_ITPEARLS_CANDIDATE_CV_ON_RESUME_POSITION on ITPEARLS_CANDIDATE_CV (RESUME_POSITION_ID);
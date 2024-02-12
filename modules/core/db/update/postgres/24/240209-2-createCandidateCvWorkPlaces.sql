alter table ITPEARLS_CANDIDATE_CV_WORK_PLACES add constraint FK_ITPEARLS_CANDIDATE_CV_WORK_PLACES_ON_CANDIDATE_CV foreign key (CANDIDATE_CV_ID) references ITPEARLS_CANDIDATE_CV(ID);
alter table ITPEARLS_CANDIDATE_CV_WORK_PLACES add constraint FK_ITPEARLS_CANDIDATE_CV_WORK_PLACES_ON_WORK_PLACE foreign key (WORK_PLACE_ID) references ITPEARLS_COMPANY(ID);
create index IDX_ITPEARLS_CANDIDATE_CV_WORK_PLACES_ON_CANDIDATE_CV on ITPEARLS_CANDIDATE_CV_WORK_PLACES (CANDIDATE_CV_ID);
create index IDX_ITPEARLS_CANDIDATE_CV_WORK_PLACES_ON_WORK_PLACE on ITPEARLS_CANDIDATE_CV_WORK_PLACES (WORK_PLACE_ID);

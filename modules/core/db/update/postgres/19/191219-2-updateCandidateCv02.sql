alter table ITPEARLS_CANDIDATE_CV rename column some_files_id to some_files_id__u39126 ;
alter table ITPEARLS_CANDIDATE_CV drop constraint FK_ITPEARLS_CANDIDATE_CV_ON_SOME_FILES ;
drop index IDX_ITPEARLS_CANDIDATE_CV_ON_SOME_FILES ;

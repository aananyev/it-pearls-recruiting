alter table ITPEARLS_JOB_CANDIDATE rename column dtype to dtype__u86327 ;
alter table ITPEARLS_JOB_CANDIDATE rename column partners_id to partners_id__u14479 ;
alter table ITPEARLS_JOB_CANDIDATE drop constraint FK_ITPEARLS_JOB_CANDIDATE_ON_PARTNERS ;
drop index IDX_ITPEARLS_JOB_CANDIDATE_ON_PARTNERS ;

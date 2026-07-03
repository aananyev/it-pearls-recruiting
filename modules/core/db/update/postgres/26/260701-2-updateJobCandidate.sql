alter table HUNTTECH_JOB_CANDIDATE rename column dtype to dtype__u86327 ;
alter table HUNTTECH_JOB_CANDIDATE rename column partners_id to partners_id__u14479 ;
alter table HUNTTECH_JOB_CANDIDATE drop constraint FK_HUNTTECH_JOB_CANDIDATE_ON_PARTNERS ;
drop index IDX_HUNTTECH_JOB_CANDIDATE_ON_PARTNERS ;

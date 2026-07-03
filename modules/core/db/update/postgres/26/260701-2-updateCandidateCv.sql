alter table HUNTTECH_CANDIDATE_CV rename column dtype to dtype__u67733 ;
alter table HUNTTECH_CANDIDATE_CV rename column partners_id to partners_id__u85584 ;
alter table HUNTTECH_CANDIDATE_CV drop constraint FK_HUNTTECH_CANDIDATE_CV_ON_PARTNERS ;
drop index IDX_HUNTTECH_CANDIDATE_CV_ON_PARTNERS ;

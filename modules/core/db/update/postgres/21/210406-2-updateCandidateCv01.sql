alter table ITPEARLS_CANDIDATE_CV add constraint FK_ITPEARLS_CANDIDATE_CV_ON_FILE_IMAGE_FACE foreign key (FILE_IMAGE_FACE) references SYS_FILE(ID);
create index IDX_ITPEARLS_CANDIDATE_CV_ON_FILE_IMAGE_FACE on ITPEARLS_CANDIDATE_CV (FILE_IMAGE_FACE);
alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_FILE_IMAGE_FACE foreign key (FILE_IMAGE_FACE) references SYS_FILE(ID);
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_FILE_IMAGE_FACE on ITPEARLS_JOB_CANDIDATE (FILE_IMAGE_FACE);
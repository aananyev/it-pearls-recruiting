-- alter table ITPEARLS_CANDIDATE_CV_WORK_PLACES add column POSITION_ID uuid ^
-- update ITPEARLS_CANDIDATE_CV_WORK_PLACES set POSITION_ID = <default_value> ;
-- alter table ITPEARLS_CANDIDATE_CV_WORK_PLACES alter column POSITION_ID set not null ;
alter table ITPEARLS_CANDIDATE_CV_WORK_PLACES add column POSITION_ID uuid not null ;

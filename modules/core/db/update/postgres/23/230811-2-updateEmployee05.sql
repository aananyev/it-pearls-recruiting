-- update ITPEARLS_EMPLOYEE set JOB_CANDIDATE_ID = <default_value> where JOB_CANDIDATE_ID is null ;
alter table ITPEARLS_EMPLOYEE alter column JOB_CANDIDATE_ID set not null ;

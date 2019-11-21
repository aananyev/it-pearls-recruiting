-- update ITPEARLS_ITERACTION_LIST set CANDIDATE_ID = <default_value> where CANDIDATE_ID is null ;
alter table ITPEARLS_ITERACTION_LIST alter column CANDIDATE_ID set not null ;

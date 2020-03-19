alter table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION rename column end_date to end_date__u84104 ;
alter table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION rename column start_date to start_date__u15341 ;
alter table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION add column START_DATE date ;
alter table ITPEARLS_SUBSCRIBE_CANDIDATE_ACTION add column END_DATE date ;

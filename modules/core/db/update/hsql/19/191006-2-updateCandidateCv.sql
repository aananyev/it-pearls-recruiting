alter table ITPEARLS_CANDIDATE_CV add column DATE_POST date ^
update ITPEARLS_CANDIDATE_CV set DATE_POST = current_date where DATE_POST is null ;
alter table ITPEARLS_CANDIDATE_CV alter column DATE_POST set not null ;

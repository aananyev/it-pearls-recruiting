alter table ITPEARLS_EMPLOYEE rename column labor_agreement_id to labor_agreement_id__u58956 ;
alter table ITPEARLS_EMPLOYEE drop constraint FK_ITPEARLS_EMPLOYEE_ON_LABOR_AGREEMENT ;
drop index IDX_ITPEARLS_EMPLOYEE_ON_LABOR_AGREEMENT ;
alter table ITPEARLS_EMPLOYEE rename column job_candidate_id to job_candidate_id__u86333 ;
alter table ITPEARLS_EMPLOYEE alter column job_candidate_id__u86333 drop not null ;
alter table ITPEARLS_EMPLOYEE drop constraint FK_ITPEARLS_EMPLOYEE_ON_JOB_CANDIDATE ;
drop index IDX_ITPEARLS_EMPLOYEE_ON_JOB_CANDIDATE ;

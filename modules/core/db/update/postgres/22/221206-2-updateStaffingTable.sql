alter table ITPEARLS_STAFFING_TABLE rename column project_id to project_id__u38413 ;
alter table ITPEARLS_STAFFING_TABLE drop constraint FK_ITPEARLS_STAFFING_TABLE_ON_PROJECT ;
drop index IDX_ITPEARLS_STAFFING_TABLE_ON_PROJECT ;
alter table ITPEARLS_STAFFING_TABLE rename column position_id to position_id__u82448 ;
alter table ITPEARLS_STAFFING_TABLE drop constraint FK_ITPEARLS_STAFFING_TABLE_ON_POSITION ;
drop index IDX_ITPEARLS_STAFFING_TABLE_ON_POSITION ;
-- alter table ITPEARLS_STAFFING_TABLE add column GRADE_ID uuid ^
-- update ITPEARLS_STAFFING_TABLE set GRADE_ID = <default_value> ;
-- alter table ITPEARLS_STAFFING_TABLE alter column GRADE_ID set not null ;
alter table ITPEARLS_STAFFING_TABLE add column GRADE_ID uuid not null ;
alter table ITPEARLS_STAFFING_TABLE add column SALARY_MIN decimal(19, 2) ;
alter table ITPEARLS_STAFFING_TABLE add column SALARY_MAX varchar(255) ;
-- update ITPEARLS_STAFFING_TABLE set OPEN_POSITION_ID = <default_value> where OPEN_POSITION_ID is null ;
alter table ITPEARLS_STAFFING_TABLE alter column OPEN_POSITION_ID set not null ;
update ITPEARLS_STAFFING_TABLE set NUMBER_OF_STAFF = 0 where NUMBER_OF_STAFF is null ;
alter table ITPEARLS_STAFFING_TABLE alter column NUMBER_OF_STAFF set not null ;

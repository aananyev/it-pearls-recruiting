alter table ITPEARLS_APPLICATION_RECRUITMENT_LIST add column QUICK_DESCRIPTION varchar(80) ^
update ITPEARLS_APPLICATION_RECRUITMENT_LIST set QUICK_DESCRIPTION = '' where QUICK_DESCRIPTION is null ;
alter table ITPEARLS_APPLICATION_RECRUITMENT_LIST alter column QUICK_DESCRIPTION set not null ;
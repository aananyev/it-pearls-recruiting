update HUNTTECH_APPLICATION_RECRUITMENT_LIST set OPEN_DATE = current_date where OPEN_DATE is null ;
alter table HUNTTECH_APPLICATION_RECRUITMENT_LIST alter column OPEN_DATE set not null ;

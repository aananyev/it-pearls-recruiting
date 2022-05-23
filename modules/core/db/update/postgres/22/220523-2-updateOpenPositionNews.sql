alter table ITPEARLS_OPEN_POSITION_NEWS add column SUBJECT varchar(255) ^
update ITPEARLS_OPEN_POSITION_NEWS set SUBJECT = '' where SUBJECT is null ;
alter table ITPEARLS_OPEN_POSITION_NEWS alter column SUBJECT set not null ;

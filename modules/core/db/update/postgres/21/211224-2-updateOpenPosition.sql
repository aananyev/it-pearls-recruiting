alter table ITPEARLS_OPEN_POSITION alter column VACANSY_NAME set data type varchar(250) ;
update ITPEARLS_OPEN_POSITION set WORK_EXPERIENCE = 0 where WORK_EXPERIENCE is null ;
alter table ITPEARLS_OPEN_POSITION alter column WORK_EXPERIENCE set not null ;
update ITPEARLS_OPEN_POSITION set COMMAND_EXPERIENCE = 0 where COMMAND_EXPERIENCE is null ;
alter table ITPEARLS_OPEN_POSITION alter column COMMAND_EXPERIENCE set not null ;

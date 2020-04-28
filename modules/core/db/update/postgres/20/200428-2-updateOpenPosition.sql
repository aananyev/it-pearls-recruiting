alter table ITPEARLS_OPEN_POSITION add column REMOTE_WORK integer ^
update ITPEARLS_OPEN_POSITION set REMOTE_WORK = 0 where REMOTE_WORK is null ;
alter table ITPEARLS_OPEN_POSITION alter column REMOTE_WORK set not null ;

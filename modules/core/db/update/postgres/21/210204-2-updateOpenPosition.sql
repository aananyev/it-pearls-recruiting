update ITPEARLS_OPEN_POSITION set COMMAND_CANDIDATE = 0 where COMMAND_CANDIDATE is null ;
alter table ITPEARLS_OPEN_POSITION alter column COMMAND_CANDIDATE set not null ;
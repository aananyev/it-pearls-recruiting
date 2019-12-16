update ITPEARLS_RECRUTIES_TASKS set START_DATE = current_date where START_DATE is null ;
alter table ITPEARLS_RECRUTIES_TASKS alter column START_DATE set not null ;
-- update ITPEARLS_RECRUTIES_TASKS set REACRUTIER_ID = <default_value> where REACRUTIER_ID is null ;
alter table ITPEARLS_RECRUTIES_TASKS alter column REACRUTIER_ID set not null ;
-- update ITPEARLS_RECRUTIES_TASKS set OPEN_POSITION_ID = <default_value> where OPEN_POSITION_ID is null ;
alter table ITPEARLS_RECRUTIES_TASKS alter column OPEN_POSITION_ID set not null ;

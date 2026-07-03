alter table HUNTTECH_ITERACTION_LIST alter column DATE_INTERVIEW rename to DATE_INTERVIEW__U79032 ^
alter table HUNTTECH_ITERACTION_LIST alter column SALARY rename to SALARY__U97276 ^
alter table HUNTTECH_ITERACTION_LIST add column ADD_TYPE integer ;
alter table HUNTTECH_ITERACTION_LIST add column ADD_STRING varchar(255) ;
alter table HUNTTECH_ITERACTION_LIST add column ADD_INTEGER integer ;
alter table HUNTTECH_ITERACTION_LIST add column ADD_DATE timestamp ;

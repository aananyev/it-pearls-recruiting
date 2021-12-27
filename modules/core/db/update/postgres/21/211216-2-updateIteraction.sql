alter table ITPEARLS_ITERACTION add column OUTSTAFFING_SIGN boolean ^
update ITPEARLS_ITERACTION set OUTSTAFFING_SIGN = false where OUTSTAFFING_SIGN is null ;
alter table ITPEARLS_ITERACTION alter column OUTSTAFFING_SIGN set not null ;

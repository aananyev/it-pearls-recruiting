alter table HUNTTECH_ITERACTION add column OUTSTAFFING_SIGN boolean ^
update HUNTTECH_ITERACTION set OUTSTAFFING_SIGN = false where OUTSTAFFING_SIGN is null ;
alter table HUNTTECH_ITERACTION alter column OUTSTAFFING_SIGN set not null ;

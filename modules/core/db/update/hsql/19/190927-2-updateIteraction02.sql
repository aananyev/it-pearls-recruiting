-- update HUNTTECH_ITERACTION set NUMBER_ = <default_value> where NUMBER_ is null ;
alter table HUNTTECH_ITERACTION alter column NUMBER_ set not null ;

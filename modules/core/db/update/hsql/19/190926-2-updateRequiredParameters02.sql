-- update HUNTTECH_REQUIRED_PARAMETERS set NUMBER_ = <default_value> where NUMBER_ is null ;
alter table HUNTTECH_REQUIRED_PARAMETERS alter column NUMBER_ set not null ;

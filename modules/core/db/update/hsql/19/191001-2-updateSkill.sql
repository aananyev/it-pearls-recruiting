-- update HUNTTECH_SKILL set SKILL_TYPE_ID = <default_value> where SKILL_TYPE_ID is null ;
alter table HUNTTECH_SKILL alter column SKILL_TYPE_ID set not null ;

alter table HUNTTECH_SKILL alter column SKILL_TYPE_ID rename to SKILL_TYPE_ID__U72697 ^
alter table HUNTTECH_SKILL alter column SKILL_TYPE_ID__U72697 set null ;
alter table HUNTTECH_SKILL drop constraint FK_HUNTTECH_SKILL_ON_SKILL_TYPE ;
drop index IDX_HUNTTECH_SKILL_ON_SKILL_TYPE ;

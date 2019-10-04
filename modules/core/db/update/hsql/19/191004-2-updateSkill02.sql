alter table ITPEARLS_SKILL alter column SKILL_TYPE_ID rename to SKILL_TYPE_ID__U72697 ^
alter table ITPEARLS_SKILL alter column SKILL_TYPE_ID__U72697 set null ;
alter table ITPEARLS_SKILL drop constraint FK_ITPEARLS_SKILL_ON_SKILL_TYPE ;
drop index IDX_ITPEARLS_SKILL_ON_SKILL_TYPE ;

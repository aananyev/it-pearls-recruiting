alter table ITPEARLS_SKILL alter column SPECIALISATION_ID rename to SPECIALISATION_ID__U86432 ^
alter table ITPEARLS_SKILL drop constraint FK_ITPEARLS_SKILL_ON_SPECIALISATION ;
drop index IDX_ITPEARLS_SKILL_ON_SPECIALISATION ;
alter table ITPEARLS_SKILL add column SKILL_SPECIALISATION_ID varchar(36) ;

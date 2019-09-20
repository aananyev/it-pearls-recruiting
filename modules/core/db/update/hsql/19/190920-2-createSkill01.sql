alter table ITPEARLS_SKILL add constraint FK_ITPEARLS_SKILL_ON_SKILL_TYPE foreign key (SKILL_TYPE_ID) references ITPEARLS_SPECIALISATION(ID);
create unique index IDX_ITPEARLS_SKILL_UNIQ_SKILL_NAME on ITPEARLS_SKILL (SKILL_NAME) ;
create index IDX_ITPEARLS_SKILL_ON_SKILL_TYPE on ITPEARLS_SKILL (SKILL_TYPE_ID);

alter table ITPEARLS_JOB_CANDIDATE add constraint FK_ITPEARLS_JOB_CANDIDATE_ON_SKILL_TREE foreign key (SKILL_TREE_ID) references ITPEARLS_SKILL_TREE(ID);
create index IDX_ITPEARLS_JOB_CANDIDATE_ON_SKILL_TREE on ITPEARLS_JOB_CANDIDATE (SKILL_TREE_ID);

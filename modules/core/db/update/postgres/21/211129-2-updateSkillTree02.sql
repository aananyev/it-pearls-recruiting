alter table ITPEARLS_SKILL_TREE add constraint FK_ITPEARLS_SKILL_TREE_ON_SKILL_TREE foreign key (SKILL_TREE_ID) references ITPEARLS_SKILL_TREE(ID);
create index IDX_ITPEARLS_SKILL_TREE_ON_SKILL_TREE on ITPEARLS_SKILL_TREE (SKILL_TREE_ID);
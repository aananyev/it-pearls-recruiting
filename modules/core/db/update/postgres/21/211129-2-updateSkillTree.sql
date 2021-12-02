alter table ITPEARLS_SKILL_TREE rename column skill_tree_id to skill_tree_id__u84606 ;
alter table ITPEARLS_SKILL_TREE drop constraint FK_ITPEARLS_SKILL_TREE_ON_SKILL_TREE ;
drop index IDX_ITPEARLS_SKILL_TREE_ON_SKILL_TREE ;

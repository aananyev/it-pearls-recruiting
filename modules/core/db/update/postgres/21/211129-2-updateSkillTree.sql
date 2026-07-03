alter table HUNTTECH_SKILL_TREE rename column skill_tree_id to skill_tree_id__u84606 ;
alter table HUNTTECH_SKILL_TREE drop constraint FK_HUNTTECH_SKILL_TREE_ON_SKILL_TREE ;
drop index IDX_HUNTTECH_SKILL_TREE_ON_SKILL_TREE ;

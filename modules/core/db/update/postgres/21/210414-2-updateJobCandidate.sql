alter table ITPEARLS_JOB_CANDIDATE rename column skill_tree_id to skill_tree_id__u25456 ;
alter table ITPEARLS_JOB_CANDIDATE drop constraint FK_ITPEARLS_JOB_CANDIDATE_ON_SKILL_TREE ;
drop index IDX_ITPEARLS_JOB_CANDIDATE_ON_SKILL_TREE ;
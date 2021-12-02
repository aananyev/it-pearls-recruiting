alter table ITPEARLS_PROJECT rename column project_tree_id to project_tree_id__u03647 ;
alter table ITPEARLS_PROJECT drop constraint FK_ITPEARLS_PROJECT_ON_PROJECT_TREE ;
drop index IDX_ITPEARLS_PROJECT_ON_PROJECT_TREE ;

alter table ITPEARLS_PROJECT rename column project_group to project_group__u43909 ;
alter table ITPEARLS_PROJECT drop constraint FK_ITPEARLS_PROJECT_ON_PROJECT_GROUP ;
drop index IDX_ITPEARLS_PROJECT_ON_PROJECT_GROUP ;
alter table ITPEARLS_PROJECT add column PROJECT_TREE_ID uuid ;

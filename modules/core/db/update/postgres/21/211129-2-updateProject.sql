alter table HUNTTECH_PROJECT rename column project_tree_id to project_tree_id__u03647 ;
alter table HUNTTECH_PROJECT drop constraint FK_HUNTTECH_PROJECT_ON_PROJECT_TREE ;
drop index IDX_HUNTTECH_PROJECT_ON_PROJECT_TREE ;

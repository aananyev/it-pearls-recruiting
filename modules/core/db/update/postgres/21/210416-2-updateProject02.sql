alter table HUNTTECH_PROJECT rename column project_group to project_group__u43909 ;
alter table HUNTTECH_PROJECT drop constraint FK_HUNTTECH_PROJECT_ON_PROJECT_GROUP ;
drop index IDX_HUNTTECH_PROJECT_ON_PROJECT_GROUP ;
alter table HUNTTECH_PROJECT add column PROJECT_TREE_ID uuid ;

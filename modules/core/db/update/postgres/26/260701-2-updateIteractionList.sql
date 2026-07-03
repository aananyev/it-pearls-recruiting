alter table HUNTTECH_ITERACTION_LIST rename column dtype to dtype__u23056 ;
alter table HUNTTECH_ITERACTION_LIST rename column partner_id to partner_id__u73602 ;
alter table HUNTTECH_ITERACTION_LIST drop constraint FK_HUNTTECH_ITERACTION_LIST_ON_PARTNER ;
drop index IDX_HUNTTECH_ITERACTION_LIST_ON_PARTNER ;

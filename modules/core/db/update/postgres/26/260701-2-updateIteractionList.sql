alter table ITPEARLS_ITERACTION_LIST rename column dtype to dtype__u23056 ;
alter table ITPEARLS_ITERACTION_LIST rename column partner_id to partner_id__u73602 ;
alter table ITPEARLS_ITERACTION_LIST drop constraint FK_ITPEARLS_ITERACTION_LIST_ON_PARTNER ;
drop index IDX_ITPEARLS_ITERACTION_LIST_ON_PARTNER ;

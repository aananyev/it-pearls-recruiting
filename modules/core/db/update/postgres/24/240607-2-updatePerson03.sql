alter table ITPEARLS_PERSON rename column dtype to dtype__u83346 ;
alter table ITPEARLS_PERSON rename column partner_id to partner_id__u01374 ;
alter table ITPEARLS_PERSON drop constraint FK_ITPEARLS_PERSON_ON_PARTNER ;
drop index IDX_ITPEARLS_PERSON_ON_PARTNER ;

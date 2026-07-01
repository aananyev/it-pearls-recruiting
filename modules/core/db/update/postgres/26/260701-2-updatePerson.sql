alter table ITPEARLS_PERSON rename column dtype to dtype__u65187 ;
alter table ITPEARLS_PERSON rename column partners_id to partners_id__u90873 ;
alter table ITPEARLS_PERSON drop constraint FK_ITPEARLS_PERSON_ON_PARTNERS ;
drop index IDX_ITPEARLS_PERSON_ON_PARTNERS ;
alter table ITPEARLS_PERSON rename column partner_person_login to partner_person_login__u05504 ;

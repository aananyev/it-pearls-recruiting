alter table ITPEARLS_ITERACTION_LIST rename column salary to salary__u93408 ;
alter table ITPEARLS_ITERACTION_LIST rename column date_interview to date_interview__u21654 ;
alter table ITPEARLS_ITERACTION_LIST add column ADD_TYPE integer ;
alter table ITPEARLS_ITERACTION_LIST add column ADD_DATE timestamp ;
alter table ITPEARLS_ITERACTION_LIST add column ADD_SUM decimal(19, 2) ;

alter table ITPEARLS_EMPLOYEE rename column currency_id to currency_id__u24985 ;
alter table ITPEARLS_EMPLOYEE drop constraint FK_ITPEARLS_EMPLOYEE_ON_CURRENCY ;
drop index IDX_ITPEARLS_EMPLOYEE_ON_CURRENCY ;

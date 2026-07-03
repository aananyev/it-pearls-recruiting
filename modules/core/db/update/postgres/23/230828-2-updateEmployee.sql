alter table HUNTTECH_EMPLOYEE rename column currency_id to currency_id__u24985 ;
alter table HUNTTECH_EMPLOYEE drop constraint FK_HUNTTECH_EMPLOYEE_ON_CURRENCY ;
drop index IDX_HUNTTECH_EMPLOYEE_ON_CURRENCY ;

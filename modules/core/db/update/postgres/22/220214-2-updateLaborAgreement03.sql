alter table ITPEARLS_LABOR_AGREEMENT rename column employee_or_customer to employee_or_customer__u34748 ;
alter table ITPEARLS_LABOR_AGREEMENT alter column employee_or_customer__u34748 drop not null ;
alter table ITPEARLS_LABOR_AGREEMENT add column EMPLOYEE_OR_CUSTOMER integer ^
update ITPEARLS_LABOR_AGREEMENT set EMPLOYEE_OR_CUSTOMER = 0 where EMPLOYEE_OR_CUSTOMER is null ;
alter table ITPEARLS_LABOR_AGREEMENT alter column EMPLOYEE_OR_CUSTOMER set not null ;

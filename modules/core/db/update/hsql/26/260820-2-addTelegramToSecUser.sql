-- HRM HuntTech: добавление поля TELEGRAM в таблицу SEC_USER для ExtUser (HSQL).
alter table SEC_USER add column TELEGRAM varchar(64);

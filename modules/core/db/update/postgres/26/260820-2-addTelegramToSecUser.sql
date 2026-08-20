-- HRM HuntTech: добавление поля TELEGRAM в таблицу SEC_USER для ExtUser.
alter table SEC_USER add column if not exists TELEGRAM varchar(64);

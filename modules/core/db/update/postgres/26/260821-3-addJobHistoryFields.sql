-- HRM HuntTech: расширение HUNTTECH_JOB_HISTORY для хранения полной истории работы кандидата
alter table HUNTTECH_JOB_HISTORY alter column CURRENT_POSITION_ID drop not null;
alter table HUNTTECH_JOB_HISTORY add column if not exists START_DATE date;
alter table HUNTTECH_JOB_HISTORY add column if not exists END_DATE date;
alter table HUNTTECH_JOB_HISTORY add column if not exists DUTIES text;
alter table HUNTTECH_JOB_HISTORY add column if not exists RAW_POSITION_NAME varchar(255);
alter table HUNTTECH_JOB_HISTORY add column if not exists RAW_COMPANY_NAME varchar(255);

alter table HUNTTECH_COUNTRY add column if not exists FILE_FLAG_ID uuid;
alter table HUNTTECH_CITY add column if not exists FILE_CITY_EMBLEM_ID uuid;
alter table HUNTTECH_REGION add column if not exists FILE_REGION_EMBLEM_ID uuid;

alter table HUNTTECH_COUNTRY drop constraint if exists FK_HUNTTECH_COUNTRY_FILE_FLAG;
alter table HUNTTECH_COUNTRY add constraint FK_HUNTTECH_COUNTRY_FILE_FLAG foreign key (FILE_FLAG_ID) references SYS_FILE(ID);

alter table HUNTTECH_CITY drop constraint if exists FK_HUNTTECH_CITY_FILE_CITY_EMBLEM;
alter table HUNTTECH_CITY add constraint FK_HUNTTECH_CITY_FILE_CITY_EMBLEM foreign key (FILE_CITY_EMBLEM_ID) references SYS_FILE(ID);

alter table HUNTTECH_REGION drop constraint if exists FK_HUNTTECH_REGION_FILE_REGION_EMBLEM;
alter table HUNTTECH_REGION add constraint FK_HUNTTECH_REGION_FILE_REGION_EMBLEM foreign key (FILE_REGION_EMBLEM_ID) references SYS_FILE(ID);

create index if not exists IDX_HUNTTECH_COUNTRY_FILE_FLAG on HUNTTECH_COUNTRY (FILE_FLAG_ID);
create index if not exists IDX_HUNTTECH_CITY_FILE_CITY_EMBLEM on HUNTTECH_CITY (FILE_CITY_EMBLEM_ID);
create index if not exists IDX_HUNTTECH_REGION_FILE_REGION_EMBLEM on HUNTTECH_REGION (FILE_REGION_EMBLEM_ID);

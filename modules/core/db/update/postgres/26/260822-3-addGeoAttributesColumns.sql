-- 260822-3-addGeoAttributesColumns.sql
-- Расширение схемы гео-справочников (Страны, Регионы, Города) и добавление настроек Geo-API в UserSettings

-- HUNTTECH_COUNTRY
alter table HUNTTECH_COUNTRY add column if not exists COUNTRY_ENG_NAME varchar(100);
alter table HUNTTECH_COUNTRY add column if not exists ALPHA3_CODE varchar(3);
alter table HUNTTECH_COUNTRY add column if not exists NUMERIC_CODE varchar(3);
alter table HUNTTECH_COUNTRY add column if not exists CURRENCY_CODE varchar(3);
alter table HUNTTECH_COUNTRY add column if not exists CAPITAL varchar(100);

-- HUNTTECH_REGION
alter table HUNTTECH_REGION add column if not exists REGION_ENG_NAME varchar(100);
alter table HUNTTECH_REGION add column if not exists ISO_CODE varchar(10);
alter table HUNTTECH_REGION add column if not exists FIAS_ID varchar(50);
alter table HUNTTECH_REGION add column if not exists REGION_TYPE varchar(50);
alter table HUNTTECH_REGION add column if not exists CAPITAL varchar(100);
alter table HUNTTECH_REGION add column if not exists TIME_ZONE varchar(50);

-- HUNTTECH_CITY
alter table HUNTTECH_CITY alter column CITY_PHONE_CODE type varchar(10);
drop index if exists IDX_HUNTTECH_CITY_UK_CITY_PHONE_CODE;
alter table HUNTTECH_CITY add column if not exists CITY_ENG_NAME varchar(100);
alter table HUNTTECH_CITY add column if not exists POSTAL_CODE varchar(20);
alter table HUNTTECH_CITY add column if not exists FIAS_ID varchar(50);
alter table HUNTTECH_CITY add column if not exists POPULATION bigint;
alter table HUNTTECH_CITY add column if not exists LATITUDE double precision;
alter table HUNTTECH_CITY add column if not exists LONGITUDE double precision;
alter table HUNTTECH_CITY add column if not exists TIME_ZONE varchar(50);

-- HUNTTECH_USER_SETTINGS
alter table HUNTTECH_USER_SETTINGS add column if not exists GEO_API_KEY varchar(255);
alter table HUNTTECH_USER_SETTINGS add column if not exists GEO_API_SECRET varchar(255);
alter table HUNTTECH_USER_SETTINGS add column if not exists GEO_API_URL varchar(255);
alter table HUNTTECH_USER_SETTINGS add column if not exists GEO_AUTO_FETCH_FLAGS boolean default true;


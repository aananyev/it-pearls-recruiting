-- 260823-1-addGeoBlobImageFields.sql
-- Добавление BLOB полей для хранения флагов и гербов в БД (для миграции без файлового хранилища)

-- HUNTTECH_COUNTRY
alter table HUNTTECH_COUNTRY add column if not exists FLAG_IMAGE bytea;
alter table HUNTTECH_COUNTRY add column if not exists FLAG_URL varchar(500);

-- HUNTTECH_REGION
alter table HUNTTECH_REGION add column if not exists EMBLEM_IMAGE bytea;
alter table HUNTTECH_REGION add column if not exists EMBLEM_URL varchar(500);

-- HUNTTECH_CITY
alter table HUNTTECH_CITY add column if not exists EMBLEM_IMAGE bytea;
alter table HUNTTECH_CITY add column if not exists EMBLEM_URL varchar(500);

-- Комментарии для документации схемы
comment on column HUNTTECH_COUNTRY.FLAG_IMAGE is 'Флаг страны в байтах (BLOB) — хранится в БД для упрощения миграций';
comment on column HUNTTECH_COUNTRY.FLAG_URL is 'Исходный URL флага для повторной загрузки';
comment on column HUNTTECH_REGION.EMBLEM_IMAGE is 'Герб региона в байтах (BLOB)';
comment on column HUNTTECH_REGION.EMBLEM_URL is 'Исходный URL герба региона';
comment on column HUNTTECH_CITY.EMBLEM_IMAGE is 'Герб города в байтах (BLOB)';
comment on column HUNTTECH_CITY.EMBLEM_URL is 'Исходный URL герба города';
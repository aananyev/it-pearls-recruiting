-- update HUNTTECH_SOME_FILES set FILE_OWNER_ID = <default_value> where FILE_OWNER_ID is null ;
alter table HUNTTECH_SOME_FILES alter column FILE_OWNER_ID set not null ;
-- update HUNTTECH_SOME_FILES set FILE_TYPE_ID = <default_value> where FILE_TYPE_ID is null ;
alter table HUNTTECH_SOME_FILES alter column FILE_TYPE_ID set not null ;
alter table HUNTTECH_SOME_FILES alter column FILE_COMMENT drop not null ;

update HUNTTECH_SOME_FILES set FILE_DESCRIPTION = '' where FILE_DESCRIPTION is null ;
alter table HUNTTECH_SOME_FILES alter column FILE_DESCRIPTION set not null ;

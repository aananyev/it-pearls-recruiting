-- update ITPEARLS_SOME_FILES set FILE_DESCRIPTOR_ID = <default_value> where FILE_DESCRIPTOR_ID is null ;
alter table ITPEARLS_SOME_FILES alter column FILE_DESCRIPTOR_ID set not null ;
update ITPEARLS_SOME_FILES set FILE_DESCRIPTION = '' where FILE_DESCRIPTION is null ;
alter table ITPEARLS_SOME_FILES alter column FILE_DESCRIPTION set not null ;
update ITPEARLS_SOME_FILES set FILE_COMMENT = '' where FILE_COMMENT is null ;
alter table ITPEARLS_SOME_FILES alter column FILE_COMMENT set not null ;

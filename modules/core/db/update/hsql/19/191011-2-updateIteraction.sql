alter table ITPEARLS_ITERACTION alter column NUMBER_ rename to NUMBER___U43469 ^
alter table ITPEARLS_ITERACTION alter column NUMBER___U43469 set null ;
-- alter table ITPEARLS_ITERACTION add column NUMBER_ varchar(5) ^
-- update ITPEARLS_ITERACTION set NUMBER_ = <default_value> ;
-- alter table ITPEARLS_ITERACTION alter column NUMBER_ set not null ;
alter table ITPEARLS_ITERACTION add column NUMBER_ varchar(5) ;

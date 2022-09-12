alter table ITPEARLS_EMAILER rename column to_email to to_email__u36487 ;
alter table ITPEARLS_EMAILER alter column to_email__u36487 drop not null ;
-- alter table ITPEARLS_EMAILER add column TO_EMAIL_ID uuid ^
-- update ITPEARLS_EMAILER set TO_EMAIL_ID = <default_value> ;
-- alter table ITPEARLS_EMAILER alter column TO_EMAIL_ID set not null ;
alter table ITPEARLS_EMAILER add column TO_EMAIL_ID uuid not null ;
alter table ITPEARLS_EMAILER add column BODY_HTML boolean ;

alter table ITPEARLS_INTERNAL_EMAIL_TEMPLATE add constraint FK_ITPEARLS_INTERNAL_EMAIL_TEMPLATE_ON_TEMPLATE_AUTHOR foreign key (TEMPLATE_AUTHOR_ID) references SEC_USER(ID);
create index IDX_ITPEARLS_INTERNAL_EMAIL_TEMPLATE_ON_TEMPLATE_AUTHOR on ITPEARLS_INTERNAL_EMAIL_TEMPLATE (TEMPLATE_AUTHOR_ID);

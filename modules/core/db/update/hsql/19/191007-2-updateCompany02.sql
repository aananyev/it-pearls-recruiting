alter table ITPEARLS_COMPANY add constraint FK_ITPEARLS_COMPANY_ON_COUNTRY_OF_COMPANY foreign key (COUNTRY_OF_COMPANY_ID) references ITPEARLS_COUNTRY(ID);
create index IDX_ITPEARLS_COMPANY_ON_COUNTRY_OF_COMPANY on ITPEARLS_COMPANY (COUNTRY_OF_COMPANY_ID);

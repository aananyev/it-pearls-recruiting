alter table ITPEARLS_OUTSTAFFING_RATES add constraint FK_ITPEARLS_OUTSTAFFING_RATES_ON_CURRENCY foreign key (CURRENCY_ID) references ITPEARLS_CURRENCY(ID);
create index IDX_ITPEARLS_OUTSTAFFING_RATES_ON_CURRENCY on ITPEARLS_OUTSTAFFING_RATES (CURRENCY_ID);
alter table ITPEARLS_OPEN_POSITION add constraint FK_ITPEARLS_OPEN_POSITION_ON_CITY_POSITION foreign key (CITY_POSITION_ID) references ITPEARLS_CITY(ID);

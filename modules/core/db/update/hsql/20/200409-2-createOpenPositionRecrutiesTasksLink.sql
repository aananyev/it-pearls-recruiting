alter table ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK add constraint FK_OPEPOSRECTAS_ON_OPEN_POSITION foreign key (OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID);
alter table ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK add constraint FK_OPEPOSRECTAS_ON_RECRUTIES_TASKS foreign key (RECRUTIES_TASKS_ID) references ITPEARLS_RECRUTIES_TASKS(ID);
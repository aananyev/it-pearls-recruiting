alter table ITPEARLS_INTERVIEW_OPEN_POSITION_LINK add constraint FK_INTOPEPOS_ON_INTERVIEW foreign key (INTERVIEW_ID) references ITPEARLS_INTERVIEW(ID);
alter table ITPEARLS_INTERVIEW_OPEN_POSITION_LINK add constraint FK_INTOPEPOS_ON_OPEN_POSITION foreign key (OPEN_POSITION_ID) references ITPEARLS_OPEN_POSITION(ID);
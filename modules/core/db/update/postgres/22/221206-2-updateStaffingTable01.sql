alter table ITPEARLS_STAFFING_TABLE add constraint FK_ITPEARLS_STAFFING_TABLE_ON_GRADE foreign key (GRADE_ID) references ITPEARLS_GRADE(ID);
create index IDX_ITPEARLS_STAFFING_TABLE_ON_GRADE on ITPEARLS_STAFFING_TABLE (GRADE_ID);

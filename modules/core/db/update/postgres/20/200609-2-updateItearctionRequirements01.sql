alter table ITPEARLS_ITEARCTION_REQUIREMENTS add constraint FK_ITPEARLS_ITEARCTION_REQUIREMENTS_ON_ITERACTION foreign key (ITERACTION_ID) references ITPEARLS_ITERACTION(ID);

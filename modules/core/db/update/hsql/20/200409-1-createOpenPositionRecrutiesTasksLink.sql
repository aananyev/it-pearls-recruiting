create table ITPEARLS_OPEN_POSITION_RECRUTIES_TASKS_LINK (
    OPEN_POSITION_ID varchar(36) not null,
    RECRUTIES_TASKS_ID varchar(36) not null,
    primary key (OPEN_POSITION_ID, RECRUTIES_TASKS_ID)
);

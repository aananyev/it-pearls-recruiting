-- IteractionList candidate timeline: speeds last-interaction lookups and candidate interaction dialogs.
create index IDX_HUNTTECH_ITERACTION_LIST_CANDIDATE_NUMBER on HUNTTECH_ITERACTION_LIST (CANDIDATE_ID, NUMBER_ITERACTION, ID);

-- IteractionList candidate/vacancy chain: speeds chainInteraction, duplicate checks and project continuation checks.
create index IDX_HUNTTECH_ITERACTION_LIST_CANDIDATE_VACANCY_DATE on HUNTTECH_ITERACTION_LIST (CANDIDATE_ID, VACANCY_ID, DATE_ITERACTION, ID);

-- IteractionList recruiter statistics: speeds getMostPolularIteraction by recruiter/date/type.
create index IDX_HUNTTECH_ITERACTION_LIST_RECRUTIER_DATE_TYPE on HUNTTECH_ITERACTION_LIST (RECRUTIER_ID, DATE_ITERACTION, ITERACTION_TYPE_ID);

-- IteractionList type/date browsing: speeds manager/outstaffing filters plus recent ordering.
create index IDX_HUNTTECH_ITERACTION_LIST_TYPE_DATE_NUMBER on HUNTTECH_ITERACTION_LIST (ITERACTION_TYPE_ID, DATE_ITERACTION, NUMBER_ITERACTION);

-- IteractionList main browse ordering: keeps recent transactional pages off the legacy full-table number index.
create index IDX_HUNTTECH_ITERACTION_LIST_ACTIVE_NUMBER on HUNTTECH_ITERACTION_LIST (NUMBER_ITERACTION, ID);

-- Iteraction tree picker: speeds child/root type loading ordered by number.
create index IDX_HUNTTECH_ITERACTION_TREE_NUMBER on HUNTTECH_ITERACTION (ITERACTION_TREE_ID, NUMBER_, ID);

-- Iteraction outstaffing filter: speeds the cached lookup of outstaffing interaction type IDs.
create index IDX_HUNTTECH_ITERACTION_OUTSTAFFING on HUNTTECH_ITERACTION (OUTSTAFFING_SIGN, ID);

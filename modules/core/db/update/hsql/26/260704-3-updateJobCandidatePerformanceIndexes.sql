-- JobCandidateBrowse main page: active candidates ordered by surname/name with stable pagination.
create index IDX_HUNTTECH_JOB_CANDIDATE_ACTIVE_NAME on HUNTTECH_JOB_CANDIDATE (SECOND_NAME, FIRST_NAME, ID);

-- JobCandidateBrowse trainee filter: createdBy contains user login and then uses the same name ordering.
create index IDX_HUNTTECH_JOB_CANDIDATE_ACTIVE_CREATED_NAME on HUNTTECH_JOB_CANDIDATE (CREATED_BY, SECOND_NAME, FIRST_NAME, ID);

-- JobCandidateBrowse "with CV" filter and resume icon batch cache.
create index IDX_HUNTTECH_CANDIDATE_CV_ACTIVE_CANDIDATE_DATE on HUNTTECH_CANDIDATE_CV (CANDIDATE_ID, DATE_POST, ID);

-- JobCandidateBrowse last-interaction cache: one latest IteractionList row per visible candidate.
create index IDX_HUNTTECH_ITERACTION_LIST_ACTIVE_CANDIDATE_NUMBER on HUNTTECH_ITERACTION_LIST (CANDIDATE_ID, NUMBER_ITERACTION, ID);

-- JobCandidateBrowse rating filter combined with candidate lookup.
create index IDX_HUNTTECH_ITERACTION_LIST_ACTIVE_CANDIDATE_RATING on HUNTTECH_ITERACTION_LIST (CANDIDATE_ID, RATING);

-- JobCandidateBrowse "with my participation" filter.
create index IDX_HUNTTECH_ITERACTION_LIST_ACTIVE_RECRUTIER_CANDIDATE on HUNTTECH_ITERACTION_LIST (RECRUTIER_ID, CANDIDATE_ID);

-- JobCandidateEdit comments tab: comments are loaded lazily and ordered by interaction date.
create index IDX_HUNTTECH_ITERACTION_LIST_ACTIVE_COMMENTS on HUNTTECH_ITERACTION_LIST (CANDIDATE_ID, DATE_ITERACTION, ID);

-- JobCandidateBrowse sign filter: find candidates marked by a selected sign icon.
create index IDX_HUNTTECH_JOB_CANDIDATE_SIGN_ICON_ACTIVE_SIGN_CANDIDATE on HUNTTECH_JOB_CANDIDATE_SIGN_ICON (SIGN_ICON_ID, JOB_CANDIDATE_ID);

-- JobCandidateBrowse employee-status icon batch cache.
create index IDX_HUNTTECH_EMPLOYEE_ACTIVE_JOB_CANDIDATE_STATUS on HUNTTECH_EMPLOYEE (JOB_CANDIDATE_ID, WORK_STATUS_ID);

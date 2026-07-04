-- JobCandidateBrowse main page: active candidates ordered by surname/name with stable pagination.
create index if not exists IDX_HUNTTECH_JOB_CANDIDATE_ACTIVE_NAME
on HUNTTECH_JOB_CANDIDATE (SECOND_NAME, FIRST_NAME, ID)
where DELETE_TS is null;

-- JobCandidateBrowse trainee filter: createdBy contains user login and then uses the same name ordering.
create index if not exists IDX_HUNTTECH_JOB_CANDIDATE_ACTIVE_CREATED_NAME
on HUNTTECH_JOB_CANDIDATE (CREATED_BY, SECOND_NAME, FIRST_NAME, ID)
where DELETE_TS is null;

-- JobCandidateBrowse "with CV" filter and resume icon batch cache.
create index if not exists IDX_HUNTTECH_CANDIDATE_CV_ACTIVE_CANDIDATE_DATE
on HUNTTECH_CANDIDATE_CV (CANDIDATE_ID, DATE_POST desc, ID)
where DELETE_TS is null;

-- JobCandidateBrowse last-interaction cache: one latest IteractionList row per visible candidate.
create index if not exists IDX_HUNTTECH_ITERACTION_LIST_ACTIVE_CANDIDATE_NUMBER
on HUNTTECH_ITERACTION_LIST (CANDIDATE_ID, NUMBER_ITERACTION desc, ID)
where DELETE_TS is null;

-- JobCandidateBrowse rating filter combined with candidate lookup.
create index if not exists IDX_HUNTTECH_ITERACTION_LIST_ACTIVE_CANDIDATE_RATING
on HUNTTECH_ITERACTION_LIST (CANDIDATE_ID, RATING)
where DELETE_TS is null;

-- JobCandidateBrowse "with my participation" filter.
create index if not exists IDX_HUNTTECH_ITERACTION_LIST_ACTIVE_RECRUTIER_CANDIDATE
on HUNTTECH_ITERACTION_LIST (RECRUTIER_ID, CANDIDATE_ID)
where DELETE_TS is null;

-- JobCandidateEdit comments tab: comments are loaded lazily and ordered by interaction date.
create index if not exists IDX_HUNTTECH_ITERACTION_LIST_ACTIVE_COMMENTS
on HUNTTECH_ITERACTION_LIST (CANDIDATE_ID, DATE_ITERACTION desc, ID)
where DELETE_TS is null and COMMENT_ is not null and COMMENT_ <> '';

-- JobCandidateBrowse sign filter: find candidates marked by a selected sign icon.
create index if not exists IDX_HUNTTECH_JOB_CANDIDATE_SIGN_ICON_ACTIVE_SIGN_CANDIDATE
on HUNTTECH_JOB_CANDIDATE_SIGN_ICON (SIGN_ICON_ID, JOB_CANDIDATE_ID)
where DELETE_TS is null;

-- JobCandidateBrowse employee-status icon batch cache.
create index if not exists IDX_HUNTTECH_EMPLOYEE_ACTIVE_JOB_CANDIDATE_STATUS
on HUNTTECH_EMPLOYEE (JOB_CANDIDATE_ID, WORK_STATUS_ID)
where DELETE_TS is null;

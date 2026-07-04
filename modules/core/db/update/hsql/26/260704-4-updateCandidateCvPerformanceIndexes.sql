-- CandidateCV candidate history: speeds candidate resume tabs and "latest CV" lookups by candidate/date.
create index IDX_HUNTTECH_CANDIDATE_C_V_CANDIDATE_DATE on HUNTTECH_CANDIDATE_CV (CANDIDATE_ID, DATE_POST, ID);

-- CandidateCV vacancy drill-down: speeds screens that find resumes attached to an OpenPosition.
create index IDX_HUNTTECH_CANDIDATE_C_V_VACANCY_DATE on HUNTTECH_CANDIDATE_CV (TO_VACANCY_ID, DATE_POST, ID);

-- CandidateCV position filtering: supports resume-position searches and HR master matching screens.
create index IDX_HUNTTECH_CANDIDATE_C_V_POSITION_DATE on HUNTTECH_CANDIDATE_CV (RESUME_POSITION_ID, DATE_POST, ID);

-- CandidateCV owner/date listing: supports recruiter-owned CV lists without scanning old soft-deleted rows.
create index IDX_HUNTTECH_CANDIDATE_C_V_OWNER_DATE on HUNTTECH_CANDIDATE_CV (OWNER_ID, DATE_POST, ID);

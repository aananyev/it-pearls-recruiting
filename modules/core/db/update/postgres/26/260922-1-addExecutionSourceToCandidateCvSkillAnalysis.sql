-- Фиксация фактического источника результата анализа навыков.
ALTER TABLE HUNTTECH_CANDIDATE_CV_SKILL_ANALYSIS
    ADD COLUMN IF NOT EXISTS EXECUTION_SOURCE character varying(32);

-- Переназначаем только ранее OpenRouter-bound маршрут подбора вакансий.
-- Маркер rollback занимает 44 символа и помещается в UPDATED_BY varchar(50).
DO $$
DECLARE
    v_expected_source_model CONSTANT TEXT := 'nvidia/nemotron-3-ultra-550b-a55b:free';
    v_updated_count INTEGER;
    v_existing_route_count INTEGER;
    v_deepseek_config_count INTEGER;
    v_openrouter_route_count INTEGER;
    v_source_route_count INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_deepseek_config_count
      FROM HUNTTECH_ADMIN_AI_CONFIGURATION
     WHERE PROVIDER_CODE = 'deepseek'
       AND DELETE_TS IS NULL
       AND IS_ACTIVE = TRUE
       AND API_KEY_ENCRYPTED IS NOT NULL
       AND DEFAULT_MODEL_NAME IS NOT NULL;
    IF v_deepseek_config_count != 1 THEN
        RAISE EXCEPTION 'Expected exactly one active credentialed DeepSeek configuration';
    END IF;

    SELECT COUNT(*)
      INTO v_source_route_count
      FROM HUNTTECH_AI_FUNCTION_CONFIGURATION f
      JOIN HUNTTECH_ADMIN_AI_CONFIGURATION a ON a.ID = f.ADMIN_CONFIGURATION_ID
     WHERE f.CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'
       AND f.DELETE_TS IS NULL
       AND f.IS_ACTIVE = TRUE
       AND f.VERSION IS NOT NULL
       AND f.ADMIN_MODEL_NAME = v_expected_source_model
       AND a.PROVIDER_CODE = 'openrouter'
       AND a.DELETE_TS IS NULL;
    -- Zero eligible source rows is accepted only by the valid DeepSeek no-op check below.
    IF v_source_route_count > 1 THEN
        RAISE EXCEPTION 'Expected at most one OpenRouter/Nemotron candidate matching route; found %', v_source_route_count;
    END IF;

    WITH eligible_deepseek AS (
        SELECT ID, DEFAULT_MODEL_NAME
          FROM HUNTTECH_ADMIN_AI_CONFIGURATION
         WHERE PROVIDER_CODE = 'deepseek'
           AND DELETE_TS IS NULL
           AND IS_ACTIVE = TRUE
           AND API_KEY_ENCRYPTED IS NOT NULL
           AND DEFAULT_MODEL_NAME IS NOT NULL
    ),
    target_deepseek AS (
        SELECT ID, DEFAULT_MODEL_NAME
          FROM eligible_deepseek
         WHERE (SELECT COUNT(*) FROM eligible_deepseek) = 1
    ),
    eligible_candidate_match AS (
        SELECT f.ID, f.ADMIN_CONFIGURATION_ID AS PREVIOUS_ADMIN_CONFIGURATION_ID
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION f
          JOIN HUNTTECH_ADMIN_AI_CONFIGURATION source ON source.ID = f.ADMIN_CONFIGURATION_ID
         WHERE f.CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'
           AND f.DELETE_TS IS NULL
           AND f.IS_ACTIVE = TRUE
           AND f.VERSION IS NOT NULL
           AND f.ADMIN_MODEL_NAME = v_expected_source_model
           AND source.PROVIDER_CODE = 'openrouter'
           AND source.DELETE_TS IS NULL
    ),
    target_candidate_match AS (
        SELECT ID, PREVIOUS_ADMIN_CONFIGURATION_ID
          FROM eligible_candidate_match
         WHERE (SELECT COUNT(*) FROM eligible_candidate_match) = 1
    )
    UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION f
       SET ADMIN_CONFIGURATION_ID = d.ID,
           ADMIN_MODEL_NAME = d.DEFAULT_MODEL_NAME,
           UPDATE_TS = CURRENT_TIMESTAMP,
           UPDATED_BY = 'mig:CVM:' || c.PREVIOUS_ADMIN_CONFIGURATION_ID::text
      FROM target_candidate_match c
      JOIN target_deepseek d ON TRUE
     WHERE f.ID = c.ID
       AND f.ADMIN_CONFIGURATION_ID = c.PREVIOUS_ADMIN_CONFIGURATION_ID
       AND f.CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'
       AND f.DELETE_TS IS NULL
       AND f.IS_ACTIVE = TRUE
       AND f.VERSION IS NOT NULL
       AND f.ADMIN_MODEL_NAME = v_expected_source_model
       AND EXISTS (
           SELECT 1
             FROM HUNTTECH_ADMIN_AI_CONFIGURATION source
            WHERE source.ID = f.ADMIN_CONFIGURATION_ID
              AND source.PROVIDER_CODE = 'openrouter'
              AND source.DELETE_TS IS NULL
       )
       AND EXISTS (
           SELECT 1
             FROM HUNTTECH_ADMIN_AI_CONFIGURATION target
            WHERE target.ID = d.ID
              AND target.PROVIDER_CODE = 'deepseek'
              AND target.DELETE_TS IS NULL
              AND target.IS_ACTIVE = TRUE
              AND target.API_KEY_ENCRYPTED IS NOT NULL
              AND target.DEFAULT_MODEL_NAME = d.DEFAULT_MODEL_NAME
       );

    GET DIAGNOSTICS v_updated_count = ROW_COUNT;
    IF v_updated_count > 1 THEN
        RAISE EXCEPTION 'DeepSeek route migration matched more than one candidate matching function';
    ELSIF v_updated_count = 0 THEN
        -- A valid DeepSeek route here predates this update (or won a race); no marker means rollback must leave it unchanged.
        SELECT COUNT(*)
          INTO v_existing_route_count
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION f
          JOIN HUNTTECH_ADMIN_AI_CONFIGURATION a ON a.ID = f.ADMIN_CONFIGURATION_ID
         WHERE f.CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'
           AND f.DELETE_TS IS NULL
           AND f.IS_ACTIVE = TRUE
           AND f.VERSION IS NOT NULL
           AND f.ADMIN_MODEL_NAME = a.DEFAULT_MODEL_NAME
           AND a.PROVIDER_CODE = 'deepseek'
           AND a.DELETE_TS IS NULL
           AND a.IS_ACTIVE = TRUE
           AND a.API_KEY_ENCRYPTED IS NOT NULL;
        SELECT COUNT(*)
          INTO v_openrouter_route_count
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION f
          JOIN HUNTTECH_ADMIN_AI_CONFIGURATION a ON a.ID = f.ADMIN_CONFIGURATION_ID
         WHERE f.CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'
           AND f.DELETE_TS IS NULL
           AND f.IS_ACTIVE = TRUE
           AND f.VERSION IS NOT NULL
           AND a.PROVIDER_CODE = 'openrouter'
           AND a.DELETE_TS IS NULL;
        IF v_existing_route_count != 1 OR v_openrouter_route_count != 0 THEN
            RAISE EXCEPTION 'DeepSeek route migration found no unique eligible source route or valid existing DeepSeek route';
        END IF;
    END IF;
END $$;

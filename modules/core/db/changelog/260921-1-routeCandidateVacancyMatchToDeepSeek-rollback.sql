    DO $$
    DECLARE
        v_previous_admin_id UUID;
        v_previous_admin_count INTEGER;
        v_marker_count INTEGER;
        v_marker_format_count INTEGER;
        v_current_route_count INTEGER;
        v_restored_count INTEGER;
    BEGIN
        IF to_regclass('public.hunttech_admin_ai_configuration') IS NULL
           OR to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
            RETURN;
        END IF;

        SELECT COUNT(*)
          INTO v_marker_count
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'
           AND DELETE_TS IS NULL
           AND UPDATED_BY LIKE 'mig:CVM:%';
        IF v_marker_count = 0 THEN
            RAISE NOTICE 'No migration marker found: assuming the route predated this changeset; rollback leaves it unchanged. Inspect manually if the marker was overwritten.';
            RETURN;
        ELSIF v_marker_count != 1 THEN
            RAISE EXCEPTION 'Rollback stopped: more than one candidate matching route has the migration marker';
        END IF;

        IF v_marker_count = 1 THEN
            SELECT COUNT(*)
              INTO v_marker_format_count
              FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
             WHERE CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'
               AND DELETE_TS IS NULL
               AND UPDATED_BY ~ '^mig:CVM:[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$';
            IF v_marker_format_count != 1 THEN
                RAISE EXCEPTION 'Rollback stopped: migration marker does not contain a valid prior configuration UUID';
            END IF;

            SELECT SUBSTRING(UPDATED_BY FROM LENGTH('mig:CVM:') + 1)::uuid
              INTO v_previous_admin_id
              FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
             WHERE CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'
               AND DELETE_TS IS NULL
               AND UPDATED_BY LIKE 'mig:CVM:%';
        END IF;

        SELECT COUNT(*)
          INTO v_current_route_count
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION f
          JOIN HUNTTECH_ADMIN_AI_CONFIGURATION a ON a.ID = f.ADMIN_CONFIGURATION_ID
         WHERE f.CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'
           AND f.DELETE_TS IS NULL
           AND f.IS_ACTIVE = TRUE
           AND f.VERSION IS NOT NULL
           AND f.UPDATED_BY LIKE 'mig:CVM:%'
           AND a.PROVIDER_CODE = 'deepseek'
           AND a.DELETE_TS IS NULL
           AND f.ADMIN_MODEL_NAME = a.DEFAULT_MODEL_NAME;
        IF v_current_route_count != 1 THEN
            RAISE EXCEPTION 'Rollback stopped: candidate matching route or model changed after migration; inspect and restore manually';
        END IF;

        SELECT COUNT(*)
          INTO v_previous_admin_count
          FROM HUNTTECH_ADMIN_AI_CONFIGURATION
         WHERE ID = v_previous_admin_id
           AND PROVIDER_CODE = 'openrouter'
           AND DELETE_TS IS NULL;
        IF v_previous_admin_count != 1 THEN
            RAISE EXCEPTION 'Rollback stopped: the exact prior OpenRouter configuration is unavailable';
        END IF;

        -- The forward guard requires Nemotron here; restore that exact function override, independent of admin defaults.
        -- If the source-model contract changed, review and restore manually rather than changing this literal blindly.
        UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION f
           SET ADMIN_CONFIGURATION_ID = v_previous_admin_id,
               ADMIN_MODEL_NAME = 'nvidia/nemotron-3-ultra-550b-a55b:free',
               UPDATE_TS = CURRENT_TIMESTAMP,
               UPDATED_BY = 'rollback'
         WHERE f.CODE = 'CANDIDATE_VACANCY_MATCH_ANALYZE'
           AND f.DELETE_TS IS NULL
           AND f.IS_ACTIVE = TRUE
           AND f.VERSION IS NOT NULL
           AND f.UPDATED_BY = 'mig:CVM:' || v_previous_admin_id::text
           AND EXISTS (
               SELECT 1
                 FROM HUNTTECH_ADMIN_AI_CONFIGURATION a
                WHERE a.ID = f.ADMIN_CONFIGURATION_ID
                  AND a.PROVIDER_CODE = 'deepseek'
                  AND a.DELETE_TS IS NULL
                  AND f.ADMIN_MODEL_NAME = a.DEFAULT_MODEL_NAME
           )
           AND EXISTS (
               SELECT 1
                 FROM HUNTTECH_ADMIN_AI_CONFIGURATION previous
                WHERE previous.ID = v_previous_admin_id
                  AND previous.PROVIDER_CODE = 'openrouter'
                  AND previous.DELETE_TS IS NULL
           );
        GET DIAGNOSTICS v_restored_count = ROW_COUNT;
        IF v_restored_count != 1 THEN
            RAISE EXCEPTION 'Rollback stopped: exactly one unchanged candidate matching route was not restored';
        END IF;
    END $$;

-- HRM HuntTech: Разрешение переопределения модели пользователем для фонового определения навыков (BL-2026-020).
-- Перевод SKILLS_EXTRACT_BACKGROUND на USER_OVERRIDE_ALLOWED + ALLOW_MODEL_OVERRIDE = TRUE.

DO $$
DECLARE
    v_count integer;
BEGIN
    IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
        RAISE EXCEPTION 'AI Control Plane не мигрирован: отсутствует HUNTTECH_AI_FUNCTION_CONFIGURATION';
    END IF;

    UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
       SET EXECUTION_POLICY = 'USER_OVERRIDE_ALLOWED',
           ALLOW_MODEL_OVERRIDE = TRUE,
           DESCRIPTION = 'Фоновый анализ резюме кандидатов (Candidate Skills Enrichment): нейросеть возвращает JSON-массив названий навыков по уровню анализа ALL/MAIN/SECONDARY/TERTIARY и ОДИН навык опыта в годах. Допускается пользовательская переопределённая модель (USER_OVERRIDE_ALLOWED).',
           UPDATE_TS = CURRENT_TIMESTAMP,
           UPDATED_BY = 'migration'
     WHERE CODE = 'SKILLS_EXTRACT_BACKGROUND'
       AND DELETE_TS IS NULL;

    GET DIAGNOSTICS v_count = ROW_COUNT;
    IF v_count = 0 THEN
        RAISE NOTICE 'SKILLS_EXTRACT_BACKGROUND не найдена или уже удалена';
    END IF;
END
$$;

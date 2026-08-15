-- HRM HuntTech: production-safe seed AI-функции извлечения навыков из текста.
-- Скрипт INSERT-only и идемпотентный: существующая административная настройка
-- SKILLS_EXTRACT не изменяется и не перезаписывает prompt/model/policy.

DO $$
BEGIN
    IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
        RAISE EXCEPTION
            'AI Control Plane не мигрирован: отсутствует HUNTTECH_AI_FUNCTION_CONFIGURATION';
    END IF;

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    ) VALUES (
        'a1f3b2c4-5d6e-4f8a-9b0c-1d2e3f4a5b6c'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'SKILLS_EXTRACT',
        'Извлечение навыков из текста',
        'Анализ резюме кандидата или описания вакансии (сервис SkillAnalysisService): нейросеть возвращает JSON-массив названий навыков по уровню анализа ALL/MAIN/SECONDARY/TERTIARY; сервис сопоставляет их со справочником skilltree',
        'TEXT_GENERATION',
        'Ты — сервис анализа навыков HRM HuntTech. Из текста резюме кандидата или описания вакансии извлеки навыки и верни их списком. Уровень анализа задаётся в запросе: ALL — все навыки, упомянутые в тексте; MAIN — основные/обязательные навыки (в вакансии — обязательные требования, в резюме — ключевые навыки); SECONDARY — второстепенные/желательные навыки (в вакансии — «желательно», в резюме — дополнительные навыки); TERTIARY — третьестепенные навыки, если такие есть (редко упоминаемые, не ключевые). Названия навыков бери из текста, в единственном числе, на языке исходного текста (например: Java, SQL, Scrum, 1С). Не выдумывай навыков, которых нет в тексте. Верни строго JSON-массив строк с названиями навыков без пояснений, например: ["Java", "SQL", "Scrum"].',
        E'Уровень анализа: ${skillLevel}\n\nТекст для анализа:\n${sourceText}',
        0.3,
        500,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        1
    )
    ON CONFLICT (CODE) DO NOTHING;
END
$$;

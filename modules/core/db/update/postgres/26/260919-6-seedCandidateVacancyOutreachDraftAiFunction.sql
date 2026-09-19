-- HRM HuntTech: Добавление AI-функции CANDIDATE_VACANCY_OUTREACH_DRAFT (Этап 3)
-- Назначение: Генерация персонализированного черновика предложения кандидату

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
        IS_ACTIVE, INCLUDE_USER_CONTEXT, CONFIGURATION_VERSION
    )
    SELECT
        'c4d6e8f0-8a1b-4c2d-9e4f-6a8b0c2d4e6f'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'CANDIDATE_VACANCY_OUTREACH_DRAFT',
        'Подготовка персонализированного предложения вакансии',
        'AI-генерация персонализированного черновика сообщения кандидату по результатам AI-подбора на вакансию без выдумывания условий и без раскрытия AI score.',
        'TEXT_GENERATION',
        'Ты — профессиональный IT-рекрутер HuntTech.

Подготовь короткое персонализированное сообщение кандидату с предложением рассмотреть вакансию.

Используй только факты из предоставленных данных.

Не придумывай:
- условия;
- зарплату;
- технологии;
- название клиента;
- формат работы;
если они не переданы явно.

Не сообщай кандидату AI score.
Не сообщай, что кандидат был выбран искусственным интеллектом.
Укажи только реально релевантные причины, почему вакансия может быть интересна кандидату.

Текст должен быть профессиональным, коротким и естественным.
Верни только текст сообщения.',
        E'Вакансия: ${vacancyTitle}\nПроект: ${projectName}\nФормат работы: ${workFormat}\nКлючевые задачи и описание: ${vacancySummary}\nКлючевые требования: ${keyRequirements}\n\nКандидат: ${candidateName}\nТекущая роль/профиль: ${candidateRole}\nРелевантные навыки: ${relevantSkills}\nРелевантный опыт: ${relevantExperience}\nПричины предложения: ${reasonsToOffer}',
        0.3,
        800,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        TRUE,
        TRUE,
        FALSE,
        1
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'CANDIDATE_VACANCY_OUTREACH_DRAFT'
    );
END
$$;

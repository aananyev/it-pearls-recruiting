-- HRM HuntTech: seed AI-функции VACANCY_SMART_PARSE_JSON (SmartOpenPositionIngestService) для HSQL.
INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
    ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
    CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
    EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
    IS_ACTIVE, CONFIGURATION_VERSION
)
SELECT
    'c3f5d7b9-2e4a-5f6b-9c8d-0a1b2c3d4e5f',
    1,
    CURRENT_TIMESTAMP,
    'migration',
    'VACANCY_SMART_PARSE_JSON',
    'Умное распознавание вакансии (JSON)',
    'Извлечение структурированных данных вакансии и проекта из текста (название, должность, проект, компания, грейд, ЗП, опыт, стек, условия, города)',
    'DOCUMENT_ANALYSIS',
    'Ты — интеллектуальный модуль распознавания и создания вакансий HRM HuntTech. Твоя задача — внимательно изучить текст описания вакансии от заказчика и извлечь все структурированные данные о позиции и проекте. Верни строго один JSON-объект следующей структуры: {"vacancyName": "Название вакансии", "positionName": "Стандартизированная IT-должность", "grade": "Junior|Middle|Senior|Lead|Architect", "remoteWork": "REMOTE|OFFICE|HYBRID", "salaryMin": 250000, "salaryMax": 350000, "workExperience": "От 3 до 6 лет", "numberPosition": 1, "projectName": "Название проекта", "projectShortDescription": "Краткое описание проекта (1-2 предложения)", "projectFullDescription": "Развернутое описание сути проекта и стека", "companyName": "Компания-клиент/работодатель", "projectOwner": "Куратор/Лид проекта", "cities": ["Москва"], "skills": ["Java", "Spring Boot", "Kafka"], "description": "Общее описание", "requirements": "Требования", "conditions": "Условия", "testExercise": "Тестовое задание", "memoForCandidate": "Памятка для кандидата"}. Правила: 1. Источник правды — исключительно текст вакансии. 2. Для проекта (projectName) извлеки четкое наименование и сформируй краткое и развернутое описания. 3. Верни только чистый JSON без markdown-блоков, без кавычек ```json и без пояснений.',
    'Текст вакансии для анализа:
${sourceText}',
    0.2,
    4000,
    'USER_OVERRIDE_ALLOWED',
    'FALLBACK_TO_ADMIN',
    FALSE,
    TRUE,
    1
FROM (VALUES(1)) AS dual
WHERE NOT EXISTS (
    SELECT 1 FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
     WHERE CODE = 'VACANCY_SMART_PARSE_JSON'
);

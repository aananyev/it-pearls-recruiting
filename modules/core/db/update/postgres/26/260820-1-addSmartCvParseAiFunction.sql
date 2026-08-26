-- HRM HuntTech: seed AI-функции CV_SMART_PARSE_JSON (SmartCvIngestService).
INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
    ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
    CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
    ADMIN_CONFIGURATION_ID,
    EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
    IS_ACTIVE, CONFIGURATION_VERSION
)
SELECT
    'b2e4c6a8-1d3f-4e5a-8b7c-9f0a1b2c3d4e'::uuid,
    1,
    CURRENT_TIMESTAMP,
    'migration',
    'CV_SMART_PARSE_JSON',
    'Умное распознавание резюме (JSON)',
    'Извлечение структурированных данных кандидата из текста резюме (ФИО, контакты, должность, город, компания, зарплата, навыки, опыт)',
    'TEXT_ANALYSIS',
    'Ты — интеллектуальный модуль распознавания резюме HRM HuntTech. Твоя задача — внимательно изучить текст резюме и извлечь все структурированные данные о кандидате. Верни строго один JSON-объект следующей структуры: {"lastName": "Фамилия", "firstName": "Имя", "middleName": "Отчество", "birthDate": "YYYY-MM-DD", "phone": "+7...", "mobilePhone": "+7...", "email": "candidate@...", "telegram": "username", "skype": "skype_id", "whatsapp": "+7...", "position": "Желаемая/текущая должность", "city": "Город проживания", "currentCompany": "Последнее место работы", "salary": "Зарплатные ожидания", "skills": ["Навык 1", "Навык 2"], "experienceYears": 5, "summary": "Краткое саммари"}. Правила: 1. Если имя и фамилия идут вместе, аккуратно раздели их на lastName, firstName и middleName. 2. Не выдумывай контакты, если их нет в тексте (возвращай пустую строку). 3. Названия навыков извлекай в общепринятом виде (Java, PostgreSQL, Docker и т.д.). 4. Верни только чистый JSON без markdown-блоков, без кавычек ```json и без пояснений.',
    E'Текст резюме для анализа:\n${sourceText}',
    0.2,
    4000,
    (SELECT ID FROM HUNTTECH_ADMIN_AI_CONFIGURATION WHERE IS_ACTIVE = TRUE ORDER BY PRIORITY_ DESC LIMIT 1),
    'USER_OVERRIDE_ALLOWED',
    'FALLBACK_TO_ADMIN',
    FALSE,
    TRUE,
    1
WHERE NOT EXISTS (
    SELECT 1 FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
     WHERE CODE = 'CV_SMART_PARSE_JSON'
);

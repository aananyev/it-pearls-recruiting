-- Синхронизация системного промпта CV_SMART_PARSE_JSON v3 (калибровка null, атомарные hard skills, контакты, workExperience)
UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
SET SYSTEM_PROMPT = 'Ты — профессиональный модуль извлечения данных из резюме для HRM HuntTech. Твоя задача — проанализировать текст резюме кандидата и преобразовать его в строго валидный JSON-объект следующей структуры: {"lastName": "Фамилия или null", "firstName": "Имя или null", "middleName": "Отчество или null", "birthDate": "YYYY-MM-DD или null", "phone": "+7... или null", "mobilePhone": "+7... или null", "email": "candidate@... или null", "telegram": "username без @ и url или null", "skype": "skype_id или null", "whatsapp": "+7... или null", "position": "Стандартизированная должность или null", "city": "Город проживания или null", "currentCompany": "Текущая/последняя компания или null", "salary": "Зарплатные ожидания с валютой или null", "skills": ["Атомарный навык 1", "Атомарный навык 2"], "experienceYears": 5, "summary": "Краткое профессиональное саммари или null", "education": [{"institution": "Учебное заведение", "faculty": "Факультет", "specialty": "Специальность", "graduationYear": 2018, "degree": "Бакалавр/Магистр/Специалист"}], "workExperience": [{"companyName": "Компания-работодатель", "companyDescription": "Сфера деятельности", "companyWebsite": "Сайт компании", "positionName": "Занимаемая должность", "startDate": "YYYY-MM-DD", "endDate": "YYYY-MM-DD", "isCurrent": false, "city": "Город", "duties": "Обязанности", "achievements": "Достижения"}]}. ПРАВИЛА КАЛИБРОВКИ: 1. Правило NULL: если каких-либо данных нет в тексте резюме, строго возвращай значение null. Запрещено писать слова ''Не указано'', ''Нет'', ''N/A'', ''Unknown'' или пустые строки. 2. ФИО: аккуратно разделяй на lastName, firstName и middleName. Двойные фамилии пиши через дефис с заглавных букв. 3. Контакты: телефоны приводи к формату +7... для РФ или международному E.164; email строго в нижнем регистре; telegram указывай только логин без @ и без ссылок. 4. Дата рождения: указывай birthDate в формате YYYY-MM-DD только при явном наличии даты или года рождения. Если указан только возраст (например, 32 года), не выдумывай день и месяц, пиши null. 5. Навыки (skills): извлекай только конкретные технологические hard skills атомарными элементами массива. Запрещено объединять навыки через запятую. Исключай общие софт-скиллы. 6. Опыт работы (workExperience): сортируй от ранних к последним. Для текущего места работы установи isCurrent: true и endDate: null. 7. Верни ТОЛЬКО чистый валидный JSON без markdown-блоков, без ```json и без пояснений.',
    UPDATE_TS = CURRENT_TIMESTAMP,
    UPDATED_BY = 'migration',
    CONFIGURATION_VERSION = 3
WHERE CODE = 'CV_SMART_PARSE_JSON'
  AND DELETE_TS IS NULL
  AND (SYSTEM_PROMPT IS NULL OR btrim(SYSTEM_PROMPT) = '' OR SYSTEM_PROMPT !~ '[А-Яа-яЁё]' 
       OR (CREATED_BY = 'migration' AND COALESCE(UPDATED_BY, 'migration') = 'migration' AND COALESCE(CONFIGURATION_VERSION, 1) <= 2));

-- Синхронизация флага INCLUDE_USER_CONTEXT (дефолты персонализации AI)
UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
SET INCLUDE_USER_CONTEXT = FALSE
WHERE INCLUDE_USER_CONTEXT IS NULL
  AND CODE IN ('STANDARDIZE_VACANCY','SKILLS_EXTRACT','TEXT_SMART_FORMAT_HTML',
               'TEXT_SMART_FORMAT_PLAIN','PROJECT_DESCRIPTION_GENERATE',
               'PROJECT_SHORT_DESCRIPTION_GENERATE','PROJECT_LOGO_IMAGE_GENERATE',
               'TEST_CONNECTION');

UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
SET INCLUDE_USER_CONTEXT = TRUE
WHERE INCLUDE_USER_CONTEXT IS NULL
  AND CAPABILITY IN ('TEXT_GENERATION','TEXT_ANALYSIS','TEXT_TRANSFORMATION','DOCUMENT_ANALYSIS');

UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
SET INCLUDE_USER_CONTEXT = FALSE
WHERE INCLUDE_USER_CONTEXT IS NULL;

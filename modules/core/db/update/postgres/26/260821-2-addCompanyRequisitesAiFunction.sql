-- Seed AI-функции парсинга реквизитов компании (COMPANY_REQUISITES_PARSE_JSON)
INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
    ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
    CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
    ADMIN_CONFIGURATION_ID,
    EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
    IS_ACTIVE, CONFIGURATION_VERSION
)
SELECT
    'd5e7f9a1-3b4c-5d6e-8f9a-1b2c3d4e5f6a'::uuid,
    1,
    CURRENT_TIMESTAMP,
    'migration',
    'COMPANY_REQUISITES_PARSE_JSON',
    'Умное распознавание реквизитов компании (JSON)',
    'Извлечение структурированных реквизитов компании (наименование, форма, ИНН, КПП, ОГРН, адреса, банк, счета, контакты, генеральный директор) из текста карточки компании',
    'TEXT_ANALYSIS',
    'Ты — интеллектуальный модуль распознавания карточек организаций и реквизитов компаний HRM HuntTech. Твоя задача — внимательно изучить предоставленный текст и извлечь все официальные реквизиты юридического лица или ИП. Верни строго один валидный JSON-объект следующей структуры: {"companyName": "Полное наименование организации", "companyShortName": "Краткое наименование", "ownership": "ООО/АО/ПАО/ИП", "inn": "ИНН", "kpp": "КПП", "ogrn": "ОГРН/ОГРНИП", "okpo": "ОКПО", "oktmo": "ОКТМО", "okved": "Код и наименование основного ОКВЭД", "legalAddress": "Полный юридический адрес", "actualAddress": "Фактический адрес", "postalAddress": "Почтовый адрес", "bik": "БИК банка", "bankName": "Наименование банка", "settlementAccount": "Расчетный счет (20 цифр)", "correspondentAccount": "Корреспондентский счет (20 цифр)", "phone": "Официальный телефон", "email": "Официальный email", "website": "Сайт компании", "directorLastName": "Фамилия руководителя", "directorFirstName": "Имя руководителя", "directorMiddleName": "Отчество руководителя", "directorPosition": "Генеральный директор / Директор / Президент", "directorPhone": "Телефон руководителя", "directorEmail": "Email руководителя"}. Правила: 1. Если данные отсутствуют в тексте, возвращай пустую строку "". 2. Аккуратно раздели ФИО руководителя на directorLastName, directorFirstName и directorMiddleName. 3. Номера расчетных и корр. счетов извлекай только из 20 цифр без пробелов. 4. Верни только чистый JSON без markdown блоков, без кавычек ```json и без пояснений.',
    E'Текст реквизитов/карточки компании:\n${sourceText}',
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
     WHERE CODE = 'COMPANY_REQUISITES_PARSE_JSON'
);

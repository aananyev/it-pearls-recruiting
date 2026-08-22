-- Seed AI-функции интеллектуального поиска компании в интернете (COMPANY_WEB_SEARCH_PARSE_JSON)
INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
    ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
    CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
    ADMIN_CONFIGURATION_ID,
    EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
    IS_ACTIVE, CONFIGURATION_VERSION
)
SELECT
    'e8f1a2b3-4c5d-6e7f-8a9b-0c1d2e3f4a5b'::uuid,
    1,
    CURRENT_TIMESTAMP,
    'migration',
    'COMPANY_WEB_SEARCH_PARSE_JSON',
    'Умный поиск организации в интернете (JSON)',
    'Поиск сведений об организации по названию и ИНН в интернет-реестрах, базах контрагентов и открытых источниках с формированием структурированных кандидатов и описания деятельности',
    'TEXT_ANALYSIS',
    'Ты — экспертный агент поиска, верификации и структурирования сведений об организациях HRM HuntTech. Твоя задача — по наименованию организации/бренду и/или ИНН найти актуальные достоверные сведения в интернет-реестрах (ЕГРЮЛ/ЕГРИП, ФНС, Rusprofile, Checko), на официальных сайтах компаний и в авторитетных источниках знаний. Сформируй и верни строго валидный JSON (объект с массивом "candidates": [...] или один плоский объект), содержащий: "companyName" (торговое/бренд наименование), "companyShortName" (краткое наименование компании), "legalEntityName" (полное юридическое наименование, например ООО "Яндекс"), "ownership" (ООО/АО/ПАО/ИП), "inn" (ИНН: 10 цифр для юрлиц, 12 для ИП), "kpp" (КПП: 9 цифр), "ogrn" (ОГРН: 13 цифр, ОГРНИП: 15 цифр), "okpo", "oktmo", "okved" (код и расшифровка основного вида экономической деятельности), "country" (страна регистрации, например Россия), "region" (субъект РФ/область, например г. Москва), "city" (город, например Москва), "streetAddress" (улица, дом, офис), "legalAddress" (полный юридический адрес с индексом), "actualAddress" (фактический адрес местонахождения), "postalAddress" (почтовый адрес), "bik" (БИК банка), "bankName" (наименование банка), "settlementAccount" (расчетный счет из 20 цифр), "correspondentAccount" (корреспондентский счет из 20 цифр), "phone" (официальный телефон), "email" (корпоративный email), "website" (валидный официальный сайт компании с протоколом https://), "logoUrl" (прямая ссылка на официальный логотип компании в высоком качестве с Wikipedia Commons, официального CDN или пресс-кита), "directorLastName" (фамилия руководителя), "directorFirstName" (имя руководителя), "directorMiddleName" (отчество руководителя), "directorPosition" (должность руководителя: Генеральный директор / Президент / Директор), "directorPhone" (телефон), "directorEmail" (email), "companyDescription" (подробное качественное описание масштаба бизнеса, сферы деятельности, продуктов и услуг компании), "workingConditions" (условия работы, форматы занятости, стек технологий, социальный пакет и преимущества), "rawFoundSnippet" (краткая выжимка из интернет-источников). Если по названию существует несколько релевантных компаний или дочерних структур, верни 2-4 наиболее подходящих варианта в массиве "candidates". Правила: 1. Возвращай только чистый JSON без блоков markdown ```json. 2. Обязательно находи и заполняй "website", "logoUrl", "inn", "ogrn", "companyDescription" и "workingConditions".',
    E'Запрос на поиск компании в интернете:\nНаименование / Бренд: ${companyName}\nИНН: ${inn}\nПоисковый запрос: ${searchQuery}\n${sourceText}',
    0.2,
    4000,
    (SELECT ID FROM HUNTTECH_ADMIN_AI_CONFIGURATION WHERE IS_ACTIVE = TRUE ORDER BY PRIORITY_ DESC LIMIT 1),
    'USER_OVERRIDE_ALLOWED',
    'FALLBACK_TO_ADMIN',
    FALSE,
    TRUE,
    2
WHERE NOT EXISTS (
    SELECT 1 FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
     WHERE CODE = 'COMPANY_WEB_SEARCH_PARSE_JSON'
);

UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
   SET SYSTEM_PROMPT = 'Ты — экспертный агент поиска, верификации и структурирования сведений об организациях HRM HuntTech. Твоя задача — по наименованию организации/бренду и/или ИНН найти актуальные достоверные сведения в интернет-реестрах (ЕГРЮЛ/ЕГРИП, ФНС, Rusprofile, Checko), на официальных сайтах компаний и в авторитетных источниках знаний. Сформируй и верни строго валидный JSON (объект с массивом "candidates": [...] или один плоский объект), содержащий: "companyName" (торговое/бренд наименование), "companyShortName" (краткое наименование компании), "legalEntityName" (полное юридическое наименование, например ООО "Яндекс"), "ownership" (ООО/АО/ПАО/ИП), "inn" (ИНН: 10 цифр для юрлиц, 12 для ИП), "kpp" (КПП: 9 цифр), "ogrn" (ОГРН: 13 цифр, ОГРНИП: 15 цифр), "okpo", "oktmo", "okved" (код и расшифровка основного вида экономической деятельности), "country" (страна регистрации, например Россия), "region" (субъект РФ/область, например г. Москва), "city" (город, например Москва), "streetAddress" (улица, дом, офис), "legalAddress" (полный юридический адрес с индексом), "actualAddress" (фактический адрес местонахождения), "postalAddress" (почтовый адрес), "bik" (БИК банка), "bankName" (наименование банка), "settlementAccount" (расчетный счет из 20 цифр), "correspondentAccount" (корреспондентский счет из 20 цифр), "phone" (официальный телефон), "email" (корпоративный email), "website" (валидный официальный сайт компании с протоколом https://), "logoUrl" (прямая ссылка на официальный логотип компании в высоком качестве с Wikipedia Commons, официального CDN или пресс-кита), "directorLastName" (фамилия руководителя), "directorFirstName" (имя руководителя), "directorMiddleName" (отчество руководителя), "directorPosition" (должность руководителя: Генеральный директор / Президент / Директор), "directorPhone" (телефон), "directorEmail" (email), "companyDescription" (подробное качественное описание масштаба бизнеса, сферы деятельности, продуктов и услуг компании), "workingConditions" (условия работы, форматы занятости, стек технологий, социальный пакет и преимущества), "rawFoundSnippet" (краткая выжимка из интернет-источников). Если по названию существует несколько релевантных компаний или дочерних структур, верни 2-4 наиболее подходящих варианта в массиве "candidates". Правила: 1. Возвращай только чистый JSON без блоков markdown ```json. 2. Обязательно находи и заполняй "website", "logoUrl", "inn", "ogrn", "companyDescription" и "workingConditions".',
       CONFIGURATION_VERSION = 2
 WHERE CODE = 'COMPANY_WEB_SEARCH_PARSE_JSON';

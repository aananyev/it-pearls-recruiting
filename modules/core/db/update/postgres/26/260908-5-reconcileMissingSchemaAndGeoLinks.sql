-- 260908-5-reconcileMissingSchemaAndGeoLinks.sql
-- 1. Перепривязка городов и компаний с удаленных дублей регионов на канонические регионы (безопасно, без зануления)
-- 2. DDL для HUNTTECH_JOB_HISTORY (START_DATE, END_DATE, DUTIES, RAW_POSITION_NAME, RAW_COMPANY_NAME)
-- 3. DDL для HUNTTECH_AI_CALL_LOG (CONTEXT_INCLUDED, CONTEXT_CODE_POINTS)
-- 4. Обновление и посев AI-функций анализа компаний (COMPANY_REQUISITES_PARSE_JSON v2, COMPANY_WEB_SEARCH_PARSE_JSON)

-- 1. Перепривязка городов
UPDATE hunttech_city
SET city_region_id = (SELECT id FROM hunttech_region WHERE iso_code = 'RU-SEV' AND delete_ts IS NULL LIMIT 1)
WHERE city_ru_name = 'Севастополь'
  AND (SELECT id FROM hunttech_region WHERE iso_code = 'RU-SEV' AND delete_ts IS NULL LIMIT 1) IS NOT NULL;

UPDATE hunttech_city
SET city_region_id = (SELECT id FROM hunttech_region WHERE iso_code = 'RU-CR' AND delete_ts IS NULL LIMIT 1)
WHERE city_region_id = (SELECT id FROM hunttech_region WHERE region_ru_name = 'Крым' AND delete_ts IS NOT NULL LIMIT 1)
  AND city_ru_name != 'Севастополь'
  AND (SELECT id FROM hunttech_region WHERE iso_code = 'RU-CR' AND delete_ts IS NULL LIMIT 1) IS NOT NULL;

UPDATE hunttech_city c
SET city_region_id = canon.id
FROM hunttech_region old_reg
JOIN LATERAL (
    SELECT id FROM hunttech_region
    WHERE delete_ts IS NULL
      AND (
        (old_reg.region_ru_name = 'Архангельская облась' AND iso_code = 'RU-ARK') OR
        (old_reg.region_ru_name = 'Башкортостан' AND iso_code = 'RU-BA') OR
        (old_reg.region_ru_name = 'Белгородская обоасть' AND iso_code = 'RU-BEL') OR
        (old_reg.region_ru_name = 'Оренбуржская область' AND iso_code = 'RU-ORE') OR
        (old_reg.region_ru_name = 'Саратовская обл' AND iso_code = 'RU-SAR') OR
        (old_reg.region_ru_name = 'Северная Осетия' AND iso_code = 'RU-SE') OR
        (old_reg.region_ru_name = 'Удмуртия' AND iso_code = 'RU-UD') OR
        (old_reg.region_ru_name = 'Хакасия' AND iso_code = 'RU-KK') OR
        (old_reg.region_ru_name = 'Хантымансийский автономный округ' AND iso_code = 'RU-KHM') OR
        (old_reg.region_ru_name = 'Чечернская республика' AND iso_code = 'RU-CE') OR
        (old_reg.region_ru_name = 'Чувашская республика' AND iso_code = 'RU-CU')
      )
    LIMIT 1
) canon ON TRUE
WHERE c.city_region_id = old_reg.id
  AND old_reg.delete_ts IS NOT NULL
  AND canon.id IS NOT NULL;

-- 2. Перепривязка компаний
UPDATE hunttech_company
SET region_of_company_id = (SELECT id FROM hunttech_region WHERE iso_code = 'RU-CR' AND delete_ts IS NULL LIMIT 1)
WHERE region_of_company_id = (SELECT id FROM hunttech_region WHERE region_ru_name = 'Крым' AND delete_ts IS NOT NULL LIMIT 1)
  AND (SELECT id FROM hunttech_region WHERE iso_code = 'RU-CR' AND delete_ts IS NULL LIMIT 1) IS NOT NULL;

UPDATE hunttech_company comp
SET region_of_company_id = canon.id
FROM hunttech_region old_reg
JOIN LATERAL (
    SELECT id FROM hunttech_region
    WHERE delete_ts IS NULL
      AND (
        (old_reg.region_ru_name = 'Архангельская облась' AND iso_code = 'RU-ARK') OR
        (old_reg.region_ru_name = 'Башкортостан' AND iso_code = 'RU-BA') OR
        (old_reg.region_ru_name = 'Белгородская обоасть' AND iso_code = 'RU-BEL') OR
        (old_reg.region_ru_name = 'Оренбуржская область' AND iso_code = 'RU-ORE') OR
        (old_reg.region_ru_name = 'Саратовская обл' AND iso_code = 'RU-SAR') OR
        (old_reg.region_ru_name = 'Северная Осетия' AND iso_code = 'RU-SE') OR
        (old_reg.region_ru_name = 'Удмуртия' AND iso_code = 'RU-UD') OR
        (old_reg.region_ru_name = 'Хакасия' AND iso_code = 'RU-KK') OR
        (old_reg.region_ru_name = 'Хантымансийский автономный округ' AND iso_code = 'RU-KHM') OR
        (old_reg.region_ru_name = 'Чечернская республика' AND iso_code = 'RU-CE') OR
        (old_reg.region_ru_name = 'Чувашская республика' AND iso_code = 'RU-CU')
      )
    LIMIT 1
) canon ON TRUE
WHERE comp.region_of_company_id = old_reg.id
  AND old_reg.delete_ts IS NOT NULL
  AND canon.id IS NOT NULL;

-- 3. HUNTTECH_JOB_HISTORY
ALTER TABLE HUNTTECH_JOB_HISTORY ALTER COLUMN CURRENT_POSITION_ID DROP NOT NULL;
ALTER TABLE HUNTTECH_JOB_HISTORY ADD COLUMN IF NOT EXISTS START_DATE date;
ALTER TABLE HUNTTECH_JOB_HISTORY ADD COLUMN IF NOT EXISTS END_DATE date;
ALTER TABLE HUNTTECH_JOB_HISTORY ADD COLUMN IF NOT EXISTS DUTIES text;
ALTER TABLE HUNTTECH_JOB_HISTORY ADD COLUMN IF NOT EXISTS RAW_POSITION_NAME varchar(255);
ALTER TABLE HUNTTECH_JOB_HISTORY ADD COLUMN IF NOT EXISTS RAW_COMPANY_NAME varchar(255);

-- 4. HUNTTECH_AI_CALL_LOG
ALTER TABLE HUNTTECH_AI_CALL_LOG ADD COLUMN IF NOT EXISTS CONTEXT_INCLUDED boolean;
ALTER TABLE HUNTTECH_AI_CALL_LOG ADD COLUMN IF NOT EXISTS CONTEXT_CODE_POINTS integer;

-- 5. Обновление промпта и версии COMPANY_REQUISITES_PARSE_JSON v2 (с разбивкой country, region, city)
UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
SET PROMPT_TEMPLATE = E'Текст реквизитов/карточки компании:\n${sourceText}',
    SYSTEM_PROMPT = 'Ты — интеллектуальный модуль распознавания карточек организаций и реквизитов компаний HRM HuntTech. Твоя задача — внимательно изучить предоставленный текст и извлечь все официальные реквизиты юридического лица или ИП с четким разбиением адреса на составляющие. Верни строго один валидный JSON-объект следующей структуры: {"companyName": "Полное наименование организации", "companyShortName": "Краткое наименование", "ownership": "ООО/АО/ПАО/ИП", "inn": "ИНН", "kpp": "КПП", "ogrn": "ОГРН/ОГРНИП", "okpo": "ОКПО", "oktmo": "ОКТМО", "okved": "Код и наименование основного ОКВЭД", "country": "Страна (например, Россия)", "region": "Регион/область/край (например, г. Москва, Московская область)", "city": "Город/населенный пункт (например, Москва)", "streetAddress": "Улица, дом, строение, офис", "legalAddress": "Полный юридический адрес", "actualAddress": "Фактический адрес", "postalAddress": "Почтовый адрес", "bik": "БИК банка", "bankName": "Наименование банка", "settlementAccount": "Расчетный счет (20 цифр)", "correspondentAccount": "Корреспондентский счет (20 цифр)", "phone": "Официальный телефон", "email": "Официальный email", "website": "Сайт компании", "directorLastName": "Фамилия руководителя", "directorFirstName": "Имя руководителя", "directorMiddleName": "Отчество руководителя", "directorPosition": "Генеральный директор / Директор / Президент", "directorPhone": "Телефон руководителя", "directorEmail": "Email руководителя"}. Правила: 1. Если данные отсутствуют в тексте, возвращай пустую строку "". 2. Аккуратно раздели адрес на country, region, city и streetAddress. 3. Аккуратно раздели ФИО руководителя на directorLastName, directorFirstName и directorMiddleName. 4. Номера расчетных и корр. счетов извлекай только из 20 цифр без пробелов. 5. Верни только чистый JSON без markdown блоков, без кавычек ```json и без пояснений.',
    CONFIGURATION_VERSION = 2,
    UPDATE_TS = CURRENT_TIMESTAMP
WHERE CODE = 'COMPANY_REQUISITES_PARSE_JSON' AND DELETE_TS IS NULL;

-- Посев COMPANY_REQUISITES_PARSE_JSON (если еще не была создана)
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
    'Ты — интеллектуальный модуль распознавания карточек организаций и реквизитов компаний HRM HuntTech. Твоя задача — внимательно изучить предоставленный текст и извлечь все официальные реквизиты юридического лица или ИП с четким разбиением адреса на составляющие. Верни строго один валидный JSON-объект следующей структуры: {"companyName": "Полное наименование организации", "companyShortName": "Краткое наименование", "ownership": "ООО/АО/ПАО/ИП", "inn": "ИНН", "kpp": "КПП", "ogrn": "ОГРН/ОГРНИП", "okpo": "ОКПО", "oktmo": "ОКТМО", "okved": "Код и наименование основного ОКВЭД", "country": "Страна (например, Россия)", "region": "Регион/область/край (например, г. Москва, Московская область)", "city": "Город/населенный пункт (например, Москва)", "streetAddress": "Улица, дом, строение, офис", "legalAddress": "Полный юридический адрес", "actualAddress": "Фактический адрес", "postalAddress": "Почтовый адрес", "bik": "БИК банка", "bankName": "Наименование банка", "settlementAccount": "Расчетный счет (20 цифр)", "correspondentAccount": "Корреспондентский счет (20 цифр)", "phone": "Официальный телефон", "email": "Официальный email", "website": "Сайт компании", "directorLastName": "Фамилия руководителя", "directorFirstName": "Имя руководителя", "directorMiddleName": "Отчество руководителя", "directorPosition": "Генеральный директор / Директор / Президент", "directorPhone": "Телефон руководителя", "directorEmail": "Email руководителя"}. Правила: 1. Если данные отсутствуют в тексте, возвращай пустую строку "". 2. Аккуратно раздели адрес на country, region, city и streetAddress. 3. Аккуратно раздели ФИО руководителя на directorLastName, directorFirstName и directorMiddleName. 4. Номера расчетных и корр. счетов извлекай только из 20 цифр без пробелов. 5. Верни только чистый JSON без markdown блоков, без кавычек ```json и без пояснений.',
    E'Текст реквизитов/карточки компании:\n${sourceText}',
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
     WHERE CODE = 'COMPANY_REQUISITES_PARSE_JSON'
);

-- Посев COMPANY_WEB_SEARCH_PARSE_JSON (если еще не была создана)
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
    'Ты — интеллектуальный агент поиска и структурирования информации об организациях HRM HuntTech. Твоя задача — по предоставленному наименованию организации/бренду и/или ИНН найти актуальные сведения в интернет-реестрах (ЕГРЮЛ/ЕГРИП, Rusprofile, Checko), официальных сайтах и базах знаний. Верни строго валидный JSON (объект со списком "candidates": [...] или один объект), содержащий: "companyName" (торговое наименование/бренд), "companyShortName" (краткое наименование), "legalEntityName" (полное юридическое наименование, например ООО "Яндекс"), "ownership" (ООО/АО/ПАО/ИП), "inn" (10 или 12 цифр), "kpp" (9 цифр), "ogrn" (13 или 15 цифр), "okpo", "oktmo", "okved" (код и расшифровка основного вида деятельности), "country" (страна, например Россия), "region" (регион/область, например г. Москва), "city" (город, например Москва), "streetAddress" (улица, дом, офис), "legalAddress" (полный юр. адрес), "actualAddress" (фактический адрес), "postalAddress" (почтовый адрес), "bik" (БИК банка), "bankName" (наименование банка), "settlementAccount" (р/с из 20 цифр), "correspondentAccount" (к/с из 20 цифр), "phone" (официальный телефон), "email" (корпоративный email), "website" (официальный сайт), "directorLastName" (фамилия руководителя), "directorFirstName" (имя руководителя), "directorMiddleName" (отчество руководителя), "directorPosition" (Генеральный директор / Директор), "directorPhone" (телефон), "directorEmail" (email), "companyDescription" (развернутое описание сферы деятельности компании, ключевых продуктов, масштаба и услуг), "workingConditions" (условия работы, график, стек технологий, социальный пакет и особенности), "rawFoundSnippet" (краткая выжимка из интернет-источников). Если по названию существует несколько юридических лиц или брендов, верни 2-4 наиболее релевантных варианта в массиве "candidates". Правила: 1. Возвращай только чистый JSON без markdown блоков ```json и без лишнего текста. 2. Всегда генерируй подробное описание деятельности (companyDescription) и условия работы (workingConditions).',
    E'Запрос на поиск компании в интернете:\nНаименование / Бренд: ${companyName}\nИНН: ${inn}\nПоисковый запрос: ${searchQuery}\n${sourceText}',
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
     WHERE CODE = 'COMPANY_WEB_SEARCH_PARSE_JSON'
);

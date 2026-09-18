-- 1. Создание таблицы HUNTTECH_LLM_CHAT_PENDING_ACTION для многошаговых диалогов и подтверждений
CREATE TABLE IF NOT EXISTS HUNTTECH_LLM_CHAT_PENDING_ACTION (
    ID uuid NOT NULL,
    VERSION integer NOT NULL,
    CREATE_TS timestamp without time zone,
    CREATED_BY character varying(50),
    UPDATE_TS timestamp without time zone,
    UPDATED_BY character varying(50),
    DELETE_TS timestamp without time zone,
    DELETED_BY character varying(50),
    USER_ID uuid NOT NULL,
    CONVERSATION_ID uuid NOT NULL,
    ACTION_TYPE character varying(64) NOT NULL,
    STATUS character varying(32) DEFAULT 'PENDING' NOT NULL,
    EXPIRES_AT timestamp without time zone NOT NULL,
    STATE_DATA text,
    REQUEST_ID character varying(64),
    CONSTRAINT PK_HUNTTECH_LLM_CHAT_PENDING_ACTION PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_PENDING_ACTION_USER_CONV
    ON HUNTTECH_LLM_CHAT_PENDING_ACTION (USER_ID, CONVERSATION_ID, STATUS, EXPIRES_AT);

-- 2. Seed-миграция AI-функции INTERVIEW_SCHEDULING_PARSE
INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
    ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
    CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
    DEFAULT_MONTHLY_TOKEN_QUOTA,
    EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
    IS_ACTIVE, INCLUDE_USER_CONTEXT, CONFIGURATION_VERSION
)
SELECT
    'c0ffee00-0000-4000-8000-000000000020'::uuid,
    1, CURRENT_TIMESTAMP, 'migration', 'INTERVIEW_SCHEDULING_PARSE',
    'NLU разбор назначения собеседования кандидату',
    'Интеллектуальное структурирование запроса рекрутера на назначение собеседования кандидату на вакансию и проект с определением даты и времени.',
    'TEXT_GENERATION',
    'Ты — высокоточный модуль NLU-распознавания намерений HRM HuntTech.
Твоя задача — извлечь из русского текста запроса рекрутера структурированные параметры назначения собеседования кандидату.

Верни СТРОГО один валидный JSON-объект без markdown-разметки (без ```json), без вводных слов и пояснений со следующими полями:
{
  "candidateQuery": "ФИО или фамилия кандидата в исходной или именительной форме (или null)",
  "vacancyQuery": "Название или ключевые слова вакансии/должности (например «Системный аналитик», «Java разработчик», или null)",
  "projectQuery": "Название проекта или компании клиента (например «ВТБ», «Сбер», или null)",
  "projectOwnerQuery": "ФИО или фамилия руководителя/владельца проекта (например «Денис Гаркушин», «Гаркушин», или null)",
  "interactionTypeQuery": "Желаемый тип взаимодействия («Собеседование», «Интервью с заказчиком», «Техническое интервью», или null)",
  "rawDateTime": "Сырая строка даты и времени из сообщения (например «завтра на 17-00», «в пятницу в 15:30 по Москве»)",
  "timeZone": "Europe/Moscow или Europe/Saratov (если упомянуто самарское/саратовское время), по умолчанию Europe/Moscow",
  "online": true,
  "intentDetected": true
}

ПРАВИЛА:
1. Если в тексте есть фразы «в проект Дениса Гаркушина», «у Гаркушина», поле projectOwnerQuery = "Денис Гаркушин", projectQuery = null (или название проекта, если указано отдельно).
2. Если в тексте «в проект ВТБ», projectQuery = "ВТБ", projectOwnerQuery = null.
3. Не придумывай данные, которых нет в сообщении. Если поле не упомянуто, установи null.
4. intentDetected = true, если пользователь выражает намерение назначить, запланировать или провести собеседование/интервью/встречу с кандидатом.',
    E'Запрос пользователя:\n${sourceText}',
    0.1, 1000, NULL, 'USER_OVERRIDE_ALLOWED', 'FALLBACK_TO_ADMIN', FALSE,
    TRUE, FALSE, 1
WHERE NOT EXISTS (
    SELECT 1 FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
     WHERE CODE = 'INTERVIEW_SCHEDULING_PARSE'
);

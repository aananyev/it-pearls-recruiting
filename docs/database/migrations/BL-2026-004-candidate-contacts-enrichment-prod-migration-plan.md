# План безопасной миграции на прод: Фоновое определение контактных данных и фото кандидатов (BL-2026-004)

> [!IMPORTANT]
> **ПРАВИЛО БЕЗОПАСНОСТИ**: Прод менять запрещается в рамках задачи! Данный документ является утверждённым планом выполнения миграций структуры данных и системных промптов на продакшене при релизе.

---

## 1. Состав и цели миграции

| Параметр | Значение |
|---|---|
| **Идентификатор задачи** | `BL-2026-004` |
| **Компоненты** | База данных PostgreSQL (`hunttech_db`), AI Control Plane (`HUNTTECH_AI_FUNCTION_CONFIGURATION`) |
| **Тип изменений** | 1) DDL: добавление колонки `IMAGE_BYTE_ARRAY` в `HUNTTECH_JOB_CANDIDATE`<br>2) DDL: создание таблицы `HUNTTECH_CAND_CV_CONTACT_ANALYSIS` и индексов<br>3) DML/Seed: регистрация AI-функции `CONTACTS_EXTRACT_BACKGROUND` |
| **Время простоя (Downtime)** | 0 секунд (Zero-Downtime, неблокирующие DDL-операции) |

---

## 2. Предварительные шаги перед применением на продакшене

1. **Создание полного бэкапа рабочей БД**:
   ```bash
   pg_dump -h $PROD_DB_HOST -U $PROD_DB_USER -d $PROD_DB_NAME -F c -b -v -f "/backups/hunttech_prod_pre_bl2026_004_$(date +%Y%m%d_%H%M%S).dump"
   ```
2. **Проверка свободного места на диске**:
   - Минимум 10 GB свободного дискового пространства для временных файлов индексов.

---

## 3. Пошаговые скрипты миграции

### Шаг 3.1. DDL — Структура данных (`260924-1-createCandidateCvContactAnalysisTable.sql`)

```sql
-- 1. Добавление BLOB-поля для фотографии кандидата
ALTER TABLE HUNTTECH_JOB_CANDIDATE
    ADD COLUMN IF NOT EXISTS IMAGE_BYTE_ARRAY bytea;

-- 2. Таблица аудита и состояний анализа контактов
CREATE TABLE IF NOT EXISTS HUNTTECH_CAND_CV_CONTACT_ANALYSIS (
    ID uuid NOT NULL,
    VERSION integer NOT NULL,
    CREATE_TS timestamp without time zone,
    CREATED_BY character varying(50),
    UPDATE_TS timestamp without time zone,
    UPDATED_BY character varying(50),
    DELETE_TS timestamp without time zone,
    DELETED_BY character varying(50),
    CANDIDATE_ID uuid NOT NULL,
    CANDIDATE_CV_ID uuid NOT NULL,
    CV_CONTENT_HASH character varying(64),
    STATUS integer NOT NULL,
    CONTACTS_ANALYZED_AT timestamp without time zone,
    PROCESSING_STARTED_AT timestamp without time zone,
    PROCESSING_FINISHED_AT timestamp without time zone,
    DURATION_MS bigint,
    CONTACTS_CONFIG_VERSION integer,
    RETRY_COUNT integer DEFAULT 0,
    NEXT_RETRY_AT timestamp without time zone,
    LAST_ERROR character varying(2000),
    PROVIDER_CODE character varying(64),
    MODEL_NAME character varying(128),
    EXECUTION_SOURCE character varying(32),
    AI_FUNCTION_CODE character varying(64),
    PROMPT_TOKENS integer,
    COMPLETION_TOKENS integer,
    TOTAL_TOKENS integer,
    ESTIMATED_COST numeric(19, 6),
    CONTACTS_FOUND_COUNT integer DEFAULT 0,
    CONTACTS_UPDATED_COUNT integer DEFAULT 0,
    PHOTO_EXTRACTED boolean DEFAULT false,
    EXTRACTED_PHONE character varying(64),
    EXTRACTED_EMAIL character varying(128),
    EXTRACTED_TELEGRAM character varying(64),
    EXTRACTED_CITY character varying(128),
    DELTA_DETAILS_JSON text,
    PRIORITY integer DEFAULT 0,
    CONSTRAINT PK_HUNTTECH_CAND_CV_CONTACT_ANALYSIS PRIMARY KEY (ID)
);

CREATE INDEX IF NOT EXISTS IDX_CAND_CV_CNT_STATUS
    ON HUNTTECH_CAND_CV_CONTACT_ANALYSIS (STATUS);

CREATE UNIQUE INDEX IF NOT EXISTS IDX_CAND_CV_CNT_UNQ_CV
    ON HUNTTECH_CAND_CV_CONTACT_ANALYSIS (CANDIDATE_CV_ID)
    WHERE DELETE_TS IS NULL;

CREATE INDEX IF NOT EXISTS IDX_CAND_CV_CNT_CAND
    ON HUNTTECH_CAND_CV_CONTACT_ANALYSIS (CANDIDATE_ID);

CREATE INDEX IF NOT EXISTS IDX_CAND_CV_CNT_NEXT_RETRY
    ON HUNTTECH_CAND_CV_CONTACT_ANALYSIS (NEXT_RETRY_AT);

CREATE INDEX IF NOT EXISTS IDX_CAND_CV_CNT_STARTED
    ON HUNTTECH_CAND_CV_CONTACT_ANALYSIS (PROCESSING_STARTED_AT);

CREATE INDEX IF NOT EXISTS IDX_CAND_CV_CNT_PRIORITY
    ON HUNTTECH_CAND_CV_CONTACT_ANALYSIS (STATUS, PRIORITY, CREATE_TS);
```

### Шаг 3.2. DML — Seed системной функции AI (`260924-2-seedContactsExtractBackgroundAiFunction.sql`)

```sql
DO $$
DECLARE
    v_admin_config_id UUID;
    v_admin_model TEXT := 'glm-5.2';
BEGIN
    SELECT c.ID, COALESCE(c.DEFAULT_MODEL_NAME, 'glm-5.2')
      INTO v_admin_config_id, v_admin_model
      FROM HUNTTECH_ADMIN_AI_CONFIGURATION c
     WHERE c.DELETE_TS IS NULL
       AND c.IS_ACTIVE = TRUE
       AND (c.IS_FREE_MODEL = TRUE OR c.ID = 'e1a2b3c4-0001-4000-8000-000000000001'::uuid)
     ORDER BY (CASE WHEN c.ID = 'e1a2b3c4-0001-4000-8000-000000000001'::uuid THEN 0 ELSE 1 END),
              c.PRIORITY_ DESC NULLS LAST
     LIMIT 1;

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, INCLUDE_USER_CONTEXT, CONFIGURATION_VERSION,
        ADMIN_CONFIGURATION_ID, ADMIN_MODEL_NAME
    )
    SELECT
        'c4d6e8f0-1a2b-4c3d-9e5f-7a9b1c3d5e7f'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'CONTACTS_EXTRACT_BACKGROUND',
        'Извлечение контактов из резюме (фоновое)',
        'Фоновый анализ резюме кандидатов (Candidate Contact Enrichment): нейросеть возвращает строгий JSON с контактными данными кандидата.',
        'TEXT_GENERATION',
        'Ты — специализированный интеллектуальный экстрактор контактных данных кандидатов HRM HuntTech. Твоя задача: найти в переданном тексте резюме контактную информацию и вернуть строгий JSON без пояснений и без markdown-разметки: {"phone": "+7 999 123-45-67", "mobilePhone": "+7 999 123-45-67", "email": "candidate@example.com", "telegramName": "username", "skypeName": "skype.login", "whatsupName": "+79991234567", "wiberName": "+79991234567", "city": "Москва"}. Правила: 1. Номера телефонов форматируй с международным кодом. 2. Telegram: укажи никнейм без символа @ и без ссылки t.me/. 3. Город: укажи город проживания кандидата без страны. 4. Если контакт не указан в тексте, верни для этого поля null. 5. Не придумывай контакты, которых нет в тексте.',
        E'Текст резюме для извлечения контактов:\n${sourceText}',
        0.1,
        500,
        'ADMIN_ONLY',
        'FALLBACK_TO_ADMIN',
        TRUE,
        TRUE,
        FALSE,
        1,
        v_admin_config_id,
        v_admin_model
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'CONTACTS_EXTRACT_BACKGROUND'
    );
END
$$;
```

---

## 4. Верификация после применения (Post-Deploy Checks)

1. Проверить наличие колонки `IMAGE_BYTE_ARRAY`:
   ```sql
   SELECT column_name, data_type FROM information_schema.columns
    WHERE table_name = 'hunttech_job_candidate' AND column_name = 'image_byte_array';
   ```
2. Проверить создание таблицы и индексов `HUNTTECH_CAND_CV_CONTACT_ANALYSIS`:
   ```sql
   SELECT table_name FROM information_schema.tables WHERE table_name = 'hunttech_cand_cv_contact_analysis';
   ```
3. Проверить активность функции `CONTACTS_EXTRACT_BACKGROUND`:
   ```sql
   SELECT code, capability, is_active, admin_model_name, fallback_policy
     FROM hunttech_ai_function_configuration
    WHERE code = 'CONTACTS_EXTRACT_BACKGROUND';
   ```

---

## 5. План отката (Rollback Plan)

```sql
DROP TABLE IF EXISTS HUNTTECH_CAND_CV_CONTACT_ANALYSIS;
ALTER TABLE HUNTTECH_JOB_CANDIDATE DROP COLUMN IF EXISTS IMAGE_BYTE_ARRAY;
DELETE FROM HUNTTECH_AI_FUNCTION_CONFIGURATION WHERE CODE = 'CONTACTS_EXTRACT_BACKGROUND';
```

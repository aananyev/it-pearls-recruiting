# План безопасной миграции на прод: Фоновое определение контактных данных и фото кандидатов (BL-2026-004)

> [!IMPORTANT]
> **ПРАВИЛО БЕЗОПАСНОСТИ**: Прод менять напрямую запрещается! Данный документ является утверждённым регламентом и планом выполнения миграций структуры данных, системных настроек ИИ и накопленных данных контактов кандидатов на продакшене при релизе.

---

## 1. Состав и цели миграции

| Параметр | Значение |
|---|---|
| **Идентификатор задачи** | `BL-2026-004` (Candidate Contact Enrichment) |
| **Компоненты** | База данных PostgreSQL (`hunttech`), AI Control Plane (`HUNTTECH_AI_FUNCTION_CONFIGURATION`), карточки кандидатов (`HUNTTECH_JOB_CANDIDATE`), аудит анализа (`HUNTTECH_CAND_CV_CONTACT_ANALYSIS`) |
| **Тип изменений** | 1) **DDL**: колонка `IMAGE_BYTE_ARRAY` в `HUNTTECH_JOB_CANDIDATE`<br>2) **DDL**: таблица `HUNTTECH_CAND_CV_CONTACT_ANALYSIS` и индексы<br>3) **DML/Seed**: регистрация AI-функции `CONTACTS_EXTRACT_BACKGROUND`<br>4) **DML/Data**: миграция накопленных распознанных контактных данных (28 CV, 7 кандидатов) |
| **Время простоя (Downtime)** | 0 секунд (Zero-Downtime, неблокирующие DDL и идемпотентные DML-операции) |

---

## 2. Предварительные шаги перед применением на продакшене (Pre-Checks)

1. **Создание полного бэкапа рабочей БД**:
   ```bash
   pg_dump -h $PROD_DB_HOST -U $PROD_DB_USER -d $PROD_DB_NAME -F c -b -v -f "/backups/hunttech_prod_pre_bl2026_004_$(date +%Y%m%d_%H%M%S).dump"
   ```
2. **Проверка свободного места на диске**:
   - Минимум 10 GB свободного дискового пространства для временных файлов и построения индексов.
3. **Проверка подключения и целостности связей**:
   ```sql
   SELECT count(*) FROM HUNTTECH_JOB_CANDIDATE WHERE DELETE_TS IS NULL;
   SELECT count(*) FROM HUNTTECH_CANDIDATE_CV WHERE DELETE_TS IS NULL;
   ```

---

## 3. Пошаговые скрипты миграции

### Шаг 3.1. DDL — Структура данных (`260924-1-createCandidateCvContactAnalysisTable.sql`)

Файл changelog: `modules/core/db/changelog/260924-1-createCandidateCvContactAnalysisTable.xml`

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

---

### Шаг 3.2. DML — Seed системной функции AI (`260924-2-seedContactsExtractBackgroundAiFunction.sql`)

Файл changelog: `modules/core/db/changelog/260924-2-seedContactsExtractBackgroundAiFunction.xml`

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

### Шаг 3.3. DML — Миграция накопленных данных контактов (`260924-3-migrateExtractedCandidateContactsData.sql`)

Файл changelog: `modules/core/db/changelog/260924-3-migrateExtractedCandidateContactsData.xml`

#### Гарантии безопасности данных:
1. **Защита от перезаписи существующих контактов на проде**:
   Применяется конструкция `COALESCE(NULLIF(COLUMN, ''), '<extracted_value>')`. Если рекрутер уже ввел телефон, email или telegram на проде вручную, эти данные **гарантированно сохраняются** и не перезаписываются.
2. **Идемпотентность аудита**:
   Все вставки в таблицу `HUNTTECH_CAND_CV_CONTACT_ANALYSIS` используют условие:
   `ON CONFLICT (CANDIDATE_CV_ID) WHERE DELETE_TS IS NULL DO NOTHING;`
   Это исключает ошибки дублирования первичных и уникальных ключей при повторном запуске.
3. **Статистика мигрируемых данных**:
   - **28 записей аудита** резюме (`HUNTTECH_CAND_CV_CONTACT_ANALYSIS`):
     - 19 записей в статусе `FRESH` (успешно проанализированы нейросетью с сохранением телеметрии токенов, стоимости и времени выполнения).
     - 9 записей системного контроля (пропущенные пустые резюме и восстановление после таймаутов).
   - **7 обогащённых кандидатов** (`HUNTTECH_JOB_CANDIDATE`):

| ID кандидата | ФИО / Резюме | Телефон | Мобильный | Email | Telegram |
|---|---|---|---|---|---|
| `07e8b22b-e420-4ac7-5ca0-aa1e44f48c2a` | Мустафа Мирзажанов | `+79636377171` | `+7 963 637-71-71` | — | `Mustafa_Mirzazhanov` |
| `1a810cba-8037-c15e-b03b-132eb980510a` | Алексей Конкин | `+7 985 185-24-40` | `+7 985 185-24-40` | `konkin.alex777@mail.ru` | `Aleksei_Konkin` |
| `21175350-c484-a80b-8fab-8600034cdcd5` | Ринат (BlackRine) | `+7 988 586-45-99` | `+7 988 586-45-99` | `black.lucky.rine@gmail.com` | `blackrine` |
| `25c1f2db-4ea4-2f20-cc72-f9e6d88db0fe` | Евгений Долгов | `+7 993 296-91-35` | `+7 993 296-91-35` | `evg.dolg.12@yandex.com` | `evg_dolg` |
| `524969b7-3398-c908-9e80-78596c4fc672` | Азмиддин | `+7 964 293-89-97` | `+7 964 293-89-97` | `azmiddin2mpxsal@mail.ru` | `misterbakss` |
| `d6f6768e-20b8-a254-fdec-a070336c582d` | Глеб Шустиков | `+7 906 940-44-87` | `+7 906 940-44-87` | `shustikov.gleb@gmail.com` | `glebash100` |
| `f46a4c84-e6d2-ac46-f785-611b2ff42fea` | Сергей Параев | `+79140042591` | `+7 914 004-25-91` | `sergey.paraev@gmail.com` | `sergeyparaev` |

#### SQL-блок обновления кандидатов:
```sql
DO $$
BEGIN
    -- 1. Перенос записей телеметрии и аудита контактов
    -- (полный список 28 INSERT приведен в файле 260924-3-migrateExtractedCandidateContactsData.sql)

    -- 2. Безопасное наполнение контактов кандидатов (только пустые поля)
    UPDATE HUNTTECH_JOB_CANDIDATE
       SET PHONE = COALESCE(NULLIF(PHONE, ''), '+79636377171'),
           MOBILE_PHONE = COALESCE(NULLIF(MOBILE_PHONE, ''), '+7 963 637-71-71'),
           TELEGRAM_NAME = COALESCE(NULLIF(TELEGRAM_NAME, ''), 'Mustafa_Mirzazhanov'),
           UPDATE_TS = CURRENT_TIMESTAMP, UPDATED_BY = 'migration'
     WHERE ID = '07e8b22b-e420-4ac7-5ca0-aa1e44f48c2a'::uuid;

    UPDATE HUNTTECH_JOB_CANDIDATE
       SET PHONE = COALESCE(NULLIF(PHONE, ''), '+7 985 185-24-40'),
           MOBILE_PHONE = COALESCE(NULLIF(MOBILE_PHONE, ''), '+7 985 185-24-40'),
           EMAIL = COALESCE(NULLIF(EMAIL, ''), 'konkin.alex777@mail.ru'),
           TELEGRAM_NAME = COALESCE(NULLIF(TELEGRAM_NAME, ''), 'Aleksei_Konkin'),
           UPDATE_TS = CURRENT_TIMESTAMP, UPDATED_BY = 'migration'
     WHERE ID = '1a810cba-8037-c15e-b03b-132eb980510a'::uuid;

    UPDATE HUNTTECH_JOB_CANDIDATE
       SET PHONE = COALESCE(NULLIF(PHONE, ''), '+7 988 586-45-99'),
           MOBILE_PHONE = COALESCE(NULLIF(MOBILE_PHONE, ''), '+7 988 586-45-99'),
           EMAIL = COALESCE(NULLIF(EMAIL, ''), 'black.lucky.rine@gmail.com'),
           TELEGRAM_NAME = COALESCE(NULLIF(TELEGRAM_NAME, ''), 'blackrine'),
           UPDATE_TS = CURRENT_TIMESTAMP, UPDATED_BY = 'migration'
     WHERE ID = '21175350-c484-a80b-8fab-8600034cdcd5'::uuid;

    UPDATE HUNTTECH_JOB_CANDIDATE
       SET PHONE = COALESCE(NULLIF(PHONE, ''), '+7 993 296-91-35'),
           MOBILE_PHONE = COALESCE(NULLIF(MOBILE_PHONE, ''), '+7 993 296-91-35'),
           EMAIL = COALESCE(NULLIF(EMAIL, ''), 'evg.dolg.12@yandex.com'),
           TELEGRAM_NAME = COALESCE(NULLIF(TELEGRAM_NAME, ''), 'evg_dolg'),
           UPDATE_TS = CURRENT_TIMESTAMP, UPDATED_BY = 'migration'
     WHERE ID = '25c1f2db-4ea4-2f20-cc72-f9e6d88db0fe'::uuid;

    UPDATE HUNTTECH_JOB_CANDIDATE
       SET PHONE = COALESCE(NULLIF(PHONE, ''), '+7 964 293-89-97'),
           MOBILE_PHONE = COALESCE(NULLIF(MOBILE_PHONE, ''), '+7 964 293-89-97'),
           EMAIL = COALESCE(NULLIF(EMAIL, ''), 'azmiddin2mpxsal@mail.ru'),
           TELEGRAM_NAME = COALESCE(NULLIF(TELEGRAM_NAME, ''), 'misterbakss'),
           UPDATE_TS = CURRENT_TIMESTAMP, UPDATED_BY = 'migration'
     WHERE ID = '524969b7-3398-c908-9e80-78596c4fc672'::uuid;

    UPDATE HUNTTECH_JOB_CANDIDATE
       SET PHONE = COALESCE(NULLIF(PHONE, ''), '+7 906 940-44-87'),
           MOBILE_PHONE = COALESCE(NULLIF(MOBILE_PHONE, ''), '+7 906 940-44-87'),
           EMAIL = COALESCE(NULLIF(EMAIL, ''), 'shustikov.gleb@gmail.com'),
           TELEGRAM_NAME = COALESCE(NULLIF(TELEGRAM_NAME, ''), 'glebash100'),
           UPDATE_TS = CURRENT_TIMESTAMP, UPDATED_BY = 'migration'
     WHERE ID = 'd6f6768e-20b8-a254-fdec-a070336c582d'::uuid;

    UPDATE HUNTTECH_JOB_CANDIDATE
       SET PHONE = COALESCE(NULLIF(PHONE, ''), '+79140042591'),
           MOBILE_PHONE = COALESCE(NULLIF(MOBILE_PHONE, ''), '+7 914 004-25-91'),
           EMAIL = COALESCE(NULLIF(EMAIL, ''), 'sergey.paraev@gmail.com'),
           TELEGRAM_NAME = COALESCE(NULLIF(TELEGRAM_NAME, ''), 'sergeyparaev'),
           UPDATE_TS = CURRENT_TIMESTAMP, UPDATED_BY = 'migration'
     WHERE ID = 'f46a4c84-e6d2-ac46-f785-611b2ff42fea'::uuid;
END $$;
```

---

## 4. Верификация после применения (Post-Deploy Checks)

Выполнить проверочные запросы на целевой БД:

1. **Проверка добавления колонки фото**:
   ```sql
   SELECT column_name, data_type
     FROM information_schema.columns
    WHERE table_name = 'hunttech_job_candidate'
      AND column_name = 'image_byte_array';
   ```
   *Ожидается:* 1 строка, тип `bytea`.

2. **Проверка структуры таблицы и индексов аудита**:
   ```sql
   SELECT count(*)
     FROM pg_indexes
    WHERE tablename = 'hunttech_cand_cv_contact_analysis';
   ```
   *Ожидается:* 6 индексов (`PK`, `IDX_CAND_CV_CNT_STATUS`, `IDX_CAND_CV_CNT_UNQ_CV`, `IDX_CAND_CV_CNT_CAND`, `IDX_CAND_CV_CNT_NEXT_RETRY`, `IDX_CAND_CV_CNT_STARTED`, `IDX_CAND_CV_CNT_PRIORITY`).

3. **Проверка активности AI-функции**:
   ```sql
   SELECT code, capability, is_active, admin_model_name, fallback_policy
     FROM hunttech_ai_function_configuration
    WHERE code = 'CONTACTS_EXTRACT_BACKGROUND';
   ```
   *Ожидается:* `is_active = true`, `capability = TEXT_GENERATION`, `fallback_policy = FALLBACK_TO_ADMIN`.

4. **Проверка перенесённых аудит-записей**:
   ```sql
   SELECT status, count(*)
     FROM hunttech_cand_cv_contact_analysis
    WHERE delete_ts IS NULL
    GROUP BY status;
   ```
   *Ожидается:* не менее 28 записей (статусы 30 FRESH, 50 TIMEOUT/SKIPPED).

5. **Проверка заполненности контактов кандидатов**:
   ```sql
   SELECT count(*)
     FROM hunttech_job_candidate
    WHERE id IN (
        '07e8b22b-e420-4ac7-5ca0-aa1e44f48c2a'::uuid,
        '1a810cba-8037-c15e-b03b-132eb980510a'::uuid,
        '21175350-c484-a80b-8fab-8600034cdcd5'::uuid,
        '25c1f2db-4ea4-2f20-cc72-f9e6d88db0fe'::uuid,
        '524969b7-3398-c908-9e80-78596c4fc672'::uuid,
        'd6f6768e-20b8-a254-fdec-a070336c582d'::uuid,
        'f46a4c84-e6d2-ac46-f785-611b2ff42fea'::uuid
    )
    AND phone IS NOT NULL;
   ```
   *Ожидается:* 7 записей с заполненными телефонами.

---

## 5. План отката (Rollback Plan)

При возникновении непредвиденных сбоев откат выполняется следующими действиями:

1. **Откат структуры и системной функции**:
   ```sql
   DROP TABLE IF EXISTS HUNTTECH_CAND_CV_CONTACT_ANALYSIS CASCADE;
   ALTER TABLE HUNTTECH_JOB_CANDIDATE DROP COLUMN IF EXISTS IMAGE_BYTE_ARRAY;
   DELETE FROM HUNTTECH_AI_FUNCTION_CONFIGURATION WHERE CODE = 'CONTACTS_EXTRACT_BACKGROUND';
   ```

2. **Откат из бэкапа (при повреждении критических данных)**:
   При необходимости восстановления исходных значений полей контактов кандидатов до применения миграции используется созданный перед миграцией дамп базы:
   ```bash
   pg_restore -h $PROD_DB_HOST -U $PROD_DB_USER -d $PROD_DB_NAME -t hunttech_job_candidate --data-only /backups/hunttech_prod_pre_bl2026_004_*.dump
   ```

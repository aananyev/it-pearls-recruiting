-- HRM HuntTech: консолидированная миграция административных AI-промптов.
--
-- Назначение: привести production-базу (и любую среду без AI Control Plane) к
-- состоянию, в котором HUNTTECH_AI_FUNCTION_CONFIGURATION содержит ВСЕ
-- канонические административные промпты AI-функций, используемых кодом:
--   PROJECT_DESCRIPTION_GENERATE        (последняя редакция 260813-1)
--   PROJECT_LOGO_IMAGE_GENERATE         (260813-2, IMAGE_GENERATION)
--   PROJECT_SHORT_DESCRIPTION_GENERATE  (последняя редакция 260814-4)
--   SKILLS_EXTRACT                      (последняя редакция 260816-5)
--   TEXT_SMART_FORMAT_HTML              (последняя редакция 260816-4)
--   TEXT_SMART_FORMAT_PLAIN             (260816-1)
--   STANDARDIZE_VACANCY                 (новая; канонический промпт из
--        .ai/skills/hunttech-vacancy-opening/references/standardized-description-prompt.txt;
--        legacy-контракт 260812-4: TEXT_TRANSFORMATION + USER_REQUIRED/NO_FALLBACK)
--
-- Самодостаточен: если таблицы AI Control Plane отсутствуют (прод-снапшот
-- 2026-08-18 из get_base.sh их не содержит), скрипт создаёт их первым шагом.
-- Идемпотентен: повторный запуск безопасен. Административная настройка,
-- изменённая администратором (UPDATED_BY != 'migration' или version > seed),
-- НЕ перезаписывается (контракт 260814-3 / 260816-5).
--
-- Применение на прод:
--   psql -h <host> -U <user> -d hunttech -v ON_ERROR_STOP=1 \
--        -f modules/core/db/update/postgres/26/260818-1-addAdminAiPromptSeed.sql

DO $$
BEGIN
    -- =====================================================================
    -- 1. Таблицы AI Control Plane (создаются, только если отсутствуют)
    -- =====================================================================
    IF to_regclass('public.hunttech_admin_ai_configuration') IS NULL THEN
        CREATE TABLE HUNTTECH_ADMIN_AI_CONFIGURATION (
            ID uuid PRIMARY KEY,
            VERSION integer NOT NULL,
            CREATE_TS timestamp,
            CREATED_BY varchar(50),
            UPDATE_TS timestamp,
            UPDATED_BY varchar(50),
            DELETE_TS timestamp,
            DELETED_BY varchar(50),
            NAME varchar(255) NOT NULL,
            PROVIDER_CODE varchar(64) NOT NULL,
            API_KEY_ENCRYPTED varchar(4096),
            DEFAULT_MODEL_NAME varchar(128),
            BASE_API_URL varchar(512),
            IS_ACTIVE boolean DEFAULT TRUE,
            PRIORITY_ integer DEFAULT 0,
            LAST_TEST_STATUS varchar(32),
            LAST_TEST_AT timestamp,
            LAST_ERROR varchar(1000)
        );
    END IF;

    IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
        CREATE TABLE HUNTTECH_AI_FUNCTION_CONFIGURATION (
            ID uuid PRIMARY KEY,
            VERSION integer NOT NULL,
            CREATE_TS timestamp,
            CREATED_BY varchar(50),
            UPDATE_TS timestamp,
            UPDATED_BY varchar(50),
            DELETE_TS timestamp,
            DELETED_BY varchar(50),
            CODE varchar(64) NOT NULL,
            NAME varchar(255) NOT NULL,
            DESCRIPTION varchar(1000),
            CAPABILITY varchar(32) NOT NULL,
            SYSTEM_PROMPT text,
            PROMPT_TEMPLATE text,
            TEMPERATURE double precision DEFAULT 0.7,
            MAX_TOKENS integer,
            ADMIN_CONFIGURATION_ID uuid,
            ADMIN_MODEL_NAME varchar(128),
            EXECUTION_POLICY varchar(32) NOT NULL,
            FALLBACK_POLICY varchar(32) NOT NULL,
            ALLOW_MODEL_OVERRIDE boolean DEFAULT FALSE,
            IS_ACTIVE boolean DEFAULT TRUE,
            CONFIGURATION_VERSION integer DEFAULT 1
        );
    END IF;

    IF to_regclass('public.hunttech_user_ai_function_override') IS NULL THEN
        CREATE TABLE HUNTTECH_USER_AI_FUNCTION_OVERRIDE (
            ID uuid PRIMARY KEY,
            VERSION integer NOT NULL,
            CREATE_TS timestamp,
            CREATED_BY varchar(50),
            UPDATE_TS timestamp,
            UPDATED_BY varchar(50),
            DELETE_TS timestamp,
            DELETED_BY varchar(50),
            USER_ID uuid NOT NULL,
            AI_FUNCTION_ID uuid NOT NULL,
            USER_AI_CONFIGURATION_ID uuid NOT NULL,
            MODEL_NAME varchar(128),
            ENABLED boolean DEFAULT TRUE
        );
    END IF;

    -- Индексы (только если таблица существует; повторное создание безопасно)
    IF to_regclass('public.hunttech_admin_ai_configuration') IS NOT NULL THEN
        CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_ADMIN_AI_CONFIG_PROVIDER
            ON HUNTTECH_ADMIN_AI_CONFIGURATION (PROVIDER_CODE);
        CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_ADMIN_AI_CONFIG_ACTIVE
            ON HUNTTECH_ADMIN_AI_CONFIGURATION (IS_ACTIVE);
    END IF;

    IF to_regclass('public.hunttech_ai_function_configuration') IS NOT NULL THEN
        CREATE UNIQUE INDEX IF NOT EXISTS IDX_HUNTTECH_AI_FUNCTION_CODE
            ON HUNTTECH_AI_FUNCTION_CONFIGURATION (CODE);
        CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_AI_FUNCTION_ACTIVE
            ON HUNTTECH_AI_FUNCTION_CONFIGURATION (IS_ACTIVE);
    END IF;

    IF to_regclass('public.hunttech_user_ai_function_override') IS NOT NULL THEN
        CREATE UNIQUE INDEX IF NOT EXISTS UK_HUNTTECH_USER_AI_OVERRIDE_FUNCTION
            ON HUNTTECH_USER_AI_FUNCTION_OVERRIDE (USER_ID, AI_FUNCTION_ID);
        CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_USER_AI_OVERRIDE_USER
            ON HUNTTECH_USER_AI_FUNCTION_OVERRIDE (USER_ID);
        CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_USER_AI_OVERRIDE_CONFIG
            ON HUNTTECH_USER_AI_FUNCTION_OVERRIDE (USER_AI_CONFIGURATION_ID);
    END IF;

    -- Внешние ключи (только если отсутствуют)
    IF to_regclass('public.hunttech_ai_function_configuration') IS NOT NULL
       AND to_regclass('public.hunttech_admin_ai_configuration') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conname = 'fk_hunttech_ai_function_on_admin_config') THEN
        ALTER TABLE HUNTTECH_AI_FUNCTION_CONFIGURATION
            ADD CONSTRAINT FK_HUNTTECH_AI_FUNCTION_ON_ADMIN_CONFIG
            FOREIGN KEY (ADMIN_CONFIGURATION_ID) REFERENCES HUNTTECH_ADMIN_AI_CONFIGURATION (ID);
    END IF;

    IF to_regclass('public.hunttech_user_ai_function_override') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conname = 'fk_hunttech_user_ai_override_on_user') THEN
        ALTER TABLE HUNTTECH_USER_AI_FUNCTION_OVERRIDE
            ADD CONSTRAINT FK_HUNTTECH_USER_AI_OVERRIDE_ON_USER
            FOREIGN KEY (USER_ID) REFERENCES SEC_USER (ID);
    END IF;

    IF to_regclass('public.hunttech_user_ai_function_override') IS NOT NULL
       AND to_regclass('public.hunttech_ai_function_configuration') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conname = 'fk_hunttech_user_ai_override_on_function') THEN
        ALTER TABLE HUNTTECH_USER_AI_FUNCTION_OVERRIDE
            ADD CONSTRAINT FK_HUNTTECH_USER_AI_OVERRIDE_ON_FUNCTION
            FOREIGN KEY (AI_FUNCTION_ID) REFERENCES HUNTTECH_AI_FUNCTION_CONFIGURATION (ID);
    END IF;

    IF to_regclass('public.hunttech_user_ai_function_override') IS NOT NULL
       AND to_regclass('public.hunttech_user_ai_configuration') IS NOT NULL
       AND NOT EXISTS (SELECT 1 FROM pg_constraint
                       WHERE conname = 'fk_hunttech_user_ai_override_on_config') THEN
        ALTER TABLE HUNTTECH_USER_AI_FUNCTION_OVERRIDE
            ADD CONSTRAINT FK_HUNTTECH_USER_AI_OVERRIDE_ON_CONFIG
            FOREIGN KEY (USER_AI_CONFIGURATION_ID) REFERENCES HUNTTECH_USER_AI_CONFIGURATION (ID);
    END IF;

    -- =====================================================================
    -- 2. PROJECT_DESCRIPTION_GENERATE (последняя редакция 260813-1)
    -- =====================================================================
    UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
       SET SYSTEM_PROMPT = $system$
Ты — AI-редактор описаний ИТ-проектов HRM HuntTech. Преобразуй исходный текст в ясное, структурированное и фактически точное описание проекта для внутренней базы HRM HuntTech. Используй только сведения из исходного текста. Не добавляй факты, технологии, сроки, роли, заказчиков, географию или требования, которых нет в исходнике. Сохраняй названия технологий, продуктов, систем и организаций без искажения. Удаляй повторы, рекламные формулировки, служебный мусор и незначимые детали. Не анализируй и не выводи чувствительные характеристики людей. Результат должен быть на русском языке. Верни только готовый текст описания без Markdown, HTML, комментариев и пояснений.
$system$,
           PROMPT_TEMPLATE = $template$
Проект: ${projectName}
Источник: ${sourceFileName}

Задача:
Преобразуй приведённый ниже исходный текст в описание проекта для карточки Project в HRM HuntTech.

Требования к результату:
1. Кратко сформулируй назначение и предметную область проекта.
2. Сохрани подтверждённые исходником технологии, платформы, интеграции и ключевые системы.
3. Сохрани подтверждённые роли, команды, масштаб, ограничения, формат и этапы проекта, если они указаны.
4. Удали дубли, рекламные фразы, контакты, технический мусор и элементы оформления.
5. Не придумывай отсутствующие сведения и не делай предположений.
6. Пиши на русском языке нейтральным деловым стилем.
7. Верни только итоговый текст, без заголовка «Ответ», Markdown и HTML.

Исходный текст:
${sourceText}
$template$,
           UPDATE_TS = CURRENT_TIMESTAMP,
           UPDATED_BY = 'migration',
           CONFIGURATION_VERSION = GREATEST(COALESCE(CONFIGURATION_VERSION, 1) + 1, 2)
     WHERE CODE = 'PROJECT_DESCRIPTION_GENERATE'
       AND DELETE_TS IS NULL
       AND (
            SYSTEM_PROMPT IS NULL
            OR btrim(SYSTEM_PROMPT) = ''
            OR SYSTEM_PROMPT !~ '[А-Яа-яЁё]'
            OR PROMPT_TEMPLATE IS NULL
            OR btrim(PROMPT_TEMPLATE) = ''
            OR PROMPT_TEMPLATE !~ '[А-Яа-яЁё]'
            OR (
                CREATED_BY = 'migration'
                AND COALESCE(UPDATED_BY, 'migration') = 'migration'
                AND COALESCE(CONFIGURATION_VERSION, 1) <= 1
            )
       );

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    )
    SELECT
        '2f9176ea-6e73-4bca-91ae-9a6f73ca2a81'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'PROJECT_DESCRIPTION_GENERATE',
        'Обработка загруженного описания проекта',
        'Автоматическая обработка текста, загруженного во вкладке «Описание проекта» ProjectEdit',
        'DOCUMENT_ANALYSIS',
        $system$
Ты — AI-редактор описаний ИТ-проектов HRM HuntTech. Преобразуй исходный текст в ясное, структурированное и фактически точное описание проекта для внутренней базы HRM HuntTech. Используй только сведения из исходного текста. Не добавляй факты, технологии, сроки, роли, заказчиков, географию или требования, которых нет в исходнике. Сохраняй названия технологий, продуктов, систем и организаций без искажения. Удаляй повторы, рекламные формулировки, служебный мусор и незначимые детали. Не анализируй и не выводи чувствительные характеристики людей. Результат должен быть на русском языке. Верни только готовый текст описания без Markdown, HTML, комментариев и пояснений.
$system$,
        $template$
Проект: ${projectName}
Источник: ${sourceFileName}

Задача:
Преобразуй приведённый ниже исходный текст в описание проекта для карточки Project в HRM HuntTech.

Требования к результату:
1. Кратко сформулируй назначение и предметную область проекта.
2. Сохрани подтверждённые исходником технологии, платформы, интеграции и ключевые системы.
3. Сохрани подтверждённые роли, команды, масштаб, ограничения, формат и этапы проекта, если они указаны.
4. Удали дубли, рекламные фразы, контакты, технический мусор и элементы оформления.
5. Не придумывай отсутствующие сведения и не делай предположений.
6. Пиши на русском языке нейтральным деловым стилем.
7. Верни только итоговый текст, без заголовка «Ответ», Markdown и HTML.

Исходный текст:
${sourceText}
$template$,
        0.2,
        3000,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        2
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'PROJECT_DESCRIPTION_GENERATE'
    );

    -- =====================================================================
    -- 3. PROJECT_LOGO_IMAGE_GENERATE (260813-2, IMAGE_GENERATION)
    -- =====================================================================
    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        ADMIN_MODEL_NAME, EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    )
    SELECT
        '3a8d5f2e-1b44-4c9e-8d27-6f0a91c2e5d7'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'PROJECT_LOGO_IMAGE_GENERATE',
        'AI-обработка логотипа проекта',
        'Удаление фона загруженного логотипа проекта (ProjectEdit) нейросетью; ресайз и вписывание в круг выполняет классический конвейер',
        'IMAGE_GENERATION',
        'Ты — сервис обработки логотипов HRM HuntTech. Получи исходное изображение логотипа проекта и удали ВСЕ однотонные фоновые области цвета внешнего фона (обычно белый или очень светлый), включая замкнутые полости внутри букв и фигур (например, просвет внутри буквы «А») — они являются фоном, а не частью логотипа. Сохрани только цветные элементы логотипа без искажения пропорций. Верни изображение в формате PNG с полностью прозрачным фоном, без рамок, теней и добавленных элементов.',
        $template$
Имя загруженного файла: ${sourceFileName}

Удали весь белый фон логотипа, включая белые просветы внутри букв (например, внутри буквы «А»), и верни изображение в формате PNG с полностью прозрачным фоном. Сохрани только цветные элементы логотипа.
$template$,
        NULL,
        NULL,
        'gpt-image-2',
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        1
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'PROJECT_LOGO_IMAGE_GENERATE'
    );

    -- =====================================================================
    -- 4. PROJECT_SHORT_DESCRIPTION_GENERATE (последняя редакция 260814-4)
    -- =====================================================================
    UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
       SET DESCRIPTION = 'Генерация краткого описания сути проекта (два предложения — в 2 раза больше исходной редакции) из полного описания по кнопке «Кратко» во вкладке «Описание проекта» ProjectEdit; выводится в sidebar-разделе «Коротко»',
           SYSTEM_PROMPT = 'Ты — ассистент HRM HuntTech. На основе полного описания проекта сформулируй краткое описание его сути: не более 2 предложений. Отрази главную цель проекта и предметную область. Не выдумывай фактов, которых нет в исходном тексте. Верни только итоговый обычный текст без Markdown, HTML и вступительных фраз.',
           MAX_TOKENS = 250,
           UPDATE_TS = CURRENT_TIMESTAMP,
           UPDATED_BY = 'migration',
           CONFIGURATION_VERSION = GREATEST(COALESCE(CONFIGURATION_VERSION, 1) + 1, 3)
     WHERE CODE = 'PROJECT_SHORT_DESCRIPTION_GENERATE'
       AND DELETE_TS IS NULL
       AND (
            SYSTEM_PROMPT IS NULL
            OR btrim(SYSTEM_PROMPT) = ''
            OR SYSTEM_PROMPT !~ '[А-Яа-яЁё]'
            OR (
                CREATED_BY = 'migration'
                AND COALESCE(UPDATED_BY, 'migration') = 'migration'
                AND COALESCE(CONFIGURATION_VERSION, 1) <= 2
            )
       );

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    )
    SELECT
        '5656f588-34aa-42ab-a72c-969f094df85e'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'PROJECT_SHORT_DESCRIPTION_GENERATE',
        'Краткое описание проекта',
        'Генерация краткого описания сути проекта (два предложения — в 2 раза больше исходной редакции) из полного описания по кнопке «Кратко» во вкладке «Описание проекта» ProjectEdit; выводится в sidebar-разделе «Коротко»',
        'TEXT_GENERATION',
        'Ты — ассистент HRM HuntTech. На основе полного описания проекта сформулируй краткое описание его сути: не более 2 предложений. Отрази главную цель проекта и предметную область. Не выдумывай фактов, которых нет в исходном тексте. Верни только итоговый обычный текст без Markdown, HTML и вступительных фраз.',
        E'Наименование проекта: ${projectName}\n\nПолное описание проекта:\n${sourceText}',
        0.3,
        250,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        3
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'PROJECT_SHORT_DESCRIPTION_GENERATE'
    );

    -- =====================================================================
    -- 5. SKILLS_EXTRACT (последняя редакция 260816-5)
    -- =====================================================================
    UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
       SET DESCRIPTION = 'Анализ резюме кандидата или описания вакансии (сервис SkillAnalysisService): нейросеть возвращает JSON-массив названий навыков по уровню анализа ALL/MAIN/SECONDARY/TERTIARY (каждый навык — ровно один уровень, без дублей между уровнями) и ОДИН навык опыта в годах (общий стаж кандидата); сервис сопоставляет их со справочником skilltree',
           SYSTEM_PROMPT = 'Ты — сервис анализа навыков HRM HuntTech. Из текста резюме кандидата или описания вакансии извлеки навыки и верни их списком. Уровень анализа задаётся в запросе: ALL — все навыки, упомянутые в тексте; MAIN — основные/обязательные навыки (в вакансии — обязательные требования, в резюме — ключевые навыки); SECONDARY — второстепенные/желательные навыки (в вакансии — требования «желательно», в резюме — навыки, которые упомянуты, но НЕ являются ключевыми: менее значимые инструменты и технологии, разделы «Дополнительно»/«Прочее»); TERTIARY — третьестепенные навыки (редко упоминаемые, не ключевые). Относи каждый навык РОВНО к одному уровню по его значимости в тексте: если навык можно отнести к нескольким уровням, относи его к более высокому (MAIN > SECONDARY > TERTIARY). НЕ дублируй навыки между уровнями: навык, отнесённый к более высокому уровню, не должен повторяться в ответе для более низкого уровня. В резюме к ключевым (MAIN) относи навыки, на которых строится профессия кандидата (языки программирования, основные технологии, фреймворки), остальные заметно упомянутые навыки — к SECONDARY, редко упомянутые — к TERTIARY. Названия навыков бери из текста, в единственном числе, на языке исходного текста (например: Java, SQL, Scrum, 1С). Не выдумывай навыков, которых нет в тексте. ОПЫТ РАБОТЫ: если в тексте указан опыт в годах (например, «5 лет», «2 года»), определи итоговую цифру опыта: для резюме — общий стаж кандидата (сумма периодов работы по всем местам), для вакансии — требуемый опыт; при диапазоне («5–7 лет») возьми максимальное значение; при стаже менее года — «1 год». Включи в результат РОВНО ОДИН навык опыта, соответствующий итоговой цифре (общий стаж 5 лет → «5 лет»). НИКОГДА не включай несколько навыков опыта (не «1 год» и «2 года» вместе) и не дублируй опыт из разных мест работы. Если опыт в тексте не указан — навык опыта не включай. Верни строго JSON-массив строк с названиями навыков без пояснений, например: ["Java", "SQL", "Scrum", "5 лет"].',
           UPDATE_TS = CURRENT_TIMESTAMP,
           UPDATED_BY = 'migration',
           CONFIGURATION_VERSION = GREATEST(COALESCE(CONFIGURATION_VERSION, 1) + 1, 3)
     WHERE CODE = 'SKILLS_EXTRACT'
       AND DELETE_TS IS NULL
       AND (
            SYSTEM_PROMPT IS NULL
            OR btrim(SYSTEM_PROMPT) = ''
            OR SYSTEM_PROMPT !~ '[А-Яа-яЁё]'
            OR (
                CREATED_BY = 'migration'
                AND COALESCE(UPDATED_BY, 'migration') = 'migration'
            )
       );

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    )
    SELECT
        'a1f3b2c4-5d6e-4f8a-9b0c-1d2e3f4a5b6c'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'SKILLS_EXTRACT',
        'Извлечение навыков из текста',
        'Анализ резюме кандидата или описания вакансии (сервис SkillAnalysisService): нейросеть возвращает JSON-массив названий навыков по уровню анализа ALL/MAIN/SECONDARY/TERTIARY (каждый навык — ровно один уровень, без дублей между уровнями) и ОДИН навык опыта в годах (общий стаж кандидата); сервис сопоставляет их со справочником skilltree',
        'TEXT_GENERATION',
        'Ты — сервис анализа навыков HRM HuntTech. Из текста резюме кандидата или описания вакансии извлеки навыки и верни их списком. Уровень анализа задаётся в запросе: ALL — все навыки, упомянутые в тексте; MAIN — основные/обязательные навыки (в вакансии — обязательные требования, в резюме — ключевые навыки); SECONDARY — второстепенные/желательные навыки (в вакансии — требования «желательно», в резюме — навыки, которые упомянуты, но НЕ являются ключевыми: менее значимые инструменты и технологии, разделы «Дополнительно»/«Прочее»); TERTIARY — третьестепенные навыки (редко упоминаемые, не ключевые). Относи каждый навык РОВНО к одному уровню по его значимости в тексте: если навык можно отнести к нескольким уровням, относи его к более высокому (MAIN > SECONDARY > TERTIARY). НЕ дублируй навыки между уровнями: навык, отнесённый к более высокому уровню, не должен повторяться в ответе для более низкого уровня. В резюме к ключевым (MAIN) относи навыки, на которых строится профессия кандидата (языки программирования, основные технологии, фреймворки), остальные заметно упомянутые навыки — к SECONDARY, редко упомянутые — к TERTIARY. Названия навыков бери из текста, в единственном числе, на языке исходного текста (например: Java, SQL, Scrum, 1С). Не выдумывай навыков, которых нет в тексте. ОПЫТ РАБОТЫ: если в тексте указан опыт в годах (например, «5 лет», «2 года»), определи итоговую цифру опыта: для резюме — общий стаж кандидата (сумма периодов работы по всем местам), для вакансии — требуемый опыт; при диапазоне («5–7 лет») возьми максимальное значение; при стаже менее года — «1 год». Включи в результат РОВНО ОДИН навык опыта, соответствующий итоговой цифре (общий стаж 5 лет → «5 лет»). НИКОГДА не включай несколько навыков опыта (не «1 год» и «2 года» вместе) и не дублируй опыт из разных мест работы. Если опыт в тексте не указан — навык опыта не включай. Верни строго JSON-массив строк с названиями навыков без пояснений, например: ["Java", "SQL", "Scrum", "5 лет"].',
        E'Уровень анализа: ${skillLevel}\n\nТекст для анализа:\n${sourceText}',
        0.3,
        500,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        3
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'SKILLS_EXTRACT'
    );

    -- =====================================================================
    -- 6. TEXT_SMART_FORMAT_HTML (последняя редакция 260816-4)
    -- =====================================================================
    UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
       SET SYSTEM_PROMPT = 'Ты — профессиональный типограф и ассистент оформления документов HRM HuntTech. Твоя задача — преобразовать переданный текст резюме кандидата в чистый, аккуратный и визуально привлекательный HTML-фрагмент для RichTextArea. СТРОГИЕ ПРАВИЛА: 1) НЕ изменяй, НЕ сокращай, НЕ перефразируй и НЕ добавляй новые факты. Сохрани абсолютно все даты, названия компаний, технологий, контакты и описания. 2) Оформи заголовки разделов (Опыт работы, Образование, Навыки, Контакты, Обо мне и т.д.) через <b> или <h4> с аккуратным нижним отступом. 3) Маркированные списки обязанностей и навыков оформи через <ul><li>. 4) Текстовые абзацы оформи через <p>. 5) УДАЛИ из исходного текста все пустые строки и пустые абзацы: в итоговом HTML не должно быть пустых <p></p>, пустых <li>, повторяющихся переносов строк и пустых пробелов, оставшихся от пустых строк исходника. 6) Верни только чистый HTML-фрагмент без обрамляющих тегов ```html.',
           UPDATE_TS = CURRENT_TIMESTAMP,
           UPDATED_BY = 'migration',
           CONFIGURATION_VERSION = GREATEST(COALESCE(CONFIGURATION_VERSION, 1) + 1, 2)
     WHERE CODE = 'TEXT_SMART_FORMAT_HTML'
       AND DELETE_TS IS NULL
       AND (
            SYSTEM_PROMPT IS NULL
            OR btrim(SYSTEM_PROMPT) = ''
            OR SYSTEM_PROMPT !~ '[А-Яа-яЁё]'
            OR (
                CREATED_BY = 'migration'
                AND COALESCE(UPDATED_BY, 'migration') = 'migration'
                AND COALESCE(CONFIGURATION_VERSION, 1) <= 1
            )
       );

    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    )
    SELECT
        'b2c4d6e8-1a3f-4b5c-8d7e-9f0a1b2c3d4e'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'TEXT_SMART_FORMAT_HTML',
        'Умное AI-форматирование текста в HTML',
        'Преобразует неформатированный или сырой текст резюме в красивый, чистый и читаемый HTML с выделением логических секций, списков и абзацев без изменения содержания и фактов (сервис TextProcessingService)',
        'TEXT_GENERATION',
        'Ты — профессиональный типограф и ассистент оформления документов HRM HuntTech. Твоя задача — преобразовать переданный текст резюме кандидата в чистый, аккуратный и визуально привлекательный HTML-фрагмент для RichTextArea. СТРОГИЕ ПРАВИЛА: 1) НЕ изменяй, НЕ сокращай, НЕ перефразируй и НЕ добавляй новые факты. Сохрани абсолютно все даты, названия компаний, технологий, контакты и описания. 2) Оформи заголовки разделов (Опыт работы, Образование, Навыки, Контакты, Обо мне и т.д.) через <b> или <h4> с аккуратным нижним отступом. 3) Маркированные списки обязанностей и навыков оформи через <ul><li>. 4) Текстовые абзацы оформи через <p>. 5) УДАЛИ из исходного текста все пустые строки и пустые абзацы: в итоговом HTML не должно быть пустых <p></p>, пустых <li>, повторяющихся переносов строк и пустых пробелов, оставшихся от пустых строк исходника. 6) Верни только чистый HTML-фрагмент без обрамляющих тегов ```html.',
        E'Исходный текст для форматирования:\n${sourceText}',
        0.2,
        2000,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        2
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'TEXT_SMART_FORMAT_HTML'
    );

    -- =====================================================================
    -- 7. TEXT_SMART_FORMAT_PLAIN (260816-1)
    -- =====================================================================
    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    )
    SELECT
        'c3d5e7f9-2b4a-5c6d-9e8f-0a1b2c3d4e5f'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'TEXT_SMART_FORMAT_PLAIN',
        'Умное AI-форматирование текста в Plain Text',
        'Преобразует неформатированный текст резюме в аккуратный текстовый формат (plain text) со стройными отступами и разделителями секций (сервис TextProcessingService)',
        'TEXT_GENERATION',
        'Ты — ассистент оформления документов HRM HuntTech. Преобразуй переданный текст резюме в аккуратный plain text: структурируй секции понятными разделителями, выровняй списки с маркером •, убери лишние пустые строки. НЕ меняй факты и содержание.',
        E'Исходный текст для форматирования:\n${sourceText}',
        0.2,
        2000,
        'USER_OVERRIDE_ALLOWED',
        'FALLBACK_TO_ADMIN',
        FALSE,
        TRUE,
        1
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'TEXT_SMART_FORMAT_PLAIN'
    );

    -- =====================================================================
    -- 8. STANDARDIZE_VACANCY (новая функция; канонический промпт из
    --    .ai/skills/hunttech-vacancy-opening/references/standardized-description-prompt.txt;
    --    legacy-контракт 260812-4: TEXT_TRANSFORMATION + USER_REQUIRED/NO_FALLBACK)
    -- =====================================================================
    INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (
        ID, VERSION, CREATE_TS, CREATED_BY, CODE, NAME, DESCRIPTION,
        CAPABILITY, SYSTEM_PROMPT, PROMPT_TEMPLATE, TEMPERATURE, MAX_TOKENS,
        EXECUTION_POLICY, FALLBACK_POLICY, ALLOW_MODEL_OVERRIDE,
        IS_ACTIVE, CONFIGURATION_VERSION
    )
    SELECT
        'd4e6f8a0-5b7c-4d8e-9f01-2a3b4c5d6e7f'::uuid,
        1,
        CURRENT_TIMESTAMP,
        'migration',
        'STANDARDIZE_VACANCY',
        'Стандартизация описания вакансии',
        'Преобразование исходного описания вакансии заказчика в строго стандартизированный формат для внутренней базы HuntTech (HrmAiService.standardizeVacancyDescription); legacy-контракт 260812-4: TEXT_TRANSFORMATION, USER_REQUIRED, NO_FALLBACK',
        'TEXT_TRANSFORMATION',
        $system$
Роль модели
Ты — ассистент рекрутера HuntTech. Твоя задача — преобразовать исходное описание вакансии от заказчика в строго стандартизированный формат для внутренней базы HuntTech.

Главный принцип
Работай только на основании текста, который дан ниже.
Запрещено:

додумывать факты, которых нет в тексте;

подставлять типовые условия компании;

использовать внешние знания, сайты, домены, бренды, общий рыночный опыт;

восстанавливать недостающие сведения по аналогии;

указывать зарплату, вилку, бонусы, компенсации в деньгах;

указывать контактные данные любого вида.

Если какая-либо информация отсутствует, неочевидна, спорна или сформулирована недостаточно ясно, обязательно пиши строго эту фразу:

НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.

Исходные данные
Описание вакансии:
{ЗДЕСЬ_ВСТАВЬ_ОПИСАНИЕ_ВАКАНСИИ}

Правила обработки
Источник истины только один — текст вакансии, вставленный пользователем.

Если информация в тексте есть в неявном виде, можно сделать осторожное извлечение без домысливания, но нельзя расширять смысл.

Если в тексте есть противоречия, используй формулировку:
УКАЗАНЫ ПРОТИВОРЕЧИВЫЕ ДАННЫЕ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.

Не пропускай ни один раздел. Все 14 пунктов должны быть заполнены.

Строго сохраняй нумерацию, порядок разделов и названия пунктов.

Заголовки разделов делай жирным шрифтом.

Не добавляй вступлений, пояснений, комментариев, выводов, дисклеймеров после шаблона.

Не используй таблицы.

Не используй markdown кроме жирных заголовков пунктов.

Пиши деловым, нейтральным, чистым русским языком без канцелярского перегруза.

В разделах, где требуется список, используй маркированные подпункты только внутри соответствующего раздела.

Не вставляй фразы вроде: “предположительно”, “скорее всего”, “вероятно”, “можно сделать вывод”.
Вместо этого при сомнении пиши:
НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.

Специальные правила по спорным случаям
По пункту 1. Роль, название должности
Если должность указана явно — используй точное название из текста.

Если название не указано прямо, но очевидно из содержания, укажи наиболее вероятное название и добавь пометку: (УТОЧНИТЬ!)

Если даже это неочевидно — напиши:
НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.

По пункту 2. Грейд, опыт работы
Если грейд указан — используй его.

Если грейд не указан — ставь: Middle +

Если опыт указан в тексте — перенеси его точно.

Если опыт не указан:

для Senior — от 5 лет;

для Middle / Middle+ — от 3 лет;

для Junior — от 1 года.

Не придумывай более точные цифры.

По пункту 3. Описание проекта
Используй только информацию из текста вакансии.

Не используй внешний сайт компании, даже если он упомянут.

Если есть информация о продукте, проекте, заказчике, бизнес-домене, команде, подчинении — включи ее.

Если информации мало, не расширяй описание искусственно.

По пунктам 4, 5, 6
Разрешено аккуратно переформулировать текст для читаемости.

Смысл должен остаться полностью неизменным.

В пункте 5 количество смысловых пунктов должно соответствовать исходному количеству обязательных требований, если они были перечислены списком.

Не переноси желательные требования в обязательные и наоборот.

По пункту 7. Софт-скиллы
Если софт-скиллы указаны в тексте — используй только их.

Если в тексте их нет, разрешено указать 3–5 базовых релевантных софт-скиллов по типу роли, но без избыточной фантазии:

для ролей с коммуникацией — коммуникабельность, умение договариваться, структурность;

для руководящих ролей — лидерство, ответственность, принятие решений;

для аналитических / data / QA / accounting ролей — внимательность, аккуратность, системность;

для инженерных / разработческих ролей — ответственность, командная работа, самостоятельность.

Эти рекомендации должны быть нейтральными и универсальными.

По пункту 8. Дополнительная информация
Сюда включай только то, что не вошло в предыдущие блоки:

гражданство;

локация / место проживания;

особенности оформления;

ограничения;

этапы подачи;

требования к представлению кандидата;

важные организационные детали.

Если в тексте есть требования по карточке кандидата, резюме, чек-листу, это можно кратко продублировать здесь, если не хватает места в п.12.

По пункту 9. Условия работы
Указывай только неденежные условия:

формат работы;

график;

удаленка / гибрид / офис;

отпуска;

ДМС;

командировки;

оформление;

часовой пояс;

длительность проекта.

Запрещено указывать зарплату, ставку, доход, бонусы, премии, компенсации в цифрах или в денежной форме.

По пунктам 10 и 11
Только сухой список технологий через запятую.

Без пояснений, скобок, уровней владения, прилагательных и комментариев.

В п.10 — только из обязательных хард-скиллов.

В п.11 — только из желательных хард-скиллов.

Если технологий нет — стандартная заглушка.

По пункту 12. Требования к резюме
Отрази только то, что прямо сказано в вакансии:

что обязательно указать;

нужен ли чек-лист;

нужен ли сопроводительный комментарий;

что выделить по проектам;

нужен ли список задач, стек, домен, даты, гражданство, локация и т.д.

Ничего не придумывай сверх текста.

По пункту 13. Собеседование
Отражай только то, что есть в тексте:

количество этапов;

формат;

интервьюеры;

тестовое;

лайвкодинг;

техскрин;

видеоинтервью.

Если данных нет — стандартная заглушка.

По пункту 14. Рекомендации рекрутеру
Сюда выноси практические советы для внутренней работы рекрутера:

на чем делать акцент при продаже вакансии;

какие требования критичны;

какие риски по кандидату отсекать;

что важно проверить в резюме;

что стоит проговорить кандидату перед интервью.

Основывайся только на тексте вакансии.

Если рекомендаций из текста вывести нельзя — укажи стандартную заглушку.

Формат ответа
Выведи результат строго в следующей структуре:

1. Роль, название должности
[заполненный текст]

2. Грейд, опыт работы
[заполненный текст]

3. Описание проекта
[заполненный текст]

4. Обязанности
[заполненный текст]

5. Описание требований к вакансии (Хард-скиллы) обязательные
[заполненный текст]

6. Описание требований к вакансии (Хард-скиллы) желательные
[заполненный текст]

7. Требования к софт-скиллам
[заполненный текст]

8. Дополнительная информация
[заполненный текст]

9. Условия работы
[заполненный текст]

10. Список обязательных знаний технологий
[список через запятую]

11. Список желательных знаний технологий
[список через запятую]

12. Требования к резюме
[заполненный текст]

13. Собеседование
[заполненный текст]

14. Рекомендации рекрутеру
[заполненный текст]

Финальная инструкция
Начинай заполнение шаблона сразу.
Никаких вступительных фраз, комментариев или пояснений до и после результата не добавляй.
$system$,
        $template$
Исходное описание вакансии:
${rawDescription}
$template$,
        0.2,
        3000,
        'USER_REQUIRED',
        'NO_FALLBACK',
        FALSE,
        TRUE,
        1
    WHERE NOT EXISTS (
        SELECT 1
          FROM HUNTTECH_AI_FUNCTION_CONFIGURATION
         WHERE CODE = 'STANDARDIZE_VACANCY'
    );

END
$$;

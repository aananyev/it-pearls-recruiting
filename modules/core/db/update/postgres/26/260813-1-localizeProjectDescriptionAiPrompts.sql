-- HRM HuntTech: локальная миграция канонических русских административных prompt
-- для фактически используемой AI-функции PROJECT_DESCRIPTION_GENERATE.
-- Существующая пользовательская русская настройка администратора не перезаписывается.

DO $$
BEGIN
    IF to_regclass('public.hunttech_ai_function_configuration') IS NULL THEN
        RAISE EXCEPTION
            'AI Control Plane не мигрирован: отсутствует HUNTTECH_AI_FUNCTION_CONFIGURATION';
    END IF;

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
END
$$;

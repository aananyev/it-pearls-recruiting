-- 260920-3-setOpenRouterGatewayBaseApiUrl.sql
-- Настройка URL локального реверс-прокси шлюза для OpenRouter Nemotron и привязка функции фонового анализа

UPDATE HUNTTECH_ADMIN_AI_CONFIGURATION
   SET BASE_API_URL = 'http://127.0.0.1:8119/api/v1/chat/completions',
       LAST_ERROR = NULL,
       LAST_TEST_STATUS = 'SUCCESS',
       LAST_TEST_AT = CURRENT_TIMESTAMP,
       UPDATE_TS = CURRENT_TIMESTAMP,
       UPDATED_BY = 'migration'
 WHERE PROVIDER_CODE = 'openrouter'
   AND DELETE_TS IS NULL;

UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
   SET ADMIN_CONFIGURATION_ID = (SELECT ID FROM HUNTTECH_ADMIN_AI_CONFIGURATION WHERE PROVIDER_CODE = 'openrouter' AND DELETE_TS IS NULL LIMIT 1),
       ADMIN_MODEL_NAME = 'nvidia/nemotron-3-ultra-550b-a55b:free',
       UPDATE_TS = CURRENT_TIMESTAMP,
       UPDATED_BY = 'migration'
 WHERE CODE = 'SKILLS_EXTRACT_BACKGROUND'
   AND DELETE_TS IS NULL;

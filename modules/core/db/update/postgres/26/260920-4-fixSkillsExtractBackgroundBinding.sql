-- 260920-4-fixSkillsExtractBackgroundBinding.sql
-- Исправление привязки функции SKILLS_EXTRACT_BACKGROUND к рабочему корпоративному подключению B.AI GLM 5.2
-- Проблема: миграция 260920-3 перезаписала привязку на несуществующий OpenRouter (subquery возвращает NULL)
-- Решение: привязываем к существующей записи B.AI GLM 5.2 (ID e1a2b3c4-0001-4000-8000-000000000001), у которой freeModel=true

-- 1. Привязываем функцию SKILLS_EXTRACT_BACKGROUND к B.AI GLM 5.2
UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
   SET ADMIN_CONFIGURATION_ID = 'e1a2b3c4-0001-4000-8000-000000000001',
       ADMIN_MODEL_NAME = 'glm-5.2',
       UPDATE_TS = CURRENT_TIMESTAMP,
       UPDATED_BY = 'migration'
 WHERE CODE = 'SKILLS_EXTRACT_BACKGROUND'
   AND DELETE_TS IS NULL;

-- 2. (Опционально) Включаем фоллбэк на корпоративные конфиги для устойчивости
-- При FALLBACK_TO_ADMIN сервис попробует другие активные freeModel=true конфиги при ошибке основного
UPDATE HUNTTECH_AI_FUNCTION_CONFIGURATION
   SET FALLBACK_POLICY = 'FALLBACK_TO_ADMIN',
       UPDATE_TS = CURRENT_TIMESTAMP,
       UPDATED_BY = 'migration'
 WHERE CODE = 'SKILLS_EXTRACT_BACKGROUND'
   AND DELETE_TS IS NULL;

-- 3. Проверка: подтверждаем, что привязка корректна
DO $$
DECLARE
    v_admin_config_id UUID;
    v_admin_model_name TEXT;
    v_fallback_policy TEXT;
    v_provider_code TEXT;
    v_is_free_model BOOLEAN;
BEGIN
    SELECT f.ADMIN_CONFIGURATION_ID, f.ADMIN_MODEL_NAME, f.FALLBACK_POLICY
      INTO v_admin_config_id, v_admin_model_name, v_fallback_policy
      FROM HUNTTECH_AI_FUNCTION_CONFIGURATION f
     WHERE f.CODE = 'SKILLS_EXTRACT_BACKGROUND'
       AND f.DELETE_TS IS NULL;

    IF v_admin_config_id IS NULL THEN
        RAISE EXCEPTION 'SKILLS_EXTRACT_BACKGROUND: ADMIN_CONFIGURATION_ID всё ещё NULL после исправления';
    END IF;

    SELECT c.PROVIDER_CODE, c.IS_FREE_MODEL
      INTO v_provider_code, v_is_free_model
      FROM HUNTTECH_ADMIN_AI_CONFIGURATION c
     WHERE c.ID = v_admin_config_id;

    RAISE NOTICE 'SKILLS_EXTRACT_BACKGROUND привязана к: provider_code=%, freeModel=%, adminModelName=%, fallbackPolicy=%',
        v_provider_code, v_is_free_model, v_admin_model_name, v_fallback_policy;

    IF v_provider_code <> 'bai' THEN
        RAISE WARNING 'Ожидался provider_code=bai (B.AI), получен: %', v_provider_code;
    END IF;
    IF NOT v_is_free_model THEN
        RAISE WARNING 'Ожидался freeModel=true, получен: %', v_is_free_model;
    END IF;
END $$;
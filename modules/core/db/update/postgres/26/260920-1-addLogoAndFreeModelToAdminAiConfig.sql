-- Добавление полей логотипа (BLOB/bytea) и признака бесплатной модели в AdminAiConfiguration
ALTER TABLE HUNTTECH_ADMIN_AI_CONFIGURATION ADD COLUMN IF NOT EXISTS LOGO_IMAGE bytea;
ALTER TABLE HUNTTECH_ADMIN_AI_CONFIGURATION ADD COLUMN IF NOT EXISTS IS_FREE_MODEL boolean DEFAULT false;

-- Инициализация признака бесплатной модели
UPDATE HUNTTECH_ADMIN_AI_CONFIGURATION
   SET IS_FREE_MODEL = false
 WHERE IS_FREE_MODEL IS NULL;

-- Автоматическая маркировка бесплатных моделей (OpenRouter free tier и аналогичные)
UPDATE HUNTTECH_ADMIN_AI_CONFIGURATION
   SET IS_FREE_MODEL = true
 WHERE (DEFAULT_MODEL_NAME LIKE '%:free' OR NAME ILIKE '%free%');

-- Добавление полей логотипа и признака бесплатной модели в AdminAiConfiguration (HSQL)
ALTER TABLE HUNTTECH_ADMIN_AI_CONFIGURATION ADD COLUMN LOGO_IMAGE longvarbinary;
ALTER TABLE HUNTTECH_ADMIN_AI_CONFIGURATION ADD COLUMN IS_FREE_MODEL boolean DEFAULT false;

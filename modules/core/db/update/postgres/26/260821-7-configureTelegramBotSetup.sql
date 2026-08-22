-- HRM HuntTech: Активация Telegram бота для активной конфигурации при наличии токена
UPDATE HUNTTECH_APPLICATION_SETUP
   SET TELEGRAM_BOT_START = true
 WHERE ACTIVE_SETUP = true
   AND TELEGRAM_BOT_START IS NULL
   AND NULLIF(TRIM(TELEGRAM_TOKEN), '') IS NOT NULL;

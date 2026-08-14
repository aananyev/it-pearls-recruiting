-- Нормализует существующие настройки: у каждого пользователя остаётся только
-- одна текущая AI-конфигурация (самая недавно изменённая активная строка).
WITH ranked_current AS (
    SELECT ID,
           row_number() OVER (
               PARTITION BY USER_ID
               ORDER BY UPDATE_TS DESC NULLS LAST, CREATE_TS DESC NULLS LAST, ID
           ) AS rn
    FROM HUNTTECH_USER_AI_CONFIGURATION
    WHERE IS_ACTIVE = TRUE
      AND DELETE_TS IS NULL
)
UPDATE HUNTTECH_USER_AI_CONFIGURATION configuration
SET IS_ACTIVE = FALSE
FROM ranked_current ranked
WHERE configuration.ID = ranked.ID
  AND ranked.rn > 1;

-- Гарантирует инвариант «один пользователь — одна текущая нейросеть»
-- с учётом soft delete CUBA.
CREATE UNIQUE INDEX IF NOT EXISTS IDX_HUNTTECH_USER_AI_CFG_ONE_CURRENT
    ON HUNTTECH_USER_AI_CONFIGURATION (USER_ID)
    WHERE IS_ACTIVE = TRUE AND DELETE_TS IS NULL;

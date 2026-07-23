-- Добавляет персональное предпочтение источника API без изменения алгоритма маршрутизации вызовов.
-- Для существующих пользователей значение false сохраняет текущее поведение сервисов HRM HuntTech.

ALTER TABLE HUNTTECH_USER_SETTINGS
    ADD COLUMN IF NOT EXISTS PREFER_PERSONAL_AI_API_SETTINGS BOOLEAN NOT NULL DEFAULT FALSE;

# Checkpoint: локальное восстановление AI-подбора вакансий

**Дата:** 2026-09-21
**Статус:** локальная реализация и проверки завершены
**Область:** только локальная БД и локальное приложение; production не затрагивается.

## Цель

Перевести корпоративный маршрут функции CANDIDATE_VACANCY_MATCH_ANALYZE на рабочий DeepSeek и проверить локальный запуск приложения.

## Предварительная сверка

- Локальная БД PostgreSQL hunttech доступна.
- DeepSeek-конфигурация существует, активна и имеет приоритет 300.
- Функция подбора вакансий уже привязана к DeepSeek (deepseek-v4-flash) с политикой USER_OVERRIDE_ALLOWED и FALLBACK_TO_ADMIN.
- Для пользователя alan локальный DeepSeek активен; OpenRouter отключён; override именно для функции подбора не найден.
- Текущая локальная функция уже направлена на активный DeepSeek `deepseek-v4-flash`; changeset 260921-1 зарегистрирован в `sys_db_changelog`.
- Рабочая ветка синхронизирована с origin/master на d5fa32e8.

## Изменение

Миграция направляет только функцию CANDIDATE_VACANCY_MATCH_ANALYZE с ожидаемого OpenRouter/Nemotron на единственную активную credentialed DeepSeek-конфигурацию. При отсутствии таблиц, credentialed DeepSeek или ожидаемого маршрута Liquibase останавливает миграцию с HALT; состояние нужно исправить до запуска. Пользовательские секреты не копируются и не перезаписываются. Rollback хранится отдельным SQL-файлом, валидирует UUID маркера и восстанавливает прежний ID OpenRouter и точное значение модели функции. Если маркера нет, rollback выводит NOTICE и не меняет маршрут, который уже был настроен до changeset.

## План отката

Перед отдельным production rollout сохранить снимок затрагиваемых строк HUNTTECH_ADMIN_AI_CONFIGURATION, HUNTTECH_AI_FUNCTION_CONFIGURATION и HUNTTECH_USER_AI_CONFIGURATION. Откатить восстановлением снимка вместе с Liquibase history. Production backup, rollout и сверка с production выполняются отдельно после требуемых разрешений.

## Чекпоинты

- [x] OCR review завершён по миграции и changelog; замечания устранены.
- [x] Пять целевых классов тестов миграции, vacancy match и AI credentials пройдены.
- [x] Changeset зарегистрирован в локальном `sys_db_changelog`; актуальный SQL-блок проверен на PostgreSQL в транзакции с последующим `ROLLBACK`.
- [ ] Приложение не перезапускалось: Java-код не менялся, текущий health check возвращает HTTP 200; branch-деплой не выполнялся из-за штатного migration guard.

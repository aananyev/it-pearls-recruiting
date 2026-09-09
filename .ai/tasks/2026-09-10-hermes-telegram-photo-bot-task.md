# Задание для Hermes: Разработка микросервиса Telegram Avatar Bot

## Дата постановки: 2026-09-10
## Инициатор: Antigravity (поток разработки №2)
## Исполнитель: Hermes (CI/CD и микросервисы)
## Статус: WAITING_FOR_HERMES

### Описание задания
Для обеспечения автоматической загрузки фотографий профиля из Telegram по username кандидатов и персон в HRM HuntTech (экран `PersonEdit` и смежные экраны), требуется реализовать отдельный легковесный микросервис `hunttech-telegram-avatar-service`.

Подробная спецификация и технический промпт подготовлены в файле:
[2026-09-10-telegram-avatar-bot-prompt.md](file:///Users/alekseyananyev/StudioProjects/hrm-antigravity/.ai/prompts/2026-09-10-telegram-avatar-bot-prompt.md)

### Основные требования к реализации
1. Создать репозиторий / каталог микросервиса (например, в каталоге `services/telegram-avatar-service` или отдельном проекте).
2. Реализовать HTTP-эндпоинт:
   `GET /avatar/{identifier}` — возвращает бинарные данные `image/jpeg` или `image/png` аватара пользователя по `username` или `user_id`.
3. Реализовать клиент Telegram MTProto (на базе Python `telethon` / `pyrogram`) для поиска пользователей по username без ограничений стандартного Bot API.
4. Добавить локальное дисковое кэширование изображений с TTL 24 часа.
5. Подготовить `Dockerfile` и `docker-compose.yml` для запуска микросервиса на порту `8088`.
6. После развертывания передать URL сервиса для включения в `hunttech.telegram.avatarServiceUrl` в `app.properties`.

### Совместимость с текущей версией HRM HuntTech
В Java-коде HRM HuntTech (`TelegramIntegrationServiceBean`) уже реализован двухуровневый механизм получения аватара:
- Уровень 1: Прямое получение публичного аватара из web-профиля Telegram (`https://t.me/<username>`).
- Уровень 2: Обращение к микросервису Hermes по адресу `hunttech.telegram.avatarServiceUrl` (если настроен).
Поэтому базовая функциональность для большинства пользователей уже работает в HRM автономно, а микросервис расширит охват на приватные профили.

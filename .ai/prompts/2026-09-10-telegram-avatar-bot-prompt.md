# Промпт для субагента Hermes: Разработка микросервиса / Telegram-бота для получения аватарок пользователей

## Роль: Python Backend-разработчик / Инженер интеграций Telegram

### Цель задания
Разработать легкий микросервис на Python (FastAPI + Telethon / Pyrogram или aiogram), который позволяет HRM-системе HuntTech получать актуальную фотографию профиля пользователя Telegram по его `username` или числовому `user_id`, включая профили, недоступные через стандартный Bot API `getChat`.

---

### Контекст и архитектура взаимодействия
1. **Проблема стандартного Telegram Bot API**:
   - Метод `getChat` у обычных ботов не может резолвить приватных пользователей по `@username`, если пользователь предварительно не запустил бота (`/start`) или не состоит в общем чате с ботом.
2. **Решение (Микросервис)**:
   - Микросервис работает как HTTP REST API сервер (на базе FastAPI / Uvicorn).
   - Для резолвинга любых пользователей по `@username` и скачивания их аватаров используется MTProto клиент (Telethon или Pyrogram) под аккаунтом бота или специального сервис-аккаунта (Client API с `api_id` и `api_hash`).
   - Дополнительно микросервис может поддерживать Telegram Bot API токен для пользователей, запустивших бота через `/start`.

---

### Спецификация REST API

#### 1. `GET /health`
- **Описание**: Проверка работоспособности сервиса.
- **Ответ (200 OK)**:
  ```json
  {
    "status": "ok",
    "service": "hunttech-telegram-avatar-service",
    "timestamp": "2026-09-10T12:00:00Z"
  }
  ```

#### 2. `GET /avatar/{identifier}`
- **Параметры пути**:
  - `identifier`: username пользователя (с символом `@` или без, например `durov` или `@durov`), либо числовой `user_id`.
- **Параметры запроса (Query params)**:
  - `redirect` (boolean, default: `false`): если `true` — возвращает HTTP 302 редирект на прямую ссылку CDN; если `false` — отдает бинарные данные картинки (`image/jpeg`).
- **Ответ при успехе (200 OK)**:
  - `Content-Type`: `image/jpeg` или `image/png`
  - Body: бинарные байты фотографии профиля высокого разрешения.
  - Headers:
    - `X-Telegram-User-Id`: числовой ID пользователя
    - `X-Telegram-Username`: username
- **Ответ, если фото отсутствует (404 Not Found)**:
  ```json
  {
    "error": "photo_not_found",
    "message": "User has no profile photo or photo is hidden by privacy settings"
  }
  ```
- **Ответ, если пользователь не найден (404 Not Found)**:
  ```json
  {
    "error": "user_not_found",
    "message": "Telegram user '@...' does not exist"
  }
  ```

#### 3. `GET /user/{identifier}`
- **Описание**: Получение информации о пользователе в формате JSON.
- **Ответ (200 OK)**:
  ```json
  {
    "id": 123456789,
    "username": "johndoe",
    "first_name": "John",
    "last_name": "Doe",
    "has_photo": true,
    "photo_url": "/avatar/johndoe"
  }
  ```

---

### Требования к реализации
1. **Стек**:
   - Python 3.10+
   - FastAPI, Uvicorn
   - Telethon (или Pyrogram) для прямого резолвинга сущностей по username через MTProto
   - Pillow (для оптимизации и проверки формата при необходимости)
   - Cachetools (кэширование аватаров в памяти / на диске на 1–24 часа, чтобы не спамить Telegram API и не ловить `FloodWaitError`).
2. **Конфигурация (через `.env` / переменные окружения)**:
   ```env
   PORT=8088
   HOST=0.0.0.0
   TELEGRAM_API_ID=1234567
   TELEGRAM_API_HASH=abcdef1234567890abcdef1234567890
   TELEGRAM_BOT_TOKEN=123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11
   AVATAR_CACHE_DIR=/var/cache/telegram-avatars
   AVATAR_CACHE_TTL_SECONDS=86400
   ```
3. **Обработка ошибок и Rate Limiting**:
   - Корректный перехват `FloodWaitError`: логирование и возвращение `503 Service Unavailable` с заголовком `Retry-After`.
   - Защита от зависаний (таймауты на запросы к Telegram не более 10 секунд).
4. **Развертывание**:
   - `Dockerfile` для сборки минимального контейнера.
   - `docker-compose.yml` для локального запуска рядом с Tomcat HRM HuntTech.

---

### Интеграция с HRM HuntTech
В конфигурации `app.properties` HRM HuntTech указывается:
```properties
hunttech.telegram.avatarServiceUrl = http://127.0.0.1:8088/avatar/%s
```
HRM HuntTech делает HTTP GET по этому адресу, получает байты изображения и сохраняет их в `FileDescriptor` кандидата или персоны.

# Локальная PostgreSQL для IT-Pearls Recruiting

Проект — приложение на **CUBA Platform 7.3** с **PostgreSQL** и JNDI-источником данных `jdbc/CubaDS`.

## Параметры подключения (локальная разработка)

| Параметр | Значение |
|----------|----------|
| Хост | `localhost` |
| Порт | `5432` |
| База данных | `itpearls` |
| Пользователь | `cuba` |
| Пароль | `cuba` |
| JDBC URL | `jdbc:postgresql://localhost:5432/itpearls` |

Файлы с настройками подключения:

- `modules/core/web/META-INF/context.xml` — Tomcat / тесты
- `modules/core/web/META-INF/war-context.xml` — сборка WAR
- `modules/core/web/META-INF/jetty-env.xml` — запуск из IDE (Jetty)
- `build.gradle` — задачи `createDb` / `updateDb`
- `modules/core/src/com/company/itpearls/app.properties` — `cuba.dbmsType=postgres`

Переменные окружения для Docker: `.env.example` → скопируйте в `.env.local`.

## Запуск PostgreSQL

### Вариант 1: Homebrew PostgreSQL 11 (macOS, уже используется в проекте)

```bash
# Установка (если ещё не установлено)
brew install postgresql@11

# Запуск / остановка / статус
./start-postgres11.sh start
./start-postgres11.sh stop
./start-postgres11.sh status
```

Данные: `/usr/local/var/postgresql@11`

### Вариант 2: Docker

```bash
cp .env.example .env.local   # при необходимости отредактируйте
docker compose up -d
docker compose ps
```

## Первичная настройка базы

### 1. Создать пользователя и базу (если их ещё нет)

```bash
chmod +x scripts/setup-local-postgres.sh
./scripts/setup-local-postgres.sh
```

Или вручную от суперпользователя `postgres`:

```sql
CREATE USER cuba WITH PASSWORD 'cuba' CREATEDB;
CREATE DATABASE itpearls OWNER cuba ENCODING 'UTF8';
```

### 2. Создать схему и начальные данные CUBA

```bash
./gradlew createDb
```

### 3. Применить миграции

```bash
./gradlew updateDb
```

Скрипты миграций: `modules/core/db/update/postgres/`  
Начальные скрипты: `modules/core/db/init/postgres/`

## Проверка подключения

```bash
psql -h localhost -p 5432 -U cuba -d itpearls -c "SELECT version();"
```

Или через скрипт проекта:

```bash
./start-postgres11.sh status
```

## Запуск приложения

Рекомендуемый способ (PostgreSQL + очистка зависшего Tomcat + deploy): `./scripts/start-app.sh`

```bash
./gradlew setupTomcat
./gradlew deploy
./gradlew start
```

Приложение: http://localhost:8080/app

## Загрузка данных с продакшена (опционально)

Скрипт `get_base.sh` загружает base backup с удалённого сервера в локальный каталог PostgreSQL 11. Используйте только при необходимости полной копии данных.

**Важно:** после base backup PostgreSQL может оказаться в режиме recovery (`pg_is_in_recovery() = true`), и миграции `./gradlew updateDb` не выполнятся (read-only). Для разработки с миграциями создайте чистую базу:

```bash
./start-postgres11.sh stop
# переименуйте или удалите /usr/local/var/postgresql@11 и инициализируйте заново:
# initdb /usr/local/var/postgresql@11
./start-postgres11.sh start
./scripts/setup-local-postgres.sh
./gradlew createDb
./gradlew updateDb
```

## Текущая локальная конфигурация (2026-06-22)

- **Подход:** свежий кластер **Homebrew PostgreSQL 11** (`initdb`), порт `5432` — Docker на машине недоступен.
- **Причина:** прежний каталог данных был **standby-репликой** (`recovery.conf`, `pg_is_in_recovery() = true`, read-only).
- **Резервная копия старого каталога:** `/usr/local/var/postgresql@11-standby-replica-backup-20260622` (можно удалить, если реплика больше не нужна).
- **Проверка:** `psql -h localhost -p 5432 -U cuba -d itpearls -c "SELECT pg_is_in_recovery();"` → `f`.

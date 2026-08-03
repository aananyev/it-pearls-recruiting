# Локальная PostgreSQL для HRM HuntTech

Проект — приложение на **CUBA Platform 7.3** с **PostgreSQL** и JNDI-источником данных `jdbc/CubaDS`.

## Параметры подключения (локальная разработка)

| Параметр | Значение |
|----------|----------|
| Хост | `localhost` |
| Порт | `5432` |
| База данных | `HuntTech` |
| Пользователь | `cuba` |
| Пароль | `cuba` |
| JDBC URL | `jdbc:postgresql://localhost:5432/HuntTech` |

Файлы с настройками подключения:

- `modules/core/web/META-INF/context.xml` — Tomcat / тесты
- `modules/core/web/META-INF/war-context.xml` — сборка WAR
- `modules/core/web/META-INF/jetty-env.xml` — запуск из IDE (Jetty)
- `build.gradle` — задачи `createDb` / `updateDb`
- `modules/core/src/com/company/hunttech/app.properties` — `cuba.dbmsType=postgres`

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
CREATE DATABASE HuntTech OWNER cuba ENCODING 'UTF8';
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

## Автоматическая синхронизация схемы при локальном запуске

Штатный сценарий `./scripts/start-app.sh` выполняет `./gradlew updateDb --no-daemon --stacktrace` после остановки локального Tomcat и до `clean deploy`.

Это обязательный порядок:

```text
PostgreSQL готов → Tomcat остановлен → updateDb → clean deploy → start
```

Такой порядок не позволяет развернуть новую entity-модель поверх устаревшей схемы. В частности, миграция `26/260723-1-addPreferPersonalAiApiSettings.sql` должна создать колонку `HUNTTECH_USER_SETTINGS.PREFER_PERSONAL_AI_API_SETTINGS` до загрузки `UserSettings` в `SettingsWindow`.

Если `updateDb` завершается ошибкой, `start-app.sh` прекращает запуск. Ошибку миграции необходимо устранить до deploy; обходить её запуском приложения на старой схеме запрещено.

## Проверка подключения

```bash
psql -h localhost -p 5432 -U cuba -d HuntTech -c "SELECT version();"
```

Или через скрипт проекта:

```bash
./start-postgres11.sh status
```

## Запуск приложения

Рекомендуемый способ (PostgreSQL + миграции + очистка зависшего Tomcat + deploy): `./scripts/start-app.sh`

```bash
./gradlew setupTomcat
./gradlew deploy
./gradlew start
```

Приложение: http://localhost:8080/app

## Загрузка данных с продакшена (опционально)

Скрипт `get_base.sh` загружает base backup с удалённого сервера в локальный каталог PostgreSQL 11. Используйте только при необходимости полной копии данных.

Ключи скрипта (2026-08-03):

| Ключ | Действие |
|------|----------|
| (без ключей) | Полная загрузка: база + fileStorage |
| `--db-only` | Только база (PostgreSQL base backup), без fileStorage |
| `--files-only` | Только файлы fileStorage (rsync, новые/изменённые), база не трогается |
| `--check` | Проверка подключения к удалённому серверу (PostgreSQL 5432 + SSH root@), без загрузки данных |
| `restart-db` (`-r`) | Перезапуск локальной PostgreSQL 11 (pg_ctl stop/start + проверка primary) |
| `check-db` (`-l`) | Проверка локальной PostgreSQL: статус, версия, recovery, список БД, размер кластера |
| `help` (`-h`) | Справка по функционалу и ключам |

Локальный `fileStorage` — симлинк на `/opt/app_home/fileStorage`; rsync докачивает только новые и изменённые файлы (без `--ignore-existing`).

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
- **Проверка:** `psql -h localhost -p 5432 -U cuba -d HuntTech -c "SELECT pg_is_in_recovery();"` → `f`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-03 | Миграция прода (hr.hunttech.ru/hunttech): применены 10 чанжсетов 26/ (перф-индексы, ИИ-профиль, AI-настройки, reconcile схемы, маржинальность рейтов); согласован `sys_db_changelog` (префикс `70-IT-Pearls/` → `70-hunttech_recruiting/`, 756 записей); исправлен баг в `260727-2-reconcileProductionSchema.sql` (висячий символ `^` в конце файла ломал psql) |
| 2026-08-03 | get_base.sh: добавлены ключи `help`, `restart-db` (перезапуск локальной PG), `check-db` (проверка локальной PG); ранее — `--db-only`, `--files-only`, `--check`; фикс validate fileStorage (BSD find + симлинк); rsync без `--ignore-existing` |
| 2026-07-23 | Штатный локальный запуск дополнен обязательным `updateDb` до deploy, чтобы исключить ошибки отсутствующих колонок при загрузке экранов |

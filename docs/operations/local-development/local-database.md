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

## Быстрый деплой XML-правок (без перекомпиляции Java)

Когда меняются **только XML-дескрипторы экранов** (layout, подписи, bindings) и Java-контроллеры не затронуты, полная пересборка не нужна: таски компиляции остаются UP-TO-DATE, и цикл «скопировать + перезапустить» занимает ~20–30 с.

Механика (эмпирика 2026-08-06, HRM HuntTech):

1. CUBA грузит screen-XML из `deploy/app_home/app/conf/com/company/hunttech/web/screens/...` — **не из** `WEB-INF/classes`. `./gradlew deploy` / `restart` этот XML **не копирует** — только ручной `cp`.
2. Детерминированный порядок (без гонок между стартом и копированием):

   ```bash
   pkill -f tomcat            # при зависании: pgrep -f catalina | xargs kill -9
   cp modules/web/build/resources/main/com/company/hunttech/web/screens/{экран}/{screen}.xml \
      deploy/app_home/app/conf/com/company/hunttech/web/screens/{экран}/
   ./gradlew restart --no-daemon
   ```

3. **Критично:** screen-XML читается при старте Tomcat. Если `cp` выполнен ПОСЛЕ старта — форма рендерит старый layout, и нужен повторный `./gradlew restart`. Горячего подхвата XML без перезагрузки Tomcat **нет**.
4. Проверка подхвата — сверить время старта процесса и время `cp` (процесс обязан стартовать ПОСЛЕ копирования):

   ```bash
   ps -p $(pgrep -f catalina | head -1) -o lstart=    # время старта Tomcat
   stat -f '%Sm' deploy/app_home/app/conf/.../{screen}.xml   # время cp
   grep -c '<новый-id/ключ>' deploy/app_home/app/conf/.../{screen}.xml
   ```

5. Смоук после перезапуска: `curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/hrm/` → 200; widgetset `.nocache.js` → 200; свежих `ERROR`/`Exception` в `deploy/tomcat/logs/catalina.out` нет (известные безобидные: NPE в Emailer, `ObjectStreamClass` на stop-фазе).

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
| 2026-08-06 | Добавлен раздел «Быстрый деплой XML-правок (без перекомпиляции Java)»: screen-XML копируется вручную в `deploy/app_home/app/conf` (deploy его не обновляет), читается при старте Tomcat, горячего подхвата без перезагрузки нет; детерминированный порядок pkill → cp → `./gradlew restart` и проверка временем старта процесса |
| 2026-08-03 | Миграция прода (hr.hunttech.ru/hunttech): применены 10 чанжсетов 26/ (перф-индексы, ИИ-профиль, AI-настройки, reconcile схемы, маржинальность рейтов); согласован `sys_db_changelog` (префикс `70-IT-Pearls/` → `70-hunttech_recruiting/`, 756 записей); исправлен баг в `260727-2-reconcileProductionSchema.sql` (висячий символ `^` в конце файла ломал psql) |
| 2026-08-03 | get_base.sh: добавлены ключи `help`, `restart-db` (перезапуск локальной PG), `check-db` (проверка локальной PG); ранее — `--db-only`, `--files-only`, `--check`; фикс validate fileStorage (BSD find + симлинк); rsync без `--ignore-existing` |
| 2026-07-23 | Штатный локальный запуск дополнен обязательным `updateDb` до deploy, чтобы исключить ошибки отсутствующих колонок при загрузке экранов |

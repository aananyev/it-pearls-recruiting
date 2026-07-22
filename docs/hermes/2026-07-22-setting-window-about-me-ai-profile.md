# Задание Hermes: локальная проверка рефакторинга ExtSettingsWindow

> Проект: **HRM HuntTech**  
> Ветка: `agent/settings-window-hunttech-refactor`  
> Среда: **строго локальный компьютер**  
> Локальная БД: `hunttech`  
> Production: **строго запрещён**  
> Дата: `2026-07-22`

## 1. Цель

Проверить точный HEAD ветки, выполнить обязательные тесты, создать проверенный backup локальной БД `hunttech`, применить локальный `updateDb`, запустить локальные блоки `core` и `web`, провести functional smoke нового дизайна `ExtSettingsWindow` и сохранить фактические отчёты.

Реализация должна включать:

- `com.company.hunttech.web.screens.extsettingswindow.ExtSettingsWindow`;
- `com.company.hunttech.entity.UserAiProfile`;
- entity name `hunttech_UserAiProfile`;
- сервис `hunttech_UserAiContextService`;
- таблицу `HUNTTECH_USER_AI_PROFILE`;
- двухпанельную вкладку «Обо мне»;
- локальный предпросмотр без внешнего LLM-вызова;
- сохранение профиля, аватара и почтовых настроек без изменения бизнес-логики рекрутмента.

## 2. Абсолютные запреты

Запрещено:

- запускать `deploy-prod.sh`;
- выполнять SSH;
- подключаться к production PostgreSQL;
- изменять production Tomcat или WAR;
- обращаться к `hr.hunttech.ru`;
- использовать БД `itpearls`;
- выполнять `createDb`, `dropDb`, `cleanDb`;
- выполнять `DROP`, `TRUNCATE`, массовый `DELETE`;
- удалять Docker volume PostgreSQL;
- удалять локальный `fileStorage`;
- продолжать миграцию без проверенного backup.

Допустимы только `localhost`, `127.0.0.1`, локальный Tomcat и локальная БД `hunttech`.

## 3. Получение точного кода

```bash
cd /path/to/it-pearls-recruiting

git fetch --all --prune
git status --short

test -z "$(git status --porcelain)" || {
  echo "Рабочее дерево не чистое";
  exit 1;
}

git checkout agent/settings-window-hunttech-refactor
git reset --hard origin/agent/settings-window-hunttech-refactor

TARGET_SHA=$(git rev-parse HEAD)
REMOTE_SHA=$(git rev-parse origin/agent/settings-window-hunttech-refactor)

test "$TARGET_SHA" = "$REMOTE_SHA" || exit 1
echo "TARGET_SHA=$TARGET_SHA"
```

После фиксации `TARGET_SHA` не переключаться на другой commit.

## 4. Проверка namespace и артефактов

```bash
grep -RIn \
  --exclude-dir=.git \
  --exclude-dir=build \
  -E 'itpearls_UserAiProfile|com\.company\.itpearls\.entity\.UserAiProfile|com\.company\.itpearls\.service\.UserAiContextService' \
  modules docs || true
```

В активном коде не должно остаться старого namespace ИИ-профиля. Упоминание `ITPEARLS_USER_AI_PROFILE` допустимо только в compatibility-миграции, которая безопасно переименовывает ранее созданную таблицу.

Проверить наличие:

```text
modules/global/src/com/company/hunttech/entity/UserAiProfile.java
modules/global/src/com/company/hunttech/service/UserAiContextService.java
modules/global/src/com/company/hunttech/service/dto/AiUserContext.java
modules/core/src/com/company/hunttech/service/UserAiContextServiceBean.java
modules/core/test/com/company/hunttech/service/UserAiContextServiceBeanTest.java
modules/web/src/com/company/hunttech/web/screens/extsettingswindow/ExtSettingsWindow.java
modules/global/src/com/company/hunttech/user-ai-profile-views.xml
modules/core/db/update/postgres/26/260722-1-createUserAiProfile.sql
modules/core/db/update/postgres/26/260722-2-migrateUserAiProfileToHunttech.sql
```

## 5. Компиляция и обязательные тесты

```bash
./gradlew \
  :app-global:compileJava \
  :app-core:compileJava \
  :app-core:compileTestJava \
  :app-web:compileJava \
  :app-web:compileTestJava \
  --no-daemon --stacktrace
```

```bash
./gradlew :app-core:test \
  --tests '*UserAiContextServiceBeanTest*' \
  --no-daemon --stacktrace
```

`ScreenViewIntegrityTest` является обязательным критерием ТЗ. Найти его:

```bash
find . -type f -name '*ScreenViewIntegrityTest*' \
  -not -path './.git/*' -not -path '*/build/*'
```

Если тест отсутствует в текущей ветке, найти каноническую реализацию в истории Git и восстановить только тестовую инфраструктуру без переноса посторонней бизнес-логики:

```bash
git log --all --oneline -- '*ScreenViewIntegrityTest*'
```

После восстановления или подтверждения наличия выполнить:

```bash
./gradlew test \
  --tests '*ScreenViewIntegrityTest*' \
  --no-daemon --stacktrace
```

Обязательный результат:

```text
ScreenViewIntegrityTest — 8/8 PASS
```

Если тест невозможно найти, восстановить или успешно выполнить, этап остановить. Нельзя писать `NOT AVAILABLE`, `NOT APPLICABLE` или продолжать к миграции.

SCSS:

```bash
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
```

Полная сборка:

```bash
./gradlew clean assemble --no-daemon --stacktrace
```

При любой ошибке миграцию и локальный запуск не начинать.

## 6. Проверка локальной БД

Подтвердить параметры подключения:

```sql
SELECT current_database();
SELECT inet_server_addr();
SELECT inet_server_port();
```

Ожидается:

```text
current_database = hunttech
```

Если подключена другая БД или удалённый сервер, немедленно остановиться.

## 7. Backup локальной БД

```bash
BACKUP_DIR="$HOME/hrm-hunttech-backups/settings-window-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"

pg_dump \
  -h "$LOCAL_DB_HOST" \
  -p "$LOCAL_DB_PORT" \
  -U "$LOCAL_DB_USER" \
  -d hunttech \
  --format=custom \
  --blobs \
  --verbose \
  --file="$BACKUP_DIR/hunttech-before-user-ai-profile.dump"

test -s "$BACKUP_DIR/hunttech-before-user-ai-profile.dump" || exit 1

pg_restore --list \
  "$BACKUP_DIR/hunttech-before-user-ai-profile.dump" \
  > "$BACKUP_DIR/restore-list.txt"

test -s "$BACKUP_DIR/restore-list.txt" || exit 1

shasum -a 256 \
  "$BACKUP_DIR/hunttech-before-user-ai-profile.dump" \
  > "$BACKUP_DIR/hunttech-before-user-ai-profile.dump.sha256"
```

До миграции сохранить точные `count(*)` ключевых таблиц без вывода персональных данных.

## 8. Локальный updateDb

Проверить:

```bash
grep -n -A12 'task updateDb' build.gradle
```

`dbName` должен быть `hunttech`.

После backup:

```bash
./gradlew :app-core:updateDb --no-daemon --stacktrace
```

После миграции проверить:

```sql
SELECT to_regclass('public.hunttech_user_ai_profile');

SELECT indexname, indexdef
FROM pg_indexes
WHERE lower(tablename) = 'hunttech_user_ai_profile'
ORDER BY indexname;

SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'hunttech_user_ai_profile'::regclass
ORDER BY conname;

SELECT count(*) FROM hunttech_user_ai_profile;
```

Повторить `count(*)` ключевых существующих таблиц. Уменьшение количества строк недопустимо.

Если одновременно существуют `ITPEARLS_USER_AI_PROFILE` и `HUNTTECH_USER_AI_PROFILE`, compatibility-миграция обязана остановиться. Не объединять и не удалять таблицы автоматически.

## 9. Локальный запуск

Изучить локальный скрипт:

```bash
sed -n '1,360p' scripts/start-app.sh
```

Убедиться, что он не использует SSH, production и удалённую БД. Затем:

```bash
./scripts/start-app.sh
```

Определить фактические context names:

```bash
grep -RIn --include='*.properties' \
  -E 'cuba.webContextName|cuba.connectionUrlList' \
  modules deploy 2>/dev/null
```

Проверить только локальные URL, обычно:

```text
http://localhost:8080/app-core/
http://localhost:8080/app/
```

## 10. Functional smoke

Проверить:

1. web соединяется с core без `RemoteAccessException`;
2. авторизация работает;
3. окно «Настройки» открывается;
4. вкладка «Обо мне» отображается двухпанельно;
5. левая панель имеет ширину около 270 px;
6. правая область прокручивается вертикально;
7. аватар отображается и сохраняется;
8. пустой профиль сохраняется;
9. заполненный профиль сохраняется после повторного открытия;
10. опыт вне диапазона 0–70 блокируется;
11. персонализация без согласия блокируется;
12. согласие сохраняет дату и версию;
13. предпросмотр раскрывается через `setExpanded(true)`;
14. предпросмотр не выполняет внешний HTTP;
15. API-ключи и почтовые пароли отсутствуют;
16. очистка профиля не удаляет аватар и почтовые настройки;
17. вкладки «Интерфейс» и «Почта» продолжают работать;
18. существующие данные пользователей, кандидатов и проектов доступны.

Для каждого пункта указать `PASS`, `FAIL` или `NOT TESTED`. Итог `PASS` допустим только при отсутствии `FAIL` и `NOT TESTED` в обязательных сценариях.

## 11. Runtime-логи

После запуска проверить только новые записи:

```bash
grep -nE \
'IllegalStateException|Cannot get unfetched attribute|detached|NullPointerException|DevelopmentException|FileStorageException|OutOfMemoryError|BeanCreationException|PersistenceException|PSQLException|RemoteAccessException|ERROR|SEVERE' \
  deploy/tomcat/logs/app-core.log \
  deploy/tomcat/logs/app.log \
  2>/dev/null || true
```

Новая ошибка, связанная с `UserAiProfile`, `UserAiContextService`, `ExtSettingsWindow`, view или миграцией, означает, что этап не принят.

## 12. Отчёты

Создать только после фактического прогона:

```text
docs/performance-archive/2026-07-22/setting-window-about-me-ai-profile/hunttech-local/
```

Файлы:

```text
implementation-report.md
test-report.md
database-safety-report.md
functional-smoke.md
runtime-errors.log
```

Обязательно указать:

- `TARGET_SHA`;
- подтверждение локальной среды;
- подтверждение, что production не затрагивался;
- имя БД `hunttech`;
- backup, размер и SHA-256;
- результаты компиляции и всех тестов;
- `ScreenViewIntegrityTest — 8/8 PASS`;
- результат `updateDb`;
- HTTP-коды;
- smoke-сценарии;
- анализ runtime-ошибок;
- подтверждение отсутствия потери данных.

## 13. Итог

Успех:

```text
Этап принят.
Среда: строго локальный компьютер.
База: hunttech.
Production: не затрагивался.
UserAiContextServiceBeanTest: PASS.
ScreenViewIntegrityTest: 8/8 PASS.
SCSS: PASS.
BUILD SUCCESSFUL.
Локальный updateDb: PASS.
Functional smoke: PASS.
Потеря данных: не обнаружена.
Переход к следующему этапу: РАЗРЕШЁН.
```

Ошибка:

```text
Этап не принят.
Production: не затрагивался.
Локальная БД hunttech сохранена.
Причина: <точный root cause>.
Переход к следующему этапу: ЗАПРЕЩЁН.
```

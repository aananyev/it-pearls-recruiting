# Задание Hermes: сборка, миграция БД и запуск профиля «Обо мне»

> Проект: **HRM HuntTech**  
> Ветка: `agent/setting-window-about-me-ai-profile`  
> Цель: развернуть точный HEAD ветки, применить миграцию `UserAiProfile`, запустить приложение и подтвердить работоспособность.  
> Дата задания: `2026-07-22`

## 1. Жёсткие ограничения

1. Не изменять код, бизнес-логику, миграции и документацию до завершения первого полного прогона.
2. Не продолжать работу при незакоммиченных локальных изменениях.
3. Не применять миграцию без успешного резервного копирования БД.
4. Не использовать `--skip-db-update`.
5. Не запускать старый локальный commit: источник истины — удалённый HEAD указанной ветки.
6. При любой ошибке остановить процесс, сохранить логи и выполнить предусмотренный `deploy-prod.sh` откат.
7. Не удалять существующие таблицы и данные.
8. Не применять миграцию вручную через произвольный SQL, если штатный `:app-core:updateDb` успешно доступен.

## 2. Получить точный исходный код

```bash
cd /path/to/it-pearls-recruiting

git fetch --all --prune
git status --short

test -z "$(git status --porcelain)" || {
  echo "Рабочее дерево не чистое";
  exit 1;
}

git checkout agent/setting-window-about-me-ai-profile
git reset --hard origin/agent/setting-window-about-me-ai-profile

TARGET_SHA=$(git rev-parse HEAD)
REMOTE_SHA=$(git rev-parse origin/agent/setting-window-about-me-ai-profile)

test "$TARGET_SHA" = "$REMOTE_SHA" || {
  echo "Локальный и удалённый SHA различаются";
  exit 1;
}

echo "TARGET_SHA=$TARGET_SHA"
```

Записать `TARGET_SHA` в отчёт. Не переключаться на другой commit после этого шага.

## 3. Предварительная проверка diff

```bash
git diff --check HEAD^

git show --stat --oneline HEAD
git show --name-status --format=fuller HEAD
```

Проверить наличие минимум следующих артефактов:

```text
modules/global/src/com/company/itpearls/entity/UserAiProfile.java
modules/core/db/update/postgres/26/260722-1-createUserAiProfile.sql
modules/core/db/changelog/260722-1-addUserAiProfile.xml
modules/web/src/com/company/itpearls/web/screens/extsettingswindow/ext-settings-window.xml
modules/web/src/com/company/itpearls/web/screens/extsettingswindow/ExtSettingsWindow.java
modules/core/test/com/company/itpearls/service/UserAiContextServiceBeanTest.java
```

## 4. Компиляция и тесты до миграции

```bash
./gradlew \
  :app-global:compileJava \
  :app-core:compileJava \
  :app-core:compileTestJava \
  :app-web:compileJava \
  :app-web:compileTestJava \
  --no-daemon --stacktrace

./gradlew :app-core:test \
  --tests '*UserAiContextServiceBeanTest*' \
  --no-daemon --stacktrace

./gradlew test \
  --tests '*ScreenViewIntegrityTest*' \
  --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
  --no-daemon --stacktrace

./gradlew clean assemble \
  --no-daemon --stacktrace
```

Критерии:

- `UserAiContextServiceBeanTest` — PASS;
- `ScreenViewIntegrityTest` — 8/8 PASS;
- SCSS — без ошибок;
- `BUILD SUCCESSFUL`.

При любой ошибке деплой не начинать.

## 5. Резервное копирование

Использовать штатный `deploy-prod.sh`, который создаёт backup PostgreSQL и WAR до остановки Tomcat.

Перед запуском проверить:

```bash
./deploy-prod.sh --help
./deploy-prod.sh --check-config
```

Убедиться, что `hunttech.conf` указывает на требуемое окружение HRM HuntTech. Зафиксировать в отчёте:

- сервер;
- имя БД;
- путь backup;
- размер и контрольную сумму dump;
- контрольные суммы текущих WAR.

## 6. Сборка WAR

Собрать WAR из зафиксированного `TARGET_SHA`:

```bash
./gradlew clean buildWar --no-daemon --stacktrace
```

Зафиксировать:

```bash
find build/distributions/war -type f -name '*.war' -exec shasum -a 256 {} \;
```

## 7. Деплой и миграция базы данных

Запустить штатный безопасный цикл **без** `--skip-db-update` и **без** `--no-start`:

```bash
./deploy-prod.sh
```

Скрипт обязан выполнить:

1. предварительные проверки;
2. backup БД и WAR;
3. остановку Tomcat;
4. передачу новых WAR;
5. SSH-туннель к PostgreSQL;
6. CUBA `:app-core:updateDb`;
7. применение `modules/core/db/update/postgres/26/260722-1-createUserAiProfile.sql`;
8. запуск Tomcat;
9. предложение отката при сбое.

Если штатный скрипт не может выполнить `updateDb`, остановиться. Не заменять миграцию ручным `psql` без отдельного решения в отчёте и подтверждения Алексея.

## 8. Проверка миграции

После успешного `updateDb` проверить через `psql`:

```sql
SELECT to_regclass('public.itpearls_user_ai_profile') AS table_name;

SELECT indexname, indexdef
FROM pg_indexes
WHERE lower(tablename) = 'itpearls_user_ai_profile'
ORDER BY indexname;

SELECT conname, pg_get_constraintdef(oid)
FROM pg_constraint
WHERE conrelid = 'itpearls_user_ai_profile'::regclass
ORDER BY conname;

SELECT count(*) AS profile_count
FROM itpearls_user_ai_profile;
```

Ожидается:

- таблица существует;
- PK существует;
- FK на `SEC_USER` существует;
- уникальный индекс `IDX_ITPEARLS_USER_AI_PROFILE_UNQ_USER` существует;
- ограничения опыта существуют;
- существующие данные других таблиц не изменены.

## 9. Запуск и HTTP-проверка

Проверить состояние Tomcat:

```bash
systemctl status tomcat --no-pager || systemctl status tomcat9 --no-pager
```

Проверить HTTP:

```bash
curl -f -sS -o /dev/null -w '%{http_code}\n' https://hr.hunttech.ru/hrm/
```

Если окружение использует другой URL, взять его из конфигурации и указать в отчёте. Ожидается HTTP 200.

## 10. Functional smoke

Войти в HRM HuntTech тестовым пользователем и проверить:

1. «Настройки» открываются без ошибки.
2. Вкладка «Обо мне» отображается двухпанельно.
3. Аватар отображается и меняется.
4. Пустой профиль сохраняется.
5. Должность, роль, опыт и текстовые поля сохраняются после повторного открытия.
6. Опыт меньше 0 и больше 70 блокируется.
7. Персонализация без согласия не сохраняется.
8. После согласия фиксируются дата и версия.
9. Предпросмотр открывается без внешнего HTTP-вызова.
10. В предпросмотре отсутствуют конфигурация подключения и почтовые реквизиты.
11. Очистка профиля не удаляет аватар и почтовые настройки.
12. Вкладки «Интерфейс» и «Почта» продолжают работать.

## 11. Runtime-логи

Проверить логи после запуска и smoke:

```bash
journalctl -u tomcat --since '30 minutes ago' --no-pager \
  | tee /tmp/user-ai-profile-tomcat.log

grep -E 'IllegalStateException|Cannot get unfetched attribute|detached|NullPointerException|DevelopmentException|FileStorageException|OutOfMemoryError|Liquibase|ERROR|SEVERE' \
  /tmp/user-ai-profile-tomcat.log || true
```

Любая новая ошибка, связанная с `UserAiProfile`, `ExtSettingsWindow` или миграцией, означает, что этап не принят.

## 12. Отчёт

Создать каталог:

```text
docs/performance-archive/2026-07-22/setting-window-about-me-ai-profile/
```

Сохранить:

```text
implementation-report.md
test-report.md
database-migration-report.md
functional-smoke.md
runtime-errors.log
```

Обязательно указать:

- `TARGET_SHA`;
- окружение и URL;
- backup БД/WAR;
- команды и exit codes;
- результат `updateDb`;
- DDL фактической таблицы и индексов;
- тесты, включая 8/8;
- HTTP-код;
- smoke-сценарии;
- новые ошибки;
- потребовался ли rollback.

## 13. Итог Hermes

Допустим только один из выводов:

```text
Этап принят.
Миграция применена.
Приложение запущено.
HTTP 200 подтверждён.
Переход к следующему этапу: РАЗРЕШЁН.
```

или:

```text
Этап не принят.
Выполнен откат или приложение оставлено остановленным по причине: <точная причина>.
Переход к следующему этапу: ЗАПРЕЩЁН.
```

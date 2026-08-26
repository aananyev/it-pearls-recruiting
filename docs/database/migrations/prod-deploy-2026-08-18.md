# Безопасный деплой HRM HuntTech на прод hr.hunttech.ru с возможностью отката

> Дата подготовки: 2026-08-18 · Worktree: hrm-antigravity (ветка `agent/antigravity-dev`)
> Прод: SSH root@hr.hunttech.ru (92.63.101.170) · Tomcat 9 (systemctl tomcat9) · webapps: `/var/lib/tomcat9/webapps/`
> БД: PostgreSQL `hunttech` (datasource jdbc/CubaDS, `jdbc:postgresql://localhost/hunttech`)

## ⚠ Правила безопасности

1. Каждый этап (бэкап, миграция БД, деплой, перезапуск) — ТОЛЬКО после явного подтверждения владельца.
2. `CURRENT_PROD_SHA` по WAR не определяется (MANIFEST без git-коммита) — деплой возможен только с явным
   допуском владельца; перед деплоем обязательно создаётся бэкап WAR (`/opt/backups/hrm/<TS>-predeploy/`).
3. `cuba.automaticDatabaseUpdate=false` на проде — миграции применяются вручную, до деплоя.
4. Откат: `bash /tmp/rollback_prod.sh <TS>-predeploy` (восстанавливает WAR, перезапускает Tomcat, HTTP-poll).
   Миграции аддитивны (только новые таблицы/колонки/промпты) — откат БД не требуется; лишние таблицы
   старому коду не мешают.

## Диагностика на 2026-08-18 (read-only, выполнено)

| Параметр | Значение |
|----------|----------|
| Прод WAR | hrm.war 184919776, hrm-core.war 169861856 (собраны 10.08 23:45, задеплоены 12.08 22:43) |
| Версия в WAR | `app-core-0.39-SNAPSHOT.jar` (старая; локальная ветка 0.172) |
| HTTP | `/hrm/` 200, widgetset 200 |
| automaticDatabaseUpdate | `false` во всех 3 local.app.properties |
| Datasource | `jdbc:postgresql://localhost/hunttech` (НЕ itpearls) |
| connectionUrlList | `http://localhost:8080/hrm-core` — дописана вручную в `/var/lib/tomcat9/webapps/hrm/WEB-INF/local.app.properties` (в build.gradle НЕ зашита → после каждого деплоя дописывать заново) |
| useLocalServiceInvocation | `false` — там же |
| БД журнал миграций | 766 записей, префикс `70-hunttech_recruiting/`, последняя `260731-1-addOutstaffingRatesMarginColumns.sql` |
| Неприменённые миграции (26/) | 16 файлов: 260812-2, 260813-1, 260813-2, 260814-1, 260814-2, 260814-3, 260814-4, 260815-1, 260815-2, 260816-1, 260816-2, 260816-3, 260816-4, 260816-5, 260816-6, 260818-1 |
| AI Control Plane | таблиц НЕТ (создаются миграциями) |
| `hunttech.ai.encryptionKey` | НЕТ на проде (нужен для AiSecretCipher; без него AI-функции не работают, приложение стартует) |
| REST (SYS_ACCESS_TOKEN) | таблицы есть, токенов 0 |
| Бэкап-скрипты | `/tmp/backup_prod.sh`, `/tmp/rollback_prod.sh` — на месте |
| Последние бэкапы | `/opt/backups/hrm/20260812-224300-predeploy`, `20260810-234449-predeploy`, `20260810-231151-predeploy` |

## Что деплоим (по согласованию)

- Кандидат по умолчанию: ветка `agent/antigravity-dev`, HEAD = `e86331fb`, версия 0.172
  (10 коммитов поверх master: переименование JobCandidateTest1Browse → JobCandidateReestr,
  кнопка «Метки», починка «Сканировать навыки», унификация сайдбара JobCandidateEdit,
  компоновка реестра, консолидированная AI-миграция 260818-1).
- Альтернативы: master (0.162, без переименования) — по явному выбору.

## Шаги деплоя

### 0. Safety gate (перед деплоем)

```bash
ssh root@hr.hunttech.ru "grep automaticDatabaseUpdate /opt/app_home/local.app.properties /opt/app_home/hrm/conf/local.app.properties /opt/app_home/hrm-core/conf/local.app.properties"
# → false везде
ssh root@hr.hunttech.ru "grep 'url=' /etc/tomcat9/Catalina/localhost/hrm-core.xml"
# → jdbc:postgresql://localhost/hunttech
ssh root@hr.hunttech.ru "grep connectionUrlList /var/lib/tomcat9/webapps/hrm/WEB-INF/local.app.properties"
# → http://localhost:8080/hrm-core
```

### 1. Локальная сборка WAR (из выбранного SHA)

```bash
cd <worktree>
git rev-parse --short HEAD   # сверка с RELEASE_SHA
rm -rf modules/web/build/libs modules/core/build/libs modules/global/build/libs modules/gui/build/libs modules/web-toolkit/build/libs
export JAVA_HOME=$(/usr/libexec/java_home -v 11)   # corretto-11.0.17 == продовый JDK
bash ../hunttech_recruiting/scripts/agent-gradle.sh buildWar -x test --no-daemon
ls -la build/distributions/war/
sha256sum build/distributions/war/app.war build/distributions/war/app-core.war
# Чистота WAR (только текущая версия jar!):
unzip -l build/distributions/war/app.war | grep -oE 'app-(web|core|global)-[0-9.]+-SNAPSHOT\.jar' | sort -u
unzip -l build/distributions/war/app-core.war | grep -oE 'app-(web|core|global)-[0-9.]+-SNAPSHOT\.jar' | sort -u
# Имена для прода:
cp build/distributions/war/app.war build/distributions/war/hrm.war
cp build/distributions/war/app-core.war build/distributions/war/hrm-core.war
```

### 2. Бэкап прода

```bash
ssh root@hr.hunttech.ru "bash /tmp/backup_prod.sh"
# → /opt/backups/hrm/<TS>-predeploy/ (hrm.war.orig + hrm-core.war.orig + SHA256SUMS + owner.txt)
```

### 3. Миграции БД (вручную, до деплоя — пока работает старый код)

Доставить 16 файлов: `scp modules/core/db/update/postgres/26/2608*.sql root@hr.hunttech.ru:/tmp/prod-migrations-20260818/`

На проде, от root:
```bash
cd /tmp/prod-migrations-20260818
# Бэкап журнала (откат одним DROP+restore):
sudo -u postgres psql -d hunttech -c "CREATE TABLE sys_db_changelog_backup_20260818 AS SELECT * FROM sys_db_changelog;"
for f in $(ls 2608*.sql | sort); do
  echo ">>> $f"
  sudo -u postgres psql -d hunttech -v ON_ERROR_STOP=1 -f "$f" || { echo "FAIL $f"; break; }
  sudo -u postgres psql -d hunttech -c \
    "INSERT INTO sys_db_changelog (script_name, is_init) VALUES ('70-hunttech_recruiting/update/postgres/26/$f', 0) ON CONFLICT (script_name) DO NOTHING;"
done
# Проверка:
sudo -u postgres psql -d hunttech -tAc "SELECT count(*) FROM sys_db_changelog WHERE script_name LIKE '%2608%';"   # → 16
sudo -u postgres psql -d hunttech -tAc "SELECT to_regclass('hunttech_ai_function_configuration'), to_regclass('hunttech_candidate_skill'), to_regclass('hunttech_open_position_skill'), to_regclass('hunttech_ai_call_log');"
sudo -u postgres psql -d hunttech -tAc "SELECT code, is_active FROM hunttech_ai_function_configuration ORDER BY code;"   # → 7 функций
```

Все миграции идемпотентны (IF NOT EXISTS / WHERE NOT EXISTS / DO $$), только аддитивные —
безопасно на работающем проде (кратковременные DDL-локи на HUNTTECH_PROJECT допустимы).

### 4. Загрузка WAR и сверка контрольных сумм

```bash
scp build/distributions/war/hrm.war build/distributions/war/hrm-core.war root@hr.hunttech.ru:/var/lib/tomcat9/webapps/hrm.war.new
scp build/distributions/war/hrm.war build/distributions/war/hrm-core.war root@hr.hunttech.ru:/var/lib/tomcat9/webapps/hrm-core.war.new
# (либо один scp обоих файлов как *.war.new)
ssh root@hr.hunttech.ru "sha256sum /var/lib/tomcat9/webapps/*.war.new"
# сверить с локальными sha256sum
```

### 5. Замена (окно простоя ~1–3 мин)

```bash
ssh root@hr.hunttech.ru "
  systemctl stop tomcat9
  sleep 3
  rm -rf /var/lib/tomcat9/webapps/hrm /var/lib/tomcat9/webapps/hrm-core
  mv /var/lib/tomcat9/webapps/hrm.war.new      /var/lib/tomcat9/webapps/hrm.war
  mv /var/lib/tomcat9/webapps/hrm-core.war.new /var/lib/tomcat9/webapps/hrm-core.war
  chown tomcat:tomcat /var/lib/tomcat9/webapps/hrm.war /var/lib/tomcat9/webapps/hrm-core.war
  systemctl start tomcat9
"
```

### 6. После старта: connectionUrlList + widgetset (ОБЯЗАТЕЛЬНО)

```bash
# Дождаться распаковки war (~20–40 с), затем:
ssh root@hr.hunttech.ru "cat /var/lib/tomcat9/webapps/hrm/WEB-INF/local.app.properties"
# Если нет connectionUrlList — дописать (файл создаётся при распаковке; бэкап старого):
ssh root@hr.hunttech.ru "
  F=/var/lib/tomcat9/webapps/hrm/WEB-INF/local.app.properties
  [ -f \$F ] && cp \$F \$F.bak
  grep -q connectionUrlList \$F 2>/dev/null || cat >> \$F <<'EOF'
cuba.connectionUrlList = http://localhost:8080/hrm-core
cuba.useLocalServiceInvocation = false
cuba.automaticDatabaseUpdate = false
cuba.dataSourceProvider = jndi
EOF
  systemctl restart tomcat9
"

# Widgetset (в WAR отсутствует):
scp -r modules/web-toolkit/build/web/VAADIN/widgetsets root@hr.hunttech.ru:/var/lib/tomcat9/webapps/hrm/VAADIN/
```

### 7. Верификация

```bash
ssh root@hr.hunttech.ru "
  curl -s -o /dev/null -w 'hrm: %{http_code}\n' --max-time 5 http://localhost:8080/hrm/
  curl -s -o /dev/null -w 'widgetset: %{http_code}\n' --max-time 5 http://localhost:8080/hrm/VAADIN/widgetsets/com.company.hunttech.web.toolkit.ui.AppWidgetSet/com.company.hunttech.web.toolkit.ui.AppWidgetSet.nocache.js
  journalctl -u tomcat9 --since '5 minutes ago' --no-pager | grep -E 'ERROR|Exception' | grep -vE 'StatusLogger|PushHandler|ExceptionReportService|ExceptionDialog' | tail -20
  grep -E 'ERROR|Exception' /opt/app_home/logs/app.log 2>/dev/null | tail -10
"
# Внешняя проверка: http://hr.hunttech.ru:8080/hrm/ → 200
# CDP-smoke (желательно): вход alan, «Открытые вакансии», реестр кандидатов «Реестр кандидатов», AI-кнопка «Сканировать навыки»
```

### 8. Откат (при необходимости)

```bash
ssh root@hr.hunttech.ru "bash /tmp/rollback_prod.sh <TS>-predeploy"
# Восстанавливает WAR из бэкапа, перезапускает Tomcat, HTTP-poll до 180 с.
# БД: миграции аддитивны — откат БД НЕ требуется. Если нужен откат журнала:
sudo -u postgres psql -d hunttech -c "DROP TABLE sys_db_changelog; ALTER TABLE sys_db_changelog_backup_20260818 RENAME TO sys_db_changelog;"
# (таблицы AI останутся — старый код их не использует)
```

## AI-функции на проде (отдельный этап, по согласованию)

Для работы AI-функций (SKILLS_EXTRACT, PROJECT_* и др.) на проде нужны:
1. `hunttech.ai.encryptionKey` в `/opt/app_home/hrm-core/conf/local.app.properties` (и/или `/opt/app_home/local.app.properties`) — мастер-ключ прода.
2. Активный корпоративный DeepSeek-конфиг в `hunttech_admin_ai_configuration` (ключ зашифровать AiSecretCipher, формат `v1:<iv>:<ct>`, ключ = весь SHA-256(masterKey)).
3. Привязка `admin_configuration_id` к 7 функциям (+ override для `STANDARDIZE_VACANCY`, USER_REQUIRED).
   Процедура идентична локальной: `docs/database/migrations/ai-api-setup-2026-08-18.md`.

Без п.1–3 приложение деплоится и работает, но AI-кнопки выдают «не настроено активное корпоративное подключение».

## Чек-лист готовности (2026-08-18, до подтверждения)

- [x] Safety gate (automaticDatabaseUpdate=false, datasource hunttech, connectionUrlList hrm-core)
- [x] Прод-диагностика (WAR 0.39, журнал 766, AI-таблиц нет, encryptionKey нет)
- [x] Локальная сборка WAR из ветки (buildWar -x test, corretto-11) — sha256 готовы
- [ ] Подтверждение владельца: что деплоим (ветка e86331fb / master)
- [ ] Подтверждение: бэкап прода (/tmp/backup_prod.sh)
- [ ] Подтверждение: миграции 260812-2..260818-1 (16 файлов)
- [ ] Подтверждение: деплой (замена WAR + connectionUrlList + widgetset)
- [ ] Подтверждение: перезапуск и верификация
- [ ] (опционально) AI: encryptionKey + admin-конфиг DeepSeek

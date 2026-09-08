# Регламент безопасного деплоя и отката HRM HuntTech на Production-сервер

> **Дата составления:** 2026-09-09  
> **Версия релиза:** 0.504 (ветка `agent/antigravity-dev`)  
> **Production сервер:** `hr.hunttech.ru` (IP: `92.63.101.170` / `85.137.95.136`)  
> **Окружение:** Ubuntu Linux, PostgreSQL 11.22 (`hunttech`), Tomcat 9 (`systemd: tomcat9` / `tomcat`)  
> **Каталог веб-приложений:** `/var/lib/tomcat9/webapps` (симлинк `/opt/tomcat/webapps`)  
> **Каталог хранилища файлов:** `/opt/app_home/fileStorage`  
> **Роли:** Аналитик, Frontend-разработчик, Java Backend-разработчик, Hermes-1 (CI/CD / Release Engineer)  

---

## 1. Паспорт релиза 0.504

### 1.1. Состав изменений
1. **Экран редактирования кандидата (`JobCandidateEdit`)**:
   - Восстановлено отображение таблиц DataGrid во вкладках **«Список резюме»** (`tabResume` / `jobCandidateCandidateCvTable`) и **«Список взаимодействий»** (`tabIteraction` / `jobCandidateIteractionListTable`), а также в таблице соцсетей (`socialNetworkTable`).
   - Устранено глобальное схлопывание высоты в 0px из-за `height: auto !important` у `.c-data-grid-composition`.
   - Внедрена отказоустойчивая flex-композиция `.c-data-grid-composition > .v-grid` с явными классами и `height="100%"`.
   - Синхронизированы стили во всех 7 темах оформления (`halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-dark`, `hunttech-modern-light`).
2. **Экран настроек (`ExtSettingsWindow`)**:
   - Ликвидировано наложение информационных карточек во вкладке «Интерфейс» (`interfaceAppearanceCard` и `mainScreenBackgroundCard`).
   - Хрупкий контейнер `<grid>` заменен на адаптивные семантические строки `hbox` (`edit-field-row interface-field-row`) с сохранением невидимого `<grid id="grid" visible="false"/>` для обратной совместимости.
   - Выравнивание левого сайдбара 312px по центру относительно аватара 120px, упорядочена 4-уровневая типографика идентификации пользователя.
   - Добавлена адаптивность `flex: 0 1 220px` для подписей полей.
3. **Статус миграций базы данных**:
   - Все миграции структуры данных (DDL) и системных данных (DML) зафиксированы в `docs/database/migrations/prod-migration-plan-2026-09-08.md` и успешно согласованы с продакшн-схемой.

---

## 2. Предварительные проверки (Pre-flight Gate)

Перед началом деплоя дежурный инженер выполняет обязательные проверки:

### 2.1. Проверка локального состояния и сборки
```bash
cd /Users/alekseyananyev/StudioProjects/hrm-antigravity

# 1. Проверка отсутствия незакоммиченных изменений
git status
# Ожидается: nothing to commit, working tree clean

# 2. Проверка сборки WAR-пакетов через обёртку
bash ../hunttech_recruiting/scripts/agent-gradle.sh buildWar
# Ожидается: BUILD SUCCESSFUL

# 3. Проверка наличия и свежести собранных WAR
ls -lh ../tomcat_wars/
# Должны присутствовать hrm.war и hrm-core.war свежей сборки
```

### 2.2. Проверка доступности продакшена и параметров SSH
```bash
# 1. Тест SSH-доступа без пароля
ssh -o ConnectTimeout=5 root@hr.hunttech.ru "uptime"

# 2. Проверка свободного места на диске сервера
ssh root@hr.hunttech.ru "df -h / /var /opt /tmp"
# Критерий: на разделах /var, /opt и /tmp должно быть не менее 5 ГБ свободного места

# 3. Проверка текущего статуса службы Tomcat
ssh root@hr.hunttech.ru "systemctl status tomcat9 || systemctl status tomcat"
```

---

## 3. Этап 1: Обязательное создание резервной копии (Safety Gate / Backup)

> [!IMPORTANT]
> **МАНДАТОРНОЕ ПРАВИЛО:** Никакие действия по замене файлов или изменению БД не производятся до успешного создания и валидации резервной копии.

### 3.1. Создание каталога бэкапа на сервере
```bash
BACKUP_TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/opt/backups/hrm/${BACKUP_TIMESTAMP}_pre_release_0504"

ssh root@hr.hunttech.ru "mkdir -p ${BACKUP_DIR}/wars"
```

### 3.2. Полный логический дамп PostgreSQL (схема + данные)
```bash
ssh root@hr.hunttech.ru "su - postgres -c \"pg_dump -p 5432 -U postgres -Fc hunttech\" > ${BACKUP_DIR}/hunttech_${BACKUP_TIMESTAMP}.dump"

# Валидация целостности дампа:
ssh root@hr.hunttech.ru "su - postgres -c \"pg_restore -l ${BACKUP_DIR}/hunttech_${BACKUP_TIMESTAMP}.dump | head -n 15\""
```

### 3.3. Резервное копирование текущих развернутых WAR и конфигураций
```bash
ssh root@hr.hunttech.ru "cp -a /var/lib/tomcat9/webapps/*.war ${BACKUP_DIR}/wars/ 2>/dev/null || cp -a /opt/tomcat/webapps/*.war ${BACKUP_DIR}/wars/ 2>/dev/null || true"
ssh root@hr.hunttech.ru "cp -a /opt/app_home/local.app.properties ${BACKUP_DIR}/ 2>/dev/null || true"
```

### 3.4. Локальный бэкап на машине инженера (рекомендуется)
```bash
cd /Users/alekseyananyev/StudioProjects/hrm-antigravity
./pull-prod.sh --check-config
./pull-prod.sh
```

---

## 4. Этап 2: Остановка сервиса и очистка кэшей

Для исключения гонок потоков и конфликтов классов Tomcat останавливается явно:

```bash
# 1. Остановка службы Tomcat
ssh root@hr.hunttech.ru "systemctl stop tomcat9 || systemctl stop tomcat"

# 2. Проверка завершения процессов Java Tomcat
ssh root@hr.hunttech.ru "pgrep -fa java || echo 'Tomcat успешно остановлен'"

# 3. Очистка распакованных каталогов и рабочего кэша Catalina
ssh root@hr.hunttech.ru "rm -rf /var/lib/tomcat9/webapps/hrm /var/lib/tomcat9/webapps/hrm-core"
ssh root@hr.hunttech.ru "rm -rf /var/lib/tomcat9/work/Catalina/localhost/hrm /var/lib/tomcat9/work/Catalina/localhost/hrm-core"
ssh root@hr.hunttech.ru "rm -rf /var/lib/tomcat9/temp/*"
```

---

## 5. Этап 3: Деплой обновлений

### Вариант А: Автоматический деплой скриптом `deploy-prod.sh` (Рекомендуется)
Скрипт `deploy-prod.sh` содержит встроенную защиту, автоматический бэкап, сверку схем и интерактивный откат.
```bash
cd /Users/alekseyananyev/StudioProjects/hrm-antigravity
./deploy-prod.sh --check-config
./deploy-prod.sh -y
```

### Вариант Б: Пошаговый контролируемый деплой (Manual / Hermes-1 Runbook)
Если деплой выполняется вручную по шагам:

1. **Копирование WAR-архивов на сервер**:
```bash
rsync -avz --progress ../tomcat_wars/hrm-core.war root@hr.hunttech.ru:/var/lib/tomcat9/webapps/
rsync -avz --progress ../tomcat_wars/hrm.war root@hr.hunttech.ru:/var/lib/tomcat9/webapps/
```

2. **Проверка прав доступа**:
```bash
ssh root@hr.hunttech.ru "chown -R tomcat:tomcat /var/lib/tomcat9/webapps/hrm*.war || chown -R tomcat9:tomcat9 /var/lib/tomcat9/webapps/hrm*.war"
```

3. **Контроль состояния БД (CUBA updateDb)**:
```bash
# Если задействованы новые миграции:
./gradlew :app-core:updateDb -I .deploy-updateDb-init.gradle -PdeployDbHost=127.0.0.1 -PdeployDbPort=15432 -PdeployDbName=hunttech -PdeployDbUser=cuba
```

---

## 6. Этап 4: Запуск сервиса и прогрев

```bash
# 1. Запуск службы Tomcat
ssh root@hr.hunttech.ru "systemctl start tomcat9 || systemctl start tomcat"

# 2. Мониторинг развертывания в реальном времени (первые 60 секунд)
ssh root@hr.hunttech.ru "journalctl -u tomcat9 -f -n 50"
```

**Критерии успешного запуска в логах:**
- `Deployment of web application archive [/var/lib/tomcat9/webapps/hrm-core.war] has finished in ... ms`
- `Deployment of web application archive [/var/lib/tomcat9/webapps/hrm.war] has finished in ... ms`
- Отсутствие исключений уровня `SEVERE`, `ClassNotFoundException`, `NullPointerException`, `UnfetchedAttributeException`.

---

## 7. Этап 5: Постдеплойная верификация (Smoke Tests)

Дежурный оператор или QA выполняет обязательную верификацию в браузере:

1. **Базовая доступность HTTP:**
   - URL: `http://hr.hunttech.ru/hrm/` (или `https://hunttech.ru/hrm/`)
   - Ответ: HTTP 200 / редирект на экран логина.
2. **Проверка авторизации:**
   - Администратор: `admin`
   - Руководство: `alan`
   - Рекрутер: `aten`
3. **Проверка ключевых исправленных экранов:**
   - **Экран `JobCandidateEdit`**:
     - Открыть любого кандидата с резюме и взаимодействиями.
     - Перейти на вкладку **«Список резюме»**: убедиться, что DataGrid отображается в полный размер, строки видны, скролл работает.
     - Перейти на вкладку **«Список взаимодействий»**: убедиться, что таблица истории коммуникаций видна и заполнена данными.
     - Проверить таблицу соцсетей.
   - **Экран `ExtSettingsWindow`**:
     - Открыть главное меню -> Настройки.
     - Сайдбар 312px: аватар 120px отцентрирован, кнопки загрузки выровнены, 4 уровня типографики читаются.
     - Вкладка **«Интерфейс»**: карточки «Внешний вид» и «Фон главного экрана» не накладываются друг на друга, поля выровнены.
4. **Проверка тем оформления**:
   - Переключить темы: Modern Light -> Modern Dark -> Helium -> Hover -> Havana -> Halo. Верстка не распадается.

---

## 8. Этап 6: Регламент отката (Rollback Runbook)

### 8.1. Критерии инициации отката (Rollback Triggers)
Откат инициируется **немедленно**, если зафиксировано хотя бы одно условие:
1. Tomcat падает при старте или входит в CrashLoop.
2. Приложение недоступно (HTTP 500 / 404 / 502) более **3 минут** после завершения старта Tomcat.
3. Ошибки `LazyInitializationException` или `UnfetchedAttributeException` в основных бизнес-процессах (карточка кандидата, карточка компании, отклики).
4. Ошибки применения миграций БД, приводящие к рассогласованию схемы данных.

---

### 8.2. Сценарий А: Автоматический откат скриптом `deploy-prod.sh`
Если деплой запускался через `./deploy-prod.sh`, при падении скрипт перехватывает ошибку через `trap` и выводит запрос:
```text
Вернуть систему в первоначальное состояние из бэкапа? [y/N]
```
- Нажать `y`.
- Скрипт выполнит:
  1. Остановку Tomcat.
  2. Очистку распакованных директорий `hrm` и `hrm-core`.
  3. Восстановление исходных `.war` из `${BACKUP_DIR}/wars/`.
  4. Восстановление базы данных через `pg_restore --clean --if-exists`.
  5. Перезапуск Tomcat и проверку статуса.

---

### 8.3. Сценарий Б: Быстрый откат только приложения (WAR-Only Rollback)
*Используется, если сбой вызван ошибкой в коде UI/Web (Java/SCSS/XML), а структура базы данных осталась целостной.*  
*Время выполнения: **~60-90 секунд**.*

```bash
ssh root@hr.hunttech.ru << 'EOF'
set -euo pipefail

BACKUP_DIR=$(ls -td /opt/backups/hrm/*_pre_release_* | head -n 1)
echo "Используется бэкап: $BACKUP_DIR"

# 1. Остановка Tomcat
systemctl stop tomcat9 || systemctl stop tomcat

# 2. Очистка кэша
rm -rf /var/lib/tomcat9/webapps/hrm /var/lib/tomcat9/webapps/hrm-core
rm -rf /var/lib/tomcat9/work/Catalina/localhost/*
rm -rf /var/lib/tomcat9/temp/*

# 3. Восстановление предыдущих WAR
cp -a "$BACKUP_DIR/wars/"*.war /var/lib/tomcat9/webapps/
chown -R tomcat:tomcat /var/lib/tomcat9/webapps/hrm*.war || chown -R tomcat9:tomcat9 /var/lib/tomcat9/webapps/hrm*.war

# 4. Запуск Tomcat
systemctl start tomcat9 || systemctl start tomcat
echo "WAR-откат завершен. Tomcat запущен."
EOF
```

---

### 8.4. Сценарий В: Полный аварийный откат (Disaster Recovery: WAR + Database)
*Используется при сбоях схемы данных, блокировках транзакций или повреждении информации.*  
*Время выполнения: **~2-4 минуты**.*

```bash
ssh root@hr.hunttech.ru << 'EOF'
set -euo pipefail

BACKUP_DIR=$(ls -td /opt/backups/hrm/*_pre_release_* | head -n 1)
DUMP_FILE=$(ls -t "$BACKUP_DIR"/hunttech_*.dump | head -n 1)
echo "Критический откат! Бэкап: $BACKUP_DIR, Дамп: $DUMP_FILE"

# 1. Немедленная остановка Tomcat для предотвращения записи в БД
systemctl stop tomcat9 || systemctl stop tomcat

# 2. Очистка кэшей Tomcat
rm -rf /var/lib/tomcat9/webapps/hrm /var/lib/tomcat9/webapps/hrm-core
rm -rf /var/lib/tomcat9/work/Catalina/localhost/*

# 3. Восстановление WAR-файлов
cp -a "$BACKUP_DIR/wars/"*.war /var/lib/tomcat9/webapps/
chown -R tomcat:tomcat /var/lib/tomcat9/webapps/hrm*.war || chown -R tomcat9:tomcat9 /var/lib/tomcat9/webapps/hrm*.war

# 4. Принудительное завершение всех открытых сессий к PostgreSQL hunttech
su - postgres -c "psql -d postgres -c \"
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = 'hunttech' AND pid <> pg_backend_pid();\""

# 5. Полное восстановление базы данных из дампа
echo "Запуск pg_restore..."
su - postgres -c "pg_restore -p 5432 -U postgres -d hunttech --clean --if-exists '$DUMP_FILE'"

# 6. Запуск Tomcat
systemctl start tomcat9 || systemctl start tomcat

echo "Полный аварийный откат успешно выполнен!"
EOF
```

---

## 9. Чеклист готовности (Verification Matrix)

| Шаг | Действие | Ответственный | Статус |
|:---:|:---|:---:|:---:|
| 1 | Локальная сборка `buildWar` без ошибок | Разработчик | [x] Выполнено |
| 2 | Сквозной аудит через `ocr review` | Разработчик | [x] 0 замечаний |
| 3 | Коммит и пуш в `agent/antigravity-dev` | Разработчик | [x] Коммит `c2d467d0` |
| 4 | Проверка SSH и дискового пространства на проде | Release Engineer | Ожидает окна |
| 5 | Создание серверного бэкапа БД (`pg_dump`) и WAR | Release Engineer | Перед деплоем |
| 6 | Остановка Tomcat и очистка кэшей | Release Engineer | Перед деплоем |
| 7 | Развертывание WAR и применение миграций | Release Engineer / `deploy-prod.sh` | В окне деплоя |
| 8 | Запуск Tomcat и мониторинг `journalctl` | Release Engineer | В окне деплоя |
| 9 | Smoke-тест `JobCandidateEdit` и `ExtSettingsWindow` | QA / Оператор HRM | После старта |
| 10 | Подтверждение стабильности релиза (Go/No-Go) | Product Owner / Руководитель | Финал |

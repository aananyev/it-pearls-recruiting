# План безопасного деплоя HRM HuntTech на Production

**Дата:** 2026-08-20  
**Профиль:** default (deepseek-v4-flash)  
**Репозиторий:** ~/StudioProjects/hrm-antigravity (worktree, ветка agent/antigravity-dev)  
**Production сервер:** hr.hunttech.ru (85.137.95.136)  
**БД:** HuntTech (PostgreSQL 11)  
**Tomcat:** /opt/tomcat/webapps, systemd-сервис `tomcat`

---

## Содержание изменений для деплоя

### 1. Миграции базы данных (новые SQL в `modules/core/db/update/postgres/26/`)
- `260820-1-addSmartCvParseAiFunction.sql` — AI-функция **CV_SMART_PARSE_JSON** (умное распознавание резюме)
- `260820-3-addSmartVacancyParseAiFunction.sql` — AI-функция **VACANCY_SMART_PARSE_JSON** (умное распознавание вакансии)
- `260820-2-addTelegramToSecUser.sql` — добавление поля Telegram в SecUser

### 2. WAR-файлы для деплоя (в `~/StudioProjects/tomcat_wars/`)
- `hrm.war` — веб-модуль (155 MB)
- `hrm-core.war` — core-модуль (169 MB)

### 3. Промпты AI-функций (в миграциях выше)
- **CV_SMART_PARSE_JSON** — извлечение структурированных данных кандидата из текста резюме
- **VACANCY_SMART_PARSE_JSON** — извлечение структурированных данных вакансии и проекта

---

## Этап 1: ПОДГОТОВКА И БЭКАП (ЛОКАЛЬНЫЙ)

### 1.1 Создать полный локальный бэкап production-окружения
```bash
cd ~/StudioProjects/hrm-antigravity
./pull-prod.sh --check-config   # проверка конфигурации, SSH, WAR
./pull-prod.sh                  # полный бэкап: pg_basebackup + fileStorage + WAR
```
**Результат:** `hunttech-basebackup.tgz` (PGDATA + tomcat_wars) в ARCHIVE_DIR (`../`)

### 1.2 Проверить локальные WAR
```bash
ls -lh ~/StudioProjects/tomcat_wars/
# Ожидаем: hrm.war, hrm-core.war (свежие от 22.07 или новее)
```

### 1.3 Синхронизировать ветку с origin/master
```bash
cd ~/StudioProjects/hrm-antigravity
git fetch origin
git merge origin/master   # разрешить конфликты обеими сторонами
# pre-commit hook сам обновит версию в build.gradle при коммите
```

---

## Этап 2: ПРОВЕРКА КОНФИГУРАЦИИ ДЕПЛОЯ

### 2.1 Проверить конфигурацию deploy-prod.sh
```bash
cd ~/StudioProjects/hrm-antigravity
./deploy-prod.sh --check-config
```
Ожидаемые проверки:
- ✅ SSH к root@hr.hunttech.ru
- ✅ Локальные WAR в `../tomcat_wars/`
- ✅ Удалённый каталог `/opt/tomcat/webapps`
- ✅ rsync (GNU или BSD fallback)
- ✅ PostgreSQL 11 клиент локально (для сравнения схем)

### 2.2 Проверить миграции (dry-run)
```bash
# Соберёт скрипты миграции локально без применения
./gradlew :app-core:assembleDbScripts --no-daemon -q
```

---

## Этап 3: БЕЗОПАСНЫЙ ДЕПЛОЙ НА PRODUCTION

### 3.1 Запуск деплоя с автоматическим бэкапом на сервере
```bash
cd ~/StudioProjects/hrm-antigravity
./deploy-prod.sh -y
```

**Что делает скрипт (автоматически с флагом -y):**
1. **Создаёт бэкап на сервере** в `/tmp/cuba_deploy_backup_YYYYMMDD_HHMMSS/`:
   - Копия всех `.war` из `/opt/tomcat/webapps/`
   - `pg_dump` базы HuntTech (custom format)
   - Метафайл `backup.meta` с timestamp

2. **Останавливает Tomcat** (`systemctl stop tomcat`)

3. **Очищает распакованные каталоги** CUBA (`app/`, `app-core/`)

4. **Загружает новые WAR** через rsync+pv (прогресс-бар)

5. **Проверяет и обновляет структуру БД** через SSH-туннель:
   - `assembleDbScripts` (dry-run)
   - Сравнение схем (локальная vs удалённая)
   - При наличии отличий — `updateDb` через CUBA (Liquibase)
   - **Новые миграции 260820-* применятся автоматически**

6. **Запускает Tomcat** (`systemctl start tomcat`)

7. **Проверяет статус** Tomcat

### 3.2 При сбое — автоматический откат
Если деплой упадёт после создания бэкапа, скрипт предложит:
```
Вернуть систему в первоначальное состояние из бэкапа? [y/N]
```
Откат восстановит: WAR + БД (pg_restore) + перезапуск Tomcat.

---

## Этап 4: ПОСЛЕДЕПЛОЙНАЯ ВЕРИФИКАЦИЯ

### 4.1 Проверить логи запуска
```bash
ssh root@hr.hunttech.ru "journalctl -u tomcat -f -n 100"
```

### 4.2 Проверить применение миграций
```bash
ssh root@hr.hunttech.ru "su - postgres -c \"psql -p 5432 -U cuba -d HuntTech -c \\\"SELECT code, name, is_active FROM hunttech_ai_function_configuration WHERE code IN ('CV_SMART_PARSE_JSON', 'VACANCY_SMART_PARSE_JSON');\\\"\""
```
Ожидаем: 2 строки с `is_active = true`

### 4.3 Проверить доступность приложения
- HTTP: http://hr.hunttech.ru/hrm/ (или https://hunttech.ru/hrm/)
- Проверить реестры: Кандидаты, Вакансии
- Проверить AI-функции в UI (кнопки «Умная загрузка», «Сканировать навыки»)

### 4.4 Проверить Telegram-боты (если задеплоены)
- recruiting bot (short_vacancy)
- docs bot
- offer bot

---

## РИСКИ И МИТИГАЦИИ

| Риск | Вероятность | Митигация |
|------|-------------|-----------|
| Конфликт схемы БД | Средняя | `check_remote_schema_drift` покажет diff перед updateDb; бэкап БД создан |
| Tomcat не стартует | Низкая | Авто-откат из бэкапа (WAR + БД) за ~2 мин |
| Новые миграции не применятся | Низкая | `updateDb` запускается после загрузки WAR; логи в `deploy-prod.log` |
| Файловые хранилища (fileStorage) | Низкая | Не трогаются при деплое WAR; pull-prod бэкапирует отдельно |
| Геоблок Google API (Antigravity) | Не относится | Деплой на сервере — без прокси; Antigravity только для разработки |

---

## КОМАНДЫ ДЛЯ РУЧНОГО УПРАВЛЕНИЯ (если нужно)

### Только БД (миграции без WAR)
```bash
./deploy-prod.sh --skip-db  # деплой WAR без updateDb
# затем вручную:
./gradlew :app-core:updateDb -I .deploy-updateDb-init.gradle -PdeployDbHost=127.0.0.1 -PdeployDbPort=15432 -PdeployDbName=HuntTech -PdeployDbUser=cuba -PdeployDbPassword=...
```

### Только проверка схемы
```bash
./deploy-prod.sh --check-config
# проверка схемы встроена в db_structure_validation_and_update
```

### Откат вручную (если авто-откат не сработал)
```bash
ssh root@hr.hunttech.ru
BACKUP_DIR="/tmp/cuba_deploy_backup_YYYYMMDD_HHMMSS"
systemctl stop tomcat
cp -a "$BACKUP_DIR/wars/"*.war /opt/tomcat/webapps/
rm -rf /opt/tomcat/webapps/app /opt/tomcat/webapps/app-core
su - postgres -c "pg_restore -p 5432 -U postgres -d HuntTech --clean --if-exists '$BACKUP_DIR/HuntTech.dump'"
systemctl start tomcat
```

---

## ЧЕКЛИСТ ГОТОВНОСТИ

- [ ] Локальные WAR собраны и лежат в `~/StudioProjects/tomcat_wars/`
- [ ] Ветка `agent/antigravity-dev` в синхронизации с `origin/master`
- [ ] Нет незакоммиченных изменений в worktree
- [ ] `pull-prod.sh` успешно создал локальный бэкап (`hunttech-basebackup.tgz`)
- [ ] `deploy-prod.sh --check-config` прошёл без ошибок
- [ ] `~/.pgpass` содержит пароль для `cuba`@`HuntTech` (для updateDb)
- [ ] SSH-ключ к `root@hr.hunttech.ru` работает без пароля
- [ ] На сервере достаточно дискового места (`df -h /opt /tmp`)

---

## ПОСЛЕ УСПЕШНОГО ДЕПЛОЯ

1. **Обновить общую копию репо** (Hermes-1 zone):
   ```bash
   cd ~/StudioProjects/hunttech_recruiting
   git pull origin master
   bash ./scripts/start-app.sh   # вернуть master на общий Tomcat
   ```

2. **Закрыть PR** в GitHub (merge делает Hermes-1)

3. **Задокументировать изменения** в `.ai/reports/` и `docs/`

4. **Уведомить заказчика** о новых AI-функциях:
   - Умная загрузка резюме (CV_SMART_PARSE_JSON)
   - Умная загрузка вакансий (VACANCY_SMART_PARSE_JSON)
   - Поле Telegram в профиле пользователя

---

**Автор плана:** Antigravity IDE (agent/antigravity-dev)  
**Статус:** Готов к исполнению после подтверждения пользователя
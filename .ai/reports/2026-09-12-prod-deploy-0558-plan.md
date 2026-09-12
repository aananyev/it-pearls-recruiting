# Безопасный план деплоя релиза 0.558 на Production (`hr.hunttech.ru`)

> **Целевой сервер:** `hr.hunttech.ru` (`92.63.101.170`)  
> **Версия релиза:** `0.558`  
> **Ветка:** `agent/antigravity-dev`  
> **Коммит:** `18ee499d`  
> **Статус:** 📋 План подготовлен к согласованию и выполнению  

---

## 1. Состав релиза 0.558

1. **Экран «Реестр стран» (`CountryReestrBrowse`)**:
   - Первая колонка таблицы («Флаг»): вывод изображения из BLOB-поля `flagImage` (`byte[]`) с каскадным переходом на `fileFlag` и тематическую иконку.
   - Сайдбар (`logoPic`): переведен на `<fallbackImage>` с размерами `160×100px`, `scaleMode="CONTAIN"` и классом `country-flag-rectangular-image` (прямоугольный флаг с сохранением пропорций).
   - Нижний блок «ФЛАГ СТРАНЫ» (`flagCard`): скрыт (`visible="false"`), логика контроллера оптимизирована.
2. **Экран «Реестр городов» (`CityReestrBrowse`)**:
   - В сайдбаре скрыт нижний блок «ГЕРБ ГОРОДА» (`emblemCard`), контроллер сделан null-safe.
3. **Главное меню (`web-menu.xml`)**:
   - Скрыты устаревшие справочники: «Страны» (`hunttech_Country.browse`), «Города» (`hunttech_City.browse`), «Регионы» (`hunttech_Region.browse`).
   - Переименованы реестры:
     - «Реестр стран» $\rightarrow$ **«Страны»**
     - «Реестр городов» $\rightarrow$ **«Города»**
     - «Реестр регионов» $\rightarrow$ **«Регионы»**
4. **Сущность «Должность» (`Position`) и пиктограммы**:
   - Добавлены поля `filePositionIcon` (`FileDescriptor`) и `iconImage` (`byte[]`).
   - Добавлена миграция БД: `modules/core/db/update/postgres/26/260912-1-addPositionIconFields.sql` и companion changelog `260912-1-addPositionIconFields.xml`.
   - В «Реестре должностей» (`PositionReestrBrowse`) в сайдбаре отображается загруженная пиктограмма.
   - В «Редактировании должности» (`PositionEdit`) добавлен загрузчик `<upload id="positionIconUpload">` с валидацией типов (`.png,.jpg,.jpeg,.webp`), лимитом размера 15 МБ и drag-and-drop.
5. **Адаптивная компоновка экрана «Редактирование должности» (`PositionEdit`)**:
   - Рабочая область адаптирована для растягивания на всю ширину и высоту экрана при любых разрешениях.
   - Синхронизированы стили `position-editor.scss` для всех 7 тем оформления (`halo`, `havana`, `helium`, `hover`, `hunttech-modern`, `hunttech-modern-light`, `hunttech-modern-dark`).

---

## 2. План безопасного выполнения деплоя

### Этап 1. Локальная сборка артефактов (WAR)
```bash
# Выполняется в worktree агента:
bash ../hunttech_recruiting/scripts/agent-gradle.sh buildWar
```
*Критерий успеха:* `BUILD SUCCESSFUL`, наличие файлов `build/distributions/war/hrm.war` и `build/distributions/war/hrm-core.war`.

---

### Этап 2. Создание полного бэкапа на боевом сервере ПЕРЕД любыми изменениями
Подключение по SSH к `root@hr.hunttech.ru`:
```bash
ssh root@hr.hunttech.ru << 'EOF'
set -euo pipefail

BACKUP_TS=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/tmp/cuba_deploy_backup_${BACKUP_TS}"
mkdir -p "$BACKUP_DIR/wars"

echo "=== 1. Создание бэкапа базы данных hunttech ==="
su - postgres -c "pg_dump -Fc -p 5432 -U postgres hunttech > '$BACKUP_DIR/hunttech.dump'"
echo "Размер дампа БД:"
ls -lh "$BACKUP_DIR/hunttech.dump"

echo "=== 2. Создание копии текущих боевых WAR ==="
cp -a /var/lib/tomcat9/webapps/hrm*.war "$BACKUP_DIR/wars/" 2>/dev/null || true
cp -a /var/lib/tomcat9/webapps/hrm "$BACKUP_DIR/wars/" 2>/dev/null || true
cp -a /var/lib/tomcat9/webapps/hrm-core "$BACKUP_DIR/wars/" 2>/dev/null || true

echo "Бэкап сохранен в $BACKUP_DIR"
EOF
```
*Критерий успеха:* файл дампа `$BACKUP_DIR/hunttech.dump` сформирован без ошибок (размер ~750 МБ), текущие WAR скопированы в `$BACKUP_DIR/wars`.

---

### Этап 3. Загрузка новых WAR на сервер
```bash
ssh root@hr.hunttech.ru "mkdir -p /tmp/cuba_new_deploy_0558"
rsync -avP --inplace build/distributions/war/hrm.war root@hr.hunttech.ru:/tmp/cuba_new_deploy_0558/
rsync -avP --inplace build/distributions/war/hrm-core.war root@hr.hunttech.ru:/tmp/cuba_new_deploy_0558/
```
*Критерий успеха:* оба файла `hrm.war` и `hrm-core.war` полностью загружены во временный каталог `/tmp/cuba_new_deploy_0558/`.

---

### Этап 4. Применение миграции структуры БД
Скрипт `260912-1-addPositionIconFields.sql` полностью идемпотентен:
```bash
ssh root@hr.hunttech.ru << 'EOF'
set -euo pipefail

su - postgres -c "psql -p 5432 -U postgres -d hunttech" << 'SQL'
-- Добавление полей пиктограммы должности в HUNTTECH_POSITION
ALTER TABLE HUNTTECH_POSITION ADD COLUMN IF NOT EXISTS FILE_POSITION_ICON_ID UUID;
ALTER TABLE HUNTTECH_POSITION ADD COLUMN IF NOT EXISTS ICON_IMAGE BYTEA;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'FK_HUNTTECH_POSITION_ON_FILE_ICON') THEN
        ALTER TABLE HUNTTECH_POSITION ADD CONSTRAINT FK_HUNTTECH_POSITION_ON_FILE_ICON
            FOREIGN KEY (FILE_POSITION_ICON_ID) REFERENCES SYS_FILE(ID);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_POSITION_ON_FILE_ICON ON HUNTTECH_POSITION(FILE_POSITION_ICON_ID);
SQL

echo "Миграция БД успешно применена."
EOF
```
*Критерий успеха:* `ALTER TABLE`, `DO`, `CREATE INDEX` выполнены успешно.

---

### Этап 5. Остановка Tomcat, замена WAR и очистка кэшей
```bash
ssh root@hr.hunttech.ru << 'EOF'
set -euo pipefail

echo "Остановка Tomcat..."
systemctl stop tomcat9

echo "Очистка кэша и распакованных каталогов..."
rm -rf /var/lib/tomcat9/webapps/hrm /var/lib/tomcat9/webapps/hrm-core
rm -rf /var/lib/tomcat9/work/Catalina/localhost/*
rm -rf /var/lib/tomcat9/temp/*

echo "Развертывание новых WAR 0.558..."
cp /tmp/cuba_new_deploy_0558/hrm.war /var/lib/tomcat9/webapps/
cp /tmp/cuba_new_deploy_0558/hrm-core.war /var/lib/tomcat9/webapps/
chown -R tomcat:tomcat /var/lib/tomcat9/webapps/hrm*.war

echo "Запуск Tomcat..."
systemctl start tomcat9
EOF
```
*Критерий успеха:* Tomcat запущен (`systemctl status tomcat9`), началась инициализация CUBA AppContext.

---

### Этап 6. Верификация боевого запуска (Smoke Testing)
1. **Проверка логов прогресса старта:**
   ```bash
   ssh root@hr.hunttech.ru "journalctl -u tomcat9 -n 60 --no-pager"
   ```
   *Ожидаемый результат:* `AppContext started` для `hrm-core` и `hrm`.
2. **Проверка доступности HTTP-сервиса:**
   ```bash
   curl -I http://hr.hunttech.ru:8080/hrm/
   curl -I http://92.63.101.170:8080/hrm/
   ```
   *Ожидаемый результат:* `HTTP/1.1 200 OK` и выдача cookie `JSESSIONID`.
3. **Проверка стилей темы:**
   ```bash
   curl -sI http://hr.hunttech.ru:8080/hrm/VAADIN/themes/hunttech-modern/styles.css | grep "HTTP/1.1 200"
   ```
4. **Функциональный smoke-тест оператором:**
   - Вход под учётной записью `alan` / `Dodo-2012`.
   - Проверка меню: разделы «Страны», «Города», «Регионы».
   - Открытие формы «Страны» (`CountryReestrBrowse`): наличие прямоугольного флага в sidebar и отображение флага в первой колонке таблицы.
   - Открытие формы «Города» (`CityReestrBrowse`): скрытие блока герба внизу sidebar.
   - Открытие формы «Должности» (`PositionReestrBrowse`): отображение пиктограммы должности в sidebar.
   - Открытие карточки «Редактирование должности» (`PositionEdit`): адаптивное растягивание правой панели на всю ширину и высоту, работа компонента загрузки пиктограммы.

---

## 3. План экстренного отката (Rollback Procedure)

В случае обнаружения критических сбоев возврат к предыдущей стабильной версии осуществляется скриптом отката:

```bash
ssh root@hr.hunttech.ru << 'EOF'
set -euo pipefail

# Укажите актуальный BACKUP_DIR, сформированный на Этапе 2:
BACKUP_DIR=$(ls -td /tmp/cuba_deploy_backup_* | head -n 1)
echo "Используется резервная копия: $BACKUP_DIR"

echo "1. Остановка Tomcat..."
systemctl stop tomcat9

echo "2. Очистка текущих каталогов..."
rm -rf /var/lib/tomcat9/webapps/hrm /var/lib/tomcat9/webapps/hrm-core
rm -rf /var/lib/tomcat9/webapps/hrm*.war
rm -rf /var/lib/tomcat9/work/Catalina/localhost/*
rm -rf /var/lib/tomcat9/temp/*

echo "3. Восстановление предыдущих WAR..."
cp -a "$BACKUP_DIR/wars/"*.war /var/lib/tomcat9/webapps/
chown -R tomcat:tomcat /var/lib/tomcat9/webapps/hrm*.war

echo "4. Восстановление базы данных..."
su - postgres -c "pg_restore -p 5432 -U postgres -d hunttech --clean --if-exists '$BACKUP_DIR/hunttech.dump'"

echo "5. Запуск Tomcat..."
systemctl start tomcat9

echo "Откат завершен. Проверка статуса..."
systemctl status tomcat9 --no-pager
EOF
```

# Отчет о деплое релиза 0.560 на Production (`hr.hunttech.ru`)

> **Целевой сервер:** `hr.hunttech.ru` (`92.63.101.170`)  
> **Версия релиза:** `0.560`  
> **Ветка:** `agent/antigravity-dev`  
> **Коммиты:** `18ee499d` (релиз 0.558) + `b450ff96` (релиз 0.560)  
> **Дата деплоя:** 12.09.2026 17:32 MSK  
> **Статус:** ✅ Успешно развернуто и верифицировано  

---

## 1. Состав релиза 0.560

1. **Реестры стран и городов**:
   - `CountryReestrBrowse`: первая колонка таблицы («Флаг») выводит изображение из BLOB-поля `flagImage` с fallback на `fileFlag` и тему.
   - `CountryReestrBrowse` (сайдбар): флаг отображается в прямоугольном формате с сохранением пропорций (`scaleMode="CONTAIN"`, класс `country-flag-rectangular-image`).
   - Нижние блоки «Флаг страны» и «Герб города» в сайдбарах реестров скрыты.
2. **Главное меню (`web-menu.xml`)**:
   - Скрыты дублирующие справочники «Страны», «Города», «Регионы».
   - Основные реестры переименованы в «Страны», «Города», «Регионы».
3. **Сущность «Должность» (`Position`) и экран `PositionEdit`**:
   - В сущность добавлены поля `filePositionIcon` и `iconImage` (BLOB).
   - `PositionReestrBrowse`: отображение пиктограммы должности в сайдбаре.
   - `PositionEdit`: исправлена ошибка `dropZone="positionLogoImage"` (заменено на `dropZone="visual"`), поддержан drag-and-drop и загрузка пиктограмм до 15 МБ.
   - Адаптивное масштабирование формы `PositionEdit` на всю ширину и высоту.
4. **Сущность «Дерево компетенций» (`SkillTree`) и экран `SkillTreeReestrBrowse`**:
   - В сущность `SkillTree` добавлено поле `@Lob LOGO_IMAGE` (`bytea`).
   - `SkillTreeEdit`: автосинхронизация файла загрузчика в BLOB-поле и отображение логотипа.
   - `SkillTreeReestrBrowse`: первая колонка «ЛОГО» отображает логотип напрямую из BLOB `logoImage`.
   - Древовидная иерархия настроена с `hierarchyColumn="skillName"`, корректно раскрывая узлы дерева без поломки первой колонки с логотипом.
   - Колонка приоритета компетенций стилизована цветными чип-бейджами `.skill-priority-chip`.
   - Стили дерева синхронизированы во всех 7 темах оформления.

---

## 2. Выполненные этапы деплоя

### Этап 1. Локальная сборка WAR
- Выполнена сборка через `agent-gradle.sh buildWar`.
- Сформированы свежие артефакты:
  - `build/distributions/war/hrm.war` (182 МБ)
  - `build/distributions/war/hrm-core.war` (163 МБ)

### Этап 2. Полный бэкап на production сервере
- Каталог резервной копии: `/tmp/cuba_deploy_backup_20260912_162556`
- Дамп PostgreSQL `hunttech`: `/tmp/cuba_deploy_backup_20260912_162556/hunttech.dump` (размер 865 МБ)
- Копия предыдущих WAR: `/tmp/cuba_deploy_backup_20260912_162556/wars/`

### Этап 3. Применение миграций БД на проде
Выполнены идемпотентные SQL-скрипты:
```sql
-- HUNTTECH_POSITION
ALTER TABLE HUNTTECH_POSITION ADD COLUMN IF NOT EXISTS FILE_POSITION_ICON_ID UUID;
ALTER TABLE HUNTTECH_POSITION ADD COLUMN IF NOT EXISTS ICON_IMAGE BYTEA;
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'FK_HUNTTECH_POSITION_ON_FILE_ICON') THEN
        ALTER TABLE HUNTTECH_POSITION ADD CONSTRAINT FK_HUNTTECH_POSITION_ON_FILE_ICON
            FOREIGN KEY (FILE_POSITION_ICON_ID) REFERENCES SYS_FILE(ID);
    END IF;
END $$;
CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_POSITION_ON_FILE_ICON ON HUNTTECH_POSITION(FILE_POSITION_ICON_ID);

-- HUNTTECH_SKILL_TREE
ALTER TABLE HUNTTECH_SKILL_TREE ADD COLUMN IF NOT EXISTS LOGO_IMAGE BYTEA;
```
Результат верификации схемы:
- `hunttech_position`: `file_position_icon_id` (uuid), `icon_image` (bytea) — подтверждено.
- `hunttech_skill_tree`: `logo_image` (bytea) — подтверждено.

### Этап 4. Развертывание WAR и рестарт Tomcat9
- Остановлен сервис `systemctl stop tomcat9`.
- Очищены временные каталоги и кэши: `work/Catalina/localhost/*`, `temp/*`, распакованные `webapps/hrm` и `webapps/hrm-core`.
- Развернуты новые `hrm.war` и `hrm-core.war`, права выставлены на пользователя `tomcat:tomcat`.
- Запущен `systemctl start tomcat9`.

### Этап 5. Верификация запуска
- Лог `app.log`:
  - `AppContext started` для веб-модуля `hrm`.
  - `AppContext started` для модуля `hrm-core`.
  - Инициализированы подсистемы BPM/Activiti и Telegram-бот.
- Проверка доступности:
  - `http://127.0.0.1:8080/hrm/` $\rightarrow$ `HTTP 200 OK`
  - `http://92.63.101.170:8080/hrm/` $\rightarrow$ `HTTP 200 OK`
  - Стили `styles.css` тем `hunttech-modern` и `hunttech-modern-dark` $\rightarrow$ `HTTP 200 OK`

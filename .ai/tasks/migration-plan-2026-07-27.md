# Task: Подготовить план миграции БД (prod → код) без потери данных

## Контекст

С продакшена загружена актуальная БД (`get_base.sh` → PostgreSQL 11 локально). 
База создавалась старой системой миграций (`70-IT-Pearls/update/`). 
Новые Liquibase changelogs (`modules/core/db/changelog/`) **не применены** к этой БД.

## Текущий разрыв

### 1. Отсутствующие таблицы (должны быть по коду, нет в БД)

| Таблица | Миграция | Статус в БД |
|---------|----------|-------------|
| `hunttech_user_ai_profile` | `260722-1-addUserAiProfile.xml` | ❌ не существует |
| `hunttech_user_ai_profile_parameters` | (в changelog) | ❌ не существует |

### 2. Отсутствующие колонки

| Таблица | Колонка | Миграция | Статус в БД |
|---------|---------|----------|-------------|
| `hunttech_user_settings` | `PREFER_PERSONAL_AI_API_SETTINGS` | `260723-1-addPreferPersonalAiApiSettings.xml` | ❌ нет |
| `hunttech_user_settings` | `PREFER_PERSONAL_PROMPTS` | `260724-1-enablePersonalAiPreferences.xml` | ❌ нет |
| `hunttech_user_settings` | `FILE_IMAGE_FACE` (вместо `IMAGE_ID`) | entity `UserSettings.fileImageFace` | ❌ есть `IMAGE_ID` (старое имя) |

### 3. Существующие entity с колонками, которые уже есть в prod БД

| Таблица | Колонки AI | Исходная миграция |
|---------|------------|-------------------|
| `hunttech_open_position` | `RAW_DESCRIPTION`, `INTERVIEW_CHECKLIST`, `SEARCH_MAP`, `INTERVIEW_PLAN` | Уже есть (создавались старыми миграциями) |
| `hunttech_user_ai_configuration` | `PROVIDER_CODE`, `API_KEY`, `DEFAULT_MODEL_NAME`, `IS_ACTIVE` | Уже есть (создана старой миграцией `260701-2-updateVacancyPromptTemplate01.sql`) |
| `hunttech_vacancy_prompt_template` | Все колонки | Уже есть |

### 4. Двойные таблицы (itpearls_ + hunttech_)

Обе группы таблиц содержат данные (с почти одинаковыми счётчиками). 
Требуется понять, какая схема активна, и есть ли синхронизация.

| Таблица | itpearls_ | hunttech_ |
|---------|-----------|-----------|
| job_candidate | 11 530 | 11 587 |
| open_position | 4 304 | 4 313 |
| iteraction | 125 | 125 |
| iteraction_list | 68 692 | 69 003 |
| person | 205 | 205 |
| company | 5 614 | 5 659 |

## Ключевые файлы

```
modules/core/db/changelog/db.changelog-master.xml
modules/core/db/changelog/260627-1-addAiEntities.xml
modules/core/db/changelog/260722-1-addUserAiProfile.xml
modules/core/db/changelog/260722-2-migrateUserAiProfileToHunttech.xml
modules/core/db/changelog/260723-1-addPreferPersonalAiApiSettings.xml
modules/core/db/changelog/260724-1-enablePersonalAiPreferences.xml

modules/global/src/com/company/hunttech/entity/UserSettings.java      (строка 32: PREFER_PERSONAL_AI_API_SETTINGS)
modules/global/src/com/company/hunttech/entity/UserAiProfile.java     (новая сущность)
modules/global/src/com/company/hunttech/entity/UserAiProfileParameters.java  (новая сущность)
```

## Задача

1. Подготовить план миграции **строго без потери данных**
2. План должен включать:
   - Какие таблицы/колонки создать (с `IF NOT EXISTS`)
   - Какие индексы добавить
   - Преобразование `IMAGE_ID → FILE_IMAGE_FACE` с сохранением данных
   - Какие already-applied колонки не трогать (mark_ran)
3. Создать новый Liquibase changelog файл, который можно применить к prod
4. Учесть, что part 1 (AI-entity миграции) уже частично применён старыми скриптами — нужны `preConditions` с `MARK_RAN`

## Запрещено
- DROP TABLE, DROP COLUMN, DELETE, TRUNCATE
- Изменение существующих данных без явного подтверждения
- Создание дублирующих колонок (если колонка уже есть в БД — MARK_RAN)

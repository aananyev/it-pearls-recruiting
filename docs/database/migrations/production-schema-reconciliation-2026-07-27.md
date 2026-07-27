# Сверка production-схемы PostgreSQL 11 с моделью HRM HuntTech

## Назначение и бизнес-смысл (What & Why)

Актуальная production-база HRM HuntTech формировалась историческими CUBA SQL-скриптами, тогда как часть новых AI-изменений была дополнительно оформлена Liquibase changelog-файлами. Из-за этого один и тот же бизнес-объект может уже существовать в БД, но отсутствовать в истории Liquibase, а прямой запуск старой последовательности приводит к повторному созданию таблиц или колонок.

Миграция `260727-1-reconcileProductionSchema.xml` выравнивает схему без удаления таблиц, колонок или строк. Каждый объект обрабатывается отдельным changeSet: отсутствующий объект создаётся, существующий получает статус `MARK_RAN`. Это позволяет применить один и тот же changelog к актуальной копии production-БД и к новой пустой схеме.

## UI Context & Navigation

Миграция не добавляет новый экран и не меняет навигацию. Она восстанавливает физический контракт для существующих функций:

- `ExtSettingsWindow` → вкладки AI и «Обо мне» → `UserSettings`, `UserAiConfiguration`, `UserAiProfile`;
- карточка вакансии → AI-описание, чек-лист интервью, карта поиска и план интервью → `HUNTTECH_OPEN_POSITION`;
- фотография пользователя → `UserSettings.fileImageFace` → `SYS_FILE`.

Двойные наборы таблиц `itpearls_*` и `hunttech_*` этой миграцией не объединяются. Их расхождение проверяется отдельно и не должно маскироваться автоматическим копированием строк.

## Behavior Summary

- объект отсутствует → precondition выполнен → Liquibase создаёт объект;
- объект уже существует → precondition не выполнен → changeSet фиксируется как `MARK_RAN`;
- `IMAGE_ID` существует, `FILE_IMAGE_FACE` отсутствует → колонка переименовывается → UUID фотографий сохраняются;
- обе колонки существуют → пустые `FILE_IMAGE_FACE` заполняются из `IMAGE_ID` → исходная колонка не удаляется;
- обе колонки отсутствуют → создаётся `FILE_IMAGE_FACE` → добавляются FK и индекс;
- обязательная базовая таблица отсутствует → changelog останавливается с `HALT` → частичная схема не маскируется;
- обнаружены `itpearls_*` и `hunttech_*` с разными данными → выполняется только read-only аудит → автоматическое объединение запрещено.

## 1. Исходное состояние

Подтверждённый разрыв актуальной копии production-БД:

| Объект | Состояние до миграции | Действие |
|---|---|---|
| `HUNTTECH_USER_AI_CONFIGURATION` | таблица уже существует | `MARK_RAN`; отдельно проверить FK и индекс |
| `HUNTTECH_VACANCY_PROMPT_TEMPLATE` | таблица уже существует | `MARK_RAN`; отдельно проверить уникальный индекс |
| AI-колонки `HUNTTECH_OPEN_POSITION` | часть/все уже существуют | каждая колонка проверяется отдельно |
| `HUNTTECH_USER_AI_PROFILE` | отсутствует | создать таблицу, FK и уникальный индекс |
| `PREFER_PERSONAL_AI_API_SETTINGS` | отсутствует | добавить `BOOLEAN NOT NULL DEFAULT TRUE` |
| `PREFER_PERSONAL_PROMPTS` | отсутствует | добавить `BOOLEAN NOT NULL DEFAULT TRUE` |
| `HUNTTECH_USER_SETTINGS.IMAGE_ID` | существует, данные сохранены | переименовать в `FILE_IMAGE_FACE` |
| `itpearls_*` и `hunttech_*` | обе группы содержат строки | не менять в рамках этой миграции |

## 2. Расхождение по таблице параметров ИИ-профиля

В актуальном `master` отсутствуют:

- Java-класс `UserAiProfileParameters`;
- `@CollectionTable` или иная связь, создающая таблицу параметров;
- Liquibase/CUBA DDL с перечнем колонок;
- документационный контракт таблицы `HUNTTECH_USER_AI_PROFILE_PARAMETERS`.

Поэтому reconciliation-changelog намеренно не создаёт эту таблицу. Создание структуры без entity-модели означало бы добавление неиспользуемого и неподтверждённого API БД. Отдельная миграция допустима только после появления согласованной сущности и точной DDL-спецификации.

## 3. Активный Liquibase-контур

`db.changelog-master.xml` включает только:

```text
260727-1-reconcileProductionSchema.xml
```

Исторические файлы сохранены в репозитории, но не выполняются напрямую:

```text
260627-1-addAiEntities.xml
260722-1-addUserAiProfile.xml
260722-2-migrateUserAiProfileToHunttech.xml
260723-1-addPreferPersonalAiApiSettings.xml
260724-1-enablePersonalAiPreferences.xml
```

Причина: первый файл не защищён preconditions от уже существующих объектов, а историческая миграция `260724-1` содержит массовое обновление существующих настроек. Изменять их задним числом нельзя из-за checksum и различий между БД.

## 4. Состав reconciliation-changelog

### 4.1. Вакансии

Каждая AI-колонка `HUNTTECH_OPEN_POSITION` добавляется отдельным changeSet:

- `RAW_DESCRIPTION`;
- `INTERVIEW_CHECKLIST`;
- `SEARCH_MAP`;
- `INTERVIEW_PLAN`.

### 4.2. Персональные AI-подключения

Для `HUNTTECH_USER_AI_CONFIGURATION`:

- таблица создаётся только при отсутствии;
- FK `USER_ID → SEC_USER.ID` создаётся только при отсутствии;
- индекс `IDX_HUNTTECH_USER_AI_CONFIGURATION_USER` создаётся только при отсутствии.

### 4.3. Шаблоны вакансий

Для `HUNTTECH_VACANCY_PROMPT_TEMPLATE`:

- таблица создаётся только при отсутствии;
- уникальный индекс `IDX_HUNTTECH_VACANCY_PROMPT_TEMPLATE_CODE` создаётся только при отсутствии.

### 4.4. Профиль пользователя для ИИ

Для `HUNTTECH_USER_AI_PROFILE`:

- создаётся полный набор полей, соответствующий `UserAiProfile`;
- добавляется FK `USER_ID → SEC_USER.ID`;
- добавляется уникальный индекс по `USER_ID`.

### 4.5. Предпочтения пользователя

В `HUNTTECH_USER_SETTINGS` добавляются:

```sql
PREFER_PERSONAL_AI_API_SETTINGS BOOLEAN NOT NULL DEFAULT TRUE
PREFER_PERSONAL_PROMPTS BOOLEAN NOT NULL DEFAULT TRUE
```

Отдельного массового `UPDATE` этих флагов нет. Значение `TRUE` применяется только при создании отсутствующих колонок и соответствует Java-default текущей entity-модели.

### 4.6. Фотография пользователя

Физический контракт после миграции:

```text
UserSettings.fileImageFace → HUNTTECH_USER_SETTINGS.FILE_IMAGE_FACE → SYS_FILE.ID
```

Варианты состояния обрабатываются так:

| `IMAGE_ID` | `FILE_IMAGE_FACE` | Результат |
|---|---|---|
| есть | нет | `renameColumn`, данные и ссылки сохраняются |
| есть | есть | заполнение только `FILE_IMAGE_FACE IS NULL` из `IMAGE_ID`; удаление запрещено |
| нет | нет | создаётся `FILE_IMAGE_FACE` |
| нет | есть | changeSet получает `MARK_RAN` |

FK и индекс переименовываются при наличии legacy-имён либо создаются, если отсутствуют.

## 5. Запрещённые операции

Changelog не содержит:

```text
DROP TABLE
DROP COLUMN
DELETE
TRUNCATE
```

Не выполняются:

- удаление или очистка legacy-таблиц;
- слияние строк `itpearls_*` и `hunttech_*`;
- перезапись существующих непустых `FILE_IMAGE_FACE`;
- изменение данных AI-конфигураций и шаблонов;
- изменение production без отдельной прямой команды.

## 6. Порядок проверки на копии production-БД

1. Зафиксировать точный HEAD PR и checksum дампа.
2. Создать отдельную disposable-БД из актуального дампа PostgreSQL 11.
3. Выполнить read-only preflight:
   - наличие обязательных базовых таблиц;
   - количество строк в обеих namespace-группах;
   - количество непустых `IMAGE_ID`;
   - список FK и индексов целевых таблиц.
4. Выполнить `liquibase validate`.
5. Сгенерировать `update-sql` и подтвердить отсутствие запрещённых операторов.
6. Применить changelog только к disposable-БД.
7. Повторно запустить Liquibase: второй запуск не должен генерировать DDL/DML.
8. Сравнить количество строк до/после во всех существовавших таблицах.
9. Подтвердить равенство UUID фотографии до/после.
10. Выполнить сборку, local deploy, HTTP 200 и smoke экранов через Hermes.

## 7. Read-only SQL для контроля

### 7.1. Счётчики двойного namespace

```sql
select 'job_candidate' as entity,
       (select count(*) from itpearls_job_candidate) as itpearls_count,
       (select count(*) from hunttech_job_candidate) as hunttech_count
union all
select 'open_position',
       (select count(*) from itpearls_open_position),
       (select count(*) from hunttech_open_position)
union all
select 'iteraction',
       (select count(*) from itpearls_iteraction),
       (select count(*) from hunttech_iteraction)
union all
select 'iteraction_list',
       (select count(*) from itpearls_iteraction_list),
       (select count(*) from hunttech_iteraction_list)
union all
select 'person',
       (select count(*) from itpearls_person),
       (select count(*) from hunttech_person)
union all
select 'company',
       (select count(*) from itpearls_company),
       (select count(*) from hunttech_company);
```

Запрос выполняется только если обе таблицы каждой пары существуют. Разница количества строк является блокером для автоматического объединения, но не блокирует additive reconciliation целевых `hunttech_*`-объектов.

### 7.2. Контроль фотографии

До миграции:

```sql
select count(*) as image_rows,
       count(distinct image_id) as distinct_images
from hunttech_user_settings
where image_id is not null;
```

После миграции:

```sql
select count(*) as image_rows,
       count(distinct file_image_face) as distinct_images
from hunttech_user_settings
where file_image_face is not null;
```

Оба результата должны совпасть.

### 7.3. Проверка целевых объектов

```sql
select table_name
from information_schema.tables
where table_schema = 'public'
  and lower(table_name) in (
      'hunttech_user_ai_configuration',
      'hunttech_vacancy_prompt_template',
      'hunttech_user_ai_profile'
  )
order by table_name;

select column_name, data_type, is_nullable, column_default
from information_schema.columns
where table_schema = 'public'
  and lower(table_name) = 'hunttech_user_settings'
  and lower(column_name) in (
      'file_image_face',
      'prefer_personal_ai_api_settings',
      'prefer_personal_prompts'
  )
order by column_name;
```

## 8. Rollback

Автоматический destructive rollback в changelog не определяется. Основной механизм возврата:

1. удалить disposable-БД при неуспехе проверки;
2. восстановить её из проверенного дампа;
3. исправить changelog отдельным коммитом;
4. повторить полный цикл на новой копии.

Для production rollback допустим только через заранее проверенный backup и отдельное решение Алексея. Production в рамках PR не изменяется.

## 9. Критерии READY_TO_MERGE

- HEAD ветки и HEAD PR совпадают с проверяемым SHA;
- conflicts = NONE;
- XML и Liquibase validation = PASS;
- статический тест reconciliation = PASS;
- `clean assemble` = `BUILD SUCCESSFUL`;
- применение на disposable PostgreSQL 11 = PASS;
- повторный запуск = no pending changes;
- количество строк существующих таблиц не уменьшилось;
- UUID фотографий сохранены;
- FK и индексы присутствуют;
- `ExtSettingsWindow`, карточка вакансии и загрузка фотографии = PASS;
- HTTP `/hrm/` = 200;
- Tomcat critical errors = NONE;
- P1 = 0, P2 = 0;
- production = NOT CHANGED.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Добавлен единый идемпотентный reconciliation-план для частично применённых AI-миграций, переименования `IMAGE_ID` и безопасной проверки двойного namespace |

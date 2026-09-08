# Отчет Аналитика: Полный аудит миграций локальной базы данных

**Дата аудита**: 2026-09-08  
**Роль**: Аналитик  
**База данных**: PostgreSQL 11 (`hunttech` на `127.0.0.1:5432`)  
**Репозиторий**: `hrm-antigravity` (`agent/antigravity-dev`)  

---

## 1. Резюме аудита

Был проведен комплексный аудит:
1. Сопоставление всех 738 файлов миграций из `modules/core/db/update/postgres/` с таблицей `sys_db_changelog` в PostgreSQL.
2. Сопоставление всех 49 модулей Liquibase changelog из `db.changelog-master.xml` с объектами БД.
3. Полный реверс-инжиниринг всех 73 JPA-сущностей проекта (667 замапленных колонок) и их сравнение с реальной схемой таблиц PostgreSQL (`information_schema.columns`).

### Ключевой вывод
Все зарегистрированные в каталоге `update/postgres/` миграции (738 файлов) в локальной БД **применены (100%)**.  
Однако выявлены **3 критические группы расхождений**, возникшие из-за того, что часть миграций в истории репозитория была создана исключительно в виде Liquibase XML без формирования парных `.sql` скриптов для CUBA `updateDb`:
1. Поля истории работы кандидата `HUNTTECH_JOB_HISTORY` (5 колонок + снятие NOT NULL).
2. Поля расширенного аудита вызовов AI `HUNTTECH_AI_CALL_LOG` (2 колонки).
3. Две системные AI-функции в `HUNTTECH_AI_FUNCTION_CONFIGURATION` (`COMPANY_REQUISITES_PARSE_JSON` и `COMPANY_WEB_SEARCH_PARSE_JSON`).

---

## 2. Детальный статус по группам миграций

### 2.1. Гео-справочники (`HUNTTECH_CITY`, `HUNTTECH_COUNTRY`, `HUNTTECH_REGION`)
* **Статус**: **ИСПРАВЛЕНО в текущей сессии**.
* **Что сделано**:
  * В БД добавлены отсутствовавшие колонки `FILE_CITY_EMBLEM_ID`, `FILE_FLAG_ID`, `FILE_REGION_EMBLEM_ID` со связями на `SYS_FILE(ID)` и индексами.
  * В `HUNTTECH_REGION` добавлена колонка `REGION_ENG_NAME VARCHAR(100)`.
  * Создана официальная миграция `260908-4-addGeoFlagsAndEmblems.sql` и Liquibase changelog `260908-4-addGeoFlagsAndEmblems.xml`, подключенный к `db.changelog-master.xml`.
  * Запись зарегистрирована в `sys_db_changelog`.

---

### 2.2. Непримененные миграции, требующие создания `.sql` скриптов на локальную БД

#### Группа A: Поля истории опыта работы (`HUNTTECH_JOB_HISTORY`)
* **Связанная сущность**: `JobHistory.java`.
* **Существующий changelog**: `260821-3-addJobHistoryFields.xml`.
* **Отсутствует в БД**:
  * `START_DATE date` — дата начала работы;
  * `END_DATE date` — дата окончания работы;
  * `DUTIES text` — обязанности кандидата;
  * `RAW_POSITION_NAME varchar(255)` — исходное наименование должности из резюме;
  * `RAW_COMPANY_NAME varchar(255)` — исходное наименование компании;
  * Снятие ограничения `NOT NULL` с колонки `CURRENT_POSITION_ID` (требуется для кандидатов, чья должность из резюме еще не сопоставлена со справочником).
* **Риск**: при открытии или сохранении карточки кандидата с блоком истории работы произойдет `PSQLException: ERROR: column "start_date" does not exist`.

#### Группа B: Поля аудита контекста AI (`HUNTTECH_AI_CALL_LOG`)
* **Связанная сущность**: `AiCallLog.java`.
* **Существующий changelog**: `260817-3-addAiCallLogContextColumns.xml`.
* **Отсутствует в БД**:
  * `CONTEXT_INCLUDED boolean` — признак включения пользовательского контекста;
  * `CONTEXT_CODE_POINTS integer` — объем контекста в символах.
* **Риск**: при логировании обращений к AI с персонализацией возникнет `PSQLException: ERROR: column "context_included" does not exist`.

#### Группа C: Конфигурации AI-функций анализа компаний (`HUNTTECH_AI_FUNCTION_CONFIGURATION`)
* **Связанные сервисы**: `CompanyRequisitesAiService`, `CompanySearchAiService`.
* **Существующие changelog**: `260821-2-addCompanyRequisitesAiFunction.xml` и `260822-2-addCompanyWebSearchAiFunction.xml`.
* **Отсутствует в БД**:
  * Запись с кодом `COMPANY_REQUISITES_PARSE_JSON`;
  * Запись с кодом `COMPANY_WEB_SEARCH_PARSE_JSON`.
* **Риск**: при нажатии кнопки автозаполнения реквизитов компании по ИНН или умного поиска компании в интернете сервис выдаст ошибку отсутствия активной AI-функции.

#### Группа D: Таблицы бухгалтерского Telegram-бота (`HUNTTECH_ACCOUNTING_*`)
* **Существующий changelog**: `260729-1-addAccountingBotEntities.xml`.
* **Отсутствует в БД**: 7 таблиц (`hunttech_accounting_company_alias`, `hunttech_accounting_email_batch`, `hunttech_accounting_document`, `hunttech_accounting_automation_settings`, `hunttech_accounting_expense_category`, `hunttech_accounting_recipient`, `hunttech_accounting_document_event`).
* **Статус**: ветка `agent/accounting-documents-telegram-ingest`. На основной функционал HRM-рекрутинга не влияет.

---

## 3. Сводная таблица необходимых SQL-скриптов для локальной БД

| № | Назначение | Требуемый DDL/DML | Приоритет |
|---|---|---|---|
| **1** | `HUNTTECH_JOB_HISTORY` | `ALTER TABLE HUNTTECH_JOB_HISTORY ALTER COLUMN CURRENT_POSITION_ID DROP NOT NULL;`<br>`ALTER TABLE HUNTTECH_JOB_HISTORY ADD COLUMN IF NOT EXISTS START_DATE date;`<br>`ALTER TABLE HUNTTECH_JOB_HISTORY ADD COLUMN IF NOT EXISTS END_DATE date;`<br>`ALTER TABLE HUNTTECH_JOB_HISTORY ADD COLUMN IF NOT EXISTS DUTIES text;`<br>`ALTER TABLE HUNTTECH_JOB_HISTORY ADD COLUMN IF NOT EXISTS RAW_POSITION_NAME varchar(255);`<br>`ALTER TABLE HUNTTECH_JOB_HISTORY ADD COLUMN IF NOT EXISTS RAW_COMPANY_NAME varchar(255);` | **Высокий** (защита от сбоев в карточке кандидата) |
| **2** | `HUNTTECH_AI_CALL_LOG` | `ALTER TABLE HUNTTECH_AI_CALL_LOG ADD COLUMN IF NOT EXISTS CONTEXT_INCLUDED boolean;`<br>`ALTER TABLE HUNTTECH_AI_CALL_LOG ADD COLUMN IF NOT EXISTS CONTEXT_CODE_POINTS integer;` | **Высокий** (защита от сбоев аудита AI) |
| **3** | `HUNTTECH_AI_FUNCTION_CONFIGURATION` | `INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (code = 'COMPANY_REQUISITES_PARSE_JSON'...) ON CONFLICT DO NOTHING;`<br>`INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION (code = 'COMPANY_WEB_SEARCH_PARSE_JSON'...) ON CONFLICT DO NOTHING;` | **Средний** (умный ввод реквизитов компаний) |
| **4** | `HUNTTECH_ACCOUNTING_*` | Создание 7 таблиц бухгалтерского модуля | **Низкий** (модуль бухгалтерского бота) |

---

## 4. Рекомендации

1. Подготовить консолидированный скрипт миграции `260908-5-reconcileMissingSchemaColumns.sql` для пунктов 1 и 2 (`HUNTTECH_JOB_HISTORY` и `HUNTTECH_AI_CALL_LOG`).
2. Применить DDL к локальной базе PostgreSQL и зарегистрировать в `sys_db_changelog`, что полностью исключит любые `PSQLException: ERROR: column ... does not exist` в остальных разделах HRM.

# План миграции структуры данных (DDL) и системных данных (DML) на Production (hr.hunttech.ru)

> **Дата составления**: 2026-09-08  
> **Окружение**: Production `hr.hunttech.ru` (92.63.101.170), PostgreSQL 11.22 (`hunttech`), Tomcat 9  
> **Статус плана**: **УСПЕШНО ПРИМЕНЁН НА ПРОД** (2026-09-08 22:37 MSK)  
> **Ответственный агент**: Разработчик (ветка `agent/antigravity-dev`)  
> **Бэкап**: `/opt/backups/hrm/20260908-212116-pre-migration` (полные дампы, кластер, fileStorage)

---

## ⚠ Базовое правило безопасности (МАНДАТОРНО)

> [!IMPORTANT]
> **Любые изменения данных или системных данных (промпты ИИ, справочники, дефолты) ВСЕГДА фиксируются в этом плане миграции и применяются при деплое на прод.**  
> **ПРЕЖДЕ чем применять любые миграции на прод, ОБЯЗАТЕЛЬНО выполняется полный бэкап базы данных (`pg_dump` / `pg_basebackup`) и артефактов (`fileStorage`, `war`), если пользователь явно не предложит иное.**

---

## 1. Сравнительный аудит (Local vs Production на 2026-09-08)

В ходе неразрушающего диагностического аудита в режиме чтения (`read-only`) выявлены следующие расхождения:

### 1.1. Таблицы (отсутствуют на PROD):
* **LLM Чат**:
  * `hunttech_llm_chat_conversation` — диалоговые сессии
  * `hunttech_llm_chat_message` — сообщения чата
  * `hunttech_llm_chat_quota_period` — периоды квот
  * `hunttech_llm_chat_quota_reservation` — резервации токенов
  * `hunttech_llm_user_quota_override` — индивидуальные квоты
* **Telegram-бот**:
  * `hunttech_candidate_bot_state` — сессии и состояния кандидатов
  * `hunttech_candidate_bot_stats` — аналитика бота
  * `hunttech_candidate_bot_migrations` — журнал миграций бота

### 1.2. Колонки (отсутствуют на PROD):
* `hunttech_company` (17 колонок): `inn`, `kpp`, `ogrn`, `okpo`, `oktmo`, `okved`, `legal_entity_name`, `legal_address`, `actual_address`, `postal_address`, `bik`, `bank_name`, `settlement_account`, `correspondent_account`, `phone`, `email`, `website`.
* `hunttech_country` (8 колонок): `alpha3_code`, `numeric_code`, `currency_code`, `capital`, `country_eng_name`, `flag_url`, `flag_image`, `file_flag_id`.
* `hunttech_region` (9 колонок): `region_eng_name`, `iso_code`, `fias_id`, `region_type`, `capital`, `time_zone`, `emblem_url`, `emblem_image`, `file_region_emblem_id`.
* `hunttech_city` (10 колонок): `city_eng_name`, `postal_code`, `fias_id`, `population`, `latitude`, `longitude`, `time_zone`, `emblem_url`, `emblem_image`, `file_city_emblem_id`.
* `hunttech_job_history` (5 колонок): `start_date`, `end_date`, `duties`, `raw_position_name`, `raw_company_name`, снятие `NOT NULL` с `current_position_id`.
* `hunttech_user_settings` (5 колонок): `llm_chat_button_position`, `geo_api_key`, `geo_api_secret`, `geo_api_url`, `geo_auto_fetch_flags`.
* `hunttech_user_ai_configuration` (4 колонки): `api_key_encrypted`, `is_primary`, `max_retries`, `priority_`.
* `hunttech_user_ai_profile` (3 колонки): `admin_fallback_consent`, `admin_fallback_consent_version`, `admin_fallback_consent_at`.
* `hunttech_ai_call_log` (5 колонок): `context_included`, `context_code_points`, `privacy_policy_version_snapshot`, `admin_fallback_consent_version_snapshot`, `external_processing_consent_version_snapshot`.
* `hunttech_ai_function_configuration` (3 колонки): `include_user_context`, `default_monthly_token_quota`, `privacy_policy_version`.

### 1.3. Системные данные (DML-расхождения на PROD):
* **AI-функции**: На проде только 9 функций; **отсутствуют 3 функции**: `COMPANY_REQUISITES_PARSE_JSON`, `COMPANY_WEB_SEARCH_PARSE_JSON`, `LLM_CHAT`. Функция `CV_SMART_PARSE_JSON` на проде имеет устаревшую версию 1 (без извлечения истории работы и образования).
* **Гео-данные**: На проде 165 регионов вместо 187; отсутствуют гербы регионов РФ (85 гербов) и флаги/коды стран (58 стран мира). Часть городов ссылается на неактуальные дубликаты регионов.
* **Настройки**: Кнопка LLM-чата не позиционирована, у пользователя `alan` нет безлимитной квоты административного ИИ и согласия на fallback.

---

## 2. Пошаговый план миграции на PROD

### Фаза 0. Обязательный бэкап данных (Safety Gate)

Перед запуском любых скриптов на проде выполнить:
```bash
# 1. Создание каталога бэкапа
BACKUP_DIR="/opt/backups/hrm/$(date +%Y%m%d-%H%M%S)-pre-migration"
mkdir -p "$BACKUP_DIR"

# 2. Полный логический дамп PostgreSQL (с данными и схемой)
su - postgres -c "pg_dump -Fc hunttech" > "$BACKUP_DIR/hunttech_pre_migration.dump"

# 3. Бэкап конфигурационных файлов
cp -a /opt/app_home/local.app.properties "$BACKUP_DIR/" 2>/dev/null || true
cp -a /var/lib/tomcat9/webapps/hrm/WEB-INF/local.app.properties "$BACKUP_DIR/" 2>/dev/null || true

# 4. Проверка целостности бэкапа
su - postgres -c "pg_restore -l $BACKUP_DIR/hunttech_pre_migration.dump | head -n 10"
```

---

### Фаза 1. Миграция структуры данных (DDL)

Все DDL-операции аддитивны и выполняются в рамках единого батча:

1. **Реквизиты компаний и опыт кандидатов**:
   * `260821-1-addCompanyRequisitesColumns.sql`
   * `260821-3-addJobHistoryFields.sql`
   * `260821-6-addCompanyLegalEntityName.sql`
2. **Гео-справочники (BLOB, коды, связи)**:
   * `260822-1-addUserSettingsGeoApiFields.sql`
   * `260823-1-addGeoBlobImageFields.sql`
   * `260908-4-addGeoFlagsAndEmblems.sql` (добавление `FILE_FLAG_ID`, `FILE_REGION_EMBLEM_ID`, `FILE_CITY_EMBLEM_ID`, `REGION_ENG_NAME` с FK на `SYS_FILE`)
3. **Фундамент LLM-чата, квотирование и шифрование**:
   * `260904-1-addAdminFallbackConsent.sql`
   * `260904-2-addLlmChatFoundation.sql`
   * `260904-3-addLlmChatQuotaTables.sql`
   * `260904-4-addUserAiEncryptedKey.sql`
   * `260904-5-addLlmChatRequestId.sql`
   * `260904-6-addLlmChatReconciliationAudit.sql`
   * `260905-1-addAiAuditSecuritySnapshots.sql`
   * `260907-1-addLlmChatButtonPosition.sql`
   * `260908-2-addUserAiConfigurationFallbackFields.sql`

---

### Фаза 2. Миграция системных данных (DML)

1. **Обогащение гео-справочников**:
   * Накат `260823-2-migrateAllRussiaRegions.sql` (85 субъектов РФ, ISO-коды, ссылки Wikimedia).
   * Накат `260823-3-updateCountriesFullInfo.sql` (58 стран, Alpha-3 коды, флаги URL+BLOB, столицы, валюты).
   * Накат `260908-5-reconcileMissingSchemaAndGeoLinks.sql` (автоматическая перепривязка всех городов и компаний с удалённых дубликатов на канонические регионы РФ).
2. **Инициализация и актуализация AI-функций (Control Plane)**:
   * Seed недостающих функций: `COMPANY_REQUISITES_PARSE_JSON`, `COMPANY_WEB_SEARCH_PARSE_JSON`, `LLM_CHAT` (через changelog `260908-5` / `260904-2`).
   * Обновление промпта `CV_SMART_PARSE_JSON` до v2 (с поддержкой `education` и `workExperience` согласно `260821-4`).
   * Накат дефолтов `INCLUDE_USER_CONTEXT` по матрице уместности персонализации (`260817-2`).
   * Актуализация системного промпта `LLM_CHAT` с персонализацией по «Обо мне» и активация профилей (`260908-6-updateLlmChatPromptAndPersonalization.sql`).
3. **Пользовательские настройки и квоты**:
   * `260907-2-setLlmChatDefaultPositionBottomRight.sql` (дефолтная позиция кнопки чата в правый нижний угол).
   * `260908-1-setAlanFallbackAndChatAdminConfig.sql` (привязка LLM_CHAT к активному провайдеру).
   * `260908-3-setAlanUnlimitedLlmQuota.sql` (безлимитная месячная квота административного API для `alan`).
   * Инициализация профиля `HUNTTECH_USER_AI_PROFILE` для `alan` с `ADMIN_FALLBACK_CONSENT = true`.
4. **Интеграции**:
   * `260821-7-configureTelegramBotSetup.sql` (активация Telegram-бота в активной конфигурации `HUNTTECH_APPLICATION_SETUP`).

---

### Фаза 3. Пост-проверочный аудит (Validation Gate)

После применения миграций запустить скрипт валидации:
1. Проверить отсутствие `null` в критических связях:
   ```sql
   SELECT count(*) FROM hunttech_city WHERE delete_ts IS NULL AND city_region_id IN (SELECT id FROM hunttech_region WHERE delete_ts IS NOT NULL);
   -- Должно быть 0
   ```
2. Проверить наличие всех 12 AI-функций:
   ```sql
   SELECT count(*) FROM hunttech_ai_function_configuration WHERE is_active = true;
   -- Должно быть 12
   ```
3. Проверить статус флагов стран и гербов РФ:
   ```sql
   SELECT count(*) FROM hunttech_region WHERE delete_ts IS NULL AND emblem_url IS NOT NULL;
   -- Должно быть >= 85
   ```
4. Убедиться, что `sys_db_changelog` зарегистрировал все примененные скрипты с префиксом `70-hunttech_recruiting/`.

---


---

## 4. Журнал точечных синхронизаций данных (DML)

### 4.1. Синхронизация профиля пользователя alan (2026-09-09 08:22 UTC)
- **Цель**: перенос профессионального ИИ-профиля (`UserAiProfile` / `HUNTTECH_USER_AI_PROFILE`) пользователя `alan` (`a9c2a715-96a4-42c2-bbb1-5603739d4fb4`) из локальной базы на Production.
- **Ограничения**: `ExtUser` (`sec_user`) не затрагивается; изменения вносятся только в `hunttech_user_ai_profile`.
- **Примененные данные**:
  - `current_position`: Директор центра разработки программного обеспечения
  - `functional_role`: 80 (Executive)
  - `seniority_level`: 60 (Executive)
  - `professional_experience_years`: 29
  - `recruiting_experience_years`: 11
  - `about_me`, `current_responsibilities`, `education`, `certifications`, `domain_expertise`, `industries`, `recruiting_specializations`, `target_roles`, `candidate_levels`, `hiring_geographies`, `decision_priorities`, `client_project_context`, `professional_goals`, `professional_interests`, `development_areas`, `current_priorities`.
- **Статус**: Успешно применен (`UPDATE 1`, версия 4).


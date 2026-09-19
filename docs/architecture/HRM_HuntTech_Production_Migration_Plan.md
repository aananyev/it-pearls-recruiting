# План безопасной миграции структуры БД и служебных промптов на продакшен

> **Статус документа:** Готов к исполнению при запросе деплоя на прод  
> **Исполнение:** Будет запущено ТОЛЬКО по прямому запросу пользователя («сделай деплой на прод-сервер»)  
> **Правило:** `.agents/rules/safe-db-and-prompt-migrations.md`  

---

## 1. Объем и назначение миграций

Миграция охватывает изменения схемы базы данных и системных AI-функций, включая:
1. **Журнал объяснения требований вакансий AI (DDL):**
   - Создание таблицы `HUNTTECH_OP_AI_EXPLANATION_LOG` (хранение текста объяснений, инициатора, модели, токенов, длительности).
   - Индексы: `IDX_HUNTTECH_OP_AI_EXP_LOG_OP`, `IDX_HUNTTECH_OP_AI_EXP_LOG_USER`, `IDX_HUNTTECH_OP_AI_EXP_LOG_TIME`, `IDX_HUNTTECH_OP_AI_EXP_LOG_TYPE`.
   - Внешние ключи на `HUNTTECH_OPEN_POSITION` и `SEC_USER`.
2. **Служебные промпты и функции AI (DML):**
   - Сидирование функций `VACANCY_EXPLAIN_REQUIREMENTS` (стандартный анализ через `AiExecutionService`) и `VACANCY_EXPLAIN_SIMPLIFIED_WEB` (житейские объяснения через Hermes).
   - Калибровка системных промптов и синхронизация параметров `INCLUDE_USER_CONTEXT`.
3. **Сопутствующие расширения структуры:**
   - Таблица запланированных собеседований `HUNTTECH_PENDING_INTERVIEW_EVENT` и поля календаря Yandex 360.
   - Поля иконки должности и логотипа дерева навыков.

---

## 2. Пререквизиты и безопасность данных (Zero Downtime / Zero Loss)

Перед выполнением любых операций на продакшене обязательно создается полный резервный дамп:

```bash
# 1. Каталог бэкапа с меткой времени
BACKUP_DIR=/opt/backups/deploy_$(date +%Y%m%d_%H%M%S)
mkdir -p "$BACKUP_DIR"

# 2. Полный бинарный дамп PostgreSQL (сжатый формат -Fc)
PGPASSWORD="${PROD_DB_PASSWORD}" pg_dump -U cuba -h localhost -Fc hunttech > "$BACKUP_DIR/hunttech_pre_migration.dump"

# 3. Проверка целостности дампа
pg_restore -l "$BACKUP_DIR/hunttech_pre_migration.dump" > /dev/null && echo "✅ Бэкап валиден"
```

---

## 3. Реестр миграционных скриптов (Порядок применения)

Миграции оформлены канонически в `modules/core/db/update/postgres/26/` и зарегистрированы в Liquibase:

| № | Скрипт | Тип | Назначение |
|---|---|---|---|
| 1 | `260912-1-addPositionIconFields.sql` | DDL | Добавление полей иконок должностей |
| 2 | `260912-2-addSkillTreeLogoImage.sql` | DDL | Добавление логотипа дерева навыков |
| 3 | `260913-1-addMaxContextTokensToAiConfigs.sql` | DDL/DML | Поле `MAX_CONTEXT_TOKENS` и системный промпт чата |
| 4 | `260913-addEnhancedSmartVacancyParseAiFunction.sql` | DML | Функция умного парсинга вакансий |
| 5 | `260913-updateStandardizeVacancyPrompt.sql` | DML | Калибровка промпта стандартизации вакансий |
| 6 | `260913-updateVacancyChecklistPrompt.sql` | DML | Обновление промпта чеклиста |
| 7 | `260913-updateSearchMapAndInterviewPlanPrompts.sql` | DML | Обновление промптов карты поиска и плана интервью |
| 8 | `260914-1-addExtraTokensToQuotaPeriod.sql` | DDL | Добавление экстра-токенов в квоты |
| 9 | `260914-2-updateYandexCalendarAndInteractionEventColumns.sql` | DDL | Поля интеграции календаря Yandex |
| 10 | `260914-3-preseedCorporateYandexCalendars.sql` | DML | Корпоративные календари по умолчанию |
| 11 | `260915-updateSmartVacancyHtmlPrompts.sql` | DML | HTML форматирование описаний вакансий |
| 12 | `260916-addInterviewSchedulingActionAndPendingTable.sql` | DDL | Таблица `HUNTTECH_PENDING_INTERVIEW_EVENT` |
| 13 | `260918-1-grantManagerHermesPermissions.sql` | DML | Права доступа к Hermes для менеджеров |
| 14 | `260918-2-createOpenPositionAiExplanationLogTable.sql` | DDL | Таблица журнала объяснений вакансий `HUNTTECH_OP_AI_EXPLANATION_LOG` |
| 15 | `260918-3-seedVacancyRequirementExplanationAiFunctions.sql` | DML | AI-функции `VACANCY_EXPLAIN_REQUIREMENTS` и `VACANCY_EXPLAIN_SIMPLIFIED_WEB` |
| 16 | `260918-4-syncAiFunctionsAndPrompts.sql` | DML | Синхронизация версий промптов и контекстов пользователей |

---

## 4. Транзакционный сценарий применения миграций

Все скрипты выполняются строго в единой транзакции с остановкой при любой ошибке (`ON_ERROR_STOP=1`) и обязательной регистрацией в служебной таблице CUBA `sys_db_changelog`:

```bash
# Транзакционный прогон через psql
psql -v ON_ERROR_STOP=1 -U cuba -h localhost hunttech << 'EOF'
BEGIN;

-- 1. Структурные таблицы и колонки
\i modules/core/db/update/postgres/26/260912-1-addPositionIconFields.sql
\i modules/core/db/update/postgres/26/260912-2-addSkillTreeLogoImage.sql
\i modules/core/db/update/postgres/26/260913-1-addMaxContextTokensToAiConfigs.sql
\i modules/core/db/update/postgres/26/260914-1-addExtraTokensToQuotaPeriod.sql
\i modules/core/db/update/postgres/26/260914-2-updateYandexCalendarAndInteractionEventColumns.sql
\i modules/core/db/update/postgres/26/260916-addInterviewSchedulingActionAndPendingTable.sql
\i modules/core/db/update/postgres/26/260918-2-createOpenPositionAiExplanationLogTable.sql

-- 2. Служебные промпты и системные настройки AI
\i modules/core/db/update/postgres/26/260913-addEnhancedSmartVacancyParseAiFunction.sql
\i modules/core/db/update/postgres/26/260913-updateStandardizeVacancyPrompt.sql
\i modules/core/db/update/postgres/26/260913-updateVacancyChecklistPrompt.sql
\i modules/core/db/update/postgres/26/260913-updateSearchMapAndInterviewPlanPrompts.sql
\i modules/core/db/update/postgres/26/260914-3-preseedCorporateYandexCalendars.sql
\i modules/core/db/update/postgres/26/260915-updateSmartVacancyHtmlPrompts.sql
\i modules/core/db/update/postgres/26/260918-1-grantManagerHermesPermissions.sql
\i modules/core/db/update/postgres/26/260918-3-seedVacancyRequirementExplanationAiFunctions.sql
\i modules/core/db/update/postgres/26/260918-4-syncAiFunctionsAndPrompts.sql

-- 3. Регистрация в sys_db_changelog (защита от повторного запуска CUBA DbUpdate)
INSERT INTO sys_db_changelog (script_name, create_ts, is_init) VALUES
('70-hunttech_recruiting/update/postgres/26/260912-1-addPositionIconFields.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260912-2-addSkillTreeLogoImage.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260913-1-addMaxContextTokensToAiConfigs.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260913-addEnhancedSmartVacancyParseAiFunction.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260913-updateStandardizeVacancyPrompt.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260913-updateVacancyChecklistPrompt.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260913-updateSearchMapAndInterviewPlanPrompts.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260914-1-addExtraTokensToQuotaPeriod.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260914-2-updateYandexCalendarAndInteractionEventColumns.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260914-3-preseedCorporateYandexCalendars.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260915-updateSmartVacancyHtmlPrompts.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260916-addInterviewSchedulingActionAndPendingTable.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260918-1-grantManagerHermesPermissions.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260918-2-createOpenPositionAiExplanationLogTable.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260918-3-seedVacancyRequirementExplanationAiFunctions.sql', CURRENT_TIMESTAMP, 0),
('70-hunttech_recruiting/update/postgres/26/260918-4-syncAiFunctionsAndPrompts.sql', CURRENT_TIMESTAMP, 0)
ON CONFLICT DO NOTHING;

COMMIT;
EOF
```

---

## 5. Валидация и глубокая сверка (Deep Diff)

Сразу после применения миграций выполняется автоматическая проверка:
1. **Проверка таблицы журнала:**
   ```sql
   SELECT table_name, column_name, data_type 
   FROM information_schema.columns 
   WHERE table_name = 'hunttech_op_ai_explanation_log'
   ORDER BY ordinal_position;
   ```
2. **Проверка сидирования AI-функций:**
   ```sql
   SELECT code, name, default_model, capability, include_user_context 
   FROM hunttech_ai_function_configuration 
   WHERE code IN ('VACANCY_EXPLAIN_REQUIREMENTS', 'VACANCY_EXPLAIN_SIMPLIFIED_WEB');
   ```
   Ожидаемый результат: 2 активные записи.
3. **Сверка отсутствия незарегистрированных скриптов в `sys_db_changelog`:**
   ```sql
   SELECT script_name FROM sys_db_changelog WHERE script_name LIKE '%260918%' ORDER BY create_ts;
   ```

---

## 6. План отката (Rollback Plan)

В случае непредвиденных сбоев:
1. **Быстрый откат транзакции:** Если скрипт упал внутри `BEGIN ... COMMIT`, PostgreSQL автоматически откатывает транзакцию полностью без изменения данных.
2. **Восстановление из дампа (при необходимости):**
   ```bash
   pg_restore -U cuba -h localhost -d hunttech --clean --if-exists "$BACKUP_DIR/hunttech_pre_migration.dump"
   ```

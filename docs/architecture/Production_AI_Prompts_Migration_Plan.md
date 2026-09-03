# План миграции и переноса AI-промптов и схемы на Production

> **Целевая среда**: Production (HRM HuntTech / IT-Pearls)  
> **Исполнитель**: Hermes-1 (CI/CD) / Ведущий администратор БД  
> **Дата формирования**: 2026-08-20  

---

## 1. Состав изменений для переноса на Production

### 1.1 Структура базы данных (DDL)
1. **Колонка `TELEGRAM` в таблице пользователей `SEC_USER`**:
   - Поле: `TELEGRAM varchar(64)` для сущности `ExtUser`.
   - Файлы миграций: `260820-2-addTelegramToSecUser.sql` и `260820-2-addTelegramToSecUser.xml`.

### 1.2 Административные AI-промпты (AI Control Plane Seed Data)
1. **`CV_SMART_PARSE_JSON`** — Умное распознавание резюме кандидата в структурированный JSON (ФИО, контакты, опыт, стек, зарплата, компания, город).
   - Файлы: `260820-1-addSmartCvParseAiFunction.sql` и `260820-1-addSmartCvParseAiFunction.xml`.
2. **`VACANCY_SMART_PARSE_JSON`** — Умное распознавание описания вакансии и проекта в структурированный JSON (название, проект, компания, условия, грейд, стек, локации, памятка).
   - Файлы: `260820-3-addSmartVacancyParseAiFunction.sql` и `260820-3-addSmartVacancyParseAiFunction.xml`.

---

## 2. Пошаговый план переноса (Runbook / Rollout Steps)

### Шаг 0. Подготовка и резервное копирование (Pre-deployment Backup)
Перед выполнением миграций на проде обязательно создание дампа базы:
```bash
# 1. Дамп текущей продакшн базы
pg_dump -h $PROD_DB_HOST -U cuba -d hunttech -F c -b -v -f "/backup/hunttech_pre_260820_$(date +%Y%m%d_%H%M%S).dump"
```

---

### Шаг 1. Применение SQL-миграций к Production PostgreSQL
Выполняется от имени пользователя `cuba` с включенным флагом `ON_ERROR_STOP=1`:

```bash
# 1. Добавление колонки TELEGRAM в SEC_USER
psql -h $PROD_DB_HOST -U cuba -d hunttech -v ON_ERROR_STOP=1 \
     -f modules/core/db/update/postgres/26/260820-2-addTelegramToSecUser.sql

# 2. Сид AI-функции умного парсинга резюме (CV_SMART_PARSE_JSON)
psql -h $PROD_DB_HOST -U cuba -d hunttech -v ON_ERROR_STOP=1 \
     -f modules/core/db/update/postgres/26/260820-1-addSmartCvParseAiFunction.sql

# 3. Сид AI-функции умного парсинга вакансий (VACANCY_SMART_PARSE_JSON)
psql -h $PROD_DB_HOST -U cuba -d hunttech -v ON_ERROR_STOP=1 \
     -f modules/core/db/update/postgres/26/260820-3-addSmartVacancyParseAiFunction.sql
```

> **Важно:** Все скрипты идемпотентны (`WHERE NOT EXISTS` и `ADD COLUMN IF NOT EXISTS`). При повторном запуске они не вызывают конфликтов и не перезаписывают пользовательские настройки, если администратор уже редактировал промпты в UI Control Plane.

---

### Шаг 2. Верификация данных в БД (Post-migration DB Verification)
Проверка успешности применения:
```bash
psql -h $PROD_DB_HOST -U cuba -d hunttech -c "
  SELECT code, name, capability, execution_policy, is_active 
  FROM hunttech_ai_function_configuration 
  WHERE code IN ('CV_SMART_PARSE_JSON', 'VACANCY_SMART_PARSE_JSON');
"

psql -h $PROD_DB_HOST -U cuba -d hunttech -c "
  SELECT column_name, data_type 
  FROM information_schema.columns 
  WHERE table_name = 'sec_user' AND column_name = 'telegram';
"
```
**Критерий успеха**: возвращаются обе AI-функции со статусом `is_active = true` и колонка `telegram`.

---

### Шаг 3. Деплой новой версии сборки приложения (Application Release)
1. Выполнить слияние ветки в `master` (через Hermes-1 / CI-CD pipeline).
2. Запустить штатную сборку и деплой Tomcat.
3. Проверить старт middleware-блока `hrm-core` и веб-интерфейса `hrm`.

---

### Шаг 4. Смоук-тестирование на Production (Smoke Tests)
1. **Проверка Telegram-поля**: Открыть карточку пользователя `ExtUserEdit`, проверить наличие поля ввода Telegram и сохранение значения.
2. **Проверка умной загрузки резюме**: В реестре кандидатов открыть диалог «Умная загрузка» 🪄, загрузить тестовое резюме, проверить AI-распознавание и сохранение карточки кандидата.
3. **Проверка AI Control Plane**: Открыть экран администрирования AI-функций, убедиться, что `CV_SMART_PARSE_JSON` и `VACANCY_SMART_PARSE_JSON` доступны для аудита и настройки провайдера/модели.

---

## 3. План отката (Rollback Strategy)

В случае непредвиденных сбоев:
```sql
-- 1. Удаление seeded функций (если требуется чистый откат)
DELETE FROM HUNTTECH_AI_FUNCTION_CONFIGURATION 
WHERE CODE IN ('CV_SMART_PARSE_JSON', 'VACANCY_SMART_PARSE_JSON') 
  AND CREATED_BY = 'migration';

-- 2. Удаление колонки TELEGRAM (опционально, колонка nullable и не ломает старый код)
-- ALTER TABLE SEC_USER DROP COLUMN IF EXISTS TELEGRAM;
```

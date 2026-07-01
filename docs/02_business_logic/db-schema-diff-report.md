# Отчёт: расхождения схемы PostgreSQL и модели приложения HRM HuntTech

> **Дата анализа:** 2026-06-30  
> **Аудитория:** разработчики, бизнес-аналитики, DevOps  
> **Связанные документы:** [LOCAL_DATABASE.md](../LOCAL_DATABASE.md) · [user-settings-photo-sync.md](user-settings-photo-sync.md) · [ExtUser](../entities/ExtUser.md)  
> **Техническая сводка (для DBA):** [scripts/db-migration/README-schema-diff-2026-06-30.md](../../scripts/db-migration/README-schema-diff-2026-06-30.md)

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-30 | Первичный отчёт: сравнение live БД `itpearls` с entity-моделью и отложенными миграциями пакета `26` |

---

## Краткий вывод (Executive Summary)

| Вопрос | Ответ |
|--------|-------|
| **Нужна ли модификация БД?** | **Да, частично** |
| **Блокирует ли работу всего приложения?** | Нет — основные модули (кандидаты, вакансии без AI, взаимодействия) работают |
| **Что именно отстаёт?** | Подсистема AI (вакансии, шаблоны, ключи пользователей) и новая модель фото пользователей |
| **Скрипты уже подготовлены?** | Да — в репозитории, **не применены** к локальной БД |
| **Рекомендуемое действие** | Выполнить `./gradlew updateDb` после бэкапа (предпочтительно) |

**Простыми словами:** код приложения уже «ждёт» новые колонки и две таблицы, которых в базе ещё нет. Пока миграции не применены, часть недавних функций (AI-помощник по вакансиям, личные AI-ключи, официальное и личное фото пользователя) будет падать при сохранении или работать некорректно.

---

## Что сравнивали

1. **Модель приложения (ожидаемая схема)** — Java-сущности в `modules/global/src/com/company/itpearls/entity/`, расширения CUBA (`ExtUser` → таблица `SEC_USER`), Liquibase-описание в `modules/core/db/changelog/`.
2. **Живая база PostgreSQL** — `itpearls` на `127.0.0.1:5432`, пользователь `cuba`. Сверка через `information_schema` и журнал миграций CUBA `SYS_DB_CHANGELOG`.
3. **Отложенные скрипты в репозитории** — папка `modules/core/db/update/postgres/26/` (пакеты `260627` и `260629`).

**Последняя применённая миграция в БД:** пакет `240325` (запись от 2026-06-26).  
**Не применены:** все скрипты `260627-*` и `260629-*`.

> Примечание: при повторной проверке на другой среде (тест, прод) сверьте `SYS_DB_CHANGELOG` — состояние может отличаться.

---

## Таблица отличий

### Критичные расхождения (требуют миграции)

| № | Область (бизнес) | Таблица | Ожидает приложение | Есть в БД сейчас | Критичность | Скрипт в репо |
|---|------------------|---------|-------------------|------------------|-------------|---------------|
| 1 | **Фото пользователя** — официальное фото (админ) и личный аватар (настройки) | `SEC_USER` | Колонки `OFFICIAL_PHOTO_ID`, `USER_AVATAR_ID` (ссылки на файл) | Только устаревшая `IMAGE_ID` | **Блокирует** сохранение фото на экранах «Пользователь» и «Настройки» | `260629-2-updateSecUser.sql` (+ `01`, `02` — FK, индексы, перенос данных) |
| 2 | **AI-поля вакансии** — черновик описания, чеклист интервью, карта поиска, план интервью | `ITPEARLS_OPEN_POSITION` | `RAW_DESCRIPTION`, `INTERVIEW_CHECKLIST`, `SEARCH_MAP`, `INTERVIEW_PLAN` | Колонки отсутствуют | **Блокирует** сохранение AI-полей на форме вакансии | `260627-1-updateOpenPosition.sql` |
| 3 | **Персональные AI-настройки** — API-ключ и модель LLM для каждого пользователя | `ITPEARLS_USER_AI_CONFIGURATION` | Таблица целиком (7 бизнес-колонок + стандартные CUBA) | Таблица отсутствует | **Блокирует** экран настройки AI пользователя | `260627-2-createUserAiConfiguration.sql`, `260627-4-updateUserAiConfiguration.sql` |
| 4 | **Шаблоны промптов для вакансий** — справочник текстов для AI | `ITPEARLS_VACANCY_PROMPT_TEMPLATE` | Таблица целиком (`CODE`, `NAME`, `PROMPT_TEXT`, …) | Таблица отсутствует | **Блокирует** CRUD шаблонов промптов | `260627-3-createVacancyPromptTemplate.sql`, `260627-4-updateUserAiConfiguration.sql` |

### Уже в синхронизации (миграция не нужна)

| Область | Таблица | Статус |
|---------|---------|--------|
| Персональные настройки (legacy-фото) | `ITPEARLS_USER_SETTINGS` | Колонка `IMAGE_ID` есть — соответствует `UserSettings.fileImageFace` |
| Трудовые договоры | `ITPEARLS_LABOR_AGREEMENT` | Все поля entity присутствуют |
| Фото кандидата | `ITPEARLS_JOB_CANDIDATE` | Поле `FILE_IMAGE_FACE` / связи в порядке |
| Связи вакансия ↔ договор | `ITPEARLS_OPEN_POSITION_LABOR_AGREEMENT_LINK` и др. | Join-таблицы существуют |
| Настройки сжатия изображений | `app.properties` (`HunttechImageConfig`) | **Не колонки БД** — только конфигурация приложения |

### Не являются проблемой (ложные срабатывания при автосравнении)

| Что видит автосравнение | Почему это нормально |
|-----------------------|----------------------|
| Колонки `OPEN_POSITION_ID`, `LABOR_AGREEMENT_ID` в link-таблицах | Это поля связующих таблиц `@JoinTable`, а не основной entity |
| `COMPANY_DEPARTMENT_ID` в `IteractionList` | Закомментирован в Java — намеренно не используется |
| `DTYPE` в `ITPEARLS_OPEN_POSITION` | Legacy-дискриминатор CUBA в БД, не объявлен в entity — не блокер |

---

## Бизнес-контекст отличий (для аналитиков)

### 1. Фото пользователей (`SEC_USER`)

Раньше одно поле `IMAGE_ID` хранило «фото профиля». В новой модели:

- **Официальное фото** (`officialPhoto`) — задаёт администратор на экране редактирования пользователя.
- **Личный аватар** (`userAvatar`) — задаёт сам пользователь в окне «Настройки».

Без новых колонок приложение не может сохранить эти два типа фото раздельно. Скрипт `260629-2-updateSecUser02.sql` **переносит** существующие данные: `IMAGE_ID` → официальное фото, фото из `UserSettings` → личный аватар.

Подробнее: [user-settings-photo-sync.md](user-settings-photo-sync.md).

### 2. AI-подсистема (пакет `260627`)

Три связанных изменения для интеграции LLM:

| Функция | Что даёт бизнесу |
|---------|------------------|
| AI-поля вакансии | Хранение черновиков и структурированных материалов (чеклист, план интервью) для AI-генерации |
| `UserAiConfiguration` | Персональный API-ключ и модель — пользователь подключает свой провайдер |
| `VacancyPromptTemplate` | Переиспользуемые шаблоны промптов для генерации текстов вакансий |

---

## Подготовленные SQL-скрипты (не выполнялись)

### Штатные CUBA-миграции (порядок важен)

```
modules/core/db/update/postgres/26/
├── 260627-1-updateOpenPosition.sql          — 4 text-колонки в вакансии
├── 260627-2-createUserAiConfiguration.sql   — CREATE TABLE персональных AI-настроек
├── 260627-3-createVacancyPromptTemplate.sql — CREATE TABLE шаблонов промптов
├── 260627-4-updateUserAiConfiguration.sql   — FK, индексы, unique на CODE
├── 260629-2-updateSecUser.sql               — ADD COLUMN officialPhoto, userAvatar
├── 260629-2-updateSecUser01.sql             — FK + индексы на фото
└── 260629-2-updateSecUser02.sql             — перенос данных из legacy IMAGE_ID
```

Дублирование в Liquibase: `modules/core/db/changelog/260627-1-addAiEntities.xml` (только пакет `260627`, **без** `260629` для фото).

### Идемпотентный скрипт (ручное применение)

| Файл | Назначение |
|------|------------|
| [scripts/db-migration/260630-schema-sync-pending-idempotent.sql](../../scripts/db-migration/260630-schema-sync-pending-idempotent.sql) | Объединяет все изменения пакетов `260627` и `260629` с `IF NOT EXISTS` / `DO $$` — безопасен для повторного запуска |
| [scripts/db-migration/README-schema-diff-2026-06-30.md](../../scripts/db-migration/README-schema-diff-2026-06-30.md) | Техническая сводка для DBA |

**Что делает идемпотентный скрипт:**

1. Добавляет 4 AI-колонки в `ITPEARLS_OPEN_POSITION`.
2. Создаёт таблицы `ITPEARLS_USER_AI_CONFIGURATION` и `ITPEARLS_VACANCY_PROMPT_TEMPLATE` с индексами и FK.
3. Добавляет `OFFICIAL_PHOTO_ID`, `USER_AVATAR_ID` в `SEC_USER`.
4. Переносит существующие фото из legacy-полей (однократно, идемпотентно).

---

## Рекомендации: что делать дальше

### Вариант A — штатный CUBA (рекомендуется для разработки)

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
cd /Users/alekseyananyev/StudioProjects/it-pearls-recruiting

# 1. Бэкап
pg_dump -h 127.0.0.1 -U cuba -d itpearls -Fc -f backup-before-updateDb.dump

# 2. Применить только незаписанные в SYS_DB_CHANGELOG скрипты
./gradlew updateDb --no-daemon
```

CUBA автоматически применит скрипты из `modules/core/db/update/postgres/` и зарегистрирует их в `SYS_DB_CHANGELOG`.

### Вариант B — ручной идемпотентный SQL

Подходит, если `updateDb` недоступен (recovery mode, нет Gradle) или нужен контролируемый запуск:

```bash
export PGPASSWORD=cuba
psql -h 127.0.0.1 -p 5432 -U cuba -d itpearls \
  -f scripts/db-migration/260630-schema-sync-pending-idempotent.sql
```

**Важно:** после ручного применения не запускайте `updateDb` без регистрации скриптов в `SYS_DB_CHANGELOG` — CUBA попытается выполнить те же `ALTER` повторно и упадёт. Либо используйте только `updateDb`, либо только ручной скрипт + ручная запись в changelog.

### Вариант C — ничего не делать

Допустимо **только** если команда не использует:

- AI-функции вакансий и шаблоны промптов;
- персональные AI-ключи;
- новые поля официального/личного фото пользователя.

Все остальные модули (кандидаты, взаимодействия, справочники) продолжат работать.

---

## Проверка после миграции

```sql
-- Последние применённые пакеты
SELECT script_name, create_ts
FROM sys_db_changelog
ORDER BY create_ts DESC
LIMIT 10;

-- Новые колонки пользователя
SELECT column_name FROM information_schema.columns
WHERE table_name = 'sec_user'
  AND column_name IN ('official_photo_id', 'user_avatar_id');

-- AI-таблицы
SELECT table_name FROM information_schema.tables
WHERE table_name IN (
  'itpearls_user_ai_configuration',
  'itpearls_vacancy_prompt_template'
);

-- AI-колонки вакансии
SELECT column_name FROM information_schema.columns
WHERE table_name = 'itpearls_open_position'
  AND column_name IN ('raw_description', 'interview_checklist', 'search_map', 'interview_plan');
```

---

## Риски и ограничения

| Риск | Митигация |
|------|-----------|
| БД в режиме recovery (read-only после restore) | Создать чистую БД — см. [LOCAL_DATABASE.md](../LOCAL_DATABASE.md) § «recovery» |
| Дублирование миграций | Использовать **один** способ: `updateDb` **или** ручной идемпотентный скрипт |
| Потеря данных при переносе фото | Скрипт `260629-02` только **копирует** ссылки на файлы, не удаляет `IMAGE_ID` |
| Прод-среда | Обязательный бэкап; тестировать на копии; окно обслуживания ~1–2 мин |

---

## Итоговая матрица критичности

| Критичность | Количество | Действие |
|-------------|------------|----------|
| **Блокирует** работу новых функций | 4 области | Применить миграции |
| **Косметика / legacy** | 3 пункта (DTYPE, link-таблицы, закомментированные поля) | Ничего |
| **Уже есть скрипт** | Все 4 блокирующих | `updateDb` или идемпотентный SQL |

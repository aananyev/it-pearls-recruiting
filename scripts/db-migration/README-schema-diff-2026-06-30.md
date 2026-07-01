# Сравнение схемы PostgreSQL и модели сущностей HRM HuntTech

**Дата проверки:** 2026-06-30  
**БД:** `itpearls` @ `127.0.0.1:5432`, пользователь `cuba`  
**Источник ожидаемой схемы:** `modules/global/src/com/company/itpearls/entity/`, `persistence.xml`, `modules/core/db/update/postgres/`

## Итог

**На момент первичной проверки (2026-06-30) схема локальной БД отставала от приложения.** Отложенные миграции: `modules/core/db/update/postgres/26/260627-*`, `260629-*`.

> **Локальная БД `itpearls`:** после проверки синтаксиса скрипта структура приведена в соответствие с entity (колонки `SEC_USER`, `ITPEARLS_OPEN_POSITION`, таблицы AI). Записей в `SYS_DB_CHANGELOG` для `260627`/`260629` по-прежнему нет — на других стендах используйте `./gradlew updateDb` или идемпотентный SQL ниже.

Последние записи в `SYS_DB_CHANGELOG` — пакет `240325` (2026-06-26). Скрипты `260627-*` и `260629-*` **не применялись**.

## Расхождения (критичные для текущего кода)

| Объект | Ожидается (entity / скрипты) | В live БД | Влияние |
|--------|------------------------------|-----------|---------|
| `SEC_USER` | `OFFICIAL_PHOTO_ID`, `USER_AVATAR_ID` (ExtUser) | только `IMAGE_ID` (legacy) | ошибки при сохранении офиц./личного фото пользователя |
| `ITPEARLS_OPEN_POSITION` | `RAW_DESCRIPTION`, `INTERVIEW_CHECKLIST`, `SEARCH_MAP`, `INTERVIEW_PLAN` | колонки отсутствуют | AI-поля вакансии, экран OpenPosition Edit |
| `ITPEARLS_USER_AI_CONFIGURATION` | таблица целиком | таблица отсутствует | настройки AI пользователя |
| `ITPEARLS_VACANCY_PROMPT_TEMPLATE` | таблица целиком | таблица отсутствует | шаблоны промптов для вакансий |

## В синхронизации (не требуют миграции)

| Объект | Примечание |
|--------|------------|
| `ITPEARLS_USER_SETTINGS` | `IMAGE_ID` (fileImageFace) — **есть**, соответствует entity |
| `ITPEARLS_LABOR_AGREEMENT` | все колонки entity присутствуют |
| `ITPEARLS_JOB_CANDIDATE` | `FILE_IMAGE_FACE` / связанные поля — в порядке |
| Join-таблицы (`ITPEARLS_OPEN_POSITION_LABOR_AGREEMENT_LINK` и др.) | существуют |
| `HunttechImageConfig` | свойства приложения (`app.properties`), **не колонки БД** |

## Ложные расхождения при автосравнении

- Колонки `@JoinTable` (`OPEN_POSITION_ID`, `LABOR_AGREEMENT_ID` в link-таблицах) — не колонки основной таблицы.
- `COMPANY_DEPARTMENT_ID` в `IteractionList` — закомментирован в Java.
- `DTYPE` в `ITPEARLS_OPEN_POSITION` — legacy в БД, не объявлен в entity (не блокер).

## Существующие CUBA-скрипты (неидемпотентные, порядок важен)

```
modules/core/db/update/postgres/26/260627-1-updateOpenPosition.sql
modules/core/db/update/postgres/26/260627-2-createUserAiConfiguration.sql
modules/core/db/update/postgres/26/260627-3-createVacancyPromptTemplate.sql
modules/core/db/update/postgres/26/260627-4-updateUserAiConfiguration.sql
modules/core/db/update/postgres/26/260629-2-updateSecUser.sql
modules/core/db/update/postgres/26/260629-2-updateSecUser01.sql
modules/core/db/update/postgres/26/260629-2-updateSecUser02.sql
```

## Рекомендуемый способ применения

### Вариант A — штатный CUBA (предпочтительно)

```bash
export JAVA_HOME=/Users/alekseyananyev/Library/Java/JavaVirtualMachines/corretto-11.0.17/Contents/Home
cd /Users/alekseyananyev/StudioProjects/it-pearls-recruiting
./gradlew updateDb --no-daemon
```

Gradle применит только незаписанные в `SYS_DB_CHANGELOG` скрипты.

### Вариант B — ручной идемпотентный SQL

```bash
export PGPASSWORD=cuba
psql -h 127.0.0.1 -p 5432 -U cuba -d itpearls \
  -f scripts/db-migration/260630-schema-sync-pending-idempotent.sql
```

После ручного применения **не** дублировать через `updateDb` без записи в `SYS_DB_CHANGELOG` — иначе CUBA попытается выполнить те же ALTER повторно. Либо зарегистрировать скрипты в changelog вручную, либо использовать только `updateDb`.

## Блокеры

Подключение к БД успешно (пароль `cuba` из `modules/core/web/META-INF/context.xml`).

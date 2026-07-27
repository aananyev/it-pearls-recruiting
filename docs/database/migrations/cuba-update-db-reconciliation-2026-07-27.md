# CUBA updateDb: безопасная сверка production-схемы

## Назначение и бизнес-смысл (What & Why)

HRM HuntTech должен запускаться на актуальной копии production PostgreSQL 11 без повторного выполнения исторических миграций и без ручного создания AI-таблиц. Production-история содержит старый component prefix `70-IT-Pearls`, а текущая сборка CUBA формирует другой путь тех же SQL-файлов. Из-за различия путей `updateDb` воспринимал уже выполненные миграции как новые и останавливался на destructive legacy-командах, например на удалении отсутствующего индекса.

Решение сохраняет данные и возвращает штатный механизм CUBA: новый update-скрипт выполняется через `CubaDbUpdate`, а после успеха его фактический assembled path автоматически записывается в `SYS_DB_CHANGELOG`.

## UI Context & Navigation

Миграция не меняет экранные формы и навигацию. Её результат используется при открытии:

- `ExtSettingsWindow` → вкладки AI и «Обо мне»;
- экранов вакансий, использующих AI-поля `HUNTTECH_OPEN_POSITION`;
- компонентов фотографии пользователя, читающих `HUNTTECH_USER_SETTINGS.FILE_IMAGE_FACE`.

## Behavior Summary

- `updateDb` на обычной или пустой БД → совпадение полного update/postgres suffix отсутствует → legacy baseline не вмешивается, CUBA работает штатно;
- `updateDb` на production-копии → для исторического script найдено точное совпадение полного пути после `/update/postgres/` под старым component folder → регистрируется только alias этого доказанно выполненного script;
- после baseline → CUBA видит новым только `260727-2-reconcileProductionSchema.sql` → запускает идемпотентную сверку и регистрирует её автоматически;
- `HUNTTECH_USER_AI_PROFILE` существует частично → каждая entity-колонка добавляется через `ADD COLUMN IF NOT EXISTS`;
- обязательный `USER_ID` отсутствует в существующей строке → миграция останавливается без удаления данных, потому что владельца нельзя достоверно восстановить;
- повторный `updateDb` → новых scripts нет → DDL/DML повторно не выполняются.

## 1. Два механизма миграций

Проект исторически содержит два представления изменений:

1. CUBA database scripts в `modules/core/db/update/postgres/` — фактически исполняются `updateDb` и серверным `DbUpdaterEngine`;
2. Liquibase XML в `modules/core/db/changelog/` — используется как декларативная спецификация и для внешней сверки.

`CubaDbUpdate` не запускает XML Liquibase. Поэтому добавлен CUBA-native mirror:

```text
modules/core/db/update/postgres/26/260727-2-reconcileProductionSchema.sql
```

После успешного выполнения CUBA сама добавляет assembled path этого SQL в `SYS_DB_CHANGELOG`. Ручная вставка имени XML-файла в CUBA-историю запрещена: XML не является CUBA update script.

## 2. Baseline исторических aliases

Файл:

```text
gradle/cuba-db-baseline.gradle
```

подключается к `app-core` из `settings.gradle` и добавляет `doFirst` к существующему `updateDb`.

Перед основной CUBA-проверкой он:

1. читает scripts, собранные `assembleDbScripts`;
2. выбирает только scripts текущего приложения, расположенные раньше `26/260727-2-reconcileProductionSchema.sql`;
3. проверяет наличие `SEC_USER` и `SYS_DB_CHANGELOG`;
4. для каждого текущего historical script ищет ранее выполненную запись с тем же полным suffix после `/update/postgres/`;
5. регистрирует только доказанные assembled aliases через `ON CONFLICT DO NOTHING`;
6. не запускает и не изменяет содержимое исторических SQL-файлов.

Baseline не применяется к app components CUBA, BPM, Reports, FTS и другим зависимостям. Для диагностики его можно отключить только явным параметром:

```bash
./gradlew updateDb -PskipLegacyCubaBaseline --no-daemon --stacktrace
```

Это диагностический режим; штатный локальный запуск использует baseline.

## 3. Полнота UserAiProfile

`UserAiProfile.java` содержит 34 физических связи/колонки: 33 `@Column` и `USER_ID` из `@JoinColumn`.

Полный контракт присутствует одновременно в:

- `260727-1-reconcileProductionSchema.xml` — создание отсутствующей таблицы;
- `260727-2-completeUserAiProfileColumns.xml` — дополнение частично существующей таблицы;
- `260727-2-reconcileProductionSchema.sql` — фактическое CUBA-исполнение.

Follow-up changelog добавляет каждую колонку отдельно и приводит обязательные Boolean-поля к `NOT NULL DEFAULT false`. Строка без `USER_ID` не исправляется догадкой: выполнение останавливается с диагностическим исключением.

## 4. Состав CUBA reconciliation SQL

Скрипт идемпотентно проверяет и создаёт:

- AI-колонки `HUNTTECH_OPEN_POSITION`;
- `HUNTTECH_USER_AI_CONFIGURATION`, FK и индекс;
- `HUNTTECH_VACANCY_PROMPT_TEMPLATE` и уникальный индекс;
- полную `HUNTTECH_USER_AI_PROFILE`, FK, уникальный индекс и checks опыта;
- `PREFER_PERSONAL_AI_API_SETTINGS` и `PREFER_PERSONAL_PROMPTS`;
- переход `IMAGE_ID → FILE_IMAGE_FACE` с сохранением UUID;
- FK и индекс `FILE_IMAGE_FACE`.

Не создаётся неподтверждённая таблица `HUNTTECH_USER_AI_PROFILE_PARAMETERS`: entity и DDL-контракт для неё отсутствуют.

## 5. Ограничения безопасности

В новых migration-артефактах отсутствуют:

- `DROP TABLE`;
- `DROP COLUMN`;
- `DELETE`;
- `TRUNCATE`.

Разрешены только:

- additive DDL;
- rename `IMAGE_ID → FILE_IMAGE_FACE`;
- копирование UUID фотографии в пустую целевую колонку;
- заполнение `NULL` обязательных Boolean-полей утверждёнными default-значениями;
- регистрация alias в `SYS_DB_CHANGELOG` только при точном совпадении полного suffix после `/update/postgres/`; новый или отсутствующий в старой истории script остаётся pending.

## 6. Проверка

Обязательные проверки Hermes:

```bash
git diff --check

./gradlew :app-core:test \
  --tests 'com.company.hunttech.core.DatabaseSchemaReconciliationChangelogTest' \
  --no-daemon --stacktrace

./gradlew test \
  --tests '*ScreenViewIntegrityTest*' \
  --no-daemon --stacktrace

./gradlew clean assemble --no-daemon --stacktrace
```

На disposable-копии PostgreSQL 11:

1. сохранить counts и UUID фотографий;
2. выполнить `./gradlew updateDb`;
3. подтвердить регистрацию `260727-2-reconcileProductionSchema.sql` в `SYS_DB_CHANGELOG`;
4. подтвердить отсутствие попытки запуска `19/191022-1-updateCountry.sql`;
5. сравнить все 34 колонки `UserAiProfile` с entity;
6. повторить `updateDb` и подтвердить отсутствие pending scripts;
7. проверить counts, FK, индексы и broken `SYS_FILE` references.

## 7. Rollback

Автоматический destructive rollback не предусмотрен. При ошибке:

1. прекратить проверку;
2. восстановить disposable-БД из backup;
3. не удалять добавленные колонки на production;
4. исправить migration-логику новым changeset/script;
5. повторить проверку на новом точном HEAD.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-27 | Добавлен CUBA-native reconciliation SQL, baseline aliases старого component prefix и отдельная сверка всех колонок частично созданного `UserAiProfile` |

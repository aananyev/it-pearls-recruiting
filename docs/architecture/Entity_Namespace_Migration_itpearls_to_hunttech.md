# Запрет legacy-сущностей `itpearls_*` и контракт namespace `hunttech_*`

## Статус документа

- Проект: **HRM HuntTech**
- Платформа: CUBA Platform 7.3
- Статус решения: обязательный архитектурный контракт
- Дата введения: 2026-07-26

## Назначение и Бизнес-смысл (What & Why)

HRM HuntTech после переноса production-данных работает в едином прикладном namespace `hunttech` и использует таблицы `HUNTTECH_*`. Сохранение активных CUBA entity names `itpearls_*` создаёт риск двойной регистрации модели, обращения приложения к несуществующим таблицам, ошибок сериализации сохранённых настроек и повторного появления legacy-объектов при обновлении БД.

Цель решения — гарантировать, что все рабочие сущности HRM HuntTech имеют CUBA name `hunttech_*`, Java package `com.company.hunttech.entity` и таблицу `HUNTTECH_*` либо явно документированную системную таблицу CUBA, например `SEC_USER` для `ExtUser`.

## UI Context & Navigation

Namespace сущности является сквозным контрактом для экранов, loaders, views, JPQL, lookup-компонентов, отчётов, фильтров и сохранённых пользовательских настроек. Изменение не добавляет и не удаляет элементы интерфейса, но защищает все точки входа приложения:

- меню и browse-экраны;
- edit-экраны и lookup;
- dashboard widgets;
- loaders и data containers;
- фоновые сервисы и scheduled tasks;
- отчёты и сериализованные CUBA-настройки.

## Behavior Summary

- Регистрация project entity → metadata-name начинается с `hunttech_` → сущность доступна через единую модель HRM HuntTech.
- Попытка добавить `@Entity(name = "itpearls_...")` → namespace guard обнаруживает нарушение → профильный тест завершается ошибкой до deploy.
- JPQL или XML обращается к `itpearls_*` → source guard обнаруживает активную legacy-ссылку → сборка не допускается к Hermes smoke.
- Исторический migration-скрипт проверяет `ITPEARLS_*` → файл находится в разрешённой migration-области → проверка не блокирует совместимость старых инсталляций.
- Запуск приложения на базе `hunttech` → legacy metadata model не регистрируется → CUBA не создаёт и не запрашивает project entities `itpearls_*`.

## 1. Обязательный runtime-контракт

Для каждого Java-класса из `com.company.hunttech.entity`:

1. `@Entity(name = "hunttech_<EntityName>")`;
2. прикладная таблица — `@Table(name = "HUNTTECH_<TABLE_NAME>")`;
3. JPQL использует `hunttech_<EntityName>`;
4. XML views и data containers используют `hunttech_<EntityName>`;
5. runtime metadata не содержит project meta classes с префиксом `itpearls_`;
6. `modules/global/src/com/company/hunttech/metadata.xml` регистрирует только root package `com.company.hunttech` с namespace `hunttech`.

Исключение для системных таблиц CUBA допускается только при явной аннотации расширения. Например, `hunttech_ExtUser` расширяет `sec$User` и хранится в `SEC_USER`.

## 2. Строгий запрет

В рабочих исходниках запрещены:

- `@Entity(name = "itpearls_...")`;
- `@Table(name = "ITPEARLS_...")`;
- JPQL `from`, `join`, `update`, `delete from` для `itpearls_*`;
- XML `entity="itpearls_..."` и `class="itpearls_..."`;
- metadata lookup `getClass()`, `getClassNN()` и аналогичные обращения к `itpearls_*`;
- native SQL к `ITPEARLS_*` в runtime Java/XML-конфигурации;
- регистрация metadata model `root-package="com.company.itpearls" namespace="itpearls"`.

Запрет применяется к:

- `modules/global/src/`;
- `modules/core/src/`;
- `modules/web/src/`;
- runtime-конфигурации приложения;
- unit-, integration- и integrity-тестам, кроме тестовых строк, которые явно проверяют отсутствие legacy-name.

Legacy screen IDs, имена исторических файлов, ключи сообщений и архивные документы не считаются CUBA entity names и не переименовываются автоматически.

## 3. Разрешённая migration-область

Упоминания `itpearls_*` и `ITPEARLS_*` разрешены только там, где они нужны для переноса или проверки старой инсталляции:

- `modules/core/db/update/`;
- ранее применённые Liquibase changesets;
- `deployment/database-migration/`;
- `scripts/db-migration/`;
- архивные migration-, backup- и rollback-отчёты.

Исторические migration-файлы не редактируются механической заменой. Новая коррекция БД оформляется отдельным идемпотентным changeset с точным отображением legacy-name → `hunttech_*`, проверкой количества строк и отдельным rollback.

## 4. Зафиксированное состояние на 2026-07-26

Аудит `master` выявил:

- `ExtUser` уже зарегистрирован как `hunttech_ExtUser`;
- `JobCandidate` уже зарегистрирован как `hunttech_JobCandidate`;
- integrity-тест сохранял устаревшие ожидания `itpearls_ExtUser` и `itpearls_JobCandidate`;
- `metadata.xml` одновременно регистрировал пустой legacy root package `com.company.itpearls` и актуальный `com.company.hunttech`;
- production-база `hunttech` после миграции не содержит relations с префиксом `itpearls_*` по зафиксированному migration-отчёту;
- исторические migration-скрипты намеренно сохраняют legacy-имена для совместимости и rollback.

## 5. Реализация защиты от регрессии

Обязательный `EntityNamespaceIntegrityTest` проверяет:

1. все зарегистрированные project meta classes из `com.company.hunttech.entity` имеют CUBA name `hunttech_*`;
2. runtime metadata не содержит project meta classes `itpearls_*`;
3. `metadata.xml` не регистрирует `com.company.itpearls`/`itpearls`;
4. рабочие Java/XML/properties-файлы не содержат активных legacy entity/table references;
5. нарушения выводятся полным списком файлов и найденных контрактов.

`ScreenViewIntegrityTest` сохраняет обязательные `8/8 PASS`, но проверяет `hunttech_ExtUser` и `hunttech_JobCandidate`.

## 6. Контроль данных

Повторная массовая миграция production-данных в рамках этого изменения запрещена. Перед любым DML требуется отдельный read-only аудит строковых metadata references в `sec_*`, `sys_*`, сохранённых фильтрах, user settings, reports и entity snapshots.

Если аудит обнаружит активные значения `itpearls_*`, создаётся отдельная задача и отдельный PR с:

- backup и restore rehearsal;
- идемпотентным changeset;
- row counts до/после;
- FK/PK/index/sequence validation;
- проверкой `sec_user`, `sys_file` и physical file storage;
- rollback без широкого неконтролируемого `replace()`.

## 7. Проверка Hermes

Hermes проверяет точный HEAD PR без изменения функционального кода:

```bash
git diff --check

./gradlew :app-core:compileTestJava \
          :app-web:compileJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.EntityNamespaceIntegrityTest' \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew clean assemble --no-daemon --stacktrace
```

После local deploy:

- `http://localhost:8080/hrm/` → HTTP 200;
- startup log не содержит unknown entity `itpearls_*`;
- приложение не выполняет SQL к `ITPEARLS_*`;
- открываются `ExtSettingsWindow`, список и карточка кандидата;
- сохраняются пользовательские настройки и карточка кандидата без изменения бизнес-поведения;
- Tomcat critical errors: NONE;
- P1 = 0, P2 = 0.

## 8. Rollback

Кодовый rollback — возврат `metadata.xml` и integrity-тестов к предыдущему HEAD. Изменение не содержит DDL/DML, не меняет данные и не требует rollback БД.

Возвращать legacy metadata model разрешено только для диагностики конкретной подтверждённой несовместимости. Постоянная двойная регистрация namespace запрещена.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-07-26 | Введён строгий запрет project entities `itpearls_*`, описана allowlist исторических миграций, namespace guard, проверка Hermes и rollback |

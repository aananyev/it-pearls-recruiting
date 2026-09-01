# План миграции AI-подсистемы и гео-справочников: локаль → прод

> **Статус:** план. Прод НЕ ТРОГАЕМ на этом этапе.
> **Дата анализа:** 2026-09-01
> **Сравниваемые среды:** `hunttech` БД на `127.0.0.1:5432` (локаль) и `hr.hunttech.ru:5432` (прод)
> **Запретная зона:** схемы `itpearls_*` (по требованию) — НЕ ТРОГАЕМ.
> **Целевая зона:** только `hunttech_*` для AI и гео (`country`, `city`, `region` при наличии, `person`).

---

## 1. Текущее состояние (факты, собранные 2026-09-01)

### 1.1 Применённые миграции (`sys_db_changelog`)

| Скрипт | Локаль | Прод |
|---|---|---|
| `260812-1..260818-1` (AI control plane) | ✓ | ✓ |
| `260820-1/2/3` (SmartCV, Telegram, SmartVacancy) | ✓ | ✓ |
| `260821-1..7` (CompanyRequisites+JobHistory+TelegramBotSetup) | ✓ | ✗ |
| `260822-2` (CompanyWebSearchAiFunction) | ✓ | ✗ |
| `260822-1` (GeoFlagsAndEmblems) — SQL напрямую | ✓ | ✗ |
| `260822-3` (GeoAttributesColumns) — SQL напрямую | ✓ | ✗ |
| `260823-1/2/3` (GeoBlob/AllRussiaRegions/CountriesFullInfo) — SQL напрямую | ✓ | ✗ |

### 1.2 Состав таблиц (структура, а не данные)

| Таблица | Локаль | Прод | Δ-колонки (нужны миграции) |
|---|---|---|---|
| `hunttech_country` | 19 кол. | 11 кол. | +`country_eng_name`, `alpha3_code`, `numeric_code`, `currency_code`, `capital`, `flag_image`, `flag_url` |
| `hunttech_city` | 23 кол. | 12 кол. | +`city_eng_name`, `postal_code`, `fias_id`, `population`, `latitude`, `longitude`, `time_zone`, `emblem_image`, `emblem_url`, `file_city_emblem_id` |
| `hunttech_region` | — | — | новая сущность (создаётся в 260823-2) |
| `hunttech_admin_ai_configuration` | 1 row | 1 row | структура =, ключ уже синхронизирован |
| `hunttech_ai_function_configuration` | 11 rows | 9 rows | +`COMPANY_REQUISITES_PARSE_JSON`, +`COMPANY_WEB_SEARCH_PARSE_JSON`; +`configuration_version` для `CV_SMART_PARSE_JSON` |
| `hunttech_user_ai_configuration` | 1 row | 1 row | = (уже одинаковые UUID) |
| `hunttech_user_ai_function_override` | 1 row | 1 row | = |
| `hunttech_user_ai_profile` | 1 row | 0 rows | локальная черновик-запись `alan` |
| `hunttech_person` | 25 кол. | 25 кол. | = структура |
| `sys_db_changelog` | 2026-08-27 | 2026-08-21 | разрыв 6 дней миграций |

### 1.3 Ключевая проблема: миграции 260821-1..7, 260822-1/2/3, 260823-1/2/3

- `260821-1..7` и `260822-2` — **есть** как `<include>` в `db.changelog-master.xml` (260821-1..7 + 260822-2) и как XML-файлы. При следующем деплое на прод они автоматически накатятся через CUBA Liquibase (есть `<preConditions onFail="MARK_RAN">` — идемпотентно).
- `260822-1` (GeoFlagsAndEmblems), `260822-3` (GeoAttributesColumns), `260823-1/2/3` (GeoBlob, AllRussiaRegions, CountriesFullInfo) — **НЕТ** ни как XML-changelog, ни как `<include>` в `master.xml`. На локали их выполнили прямым SQL в БД. На проде их физически нет, и Liquibase о них не знает.

### 1.4 Данные, требующие миграции

| Источник | Объём | Куда |
|---|---|---|
| `hunttech_country` ISO/flag/capital | 62 страны (только с заполненными `alpha3_code`) | дополнить в проде `hunttech_country` |
| `hunttech_region` (85 субъектов РФ) | 85 строк (после 260823-2) | создать + заполнить на проде |
| `hunttech_ai_function_configuration` 2 строки | 2 | вставить с `INSERT ... ON CONFLICT` |
| `CV_SMART_PARSE_JSON` `configuration_version` | +1 (1→2) | `UPDATE WHERE code=…` |
| `hunttech_user_ai_profile` черновик `alan` | 1 строка | по решению пользователя (мигрировать или нет) |
| `hunttech_person` (7 лишних) | 7 | НЕ мигрируем (новые локальные кандидаты) |

---

## 2. Цели миграции

1. Поднять схему `hunttech_country`/`hunttech_city`/`hunttech_region` на проде до уровня локали (без `itpearls_*`).
2. Поднять справочник AI-функций до уровня локали (2 новых + bump version 1→2 для CV_SMART_PARSE_JSON).
3. Не сломать прод-данные: `admin_ai_configuration` (зашифрованный ключ), `user_ai_configuration`/`override` уже синхронизированы — не трогаем.
4. Зарегистрировать в Liquibase все «прямые» SQL 260822-1/3 и 260823-1/2/3, чтобы при следующих деплоях они считались уже применёнными.
5. Не запускать прод-DDL без репетиции на локали.

---

## 3. Препятствия и риски

| Риск | Митигация |
|---|---|
| Liquibase на проде может конфликтовать с уже выполненным `updateDb` (отказы `preConditions` на отсутствующие колонки) | Использовать `<preConditions onFail="MARK_RAN">` и `IF NOT EXISTS` в DDL |
| Прямые SQL (260822-1/3, 260823-1/2/3) могут залить флаги в BYTEA как многомегабайтные payload'ы | Долгий lock; выполнять в maintenance window |
| `hunttech_country` дубли (например, «Беларусь» + «Белоруссия», оба с `BY`/`BLR`) | Сохранять обе записи, как на локали; не мерджить |
| 7 лишних `hunttech_person` на локали — это НЕ прод-данные | Не мигрировать `person` |
| `itpearls_*` (гео-параллельная схема) уже есть на проде, **трогать запрещено** | Все DDL — только с префиксом `HUNTTECH_`; явно проверять в `<preConditions onFail="MARK_RAN">` через `<tableExists tableName="HUNTTECH_COUNTRY"/>` |
| `ai_function_configuration.system_prompt`/`prompt_template` содержат большие тексты с PII-нюансами | Мигрировать как есть (те же тексты), не редактировать |
| `admin_ai_configuration.api_key_encrypted` — единственный реальный секрет | Уже одинаковый UUID/значение, не перетирать |
| Прод-деплой вне maintenance window | Все операции только в окно 02:00–05:00 МСК (см. существующую практику deploy-prod.sh) |

---

## 4. План по фазам (ВСЁ ЧЕРНОВИК, НА ПРОД НЕ ВЫХОДИМ)

### Фаза 0 — Подготовка (1–2 дня, локально)

0.1. Сверить `build.gradle` версию: текущая `0.392`, бамп `0.393` (или `0.394`).
0.2. Снять эталонные дампы **только локали** для регрессионного теста:
```
pg_dump --schema-only --no-owner --no-privileges -t 'hunttech_country' -t 'hunttech_city' -t 'hunttech_region' -t 'hunttech_ai_*' -t 'hunttech_user_ai_*' -t 'hunttech_admin_ai_configuration' hunttech > /tmp/migplan/local_schema_before.sql
pg_dump --data-only --no-owner --no-privileges -t 'hunttech_country' -t 'hunttech_city' -t 'hunttech_region' -t 'hunttech_ai_*' -t 'hunttech_user_ai_*' -t 'hunttech_admin_ai_configuration' hunttech > /tmp/migplan/local_data_before.sql
```
0.3. Создать второй локальный клон БД `hunttech_rehearsal` (на той же Postgres `127.0.0.1:5432`):
```
createdb -h 127.0.0.1 -U cuba hunttech_rehearsal
pg_restore /tmp/.../local_schema_before.sql | psql -h 127.0.0.1 -U cuba -d hunttech_rehearsal
```
0.4. Сверить `information_schema.columns` после миграции (Фаза 1+2+3) — должно стать == локали.

### Фаза 1 — Схема: обёртки для «потерянных» SQL (1 день)

Все 4 новых XML-файла + регистрация в `db.changelog-master.xml`. Идемпотентные `<preConditions>`.

1.1. **`modules/core/db/changelog/260822-1-addGeoFlagsAndEmblems.xml`** — обёртка над уже существующим SQL-логикой (восстановить по `260823-1-addGeoBlobImageFields.sql` + тому, что было до 260823-1). Поля:
- `HUNTTECH_COUNTRY.FLAG_URL`, `HUNTTECH_COUNTRY.FLAG_IMAGE`
- `HUNTTECH_CITY.EMBLEM_URL`, `HUNTTECH_CITY.EMBLEM_IMAGE`
- `HUNTTECH_REGION.EMBLEM_URL`, `HUNTTECH_REGION.EMBLEM_IMAGE`
- `HUNTTECH_COUNTRY.FILE_FLAG_ID`, `HUNTTECH_CITY.FILE_CITY_EMBLEM_ID` (FileDescriptor-ссылки)
- `<preConditions onFail="MARK_RAN"><not><columnExists tableName="HUNTTECH_COUNTRY" columnName="FLAG_IMAGE"/></not></preConditions>`
- DDL через `<addColumn>` с `IF NOT EXISTS` либо блок `<sql splitStatements="true">…</sql>`.

1.2. **`modules/core/db/changelog/260822-3-addGeoAttributesColumns.xml`** — расширение `HUNTTECH_COUNTRY` (eng_name, alpha3, numeric, currency, capital) и `HUNTTECH_CITY` (eng_name, postal_code, fias_id, population, lat, lon, time_zone). Тот же подход.

1.3. **`modules/core/db/changelog/260823-1-addGeoBlobImageFields.xml`** — пустышка (`<sql>SELECT 1;</sql>` с `<preConditions><columnExists tableName="HUNTTECH_COUNTRY" columnName="FLAG_IMAGE"/></preConditions>`) ИЛИ новый `<changeSet>` который НЕ выполняется, если колонка уже есть. Идея: **просто зарегистрировать как применённый** через `<insert tableName="sys_db_changelog" …/>`? Нет — в CUBA это не `sys_db_changelog`, а `databasechangelog` (Liquibase). Используем `<changeSet>` с пустым `<sql>` под `<preConditions onFail="MARK_RAN">`.

1.4. **`modules/core/db/changelog/260823-2-migrateAllRussiaRegions.xml`** — вызов существующего `260823-2-migrateAllRussiaRegions.sql` через `<sqlFile path="…/260823-2-migrateAllRussiaRegions.sql" relativeToChangelogFile="true"/>` под `<preConditions><sqlCheck expectedResult="0">SELECT count(*) FROM hunttech_region</sqlCheck></preConditions>` (или `>0` → MARK_RAN, т.е. уже выполнено).

1.5. **`modules/core/db/changelog/260823-3-updateCountriesFullInfo.xml`** — вызов `260823-3-updateCountriesFullInfo.sql`. Аналогичный `<preConditions>`: если для `country_ru_name='Германия'` `alpha3_code='DEU'`, MARK_RAN.

1.6. Дописать в `db.changelog-master.xml`:
```xml
<include file="260822-1-addGeoFlagsAndEmblems.xml" relativeToChangelogFile="true"/>
<include file="260822-3-addGeoAttributesColumns.xml" relativeToChangelogFile="true"/>
<include file="260823-1-addGeoBlobImageFields.xml" relativeToChangelogFile="true"/>
<include file="260823-2-migrateAllRussiaRegions.xml" relativeToChangelogFile="true"/>
<include file="260823-3-updateCountriesFullInfo.xml" relativeToChangelogFile="true"/>
```

### Фаза 2 — Данные: AI-функции (0.5 дня)

2.1. **`modules/core/db/changelog/260824-1-addNewAiFunctionsToProduction.xml`** (новый):
```xml
<changeSet id="260824-1-addNewAiFunctionsToProduction" author="HRM HuntTech">
  <preConditions onFail="MARK_RAN">
    <sqlCheck expectedResult="0">SELECT count(*) FROM hunttech_ai_function_configuration WHERE code='COMPANY_REQUISITES_PARSE_JSON'</sqlCheck>
  </preConditions>
  <insert tableName="hunttech_ai_function_configuration">…</insert>
  <insert tableName="hunttech_ai_function_configuration">…COMPANY_WEB_SEARCH_PARSE_JSON…</insert>
</changeSet>
<changeSet id="260824-2-bumpCvSmartParseJsonVersion" author="HRM HuntTech">
  <preConditions onFail="MARK_RAN">
    <sqlCheck expectedResult="1">SELECT count(*) FROM hunttech_ai_function_configuration WHERE code='CV_SMART_PARSE_JSON' AND configuration_version=1</sqlCheck>
  </preConditions>
  <update tableName="hunttech_ai_function_configuration">
    <column name="configuration_version" value="2"/>
    <column name="prompt_template" value="…"/> <!-- выгрузить с локали -->
    <where>code='CV_SMART_PARSE_JSON'</where>
  </update>
</changeSet>
```
2.2. Перед коммитом — **выгрузить эталонные `prompt_template`/`system_prompt`** для **всех 11 строк** `hunttech_ai_function_configuration` (а не только 3): «длина в БД ≠ длина в seed-файле» означает ручную правку → нужен отдельный changelog. Команда:
```sql
\copy (SELECT code, length(system_prompt) sys_len, length(prompt_template) tpl_len, configuration_version, update_ts FROM hunttech_ai_function_configuration ORDER BY code) TO '/tmp/migplan/ai_prompts_lens.csv' CSV HEADER
```
Затем сравнить с длинами из seed-файлов (`grep -c '…' modules/core/db/changelog/*.xml`). На 2026-09-01 на локали только `SKILLS_EXTRACT` имеет свежий `update_ts` (2026-08-18, после сида 260816-5) — все остальные совпадают с эталонами.

2.3. **Решение по `hunttech_user_ai_profile`** (GO/NO-GO от владельца `alan`): черновик 1 строки (28 полей) на локали — **100% ручной ввод**, в changelog его нет и быть не может. Варианты:
- (a) **Не мигрировать** (по умолчанию) — личные настройки тестовой среды, на проде `alan` заново заполнит.
- (b) **Мигрировать** как есть — тогда нужен отдельный `260824-3-mergeUserAiProfileFromLocal.xml` с `id='4cd4408b-9898-b689-8244-2000424c176c'`, `user_id=…`, всеми 28 колонками и `<preConditions><sqlCheck expectedResult="0">SELECT count(*) FROM hunttech_user_ai_profile</sqlCheck></preConditions>`.

2.4. **Проверка ручных правок `hunttech_country` после `260823-3`**: сравнить на локали для 62 стран с `alpha3_code IS NOT NULL` — каждое поле (`country_eng_name`/`alpha3_code`/`currency_code`/`capital`/`phone_code`/`flag_url`/`flag_image`) — не правил ли пользователь что-то руками в UI уже после AI-сидинга. Если правки есть — добавить отдельный `260824-4-updateManualCountryOverrides.xml`.

### Фаза 3 — Репетиция на локальном клоне `hunttech_rehearsal` (0.5 дня)

3.1. Скопировать развёрнутую БД локали в `hunttech_rehearsal` (из дампа 0.3).
3.2. Снять `databasechangelog`/`sys_db_changelog` (на CUBA это `sys_db_changelog`) из клона — чтобы Liquibase думал, что ничего не применено.
3.3. Запустить `bash scripts/start-app.sh --branch "$PWD"` с профилем `rehearsal` (`app_home` → `rehearsal.app.properties`, JNDI → `jdbc:postgresql://127.0.0.1:5432/hunttech_rehearsal`).
3.4. Смотреть `deploy/tomcat/logs/startup.log` — все 5 новых changeSet'ов должны примениться (или MARK_RAN).
3.5. Прогнать: `find modules/core/test -name "*Company*Test.java" -exec ./gradlew :app-core:test --tests {} \;` — должны быть зелёными.
3.6. Сверка `information_schema.columns` между `hunttech` и `hunttech_rehearsal` (для `hunttech_*`):
```sql
SELECT table_name, column_name, ordinal_position FROM information_schema.columns
 WHERE table_schema='public' AND table_name LIKE 'hunttech_%'
 ORDER BY table_name, ordinal_position;
```
— должно совпасть побайтово.

### Фаза 4 — Генерация готового deploy-пакета (0.5 дня)

4.1. Собрать `cuba` артефакт: `./gradlew :app-core:assembleDbScripts :app-core:dbScriptsArchive` (получим `app-core-0.393-db.zip` в `build/distributions/`).
4.2. Скопировать zip в `deploy-prod-scripts/` рядом с `start-app.sh` для Hermes-1.
4.3. Чек-лист Hermes-1 (ручной контроль):
- [ ] Бэкап `hunttech` на проде перед стартом (`pg_dump -Fc hunttech > /backup/hunttech_pre_0.393.dump`).
- [ ] `updateDb` — `gradle :app-core:updateDb -P cuba.automaticDatabaseUpdate=false` (явно, как у нас `automaticDatabaseUpdate=false`).
- [ ] Проверить `sys_db_changelog` после updateDb: появились ли `260822-1/3`, `260823-1/2/3`, `260824-1/2`.
- [ ] Сверка `hunttech_ai_function_configuration` count = 11.
- [ ] Сверка `hunttech_country` count >= 60 (не меньше, чем было 60 на проде).
- [ ] После успеха — рестарт Tomcat, smoke-test логина в проде.
- [ ] Если что-то пошло не так — `pg_restore /backup/hunttech_pre_0.393.dump` (откат ≤ 5 минут).

### Фаза 5 — Документация (0.5 дня)

5.1. Обновить `docs/services/SkillAnalysisService.md` (если есть) и `docs/db-schema.md`: добавить запись «миграция AI+гео 2026-09-01, бамп до 0.393».
5.2. CHANGELOG в `build.gradle` → `version = '0.393'`, `isSnapshot = false`.
5.3. Краткий отчёт в `.hermes/plans/2026-09-01-ai-geo-migration-result.md` после применения.

---

## 5. Что **НЕ** входит в этот план (явно)

- Миграция `hunttech_person` (214 vs 207 — несинхронизированные новые локальные кандидаты; оставить как есть).
- Миграция `itpearls_*` (по требованию пользователя).
- Миграция `hunttech_company`, `hunttech_open_position`, `hunttech_job_candidate` и т.п. — вне AI/гео-зоны.
- Изменение Java-кода (только changelog + DDL).
- Прод-выполнение (только подготовка).

---

## 6. Контрольные точки для остановки (GO/NO-GO)

- После Фазы 3 (репетиция) — если хоть один тест красный, остановить, фиксить локально, повторить репетицию.
- Если в Фазе 4 deploy-zip не собирается за 1 попытку — стоп, эскалация.
- Перед Фазой 4 — финальный апрув от владельца продукта (Яков, y.ananyev@hunttech.ru), т.к. правки затрагивают шифрованные ключи AI.

---

## 7. Ссылки

- `modules/core/db/changelog/260821-1..7` (XML, в `db.changelog-master.xml` — применится автоматически).
- `modules/core/db/changelog/260822-2-addCompanyWebSearchAiFunction.xml` (XML, в master).
- `modules/core/db/update/postgres/26/260823-1/2/3.sql` (только SQL, нужны XML-обёртки).
- `docs/db-schema.md` (после Фазы 5 обновить).

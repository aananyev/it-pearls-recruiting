# Доступ к данным HRM HuntTech через CUBA REST API v2 — для внешних приложений, сайтов и агента ChatGPT

> **Дата:** 2026-08-10
> **Проект:** HRM HuntTech, CUBA 7.3-SNAPSHOT (Vaadin 8, PostgreSQL, Tomcat 9), прод `hr.hunttech.ru`, контекст `/hrm`
> **Статус:** план (режим планирования; код не менялся)

**Goal:** дать внешним потребителям (сайт hunttech.ru, другие приложения, агент ChatGPT для отчётов и проверок) безопасный read-only доступ к данным HRM по стандартному CUBA REST API v2 (OAuth2).

**Architecture:** штатный аддон `cuba-rest` → REST API v2. Доступ наружу — ТОЛЬКО через именованные JPQL-запросы (`rest-queries.xml`) поверх узких публичных `view` (`views.xml`), аутентификация OAuth2 (client id/secret + технический пользователь с read-only ролью). Никаких write-операций через REST на проде.

**Tech Stack:** CUBA 7.3, аддон `com.haulmont.cuba:cuba-rest:$cubaVersion`, OAuth2, PostgreSQL, Tomcat 9.

---

## 0. Источники (документация)

- Руководство CUBA (RU, latest): https://doc.cuba-platform.com/manual-latest-ru/index.html — глава D.32 «REST API» перенесена в аддон.
- Документация аддона REST API 7.2 (одностраничник, RU/EN): https://doc.cuba-platform.com/restapi-7.2/index.html
  - Разделы: 3.1 Installation, 4.1 Predefined JPQL Queries, 4.4 CORS, 4.5 Anonymous, 5.1 OAuth Token, 5.4 Entity list, 5.7/5.8 JPQL GET/POST, 5.9/5.10 Services, 6 Security, Appendix A: свойства.
- Swagger всех эндпоинтов: http://files.cuba-platform.com/swagger/7.2 (там же точные параметры `limit/offset/sort/filter`).
- Предыдущий план интеграции сайта (контракт уже проработан): `.hermes/plans/2026-08-07_140000_openposition-site-integration.md` + `references/site-publication-contract.md` в скилле hrm-data-export.

## 1. Текущее состояние (проверено 10.08.2026)

| Что | Состояние |
|---|---|
| `cubaVersion` | `7.3-SNAPSHOT` (`build.gradle:2`) |
| Аддон `cuba-rest` в `build.gradle` | НЕ подключён (в `appComponent` только cuba-global, globalevents, emailtemplates, dataimport, dashboard, helium, fts, charts, reports, bpm) |
| `cuba.rest.securityScope=GENERIC_UI` | уже есть в ОБОИХ копиях web-app.properties: `com/company/hunttech/web-app.properties:43` и `modules/web/src/com/company/hunttech/web-app.properties:48` |
| `views.xml` | `modules/global/src/com/company/hunttech/views.xml` |
| `rest-queries.xml`, `rest-services.xml` | отсутствуют |
| Базовый путь REST | в контракте сайта принят `/hrm/api/v2/...`; по умолчанию у аддона `/rest/v2` — при внедрении проверить/выставить `cuba.rest.contextPath` (см. Задачу 4) |

Ключевые факты из доков аддона (7.2):
- Токен: `POST /rest/v2/oauth/token`, Basic-аутентификация клиентом (id:secret base64), параметры формы `grant_type=password&username=...&password=...` → `{access_token, refresh_token, expires_in (по умолчанию 43200 с = 12 ч), scope: "rest-api"}`.
- Продление: `grant_type=refresh_token` (дефолтный срок refresh — 365 дней).
- Список сущностей: `GET /rest/v2/entities/{entityName}?view=...&limit=...&offset=...&sort=...`.
- Именованные запросы: `GET /rest/v2/queries/{entity}/{queryName}?param=...`; POST-вариант для коллекций; `/count` для числа записей; `cacheable` для кеширования; предопределённый запрос `all`.
- Сервисные методы: конфиг `rest-services.xml` через `cuba.rest.servicesConfig`, вызов `POST /rest/v2/services/{service}/{method}`.
- Безопасность: REST использует свой security scope; пользователь без роли с permission `cuba.restApi.enabled` НЕ залогинится через REST. У нас `cuba.rest.securityScope=GENERIC_UI` → роль техпользователя должна быть scope GENERIC_UI.
- Анонимный доступ по умолчанию ВЫКЛЮЧЕН (`cuba.rest.anonymousEnabled=false`) — так и оставляем.
- CORS по умолчанию `*` — ограничить `cuba.rest.allowedOrigins` (нужно только если SPA вызывает API из браузера; server-to-server CORS не нужен).
- Токены по умолчанию хранятся В ПАМЯТИ (`cuba.rest.storeTokensInDb=false`) — рестарт Tomcat инвалидирует все токены; для долгих интеграций рассмотреть `true`.
- Маскирование токенов в логах — включено по умолчанию (`cuba.rest.tokenMaskingEnabled=true`).

## 2. Целевая архитектура

```
                    ┌──────────────────────────┐
 hunttech.ru        │  HRM HuntTech (CUBA 7.3) │
 (Next.js ISR)      │  /hrm/api/v2              │
                    │  ├─ oauth/token  (OAuth2) │
 Внешние приложения │  ├─ queries/...  (JPQL)  │
                    │  ├─ entities/... (view)  │
 Агент ChatGPT      │  └─ services/... (опц.)  │
 (через Hermes/PR)  │  Правки: ТОЛЬКО read     │
                    └──────────────────────────┘
```

Потребители и их доступ:

| Потребитель | Запросы/view | Аутентификация | ТTL/кеш |
|---|---|---|---|
| Сайт hunttech.ru (`/careers`) | `openPositionPublic` + `openPosition-public-view` | техпользователь `site-reader` | ISR `revalidate=300`; крон-резерв |
| Внешние приложения/отчёты | свои именованные запросы + свои view | отдельные техпользователи (аудит) | TTL 1–5 мин |
| Агент ChatGPT (отчёты, проверки) | read-only запросы + локальная БД | `rest-checker` (read-only) | по необходимости |

## 3. Задачи

### Задача 1. Подключить аддон cuba-rest в build.gradle

**Файлы:** `build.gradle` (секция `dependencies { appComponent(...) }`, строка ~53–63)

Добавить строку в конец блока:
```groovy
appComponent("com.haulmont.cuba:cuba-rest:$cubaVersion")
```

**Проверки:**
1. Питфолл: cuba-rest тянет зависимости → slf4j уже зафорсирован `1.7.36` в build.gradle — проверить резолюцию: `./gradlew :app-global:dependencies --configuration runtimeClasspath | grep -i slf4j` (не должно быть конфликта версий).
2. Сборка: `./gradlew :app-global:compileJava -x test --no-daemon` — BUILD SUCCESSFUL.
3. Коммит (по конвенции HRM: бамп подверсии вручную): `git commit -m "feat: подключить cuba-rest аддон"`, push.

### Задача 2. Публичные view в views.xml (global-модуль)

**Файл:** `modules/global/src/com/company/hunttech/views.xml`

Добавить view для публикации вакансий (основа — уже проработанный контракт сайта):
```xml
<view entity="hunttech_OpenPosition" name="openPosition-public-view" extends="_minimal">
    <property name="openClose"/>
    <property name="vacansyName"/>
    <property name="vacansyID"/>
    <property name="shortDescription"/>
    <property name="comment"/>               <!-- LOB: вес ответа решить с бизнесом -->
    <property name="salaryMin"/>
    <property name="salaryMax"/>
    <property name="remoteWork"/>
    <property name="remoteComment"/>
    <property name="workExperience"/>
    <property name="commandCandidate"/>
    <property name="closingDate"/>
    <property name="signDraft"/>
    <property name="internalProject"/>
    <property name="grade" view="_minimal"/>
    <property name="cityPosition" view="_minimal"/>
    <property name="positionType" view="_minimal"/>
    <property name="projectName" view="_minimal"/>
    <property name="skillsList" view="_minimal"/>
</view>
```

**Запрещено в любом внешнем view** (внутренние финансовые/рабочие): `outstaffingCost`, `percentComissionOfCompany`, `percentSalaryOfResearcher`, `percentSalaryOfRecrutier`, `typeCompanyComission`, `useTaxNDFL`, `salaryIE`, `memoForInterview`, `rawDescription`, `searchMap`, `interviewPlan`, `interviewChecklist`, `openPositionComments`, `someFiles`.

Для отчётов/проверок ChatGPT (непубличные) — отдельный view `openPosition-report-view` (те же поля + справочники), но БЕЗ финансовых.

**Проверка:** компиляция global: `./gradlew :app-global:compileJava -x test --no-daemon`; валидность XML (нет дублей имён view).

### Задача 3. rest-queries.xml + cuba.rest.queriesConfig

**Файлы:**
- Создать: `modules/web/src/com/company/hunttech/rest-queries.xml`
- Изменить: `com/company/hunttech/web-app.properties` И `modules/web/src/com/company/hunttech/web-app.properties` (ОБЕ копии — питфолл проекта: корневой файл переопределяет модульный)

```xml
<queries xmlns="http://schemas.haulmont.com/cuba/rest-queries.xsd">
    <!-- Сайт: открытые для публикации -->
    <query entity="hunttech_OpenPosition" name="openPositionPublic" view="openPosition-public-view">
        <jpql><![CDATA[
            select e from hunttech_OpenPosition e
            where (e.openClose is null or e.openClose = false)
              and (e.signDraft is null or e.signDraft = false)
              and e.internalProject = false
            order by e.vacansyName
        ]]></jpql>
    </query>
    <!-- Отчёты: все вакансии со статусами (без финансовых полей) -->
    <query entity="hunttech_OpenPosition" name="openPositionAll" view="openPosition-report-view">
        <jpql><![CDATA[
            select e from hunttech_OpenPosition e
            order by e.vacansyName
        ]]></jpql>
    </query>
    <!-- Справочники для сайта/приложений -->
    <query entity="hunttech_City" name="cityAll" view="_minimal">
        <jpql><![CDATA[ select c from hunttech_City c order by c.cityRuName ]]></jpql>
    </query>
    <query entity="hunttech_Position" name="positionAll" view="_minimal">
        <jpql><![CDATA[ select p from hunttech_Position p order by p.positionRuName ]]></jpql>
    </query>
    <query entity="hunttech_Project" name="projectAll" view="_minimal">
        <jpql><![CDATA[ select p from hunttech_Project p order by p.projectName ]]></jpql>
    </query>
</queries>
```

В свойства (обе копии):
```properties
cuba.rest.queriesConfig = +com/company/hunttech/rest-queries.xml
```

**Проверка:** сборка web-модуля; после локального деплоя — запросы выполняются (см. Задачу 6).

### Задача 4. OAuth2-клиент и параметры REST

**Файлы:** `com/company/hunttech/web-app.properties`, `modules/web/src/com/company/hunttech/web-app.properties`

```properties
cuba.rest.contextPath = api              # чтобы путь был /hrm/api/v2 (как в контракте сайта); по умолчанию "rest"
cuba.rest.client.id = hrm-rest
cuba.rest.client.secret = {noop}<СЕКРЕТ> # сгенерировать длинный случайный; НЕ в git (прод — в local.app.properties)
cuba.rest.client.tokenExpirationTimeSec = 3600        # 1 час (дефолт 12 ч — для внешних клиентов меньше лучше)
cuba.rest.client.refreshTokenExpirationTimeSec = 2592000  # 30 дней (дефолт 365)
cuba.rest.client.authorizedGrantTypes = password,refresh_token
cuba.rest.anonymousEnabled = false       # явно: анонимный доступ запрещён
```

Примечания из доков: client id/secret генерируются при установке аддона; `{noop}` — префикс PasswordEncoder (для Basic-аутентификации префикс не обязателен, но безопаснее с ним и паролем через encoder). `cuba.rest.deleteExpiredTokensCron` — чистит истёкшие токены, дефолт `0 0 3 * * ?`.

### Задача 5. Роль и технические пользователи (read-only)

Создать роль (runtime через экран «Роли», scope GENERIC_UI — т.к. `cuba.rest.securityScope=GENERIC_UI`):
- **Specific permissions:** `cuba.restApi.enabled`
- **Entity permissions (read):** `hunttech_OpenPosition`, `hunttech_SkillTree`, `hunttech_City`, `hunttech_Position`, `hunttech_Project`, `hunttech_Grade` (+ что ещё потребуется отчётам)
- **Entity permissions (create/update/delete):** НЕТ — строго read
- **Attribute permissions:** закрыть финансовые атрибуты (`outstaffingCost`, `percent*`, `typeCompanyComission`, `useTaxNDFL`, `salaryIE`, `memoForInterview`, `rawDescription`, `searchMap`, `interviewPlan`, `interviewChecklist`, `openPositionComments`, `someFiles`) — защита от утечки на уровне прав, даже если поле попадёт в view.

Пользователи:
- `site-reader` — роль чтения вакансий + справочники (для сайта).
- `rest-checker` — роль чтения вакансий + справочники (для агента ChatGPT / отчётов и проверок).

Пароли — длинные случайные, в менеджере паролей; в git НЕ хранить.

**Проверка:** вход в Generic UI под этими пользователями работает (права не сломали UI); через REST — см. Задачу 6.

### Задача 6. Репетиция на локальной копии (до прода)

Порядок (по runbook проекта): `./gradlew restart` (или deploy + чистка `work/Catalina/localhost/hrm*`). Затем smoke-скрипт:

```bash
# 1. Токен (Basic = base64("hrm-rest:<secret>"))
TOKEN=$(curl -s -X POST "http://localhost:8080/hrm/api/v2/oauth/token" \
  -H "Authorization: Basic $(printf 'hrm-rest:%s' "$SECRET" | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&username=rest-checker&password=$USER_PASS" | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')

# 2. Именованный запрос (сайт)
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/hrm/api/v2/queries/hunttech_OpenPosition/openPositionPublic?limit=5" | python3 -m json.tool

# 3. Счётчик (для отчётов)
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/hrm/api/v2/queries/hunttech_OpenPosition/openPositionPublic/count"

# 4. Продление
curl -s -X POST "http://localhost:8080/hrm/api/v2/oauth/token" \
  -H "Authorization: Basic $(printf 'hrm-rest:%s' "$SECRET" | base64)" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=refresh_token&refresh_token=$REFRESH"
```

Ожидания: 200 + JSON; открытых вакансий по фильтру ~140 в локальной БД (~84 в проде); FK в ответе — `{id, _instanceName}`; даты `YYYY-MM-DD`. Негативные проверки: неверный пароль → 401; пользователь без роли → 403/401; `openClose=true` НЕ попадает в выборку.

### Задача 7. Прод-деплой (только после явного согласования!)

Политика проекта: **прод не трогаем без необходимости**; все изменения — через git + PR-протокол, репетиция на локальной копии сначала.

1. Merge в master (координация с ChatGPT: `coordination/active-work` или PR-протокол).
2. Сборка: `./gradlew buildWar` → `hrm.war`/`hrm-core.war`.
3. Секреты: `cuba.rest.client.id/secret` + пароли техпользователей — в `${app.home}/local.app.properties` (web-блок) на проде, НЕ в WAR, НЕ в git.
4. Деплой + `systemctl restart tomcat9` (или по runbook).
5. Smoke на проде: `POST https://hr.hunttech.ru/hrm/api/v2/oauth/token` → 200 + `access_token`; `GET /hrm/api/v2/queries/hunttech_OpenPosition/openPositionPublic/count` → число ~84.
6. Отчёт в Telegram по протоколу (✅ REST API включён, HTTP 200).

### Задача 8. Контракт для внешних потребителей

Зафиксировать в репо (рядом с предыдущим контрактом) документ `docs/rest-api-contract.md`:
- Базовый URL: `https://hr.hunttech.ru/hrm/api/v2`
- Получение токена (curl-пример с Basic), продление, `expires_in`
- Доступные запросы: имя, параметры, view, пример ответа
- Правила: read-only; пагинация `limit` (max 1000) + `offset` + `sort`; кеш TTL 1–5 мин; CORS не нужен server-to-server
- Ошибки: 401 (токен/клиент), 403 (нет прав), 404 (запрос/view не найден), 429/500 (нагрузка)
- Контакты/владелец интеграции

Для сайта hunttech.ru: Next.js ISR `revalidate=300` (кеш на стороне сайта); крон-резерв — curl REST на сервере сайта (0 изменений в HRM), как в контракте 2026-08-07.

### Задача 9 (опционально). rest-services.xml — сервисные методы для агрегатов

Если для отчётов нужны агрегаты, которые плохо выражаются JPQL-запросом (группировки, расчёт ставок и т.п.):

```xml
<services xmlns="http://schemas.haulmont.com/cuba/rest-services-v2.xsd">
    <service name="hunttech_RestReportService">
        <method name="openPositionsSummary"/>
    </service>
</services>
```
- `cuba.rest.servicesConfig = +com/company/hunttech/rest-services.xml` (обе копии properties).
- Сервис — ТОЛЬКО read-only методы (никаких write на проде), покрыть autotest в `modules/core/test` (конвенция проекта).
- Вызов: `POST /hrm/api/v2/services/hunttech_RestReportService/openPositionsSummary` + Bearer.

### Задача 10. Безопасность и эксплуатация

- `cuba.rest.allowedOrigins` — ограничить, если появится браузерный SPA (для server-to-server не нужно).
- Маскирование токенов в логах — оставить `true` (дефолт).
- `cuba.rest.storeTokensInDb` — рассмотреть `true`, если будет несколько инстансов/долгие интеграции; при single Tomcat и рестартах клиенты перелогиниваются (для сайта с ISR это безболезненно).
- nginx перед прдом: ограничить `POST /hrm/api/v2/oauth/token` (rate limit, fail2ban), allowlist для серверных потребителей.
- Мониторинг: отдельный лог-файл REST (`logback`), алерт на 401-шторм.

## 4. Примеры запросов (эталон для потребителей)

```
# токен
POST /hrm/api/v2/oauth/token
Authorization: Basic base64(clientId:clientSecret)
Content-Type: application/x-www-form-urlencoded
grant_type=password&username=site-reader&password=***

# открытые вакансии (сайт)
GET /hrm/api/v2/queries/hunttech_OpenPosition/openPositionPublic?limit=20&offset=0&sort=vacansyName

# число открытых (отчёты/проверки)
GET /hrm/api/v2/queries/hunttech_OpenPosition/openPositionPublic/count

# все вакансии со статусами (внутренний отчёт)
GET /hrm/api/v2/queries/hunttech_OpenPosition/openPositionAll?limit=100&offset=0

# справочники
GET /hrm/api/v2/queries/hunttech_City/cityAll
```

Формат ответа: массив объектов с `_entityName`, `_instanceName`, `id`; FK — `{id, _instanceName}`; даты `YYYY-MM-DD`; числа — JSON number.

## 5. Рекомендации агенту ChatGPT (как подключаться для отчётов и проверок)

### 5.1. Каналы доступа (важно — приоритет сверху вниз)

1. **Локальная БД (read-only)** для проверок после PR: `PGPASSWORD=cuba psql -h 127.0.0.1 -U cuba -d hunttech` (именно `127.0.0.1`, не `localhost` — pg_hba не пускает `::1`). Освежить копию: `scripts/refresh_local_db.sh` (pg_dump → pg_restore).
2. **REST API на localhost** (`http://localhost:8080/hrm/api/v2`) — после того, как аддон включён (Задачи 1–6). Это ровно то, что увидят внешние потребители, — лучший способ проверить контракт.
3. **REST API на проде** (`https://hr.hunttech.ru/hrm/api/v2`) — ТОЛЬКО по явному запросу пользователя и только read-only запросы; секреты не логировать и не класть в код/PR.

ChatGPT (как агент в чате) напрямую в сеть прод-сервера не ходит — REST-вызовы на прод выполняет Hermes по команде; сам ChatGPT работает с репозиторием, локальной копией и PR-циклом.

### 5.2. Получение токена (паттерн для скриптов)

```python
import base64, json, time, requests

BASE = "http://localhost:8080/hrm/api/v2"   # прод: https://hr.hunttech.ru/hrm/api/v2
CLIENT_ID, CLIENT_SECRET = "hrm-rest", "<из env/local.app.properties, НЕ из кода>"
USER, PASSWORD = "rest-checker", "<пароль техпользователя>"

def get_token():
    r = requests.post(f"{BASE}/oauth/token",
        auth=(CLIENT_ID, CLIENT_SECRET),
        data={"grant_type": "password", "username": USER, "password": PASSWORD})
    r.raise_for_status()
    return r.json()  # access_token, refresh_token, expires_in

def api_get(path, token):
    return requests.get(f"{BASE}{path}", headers={"Authorization": f"Bearer {token}"})
```

Правила токена:
- Кешировать `access_token` до истечения `expires_in` (не получать на каждый запрос).
- При `401` — продлить по `refresh_token` (`grant_type=refresh_token`), при неудаче — полный логин.
- Токены живут в памяти сервера: после рестарта Tomcat все токены инвалидируются → клиент логинится заново (это нормально).

### 5.3. Правила работы с REST

1. **Только чтение.** Никогда не вызывать `POST/PUT/DELETE /entities/...` — на проде write через REST запрещён (закрыт и правами роли, и политикой).
2. **Только именованные запросы** из `rest-queries.xml`. Если нужного запроса нет — НЕ обходить через `GET /entities/...?...filter=`, а оформить задачу на добавление запроса (PR-цикл). Логика отбора живёт в HRM.
3. **Секреты** — из `local.app.properties`/env, никогда в коде, PR, логи.
4. **Пагинация и сортировка**: `limit` + `offset` + `sort=имяАтрибута`; для объёмных выборок сначала `/count`, потом страницы.
5. **Семантика статусов (эталонная ошибка 08.2026):**
   - `open_close = true` → вакансия ЗАКРЫТА; открытые = `open_close = false` или `NULL`.
   - Публикуемые = `(open_close IS NULL OR open_close = false) AND (signDraft IS NULL OR signDraft = false) AND internalProject = false`.
6. **Формат ответа:** `_entityName`, `_instanceName` (NamePattern), FK `{id, _instanceName}`, даты `YYYY-MM-DD`. Внутренние UUID и техполя БД в отчёты не выводить (конвенция проектов HuntTech).
7. **Финансовые поля** (`outstaffingCost`, `percent*`, `typeCompanyComission`, `useTaxNDFL`, `salaryIE`, `memoForInterview`, `rawDescription`, `searchMap`, `interviewPlan/Checklist`, комментарии, файлы) — в REST не отдаются и в отчётах не используются; если они нужны для проверки — только локальная БД.

### 5.4. Типовые сценарии

**Отчёт «открытые вакансии»:** `GET .../queries/hunttech_OpenPosition/openPositionPublic` (+ `?limit/offset` постранично), группировка по `cityPosition`/`projectName`/`positionType` — на стороне клиента (Python/pandas). Сверять «число открытых» через `/count` (~140 локально, ~84 прод).

**Проверка после PR (верификация):**
1. Smoke REST: токен → `/count` открытых → число в ожидаемом диапазоне.
2. Сверка REST vs локальная БД: выбрать N id через REST, те же id в psql — поля `vacansyName`, `salaryMin/Max`, `cityPosition`, `openClose` совпадают.
3. Контроль утечки: в JSON ответа публичного view отсутствуют все поля из «запрещённого списка» (см. Задачу 2).
4. Негатив: запрос с неверным/просроченным токеном → 401; `openClose=true` не появляется в `openPositionPublic`.

**Проверка данных после действий ботов:** статусы вакансий (`open_close`), справочники (`city`, `position`, `project`, `grade`), ставки — через локальную БД (REST не отдаёт финансовые поля).

### 5.5. Если данных не хватает

- Нет запроса → добавить в `rest-queries.xml` (PR, репетиция локально, потом прод по согласованию).
- Нет view → добавить view в `views.xml` (без финансовых полей).
- Нужны агрегаты/расчёт → вынести read-only метод в `rest-services.xml` (Задача 9) с autotest.
- Не забывать: секреты REST на прод — только в `local.app.properties`.

## 6. Валидация и тесты

| Уровень | Что проверяем |
|---|---|
| Сборка | `./gradlew :app-global:compileJava :app-web:compileJava -x test --no-daemon` — OK |
| Локальный smoke | скрипт Задачи 6: token, query, count, refresh, негативы (401/403) |
| Семантика | в `openPositionPublic` нет `openClose=true`; count ≈ 140 локально / ≈ 84 прод |
| Безопасность | в ответах нет запрещённых полей; анонимный доступ 401; CORS ограничен (если включён) |
| Autotest (если сервис) | `modules/core/test` для методов `RestReportService` |
| Прод | token → count → контракт-док в репо; отчёт в Telegram |

## 7. Риски и открытые вопросы

- **Прод-деплой:** включение аддона = пересборка WAR + рестарт Tomcat. Только по явному согласованию, после репетиции на локальной копии (политика «прод не трогаем»).
- **Новый публичный endpoint:** риск брутфорса/нагрузки → nginx rate limit на `/oauth/token`, allowlist серверных потребителей, мониторинг 401.
- **Один default OAuth2-клиент** (id/secret) на всех: аудит по техпользователям (логинам), а не по клиентам; если понадобятся разные client — выяснить механизм нескольких клиентов аддона (в доках default client один).
- **Токены в памяти** (`storeTokensInDb=false`): рестарт сбрасывает все сессии — клиенты должны перелогиниваться; для сайта с ISR-кешем безопасно.
- **Объём LOB `comment_`:** полный дамп всех вакансий ≈ 13–17MB — для сайта взвесить включение `comment` в публичный view или обрезать.
- **Путь API:** `/api/v2` vs дефолтный `/rest/v2` — подтвердить `cuba.rest.contextPath` при внедрении, контракт сайта уже написан под `/api/v2`.
- **Имена сущностей в REST** — camelCase (`hunttech_OpenPosition`), не имена таблиц (`hunttech_open_position`).

## 8. Порядок исполнения

1. Задачи 1–6 (локально, репетиция) → 2. Задача 8 (контракт-док) → 3. Согласование прода → 4. Задача 7 (прод) → 5. Задача 9 по необходимости → 6. Задача 10 (безопасность, мониторинг).

Каждый этап — коммит с русским описанием (конвенция HRM: bump подверсии вручную).

## 9. Статус исполнения — ЛОКАЛЬНАЯ КОПИЯ РЕАЛИЗОВАНА (2026-08-10)

**Главное отклонение от плана: аддон собран из исходников.**
`com.haulmont.cuba:cuba-rest:7.3-SNAPSHOT` отсутствует во всех доступных репозиториях (work/public/premium/marketplace + Maven Central — проверено). Официальный исходник **cuba-platform/restapi** (тег v7.2.7, группа `com.haulmont.addon.restapi`) собран под 7.3-SNAPSHOT:
- компиляция против платформы 7.3-SNAPSHOT успешна (API-совместимость подтверждена);
- артефакты завендорены в репозиторий: `libs/cuba-rest-addon/` (5 модулей, ~364K) + maven-metadata.xml для snapshot-резолва; сборка проверена без `~/.m2`;
- в build.gradle: `appComponent("com.haulmont.addon.restapi:restapi-global:$cubaVersion")` + `maven { url 'file:libs/cuba-rest-addon' }`, версия **0.18**.

**Критический питфолл: статичные web.xml.** appComponents в проекте захардкожен (не генерируется плагином) — при добавлении аддона нужно вручную дописать `com.haulmont.addon.restapi` в ТРИ файла: `modules/web/web/WEB-INF/web.xml`, `modules/web/web/WEB-INF/single-war-web.xml`, `modules/core/web/WEB-INF/web.xml`. Без этого REST-сервлет не монтируется (запросы дают 301 на /hrm).

**Что сделано:**
1. views.xml: `openPosition-public-view` (сайт) и `openPosition-report-view` (отчёты) — финансовые/внутренние поля исключены на уровне view.
2. rest-queries.xml (web-модуль): `openPositionPublic` (открытые для публикации), `openPositionAll` (все, для проверок), справочники `cityAll/positionAll/projectAll/gradeAll`.
3. web-app.properties (обе копии): oauth2-клиент `hrm-rest`, secret локальный, expiresIn 3600, `cuba.rest.queriesConfig=+com/company/hunttech/rest-queries.xml`.
4. Локальная БД: роль «REST чтение» (GENERIC_UI, STANDARD=20, `cuba.restApi.enabled`=1, чтение 8 сущностей, deny 14 финансовых атрибутов OpenPosition), пользователи `site-reader` и `rest-checker` (пароль локальный `rest-local-pass-2026`). Воспроизводимый идемпотентный скрипт: `scripts/rest_local_setup.sql`.

**Результаты тестов — 32/32 PASS** (`/tmp/rest_test.py`):
- токены: password/refresh; негативы: неверный пароль → 400 invalid_grant, неверный клиент → 401, аноним → 401, мусорный токен → 401, старый access-токен после refresh → 401 (отзывается);
- `openPositionPublic` count = 32 = эталонный SQL (delete_ts-фильтр! из 144 «сырых» строк 112 — soft-deleted);
- семантика: нет закрытых (openClose=true)/черновиков/внутренних; финансовые поля отсутствуют; FK вида `{id,_instanceName}`; сортировка по vacansyName;
- запись заблокирована: POST /entities → **403 Creation forbidden**;
- пагинация: limit/offset/sort работают, offset за пределами → [].

**Открытые вопросы (не решены):**
- путь `/api/v2` из контракта сайта vs фактический `/rest/v2` (servlet-маппинг аддона жёсткий `/rest/*`; `cuba.rest.contextPath` в документации не найден) — решить с владельцем сайта;
- прод: включение аддона + роли/пользователи — ТОЛЬКО по явному согласованию (прод не трогаем без запроса);
- `cuba.rest.storeTokensInDb` не включали (токены в памяти, слетают при рестарте) — на проде рассмотреть;
- rate-limit/CORS на nginx — не делали (локальный контур).


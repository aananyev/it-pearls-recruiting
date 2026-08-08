# Интеграция HRM HuntTech → hunttech.ru: публикация вакансий на странице «Вакансии»

> **Дата:** 2026-08-07
> **Проект:** HRM HuntTech, CUBA 7.3 (Vaadin 8, PostgreSQL, Tomcat 9, прод `hr.hunttech.ru`, контекст `/hrm`)
> **Сайт:** `hunttech.ru` — Next.js (App Router) за nginx, страница вакансий «Работа в ХантТек» = `/careers` (алиас `/jobs`). Репозиторий сайта локально недоступен.
> **Статус:** план (без реализации)
> **Принцип:** только стандартные средства CUBA Platform (модуль REST API v2), без самописных контроллеров/сервлетов.

---

## Goal

Открытые вакансии из HRM HuntTech публикуются на сайте компании hunttech.ru (страница «Вакансии» — /careers). HRM отдаёт данные по стандартному REST API; сайт тянет их сам («реальное время» — pull + короткий TTL-кеш); крон — резервный вариант для статики. Разработчику сайта передаётся контракт API.

## Архитектура (2–3 предложения)

HRM подключает стандартный модуль **CUBA REST API v2** (`com.haulmont.cuba:cuba-rest`) в web-блок. Сайт hunttech.ru авторизуется по OAuth2 (`client_credentials` или технический пользователь), делает `POST /hrm/api/v2/queries/hunttech_OpenPosition/openPositionPublic` и получает JSON-список открытых вакансий с узким публичным view (`openPosition-public-view` — без внутренних финансовых полей). «Реальное время»: сайт перезапрашивает API при каждом рендере страницы вакансий с TTL-кешем 1–5 мин (закрытие/открытие вакансии в HRM отражается на сайте в течение TTL). Крон — только если сайт не сможет опрашивать API (тогда крон на стороне сайта: curl REST → генерация статики; вариант B — Scheduled Task в HRM).

## Контекст / факты из кода (проверено)

### Про сайт hunttech.ru (внешний, наш код не трогает)
- Next.js (заголовки `X-Powered-By: Next.js`, `x-nextjs-prerender`), nginx, HTTPS.
- Страница вакансий: `/careers` → HTTP 200, `/jobs` → HTTP 200; `/vacancy`, `/vakancii` → 404.
- На `/careers` есть блок «Ищем инженеров… Загружаем…» — список вакансий подгружается динамически (клиентский fetch), в статическом HTML списка должностей нет. **Открытый вопрос: откуда сайт сейчас берёт вакансии (свой бэкенд? hh.ru? статика)?** — влияет на то, что менять на стороне сайта.
- Репозиторий сайта локально не найден (нет в `~/StudioProjects`, `~/projects`, `~/sources`, `~/IdeaProjects`) — сайт ведёт, видимо, подрядчик/другая команда. → Наша часть: HRM API + контракт.

### Про HRM (наша часть)
- Сущность `OpenPosition` (`hunttech_OpenPosition`): `modules/global/src/com/company/hunttech/entity/OpenPosition.java`. Ключевые поля:
  - `openClose` (Boolean, `@NotNull`, индекс `IDX_HUNTTECH_OPEN_POSITION_OPEN_CLOSE`) — флаг «вакансия открыта»;
  - `signDraft` (Boolean) — черновик; `internalProject` (Boolean, `@NotNull`, default false) — внутренний проект (НЕ публиковать);
  - `vacansyName`, `vacansyID` (внешний код, 16 симв.), `shortDescription` (≤250), `comment` (LOB), `commentEn` (LOB);
  - `salaryMin`, `salaryMax`, `salaryIE` (BigDecimal), `remoteWork` (Integer), `remoteComment`;
  - FK: `grade`, `cityPosition` + `cities`, `positionType` (Position), `projectName` (Project, `@NotNull`), `owner` (ExtUser);
  - `workExperience`, `commandExperience`, `commandCandidate`, `numberPosition`, `more10NumberPosition`;
  - `skillsList` (List<SkillTree>), `closingDate`;
  - **внутренние, НЕ для публики:** `outstaffingCost`, `percentComissionOfCompany`, `percentSalaryOfResearcher`, `percentSalaryOfRecrutier`, `typeCompanyComission`, `useTaxNDFL`, `memoForInterview`, `rawDescription`, `searchMap`, `interviewPlan`, `interviewChecklist`, `openPositionComments`, `someFiles`.
- `views.xml` уже содержит view для `OpenPosition` (`openPosition-picker-view` extends `_minimal` и др.) — публичный view добавим рядом.
- REST-модуль в проекте **НЕ подключён** (в `build.gradle` нет `appComponent('com.haulmont.cuba:cuba-rest:...')`). В `web-app.properties` уже есть `cuba.rest.securityScope=GENERIC_UI` (корректно для REST v2) и `cuba.anonymousSessionId`.
- В `build.gradle` global-модуля есть `HhJavaApi 0.2.6` (hh.ru), но в коде он **не используется** (поиск по `modules/` пуст) — публикации на hh.ru из HRM сейчас нет.
- Прод: WAR `hrm.war` + `hrm-core.war` на `hr.hunttech.ru` (Tomcat9, context `/hrm`). REST будет доступен по `https://hunttech.ru/...` нет — по `http://hr.hunttech.ru:8080/hrm/api/v2/...` (или через прокси). Локально: `http://localhost:8080/hrm/api/v2/...`.

## Предлагаемый подход — REST API (real-time, основной)

Сайт тянет данные сам, при каждом рендере страницы «Вакансии» или с TTL-кешем 1–5 мин. CUBA push-уведомлений (WebSocket) для внешних систем в 7.3 нет — «реальное время» = pull по запросу + короткий кеш сайта.

## Пошаговый план

### Шаг 1. Подключить CUBA REST API в сборку

**Файлы:**
- `build.gradle` — в блок `dependencies` (рядом с другими `appComponent`):
  ```groovy
  appComponent("com.haulmont.cuba:cuba-rest:$cubaVersion")
  ```

Проверка: `./gradlew dependencies --configuration appComponent` (или сборка) — модуль разрешается из `repo.cuba-platform.com`.

### Шаг 2. Создать публичный view `openPosition-public-view`

**Файл:** `modules/global/src/com/company/hunttech/views.xml` — добавить `<view entity="hunttech_OpenPosition" name="openPosition-public-view" extends="_minimal">`.

Состав (черновик, финал — по решению бизнеса):
```xml
<view entity="hunttech_OpenPosition" name="openPosition-public-view" extends="_minimal">
    <property name="openClose"/>
    <property name="vacansyName"/>
    <property name="vacansyID"/>
    <property name="shortDescription"/>
    <property name="comment"/>              <!-- LOB: решить, включать ли (вес ответа) -->
    <property name="salaryMin"/>
    <property name="salaryMax"/>
    <property name="remoteWork"/>
    <property name="remoteComment"/>
    <property name="workExperience"/>
    <property name="commandCandidate"/>
    <property name="closingDate"/>
    <property name="grade" view="_minimal"/>
    <property name="cityPosition" view="_minimal"/>
    <property name="positionType" view="_minimal"/>
    <property name="projectName" view="_minimal"/>
    <property name="skillsList" view="_minimal"/>   <!-- SkillTree: проверить display-поля -->
</view>
```
Правила:
- **НИКАКИХ** внутренних финансовых полей (`outstaffingCost`, `percent*`, `typeCompanyComission`, `useTaxNDFL`, `salaryIE` — если не решено иначе).
- Без `openPositionComments`, `someFiles`, `memoForInterview`, `rawDescription`, `searchMap`, `interviewPlan`, `interviewChecklist`.
- `_minimal` на FK даёт `id` + `_instanceName`.

### Шаг 3. Зарегистрировать именованный JPQL-запрос для публикации

**Файл (создать):** `modules/web/src/com/company/hunttech/rest-queries.xml` (стандартное имя файла CUBA REST).
```xml
<queries xmlns="http://schemas.haulmont.com/cuba/rest-queries.xsd">
    <query entity="hunttech_OpenPosition" name="openPositionPublic" view="openPosition-public-view">
        <jpql><![CDATA[
            select e from hunttech_OpenPosition e
            where (e.openClose is null or e.openClose = false)
              and (e.signDraft is null or e.signDraft = false)
              and e.internalProject = false
            order by e.vacansyName
        ]]></jpql>
    </query>
</queries>
```
Эндпоинт: `POST /hrm/api/v2/queries/hunttech_OpenPosition/openPositionPublic`.

Именованный запрос предпочтительнее `GET /entities/...?filter=`: логика отбора живёт один раз в HRM, сайту не нужно знать про `openClose`/`signDraft`/`internalProject`.

### Шаг 4. Настроить OAuth2-клиента и права доступа

**Файл:** `modules/web/src/com/company/hunttech/web-app.properties`:
```properties
cuba.rest.clientId=hunttech-site
cuba.rest.clientSecret=<длинный секрет, НЕ коммитить>
cuba.rest.oauthTokenExpirationTime=3600
```

**Права доступа** (два стандартных варианта):
- **A. `client_credentials` (анонимная сессия).** Токен: `POST /hrm/api/v2/oauth/token` (`grant_type=client_credentials&client_id=...&client_secret=...`). Сессия anonymous (`cuba.anonymousSessionId`). Права — роли анонимной сессии: **только** `read` на `hunttech_OpenPosition`, `hunttech_SkillTree`, `City`, `Position`, `Project`, `Grade`.
- **B. `grant_type=password`, технический пользователь** `site_export` с ролью «Внешний сайт: чтение вакансий» (только read-права на те же сущности). Права — штатным UI администратора.

Рекомендация: **B** — права настраиваются стандартно, без тонкостей анонимной сессии. Секрет/пароль на проде — через `local.app.properties` (не в WAR, не в git).

### Шаг 5. Контракт API для сайта hunttech.ru (передать разработчику сайта)

Сайт (Next.js, `/careers`):
1. `POST https://<hr.hunttech.ru>/hrm/api/v2/oauth/token` → `access_token` (bearer).
2. `POST https://<hr.hunttech.ru>/hrm/api/v2/queries/hunttech_OpenPosition/openPositionPublic` с заголовком `Authorization: Bearer <token>`.
3. Ответ — JSON-массив: `id`, `_instanceName`, поля view; FK — `{id, _instanceName}`.
4. Кеш на сайте: TTL 1–5 мин (страница «Вакансии»).
5. Если сайт — браузерный SPA на другом домене и вызывает API из JS — нужен CORS; server-to-server (getServerSideProps/ISR) — CORS не нужен. Для Next.js ISR: `revalidate = 300` — идеальный паттерн «реального времени» без собственного кеша.
6. **Проверить, откуда сайт сейчас берёт вакансии** (блок «Загружаем…») — если у сайта уже есть свой источник (hh.ru и т.п.), согласовать замену/дополнение на наш REST.

### Шаг 6. Прод-настройки (безопасность)

- `clientId/clientSecret` (или пароль тех.пользователя) — в `${app.home}/local.app.properties` web-блока на `hr.hunttech.ru`, **не** в WAR, **не** в git.
- В `build.gradle` task `buildWar` appProperties секретов быть не должно.
- REST слушает на том же порту, что UI (`/hrm/api/v2/...`); наружу — через https-прокси (nginx на hunttech.ru/hr.hunttech.ru).
- Сайт получает **только** read-права на публичный набор сущностей.

### Шаг 7. Крон-вариант (резервный, если сайт не сможет опрашивать API)

- **A. Крон на сервере сайта:** раз в N минут `curl` REST → генерация статики/ISR. Ноль изменений в HRM. **Рекомендуется.**
- **B. Scheduled Task в HRM** (CUBA Scheduled Tasks, Administration): core-сервис `OpenPositionExportServiceBean.exportPublicJson()` пишет JSON в `fileStorage`/на endpoint сайта; дальше сайт/rsync забирает. По конвенциям проекта — обязателен autotest в `modules/core/test/` + `docs/services/`.

### Шаг 8. Тесты и валидация

- **Локальный smoke:**
  1. `./gradlew restart --no-daemon` (или `start-app.sh`) после Шага 1.
  2. `curl http://localhost:8080/hrm/api/v2/oauth/token` — сервлет поднялся (не 404).
  3. Токен → query `openPositionPublic` → 200, только открытые вакансии, нет внутренних полей (grep по JSON: `outstaffingCost`, `percentComission` и т.п.).
- **Автотесты** (`modules/core/test/`, JUnit 4, конвенции проекта):
  - source-contract тест: view/rest-queries содержат только публичные поля; query фильтрует `openClose/signDraft/internalProject`;
  - если Шаг 7B — `OpenPositionExportServiceTest` (CRUD-паттерн, `HunttechTestContainer`);
  - прогон: `./gradlew :app-core:test --tests '...' --no-daemon`.
- **Документация (обязательна):** `docs/services/OpenPositionRestApi.md` (Business & Context Intro, контракт API, OAuth, view, прод-конфиг) + `docs/README.md` + «История изменений» YYYY-MM-DD. UI-код не меняется → GLOBAL UI TRIGGER не задействован.
- **Прод:** деплой по runbook (`buildWar` → `hrm.war`/`hrm-core.war` → rsync → `systemctl restart tomcat9`), curl-проверка с прод-сервера.

## Файлы, которые изменятся

| Файл | Действие |
|------|----------|
| `build.gradle` | + `appComponent("com.haulmont.cuba:cuba-rest:$cubaVersion")` |
| `modules/web/src/com/company/hunttech/web-app.properties` | + `cuba.rest.clientId`/`clientSecret`, OAuth-таймаут |
| `modules/global/src/com/company/hunttech/views.xml` | + `openPosition-public-view` |
| `modules/web/src/com/company/hunttech/rest-queries.xml` | новый (запрос `openPositionPublic`) |
| (если Шаг 7B) core: `OpenPositionExportServiceBean` + интерфейс | новый сервис + тест |
| (если Шаг 4B) роль/пользователь — данные, не код | тех. пользователь `site_export` |
| `docs/services/OpenPositionRestApi.md` (новый) + `docs/README.md` | документация контракта |

**Не меняются:** сущности, БД/миграции (структура БД не затрагивается), UI-экраны, widgetset.

## Тесты / валидация (сводка)

1. `./gradlew assemble -x test --no-daemon` — BUILD SUCCESSFUL.
2. Локальный REST smoke (Шаг 8) — 200 + корректный JSON (только публичные поля).
3. `:app-core:test` — зелёные (включая ScreenViewIntegrityTest 8/8).
4. Прод: `curl` на `/hrm/api/v2/oauth/token` + запрос от имени сайта; секреты не в git; `git status` чистый.

## Риски и открытые вопросы

- **Риск:** `cuba-rest:7.3-SNAPSHOT` может потянуть конфликтующие зависимости (slf4j уже зафорсирован 1.7.36 — проверить разрешение). Митигация: собрать первым делом.
- **Риск:** LOB `comment` в REST view — тяжёлый ответ. Решение: отдавать `shortDescription`; `comment` — только если нужен полный текст (тогда отдельная карточка вакансии или лимит длины). **Уточнить у бизнеса.**
- **Риск:** анонимная сессия (вариант A) может получить лишние права — строго ограничить; предпочесть вариант B.
- **Вопрос:** откуда сайт hunttech.ru сейчас берёт вакансии на /careers (блок «Загружаем…»)? Есть ли уже источник — hh.ru, свой API? Нужно согласовать с разработчиком сайта.
- **Вопрос:** кто делает сайт hunttech.ru (подрядчик)? Кому передать контракт API?
- **Вопрос:** публиковать ли `needLetter`/`templateLetter`, `needExercise`/`exercise` (письмо/тест задание)? `salaryIE` (ИП-ставка) — публичное или нет?
- **Вопрос:** нужен ли сайту `vacansyID` для маппинга/отклика (форма на сайте)?
- **Вопрос:** поиск/фильтры на сайте (город, грейд, зарплата) — если нужны, расширить query параметрами.
- **CORS:** только если сайт вызывает API из браузера; для Next.js server-side/ISR не нужен.

## Итоговая рекомендация

1. Подключить `cuba-rest` (Шаги 1–4) — стандартно, обратимо, ~полдня работы.
2. Отдавать именованный запрос `openPositionPublic` с узким view.
3. Согласовать с разработчиком сайта: Next.js ISR `revalidate=300` поверх REST (real-time без собственного кеша). Крон — только если сайт не сможет опрашивать API (тогда крон на стороне сайта).
4. Реализация — по этому плану (TDD на view/query), после деплоя — документация контракта в `docs/`.

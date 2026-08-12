# OpenPosition REST API — контракт публикации вакансий на сайт hunttech.ru

> Контракт интеграции «HRM HuntTech → сайт hunttech.ru» по CUBA REST API v2.
> Дата актуализации: 2026-08-12.

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

Сайт компании hunttech.ru публикует открытые вакансии на странице «Работа в ХантТек» (`/careers`). Источник правды по вакансиям — HRM HuntTech: рекрутёры заводят вакансии, закрывают их, меняют описание и зарплатную вилку прямо в системе. Контракт ниже даёт сайту read-only доступ к **открытым для публикации** вакансиям через стандартный REST API: сайт сам тянет актуальный список, и закрытие вакансии в HRM автоматически убирает её с сайта в течение нескольких минут (TTL-кеша сайта). Внутренние данные (финансовые условия, комментарии, планы интервью) наружу не отдаются ни при каких условиях.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Потребитель контракта — серверная часть сайта hunttech.ru (Next.js, ISR), которая при рендере `/careers` обращается к `http://hr.hunttech.ru:8080/hrm/rest/v2/...` (server-to-server; CORS не требуется). Человек на сайте нажимает «Отклик» — это уже логика сайта, REST-контракт заканчивается на выдаче списка вакансий и справочников (город, должность, проект, грейд). В HRM изменений UI нет: REST-модуль подключается отдельным аддоном, данные выдают именованные JPQL-запросы из `rest-queries.xml` поверх узких публичных view.

### Behavior Summary

- сайт открывает страницу вакансий → запрашивает OAuth2-токен (grant_type=password, технический пользователь) → вызывает запрос `openPositionPublic` → получает JSON-массив открытых вакансий (только публичные поля);
- вакансия считается открытой для публикации, если `openClose = false или NULL`, `signDraft = false или NULL`, `internalProject = false`; логика фильтра живёт в HRM, сайту знать о статусах не нужно;
- справочники (`cityAll`, `positionAll`, `projectAll`, `gradeAll`) отдаются отдельными запросами для фильтров и карточек;
- доступ read-only: у технического пользователя роль «REST чтение» (entity read + запрет внутренних атрибутов OpenPosition); write-операции через REST для сайта невозможны;
- «реальное время» = pull по запросу + TTL-кеш сайта (рекомендация: Next.js ISR `revalidate=300`); закрытие/открытие вакансии в HRM отражается на сайте в течение TTL.

## 1. Общие сведения

| Параметр | Значение |
|---|---|
| Продукт | HRM HuntTech (CUBA 7.3, PostgreSQL, Tomcat 9) |
| Прод-хост | `hr.hunttech.ru:8080`, контекст `/hrm` |
| Base-путь REST | `http://hr.hunttech.ru:8080/hrm/rest/v2` (жёсткий путь аддона; `/api/v2` НЕ обслуживается) |
| Формат данных | JSON (UTF-8) |
| Аутентификация | OAuth2 password grant + refresh_token |
| Режим доступа | read-only |

## 2. Аутентификация (OAuth2)

### 2.1 Получение токена

```
POST http://hr.hunttech.ru:8080/hrm/rest/v2/oauth/token
Authorization: Basic base64(<client_id>:<client_secret>)
Content-Type: application/x-www-form-urlencoded

grant_type=password&username=<user>&password=<password>
```

Параметры (выдаются владельцем HRM):

| Параметр | Значение (локальный стенд) |
|---|---|
| client_id | `hrm-rest` |
| client_secret | `local-hrm-rest-secret-2026` (на проде — из local.app.properties, НЕ в git) |
| username | `site-reader` (на проде — свой тех. пользователь) |
| password | см. владельца |

Ответ:

```json
{
  "access_token": "...",
  "token_type": "bearer",
  "refresh_token": "...",
  "expires_in": 3599,
  "scope": "rest-api"
}
```

### 2.2 Продление токена

```
POST /hrm/rest/v2/oauth/token
Authorization: Basic base64(<client_id>:<client_secret>)

grant_type=refresh_token&refresh_token=<refresh_token>
```

Токены по умолчанию хранятся в памяти сервера — после рестарта Tomcat сайт должен запросить новый токен (обработать 401: перезапросить token, повторить запрос один раз).

## 3. Запросы

Все запросы: заголовок `Authorization: Bearer <access_token>`.

### 3.1 Открытые вакансии (главный)

```
POST /hrm/rest/v2/queries/hunttech_OpenPosition/openPositionPublic
```

Ответ — JSON-массив объектов `openPosition-public-view`. Пример элемента:

```json
{
  "id": "3f2c…-uuid",
  "_instanceName": "Java-разработчик",
  "openClose": false,
  "signDraft": false,
  "internalProject": false,
  "vacansyName": "Java-разработчик",
  "vacansyID": "JAVA-01",
  "shortDescription": "Разработка бэкенда на Java 11+…",
  "comment": "Полное описание вакансии…",
  "salaryMin": 250000,
  "salaryMax": 350000,
  "remoteWork": 1,
  "remoteComment": "Удалёнка",
  "workExperience": 3,
  "commandCandidate": 1,
  "closingDate": "2026-09-01",
  "grade": { "id": "…", "_instanceName": "Middle" },
  "cityPosition": { "id": "…", "_instanceName": "Москва" },
  "positionType": { "id": "…", "_instanceName": "Backend" },
  "projectName": { "id": "…", "_instanceName": "ХантТек" },
  "skillsList": [ { "id": "…", "_instanceName": "Java" } ]
}
```

Поля view (полный список): `openClose`, `vacansyName`, `vacansyID`, `shortDescription`, `comment` (LOB, полный текст), `salaryMin`, `salaryMax`, `remoteWork`, `remoteComment`, `workExperience`, `commandCandidate`, `closingDate`, `signDraft`, `internalProject`, `grade`, `cityPosition`, `positionType`, `projectName`, `skillsList`.

Маппинг Integer-полей:

| Поле | Значения |
|---|---|
| `remoteWork` | -1 «Не определено», 0 «Работа в офисе», 1 «Удаленка», 2 «Гибридная работа» |
| `workExperience` / `commandCandidate` | годы/месяцы опыта (Integer) |

FK-поля — вложенные объекты `{id, _instanceName}`. Даты — `YYYY-MM-DD`.

**Нюансы формата (важно для сайта):**
- REST v2 НЕ включает в JSON атрибуты со значением `null` — ключ просто отсутствует (например, у вакансии без даты закрытия нет ключа `closingDate`). Сайт должен трактовать отсутствующий ключ как null.
- Записи с soft-delete (`delete_ts` заполнен) платформа исключает из JPQL автоматически — в выдачу они не попадают.
- `comment` — LOB (полный текст описания), может отсутствовать у части вакансий.

### 3.2 Справочники (для фильтров/карточек)

| Запрос | Что отдаёт |
|---|---|
| `POST /hrm/rest/v2/queries/hunttech_City/cityAll` | Города (`cityRuName`) |
| `POST /hrm/rest/v2/queries/hunttech_Position/positionAll` | Должности (`positionRuName` / `positionEnName`) |
| `POST /hrm/rest/v2/queries/hunttech_Project/projectAll` | Проекты (`projectName`) |
| `POST /hrm/rest/v2/queries/hunttech_Grade/gradeAll` | Грейды (`gradeName`) |

Ответы — массивы объектов `_minimal`: `id`, `_instanceName` (и display-поля справочника).

## 4. Поля, которых НЕТ в контракте (запрещены)

Внутренние поля OpenPosition никогда не отдаются через REST: `outstaffingCost`, `percentComissionOfCompany`, `percentSalaryOfResearcher`, `percentSalaryOfRecrutier`, `typeCompanyComission`, `useTaxNDFL`, `salaryIE`, `memoForInterview`, `rawDescription`, `searchMap`, `interviewPlan`, `interviewChecklist`, `openPositionComments`, `someFiles`. Защита двойная: view не включает эти поля + роль технического пользователя имеет запрет чтения этих атрибутов.

## 5. Ошибки

| HTTP | Случай | Что делать сайту |
|---|---|---|
| 401 | токен истёк/невалиден | перезапросить токен, повторить запрос 1 раз |
| 403 | нет прав на сущность/атрибут | обратиться к владельцу HRM |
| 404 | неверный путь/запрос | проверить base-путь и имя запроса |
| 500 | ошибка сервера | retry с экспоненциальной задержкой (2–3 раза) |

## 6. Рекомендации по интеграции (сторона сайта)

- Next.js ISR: `revalidate = 300` на странице `/careers` — «реальное время» ~5 минут без собственного кеша.
- Токен: получать при старте и при 401; хранить в памяти/секрете процесса, не в клиентском коде.
- Вызывать API строго server-side (CORS не требуется; секреты не попадают в браузер).
- Сетевой доступ: сервер сайта → `hr.hunttech.ru:8080` (прод по http:8080; https-обвязка при необходимости на стороне HRM/nginx).

## 7. Развёртывание и проверка (сторона HRM)

- Локальный стенд: `scripts/rest_local_setup.sql` (идемпотентный: роль «REST чтение», пользователи `site-reader`/`rest-checker`).
- Прод: тех. пользователь и роль создаются штатным UI администрирования (или SQL по образцу скрипта), OAuth2-секрет переопределяется в `${app.home}/local.app.properties` (НЕ в WAR, НЕ в git).
- Smoke: токен → `openPositionPublic` → JSON; проверить отсутствие внутренних полей и только открытых вакансий.
- Питфоллы: base-путь `/rest/v2` (не `/api/v2`); `cuba.anonymousSessionId` НЕ задавать в core (роняет старт core — коммит 31bb4461, исправлено 2026-08-12).

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-12 | Актуализация: подтверждён прод-статус REST (сервлет 200/401 на проде), исправлен старт core (убран cuba.anonymousSessionId из core app.properties), локальные пользователи REST созданы, контракт оформлен |
| 2026-08-10 | Реализация REST API v2 (аддон restapi, view, rest-queries.xml, oauth2-клиент, роль и пользователи) — коммит 31bb4461 |
| 2026-08-07 | Первичный план интеграции сайта и контракт публикации (`.hermes/plans/2026-08-07_140000_openposition-site-integration.md`) |

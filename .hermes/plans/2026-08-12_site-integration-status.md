# Интеграция HRM HuntTech → hunttech.ru: статус 2026-08-12 и план подключения сайта

> Обновление плана 2026-08-07 (`.hermes/plans/2026-08-07_140000_openposition-site-integration.md`).
> Статус: REST-код закоммичен, прод-сервлет поднят; сайт ещё НЕ подключён.

## Что уже готово (проверено 2026-08-12)

- build.gradle: `appComponent("com.haulmont.addon.restapi:restapi-global:$cubaVersion")` + локальный maven `libs/cuba-rest-addon` (закоммичено).
- web-app.properties: `cuba.rest.queriesConfig`, client `hrm-rest`, `grant_type=password,refresh_token`, `anonymousEnabled=false`, `cuba.rest.contextPath=api` (НЕ работает — см. питфолл).
- rest-queries.xml: `openPositionPublic` (фильтр `openClose=false|NULL AND signDraft!=true AND internalProject=false`), `openPositionAll`, справочники `cityAll/positionAll/projectAll/gradeAll`.
- views.xml: `openPosition-public-view` (без финансовых полей) + `openPosition-report-view`.
- Локально: `/hrm/rest/v2/oauth/token` → 401 (сервлет жив); `/hrm/api/v2/...` → 301 на /hrm (contextPath=api не работает).
- Прод `hr.hunttech.ru:8080`: `/hrm/rest/v2/oauth/token` → 401 → **REST уже задеплоен**, https (443) не поднят — прод по http:8080.

## Что НЕ готово

- Локально: **ГОТОВО 2026-08-12**: `rest_local_setup.sql` применён, токен + `openPositionPublic` работают (38 открытых, без внутренних полей; справочники: city 293 / position 217 / project 781 / grade 4). Код НЕ менялся (гипотеза «anonymousSessionId ломает core» НЕ подтвердилась: с той же строкой core стартует и anonymous логин проходит; падение в 12:36 было гонкой старта web раньше core).
- Прод: сервлет REST жив (400 invalid_grant = core и oauth-контур работают); НЕ сделано: тех. пользователь + роль «REST чтение», переопределение `cuba.rest.client.id/secret` в `/opt/app_home/hrm/conf/local.app.properties`, рестарт. Требует подтверждений (правило 3 подтверждений, runbook).

## Сайт hunttech.ru (выяснено 2026-08-12)

- Next.js (App Router) + **headless CMS** (Strapi-стиль): `GET /api/vacancies?populate=*&sort[0]=department&pagination[pageSize]=100`, `GET /api/posts?...`.
- Страница `/careers` рендерит карточки (Flutter-разработчик, QA Engineer, Golang-разработчик — «СРОЧНЫЙ НАБОР», «Отклик → …») — это записи CMS сайта, **НЕ из HRM**.
- Сейчас `/api/vacancies` отдаёт **HTTP 500** во всех вариантах (в т.ч. из браузера) — бэкенд CMS сайта нездоров/заблокирован; страница живёт на кеше/SSR.

## План интеграции

### Блок A. Доделать нашу сторону (полдня)
1. Локально: `scripts/rest_local_setup.sql` → создать `site-reader` (роль «REST чтение»), проверить `password` grant → `openPositionPublic` возвращает JSON.
2. Прод: создать тех. пользователя и роль штатным UI админа (или SQL), секреты — в `local.app.properties` прод-апп-хоума (НЕ в WAR/git).
3. Smoke с прод-сервера: токен → query → JSON; проверить отсутствие внутренних полей (grep `outstaffingCost|percent|memo|searchMap`).
4. Путь для сайта: `http://hr.hunttech.ru:8080/hrm/rest/v2/...` (base-путь аддона жёстко `/rest/v2`). Если нужен красивый `/api/v2` — nginx-rewrite на сервере HRM или на сайте.

### Блок B. Сторона сайта (передать разработчику сайта) — 2 варианта
- **Вариант A (рекомендуемый): сайт тянет наш REST напрямую.** `/careers` (Next.js ISR `revalidate=300`):
  1. `POST http://hr.hunttech.ru:8080/hrm/rest/v2/oauth/token` (password grant) → `access_token`.
  2. `POST /hrm/rest/v2/queries/hunttech_OpenPosition/openPositionPublic` + `Authorization: Bearer`.
  3. Рендер карточек из JSON. HRM — единственный источник; закрытие в HRM видно на сайте ≤5 мин. Нужен сетевой доступ сервера сайта → hr.hunttech.ru:8080.
- **Вариант B (резерв): синхронизация в их CMS.** Крон-скрипт (их сервер или наш) раз в N минут тянет `openPositionPublic` и апдейтит коллекцию `vacancies` в их CMS. Сайт не меняется; минусы: дублирование, задержка, их API сейчас 500.
- Гибрид: начать с B, параллельно делать A.

### Блок C. Контракт (документ для разработчика сайта)
- Полный контракт — `docs/` (создать `docs/services/OpenPositionRestApi.md`): endpoint, OAuth, view-поля, формат FK (`{id, _instanceName}`), даты `YYYY-MM-DD`, фильтры справочников.

## Открытые вопросы (согласовать)
1. Кто делает сайт hunttech.ru (подрядчик/команда)? Репозиторий сайта локально не найден — кому передавать контракт?
2. Заменяем источник /careers полностью на HRM или дополняем CMS-записи? (Сейчас «СРОЧНЫЙ НАБОР» — ручные записи CMS.)
3. LOB `comment` (~4.6KB средний) — публиковать полное описание или только `shortDescription`?
4. `salaryIE`, `needLetter/needExercise` — публичные или нет?
5. Фильтры на сайте (город/грейд/зарплата) — параметры в query или фильтрация на сайте?

## Файлы
- Код REST: `build.gradle`, `modules/web/src/com/company/hunttech/web-app.properties`, `modules/web/src/com/company/hunttech/rest-queries.xml`, `modules/global/src/com/company/hunttech/views.xml` (всё закоммичено).
- Скрипт: `scripts/rest_local_setup.sql` (локальный).

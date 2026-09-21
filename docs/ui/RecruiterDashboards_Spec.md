# Recruiter Dashboards — операционные дашборды рекрутера

## Business & Context Intro

Набор из трёх полноэкранных Dashboard Add-on экранов даёт рекрутеру отдельные рабочие представления для текущих кандидатов, воронки найма и кадрового резерва. Реализация следует общей UI/UX-концепции HRM HuntTech и существующему контракту `recruiter-dashboard-root`.

## UI Context & Navigation

Меню «Подбор → Дашборды рекрутера»:

- `hunttech_RecruiterKanbanDashboard` — «Кандидаты в работе — Kanban»;
- `hunttech_RecruiterFunnelDashboard` — «Воронка найма»;
- `hunttech_RecruiterReserveDashboard` — «Кадровый резерв».

Каждый экран использует `dashboard:dashboard` с `jsonPath` и `timerDelay="60"`. JSON-модель хранится в исходниках и подключает собственный `@DashboardWidget`.

## Behavior Summary

- Kanban загружает последнее `IteractionList` по каждой паре candidate+vacancy за 30 дней и нормализует этап по sign-полям `Iteraction`;
- воронка берёт активные `RecrutiesTasks` текущего рекрутера и считает уникальные candidate+vacancy, достигшие этапов за 30 дней;
- кадровый резерв читает `PersonelReserve` текущего рекрутера и одним bulk-запросом сопоставляет position с открытыми `OpenPosition`;
- все три applet реализуют `RefreshableWidget`, поэтому обновляются штатным Dashboard lifecycle;
- presentation layer не изменяет `IteractionList`, `RecrutiesTasks`, `PersonelReserve` или `OpenPosition`.

## Data contracts

### Kanban

Источник: `hunttech_IteractionList`, узкий view
`recruiter-dashboard-iteraction-list-view`. Он загружает кандидата, вакансию и
все признаки `Iteraction`, используемые при нормализации этапа. Это исключает
обращение к detached-атрибутам при отрисовке Kanban.

Последний кейс определяется по максимальному `numberIteraction` для recruiter+candidate+vacancy. Кандидат не сворачивается до одного глобального статуса: один человек может находиться на разных стадиях по разным вакансиям.

### Funnel

Источники:

- `hunttech_RecrutiesTasks` / `recrutiesTasks-view`;
- `hunttech_IteractionList` / `recruiter-dashboard-iteraction-list-view`.

Воронка считает уникальный ключ candidate+vacancy на каждом достигнутом этапе.

### Reserve

Источники читаются через `loadValues`:

- `hunttech_PersonelReserve`;
- открытые недрафтовые `hunttech_OpenPosition`.

Matching v1 намеренно использует одинаковую `Position`: это детерминированный fallback, не зависящий от полноты AI-навыков кандидата.

## Stage normalization

Порядок приоритета:

1. кадровый резерв;
2. конец кейса / отказ;
3. offer/final;
4. интервью заказчика;
5. отправка заказчику;
6. интервью HuntTech;
7. новый / прочее.

Основной источник — sign-поля `Iteraction`; строковые «оффер/offer/отказ» используются только для legacy fallback.

## Visual contract

- root: `edit-workspace recruiter-dashboard-root`;
- surface: `widget-border`, radius 8 px, theme-aware border/shadow;
- controls: общий dashboard contract 38 px;
- accent/focus: `#ffb11b`;
- KPI: компактные карточки без декоративных gauge;
- Kanban: CSS Grid 3 → 2 → 1 колонка;
- funnel: пропорциональные horizontal bars + табличная детализация;
- reserve: dense table-like list;
- global horizontal scroll запрещён;
- SCSS partial остаётся идентичным во всех семи поддерживаемых темах.

## CUBA Dashboard Add-on

Виджеты зарегистрированы через `@DashboardWidget`. Dashboard-композиции версионируются как JSON и загружаются через `jsonPath`, что является штатным механизмом add-on и не требует ручного изменения `dashboard_persistent_dashboard`.

## Verification

Обязательные проверки перед merge:

```bash
./gradlew :app-core:test --tests 'com.company.hunttech.core.RecruiterDashboardsContractTest' --no-daemon --stacktrace
./gradlew :app-core:test --tests 'com.company.hunttech.core.ScreenViewIntegrityTest' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Visual smoke: 1366×768, 1920×1080, 1920×1200; все семь тем; horizontal page scroll отсутствует.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-09-21 | Исправлена загрузка признаков этапа для Kanban и воронки: добавлен специализированный view и контрактный тест против `Unfetched Attribute Access`. |
| 2026-09-19 | Добавлены три Dashboard Add-on экрана: Kanban кандидатов, воронка найма и кадровый резерв; добавлены applet widgets, stage normalization и единый HuntTech visual contract. |

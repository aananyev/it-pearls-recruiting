# Recruiter Dashboards — операционные дашборды рекрутера

## Business & Context Intro

Набор из трёх полноэкранных Dashboard Add-on экранов даёт рекрутеру отдельные рабочие представления для текущих кандидатов, воронки найма и кадрового резерва. Kanban доступен как «Дашборды → Kanban». Верхнеуровневый раздел объединяет эти экраны с двумя dashboard аналитики AI; старый список «Кандидаты в работе» в разделе «HR-мастер» остаётся отдельным экраном с фильтрами и таблицей. Реализация следует общей UI/UX-концепции HRM HuntTech и существующему контракту `recruiter-dashboard-root`.

## UI Context & Navigation

Группа меню `application-hunting → recruiter-dashboards` — «Дашборды рекрутера» (`DASHBOARD`) содержит три операционных экрана:

- `hunttech_RecruiterKanbanDashboard` — «Kanban»;
- `hunttech_RecruiterFunnelDashboard` — «Воронка найма»;
- `hunttech_RecruiterReserveDashboard` — «Кадровый резерв».

Экраны аналитики AI расположены в группе `aiAdministration` («Управление AI»):
- `hunttech_AdminAiDashboard` — «Дашборд аналитики AI»;
- `hunttech_UserAiDashboard` — «Моя статистика AI».

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

Persistent dashboard с кодом `recruiting-dashboard` не является отдельным menu screen и намеренно не добавляется в «Дашборды»: он остаётся стартовой dashboard-моделью `HrmMainScreen`. Верхнеуровневое меню содержит только пять явно перечисленных screen IDs.

## Verification

Обязательные проверки перед merge:

```bash
./gradlew :app-core:test --tests 'com.company.hunttech.core.RecruiterDashboardsContractTest' --no-daemon --stacktrace
./gradlew :app-core:test --tests 'com.company.hunttech.core.ScreenViewIntegrityTest' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Visual smoke: 1366×768, 1920×1080, 1920×1200; все семь тем; horizontal page scroll отсутствует.

## Kanban Card Actions & Global Filter

### Меню «Дополнительные действия» в карточке
В правом верхнем углу каждой карточки кандидата размещена компактная кнопка `PopupButton` без текста с пиктограммой `font-icon:ELLIPSIS_V` («Дополнительные действия»), открывающая выпадающее меню действий:
- **Группа 1: Взаимодействия и интервью**
  - «Создать взаимодействие» (`font-icon:PLUS_CIRCLE`) — открытие `IteractionListEdit` с новым черновиком взаимодействия;
  - «Назначить собеседование рекрутера» (`font-icon:CALENDAR_PLUS_O`) — создание взаимодействия с типом назначения интервью на нашей стороне (`signOurInterviewAssigned = true`);
  - «Назначить собеседование у заказчика» (`font-icon:USERS`) — создание взаимодействия с типом интервью заказчика (`signClientInterview = true`).
- **Разделитель**
- **Группа 2: Перемещение и статус кейса**
  - «Перенести в колонку...» (`font-icon:ARROWS`) — диалог перемещения кандидата в выбранную колонку канбан-доски (программный аналог drag-and-drop);
  - «Сделать комментарий» (`font-icon:COMMENTING_O`) — создание взаимодействия типа «Комментарий» (`signComment = true`);
  - «Закрыть процесс взаимодействия / отказ» (`font-icon:BAN`) — создание взаимодействия отказа/завершения процесса (`signEndCase = true`).
- **Разделитель**
- **Группа 3: Досье и маркировка**
  - «Карточка профиля» (`font-icon:USER`) — открытие формы редактирования `JobCandidate`;
  - «Последнее резюме (открыть)» (`font-icon:FILE_TEXT_O`) — открытие последнего `CandidateCVEdit`;
  - «Поставить признак...» (`font-icon:TAG`) — диалог назначения/снятия пользовательского признака-лейбла (`SignIcons`).

### Подсказка проекта
В карточке кандидата отображается строка проекта и строка вакансии. При наведении мыши на проект выводится нативная подсказка браузера с полным наименованием проекта (Компания / Подразделение · Название проекта). Устранены ошибочные технические тултипы колонок `stage:uuid`.

### Режим «ВСЕ» в фильтре рекрутера
В выпадающем списке выбора рекрутера первым пунктом выводится вариант **«ВСЕ»**. При его выборе:
- Загружаются и отображаются кейсы кандидатов всех сотрудников без фильтра по конкретному пользователю;
- В каждую карточку добавляется плашка с назначенным рекрутером кандидата. Рекрутер определяется как сотрудник, назначивший или проведший собеседование на стороне HuntTech (последняя запись с `signOurInterviewAssigned = true` или `signOurInterview = true`), с fallback на рекрутера последнего взаимодействия.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-09-26 | В карточки Kanban добавлено меню «Дополнительные действия» (9 действий в 3 группах), всплывающая подсказка полного имени проекта, режим «ВСЕ» в фильтре рекрутеров с выводом ответственного рекрутера. |
| 2026-09-22 | Пять dashboard-экранов объединены в верхнеуровневом меню «Дашборды»; `recruiting-dashboard` зафиксирован как стартовый persistent dashboard, а не пункт меню. |
| 2026-09-21 | В меню, заголовке экрана и составе виджета название дашборда сокращено до «Kanban». |
| 2026-09-21 | Исправлена загрузка признаков этапа для Kanban и воронки: добавлен специализированный view и контрактный тест против `Unfetched Attribute Access`. |
| 2026-09-19 | Добавлены три Dashboard Add-on экрана: Kanban кандидатов, воронка найма и кадровый резерв; добавлены applet widgets, stage normalization и единый HuntTech visual contract. |

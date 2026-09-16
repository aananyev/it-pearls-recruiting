# Спецификация архитектуры и реализации сервиса назначения собеседования из LLM-чата

**Версия:** 1.0  
**Дата:** 16 сентября 2026 г.  
**Модули:** `hunttech-global`, `hunttech-core`, `hunttech-web`  
**Технологический стек:** CUBA Platform 7.3, Spring Framework, PostgreSQL, Liquibase, Yandex 360 CalDAV & Telemost API.

---

## 1. Назначение и контекст сервиса

Сервис `InterviewSchedulingActionService` (`hunttech_InterviewSchedulingActionService`) обеспечивает возможность назначения собеседования кандидату на естественном языке из обеих вкладок плавающего диалога LLM-чата HRM HuntTech:
1. **«Локальный чат»** (`LlmChatService` / `LlmChatServiceBean`, включая синхронный режим и SSE-стриминг `streamStreaming`).
2. **«Hermes»** (`HermesChatService` / `HermesChatServiceBean`).

Сервис полностью инкапсулирует:
- NLU-распознавание интента (`INTERVIEW_SCHEDULING_PARSE` через `AiExecutionService` + быстрый детерминированный regex-фильтр `isInterviewSchedulingIntent`).
- Управление состоянием многошагового диалога (`LlmChatPendingAction` в PostgreSQL) для ситуаций неоднозначности (несколько кандидатов, несколько открытых вакансий, выбор типа интервью, подтверждение даты/времени, отмена).
- Точный резолвинг сущностей HRM:
  - `JobCandidate`: поиск по частям ФИО с лемматизацией/стеммингом падежей.
  - `OpenPosition`: только открытые вакансии (`openClose != true`), поиск по должности, названию проекта (`Project.projectName`), владельцу проекта (`Project.projectOwner -> Person.firstName / secondName`).
  - `Iteraction`: определение типа интервью по признакам `calendarItem`, `addFlag`, `signOurInterviewAssigned`, `signClientInterview` (без хардкода UUID).
- Создание сущности `IteractionList` с полным соблюдением доменного жизненного цикла системы:
  - `numberIteraction = max + 1` (через `InteractionService.getCountInteraction()`);
  - `dateIteraction = now()`, `addDate = targetDate`;
  - `recrutier = currentUser`;
  - `currentPriority = vacancy.priority`, `currentOpenClose = vacancy.openClose`;
  - `chainInteraction = lastInteraction` (по вакансии и кандидату);
  - `addToCalendar = true`, `calendarId`, `calendarEventId`, `calendarSyncState = 'SYNCED'`.
- Синхронизацию события в Яндекс Календаре (CalDAV) и создание видеовстречи Телемост через существующий `YandexIntegrationService`.
- Формирование ответа пользователю с Markdown-ссылками `[ФИО](hrm://candidate/<UUID>)`, `[Вакансия](hrm://vacancy/<UUID>)`, `[Телемост](https://telemost.yandex.ru/j/...)`.

---

## 2. Архитектурная модель данных

### 2.1. Таблица и сущность `LlmChatPendingAction` (`HUNTTECH_LLM_CHAT_PENDING_ACTION`)

```sql
CREATE TABLE IF NOT EXISTS HUNTTECH_LLM_CHAT_PENDING_ACTION (
    ID uuid PRIMARY KEY,
    VERSION integer NOT NULL,
    CREATE_TS timestamp,
    CREATED_BY varchar(50),
    UPDATE_TS timestamp,
    UPDATED_BY varchar(50),
    DELETE_TS timestamp,
    DELETED_BY varchar(50),
    USER_ID uuid NOT NULL,
    CONVERSATION_ID uuid NOT NULL,
    ACTION_TYPE varchar(64) NOT NULL,
    STATUS varchar(32) NOT NULL DEFAULT 'PENDING',
    EXPIRES_AT timestamp NOT NULL,
    STATE_DATA text,
    REQUEST_ID varchar(64)
);

CREATE INDEX IF NOT EXISTS IDX_HUNTTECH_PENDING_ACTION_USER_CONV 
    ON HUNTTECH_LLM_CHAT_PENDING_ACTION (USER_ID, CONVERSATION_ID, STATUS);
```

### 2.2. DTO-модели

1. `InterviewSchedulingIntent`:
   - `String rawMessage`
   - `String candidateQuery`
   - `String vacancyQuery`
   - `String projectQuery`
   - `String projectOwnerQuery`
   - `String interactionTypeQuery`
   - `String rawDateTime`
   - `Date targetDateTime`
   - `String timeZone`
   - `boolean online`
   - `boolean intentDetected`

2. `InterviewSchedulingResult`:
   - `boolean success`
   - `boolean pendingUserClarification`
   - `String pendingStep` (`NEED_CANDIDATE`, `NEED_VACANCY`, `NEED_INTERACTION_TYPE`, `CONFIRM_SCHEDULE`, `NONE`)
   - `String message`
   - `UUID iteractionListId`
   - `String telemostJoinUrl`
   - `String eventUid`
   - `String calendarName`

---

## 3. Сквозной жизненный цикл выполнения

```
[Пользователь] "Назначь собеседование для Эльчина Аббасова на вакансию Системного аналитика в проект Дениса Гаркушина на завтра на 17-00"
       │
       ▼
[LlmChatService / HermesChatService]
       │
       ▼
[InterviewSchedulingActionService.handleMessage()]
       │
       ├──► 1. Проверка активного Pending Action пользователя (ответ "1", "первого", "да", "отмена")
       │
       ├──► 2. Быстрый regex-фильтр: isInterviewSchedulingIntent()
       │       (если false -> возврат null, управление переходит в стандартный LLM)
       │
       ├──► 3. AI NLU Intent Parse: вызов AI-функции INTERVIEW_SCHEDULING_PARSE
       │
       ├──► 4. Candidate Resolver:
       │       - 0 кандидатов -> "Кандидат не найден, уточните ФИО"
       │       - >1 кандидатов -> сохранение Pending Action (NEED_CANDIDATE), вывод нумерованного списка кандидатов со ссылками
       │       - 1 кандидат -> JobCandidate
       │
       ├──► 5. Vacancy Resolver:
       │       - Фильтр: openClose != true (только открытые вакансии)
       │       - Поиск по vacansyName + projectName + projectOwner (Денис Гаркушин)
       │       - 0 вакансий -> вывод предупреждения (или списка активных)
       │       - >1 вакансий -> сохранение Pending Action (NEED_VACANCY), нумерованный список
       │       - 1 вакансия -> OpenPosition
       │
       ├──► 6. Interaction Type Resolver:
       │       - Приоритет: calendarItem = true & signOurInterviewAssigned / signClientInterview / addFlag
       │       - Если тип встречи не указан явно -> выбор "Интервью с заказчиком" (если проект заказчика) или "Назначено интервью"
       │
       ├──► 7. Date & Time Resolver:
       │       - Вычисление даты относительно текущего дня с учетом TimeZone ("Europe/Moscow", "Europe/Saratov")
       │
       ├──► 8. Создание IteractionList:
       │       - numberIteraction = count + 1
       │       - dateIteraction = now()
       │       - addDate = targetDateTime (17:00)
       │       - chainInteraction = lastInteraction(candidate, vacancy)
       │       - currentPriority = vacancy.priority
       │       - currentOpenClose = vacancy.openClose
       │       - dataManager.commit(interaction)
       │
       ├──► 9. Yandex 360 Calendar & Telemost:
       │       - yandexIntegrationService.scheduleCalendarEvent() / syncInteractionCalendarEvent()
       │       - Добавление инвайта кандидату (если указан email) и рекрутеру
       │       - Получение eventUid и ссылки Телемост
       │
       └──► 10. Формирование ответа пользователю с активными ссылками HRM и Телемост
```

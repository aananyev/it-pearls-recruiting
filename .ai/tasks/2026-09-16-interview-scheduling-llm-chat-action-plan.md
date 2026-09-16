# План реализации: Назначение собеседования кандидату и события Yandex 360 из LLM-чата

**Дата:** 16 сентября 2026 г.  
**Ветка:** `agent/antigravity-dev`  
**Ответственный:** Руководитель проектов / Project Manager

---

## 1. Задачи и этапы выполнения

### Этап 1. Документация и AI-контракты (Аналитик и Промпт-инженер)
- [x] Создана архитектурная спецификация `docs/services/InterviewSchedulingActionService.md`.
- [x] Спроектирован системный промпт, схема JSON и FreeMarker-шаблон для AI-функции `INTERVIEW_SCHEDULING_PARSE`.
- [ ] Подготовить Liquibase миграцию `260916-1-addInterviewSchedulingActionAndPendingTable.xml` и включить в `db.changelog-master.xml`:
  - Создание таблицы `HUNTTECH_LLM_CHAT_PENDING_ACTION`.
  - Регистрация AI-функции `INTERVIEW_SCHEDULING_PARSE`.

### Этап 2. Java Backend Разработка
- [ ] Создать сущность `LlmChatPendingAction` в `modules/global/src/com/company/hunttech/entity/ai/LlmChatPendingAction.java`.
- [ ] Зарегистрировать `LlmChatPendingAction` в `persistence.xml` и `views.xml`.
- [ ] Создать DTO-классы в `com.company.hunttech.dto.action`:
  - `InterviewSchedulingIntent`
  - `InterviewSchedulingResult`
  - `PendingActionState`
- [ ] Создать интерфейс `InterviewSchedulingActionService` в `modules/global/src/com/company/hunttech/service/InterviewSchedulingActionService.java`.
- [ ] Реализовать `InterviewSchedulingActionServiceBean` в `modules/core/src/com/company/hunttech/service/InterviewSchedulingActionServiceBean.java`:
  - Regex детектор `isInterviewSchedulingIntent(message)`.
  - Парсинг интента через `AiExecutionService` (`INTERVIEW_SCHEDULING_PARSE`).
  - Резолвер кандидата с стеммингом падежей.
  - Резолвер открытых вакансий (`openClose != true`) с поддержкой названий проектов и ФИО владельцев проектов.
  - Резолвер типов `Iteraction` по метаданным.
  - Создание `IteractionList` с domain lifecycle.
  - Вызов `yandexIntegrationService` для бронирования календаря и Телемоста.
  - Управление жизненным циклом `LlmChatPendingAction` (сохранение состояния, ответы номерами "1", "первого", подтверждения "да", отмена).
- [ ] Зарегистрировать сервис в `web-spring.xml`.
- [ ] Интегрировать сервис в `LlmChatServiceBean` (`sendMessage`, `streamStreaming`) и в `HermesChatServiceBean` (`sendHermesMessage`).

### Этап 3. Актуализация документации (Технический писатель)
- [ ] Проверить соответствие документации фактически реализованным классам и методам.
- [ ] Дополнить `docs/ai/LLM_CHAT_IMPLEMENTATION.md` и `docs/services/InterviewSchedulingActionService.md`.

### Этап 4. Автотестирование (QA Тестировщик)
- [ ] Разработать контрактные тесты `InterviewSchedulingActionContractTest.java` в `modules/core/test/com/company/hunttech/core/`.
- [ ] Запустить тесты через `bash ../hunttech_recruiting/scripts/agent-gradle.sh test`.

### Этап 5. Ревизия кода и Git протокол
- [ ] Запустить Alibaba OCR (`ocr review --audience agent`).
- [ ] Устранить замечания.
- [ ] Выполнить git commit (русское сообщение) + `git push origin HEAD:agent/antigravity-dev`.
- [ ] Сформировать финальный отчет.

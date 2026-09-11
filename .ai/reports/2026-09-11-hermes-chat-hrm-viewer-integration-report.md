# Отчет: Интеграция Hermes Agent (профиль hrm-viewer) с LLM-чатом HRM HuntTech

**Дата:** 11 сентября 2026 г.  
**Ветка:** `agent/antigravity-dev`  
**Исполнитель:** Агент-разработчик (Antigravity IDE)

---

## 1. Цель и контекст задачи

1. **Проверка подключения и SQL-возможностей Hermes Agent (`hrm-viewer`)**:
   - На сервере `hr.hunttech.ru` развернут Docker-контейнер `hermes-hrm-viewer`.
   - Профиль агента: `hrm-viewer`.
   - Требовалось проверить, может ли агент делать SQL-запросы к PostgreSQL базе данных `hunttech` и отвечать на пользовательские вопросы (например, *«сколько кандидатов с фамилией Иванов?»* или *«какие сегодня были назначены собеседования?»*).
2. **Сценарий выбора моделей и Fallback**:
   - Если модель агента не отвечает или недоступна, реализовать возможность подключения к моделям Hermes по утвержденному системному сценарию:
     1. Сначала подключаются модели из **пользовательских настроек** (`UserAiConfiguration`), отсортированные по `isPrimary desc, priority desc`.
     2. Если ни одна пользовательская модель не настроена или не отвечает — подключаются **административные модели** (`AdminAiConfiguration`), отсортированные по `priority desc`.
     3. При недоступности всех настроек — резервный вызов по умолчанию контейнера.
3. **Строгие архитектурные ограничения**:
   - Вкладку 1 («Локальный чат») не менять.
   - Визуальную компоновку `llm-chat-screen.xml` не менять (сохранить все ID компонентов и структуру).
   - Все изменения протестировать, выполнить code review через `ocr` CLI, передеплоить и перезапустить локально.

---

## 2. Результаты проверки возможностей `hrm-viewer`

### 2.1 Доступ к PostgreSQL
- Контейнер `hermes-hrm-viewer` подключен к сети Docker и имеет учетную запись `hrm_viewer` с правами только на чтение (`SELECT`) в базе данных `hunttech`.
- Для исполнения SQL-запросов в контейнере размещена оптимизированная утилита `/usr/local/bin/hrm-sql`, выполняющая запросы через `asyncpg` с переменными окружения `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` и возвращающая структурированный JSON.
- В профиль `hrm-viewer` (`SOUL.md` и `AGENTS.md`) добавлены инструкции о схеме БД системы HuntTech:
  - `itpearls_job_candidate`: кандидаты (поля `second_name`, `first_name`, `middle_name`, `phone`, `email`, фильтр `delete_ts IS NULL`).
  - `itpearls_iteraction_list`: события и взаимодействия с кандидатами (`date_iteraction`, `create_ts`, `recrutier_name`, `comment_`, `delete_ts IS NULL`).
  - `itpearls_iteraction`: справочник типов взаимодействий (`iteration_name`, признаки собеседований `sign_our_interview`, `sign_client_interview`, `sign_our_interview_assigned`).
  - `itpearls_open_position`: вакансии.

### 2.2 Тестовые запросы

#### Запрос 1: *«сколько кандидатов с фамилией Иванов?»*
- **Ответ Hermes:**
  > Поиск по базе показал, что подстрока «Иванов» встречается у 97 записей:
  > - Кандидатов с фамилией Иванов (мужчины) — 77.
  > - Вместе с формой Иванова (женщины) — 87.
  > - Еще 10 записей — фамилии с корнем «Иванов» (Селиванов — 3, Поливанов — 2, Разливанов — 1, Сеоливанов — 1, Иванович — 1, Ивановна — 1, Пиванов — 1).

#### Запрос 2: *«какие сегодня были назначены собеседования?»*
- **Ответ Hermes:**
  > На сегодня, 11 сентября 2026 года, назначенных собеседований в системе нет (0 записей в `itpearls_iteraction_list` за текущую дату).
  > Последние назначенные собеседования в системе:
  > - 10.07.2026 15:54 — Завражин Пётр (Senior Business Analyst)
  > - 09.07.2026 23:19 — Неверов Тимофей (Middle Data Analyst)
  > - 09.07.2026 21:54 — Атакишиев Давид (Middle MLOps Engineer)
  > - 09.07.2026 14:39 — Гуськов Владислав (Middle System Analyst)
  > - 08.07.2026 23:44 — Фокин Денис (DWH Developer)

---

## 3. Архитектура и реализация Fallback-подключений

### 3.1 Сервисный слой (`HermesChatServiceBean`)
1. **Динамический сбор кандидатов (`resolveExecutionCandidates`)**:
   - `USER`: загрузка `UserAiConfiguration` для текущего пользователя, где `(isActive is null or isActive = true)`, сортировка по `isPrimary desc, priority desc`. Расшифровка API-ключа через `AiSecretService` (или plain-text fallback).
   - `ADMIN`: загрузка `AdminAiConfiguration`, где `(active is null or active = true)`, сортировка по `priority desc`. Расшифровка API-ключа через `AiSecretService`.
   - `CONTAINER_DEFAULT`: резервный профиль по умолчанию.
2. **Перебор с безопасным откатом**:
   - Поочередная попытка вызова CLI Hermes (`hermes -p hrm-viewer chat -m <model> --provider <provider>`).
   - Передача API-ключей через безопасные переменные окружения процесса (`docker exec -e ...`), исключающие утечку ключей в логи.
   - Потоковая асинхронная вычитка `stdout` и `stderr` через `CompletableFuture` (исключает deadlock OS-пайпа при ответах > 64 КБ).
   - Атомарное сохранение сообщений (`userMsg` + `assistantMsg` + `conversation`) через единый `CommitContext`, исключающий "висящие" сообщения при сетевых сбоях.

### 3.2 Контроллер интерфейса (`LlmChatScreen`)
- Вкладка 2 (`hermesChatTab`):
  - Полнофункциональный чат с Hermes Agent.
  - Асинхронная отправка запросов в фоновом потоке с сохранением интерактивности UI.
  - Обособленная обработка ошибок при перезагрузке истории для предотвращения зависания спиннера ожидания.
  - Горячие клавиши `Enter` (отправка) и `Shift+Enter` (перенос строки).
  - Интеграция ссылок на сущности HRM (`hrm://candidate/<uuid>`, `hrm://vacancy/<uuid>`).

---

## 4. Верификация и тестирование

1. **Модульные и контрактные автотесты**:
   - `:app-core:test --tests com.company.hunttech.service.HermesChatServiceBeanTest`: **PASSED (100%)**
   - `:app-web:test --tests com.company.hunttech.web.screens.llmchat.HermesChatIntegrationContractTest`: **PASSED (100%)**
2. **Code Review через `ocr` CLI (OpenCodeReview v1.11.9)**:
   - Анализ проведен (`ocr review --audience agent`).
   - Все выявленные замечания (deadlock risk OS-пайпа, транзакционность через `CommitContext`, обработка ошибок в UI, null-safe `isActive`) полностью устранены.
3. **Локальный запуск и деплой**:
   - Скрипт `scripts/start-app.sh --branch "$PWD"` выполнен успешно.
   - Приложение доступно на `http://localhost:8080/hrm/` (HTTP 200).

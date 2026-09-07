# ОТЧЁТ HERMES-1: Обследование окружения для authenticated staging-приёмки PR #230

**Дата:** 2026-09-05  
**Роль:** DevOps-инженер (Hermes-1)  
**Ветка:** `feat/llm-chat`, commit `25793af`  
**Статус:** Только обследование. Ничего не развёрнуто, не изменено, production не затрагивался.  
**Ограничение:** Запрещено изменять сетевые настройки локальной машины.

---

## 1. Наличие отдельной staging-среды

**Результат:** Отдельной доступной staging-среды **нет**.

- В репозитории нет конфигураций для staging (docker-compose, nginx, k8s, Helm, Terraform).
- Скрипты деплоя (`scripts/start-app.sh`, `scripts/agent-gradle.sh`) ориентированы на **локальный Tomcat** (`deploy/tomcat`).
- `verify-llm-chat-staging.sh` ожидает **внешний staging URL** как аргумент.
- Production `hr.hunttech.ru` — запрещён к вмешательству без явного распоряжения.

**Вывод:** Требуется **создать выделенную staging-среду** (отдельный хост/VM/контейнер/K8s namespace) или предоставить доступ к существующей, если она развёрнута вне репозитория.

---

## 2. Развёртывание commit `25793af` с выключенным feature flag

**Результат:** **Может**, при наличии staging-инфраструктуры.

- Feature flag реализован: чат отключается через конфигурацию (`executionPolicy`, `privacyPolicyVersion` gate).
- Сборка проходит: `BUILD SUCCESSFULL` на полном наборе тестов.
- Миграции additive/idempotent (7 changeSet), SQL-близнецы в `modules/core/db/update/postgres/26/`.
- **Требуется:** staging Tomcat + PostgreSQL, переменные окружения (`hunttech.ai.encryptionKey`, `hunttech.ai.previousEncryptionKey`), выключенный флаг чата до миграций.

---

## 3. Синтетические учётные записи для staging

**Результат:** **Можно создать/использовать** — в коде нет хардкода, блокирующего эти логины.

- `admin/admin` — стандартный CUBA-администратор (создаётся при инициализации БД).
- `alan/Dodo-2012` — пользователь с ролью Management.
- В `LLM_CHAT_STAGE4_RUNTIME_EVIDENCE_TEMPLATE.md` предусмотрены **synthetic staging fixtures** для изолированного staging.
- **Важно:** Эти учётки **не должны существовать в production**. В staging их можно завести через `DataManager` или Liquibase-seed.

---

## 4. Sandbox / Mock LLM Provider

**Результат:** **Есть в тестах (Mockito), нет как самостоятельный развёртываемый сервис.**

- В `AiExecutionServiceBeanTest` моки `AIProvider` покрывают:
  - обычный ответ + usage tokens
  - streaming (`executeTextStreaming` + `AiStreamListener`)
  - ошибка личного API (`RuntimeException`)
  - timeout/cancel (`AiRequestCancelledException`, `cancelRequest`)
  - providerRequestId
- Реальных mock-серверов (WireMock, MockServer) в репозитории **нет**.
- `AbstractOpenAiCompatibleProvider` требует HTTP-endpoint с SSE, Bearer auth, `stream_options.include_usage=true`.

**Hermes-1 может развернуть временный mock provider** (WireMock / Python FastAPI) на отдельном порту staging.

**Требуется решение:** развертывать mock provider в staging или использовать unit-тесты как доказательство (не покрывает интеграционный transport).

---

## 5. Reverse Proxy (WebSocket, sticky-session, streaming timeout, reconnect/polling recovery)

**Результат:** **Конфигурации нет в репозитории. Hermes-1 может настроить, если есть доступ к прокси.**

- `verify-llm-chat-staging.sh` проверяет **прямой доступ** к `/PUSH/` (WebSocket HTTP 101) — **без прокси**.
- Для staging нужны:
  - **WebSocket pass-through** (`Upgrade`, `Connection` headers)
  - **Sticky-session / affinity** (Vaadin push требует привязку сессии к Tomcat-инстансу)
  - **Streaming timeout** ≥ 60-120 сек (LLM streaming долгий; `READ_TIMEOUT_MS=60000` в провайдере)
  - **Reconnect/polling recovery**: UI использует polling 3 сек (`delay="3000"`) как fallback — прокси не должен блокировать частые короткие запросы.

**Требуется:** доступ к nginx/Traefik/HAProxy конфигурации staging или инструкции инфраструктурной команде.

---

## 6. Миграции и migration/rollback rehearsal в staging без изменения production

**Результат:** **Может, полностью.**

- Все 7 changeSet additive/idempotent (`IF NOT EXISTS`, `WHERE NOT EXISTS`).
- SQL-близнецы в `modules/core/db/update/postgres/26/` — для ручного применения без Liquibase.
- `LLM_CHAT_PRODUCTION_MIGRATION_PLAN.md` содержит полный rehearsal план (пункты 1–10 Preflight).
- Rollback: выключение feature flag, сохранение таблиц/истории/аудита, восстановление конфигурации из backup.
- **Hermes-1 может:** применить миграции на staging БД, прогнать rehearsal, зафиксировать counts/checksums.

**Условие:** Отдельная staging PostgreSQL база.

---

## 7. Настройка staging параметров

**Результат:** **Может, через seed-данные и конфигурацию — после получения утверждённых значений.**

| Параметр | Значение | Где задаётся | Статус |
|----------|----------|--------------|--------|
| Общая квота | `10000` токенов/мес | `AiFunctionConfiguration.defaultMonthlyTokenQuota` | Обязательный seed |
| Privacy policy version | `llm-chat-privacy-v1` | Seed в `260905-1` | Зафиксировано в plan |
| System prompt | Текущий из migration seed | `promptTemplate` + `systemPrompt` | **Требует утверждения** текста и checksum |
| `MAX_TOKENS=1200` | В options провайдера | `AiFunctionConfiguration` / `UserAiFunctionOverride` | Обязательный seed |
| `temperature=0.4` | В options провайдера | Аналогично | Обязательный seed |
| Разрешённые provider/model/region | Из AI Control Plane | `AdminAiConfiguration`, `UserAiConfiguration` | **Требует утверждения** списка |

---

## 8. Требуемые от владельца проекта доступы, параметры, решения

| # | Что требуется | От кого | Критичность |
|---|---------------|---------|-------------|
| 1 | **Staging URL / инфраструктура** (Tomcat + PostgreSQL + reverse proxy + DNS) | Владелец / Инфраструктура | **Блокер** |
| 2 | **Утверждённый system prompt** (точный текст, версия, checksum) | Владелец / Аналитик | **Блокер для seed** |
| 3 | **Список разрешённых provider/model/region** (AI Control Plane) | Владелец / Аналитик | **Блокер для routing** |
| 4 | **Sandbox credentials** для провайдеров (или решение использовать mock provider) | Владелец | **Блокер для интеграционного теста** |
| 5 | **Доступ к reverse proxy** (nginx/Traefik конфиг) для WebSocket/sticky/timeout | Инфраструктура | **Блокер для transport** |
| 6 | **Staging PostgreSQL** (отдельная база, доступы) | Инфраструктура | **Блокер для миграций** |
| 7 | **Подтверждение synthetic users** (`admin/admin`, `alan/Dodo-2012` только в staging) | Владелец | Низкая |
| 8 | **Явное решение о запуске staging rehearsal** | Владелец | Процессуальное |

---

## 9. Что Hermes-1 может сделать самостоятельно, а что требует ручного доступа/согласования

### ✅ Может самостоятельно (при наличии инфраструктуры и параметров 1–6):

1. Развернуть приложение из commit `25793af` на staging Tomcat.
2. Применить 7 миграций (Liquibase или SQL) на staging БД.
3. Выполнить migration/rollback rehearsal по плану.
4. Загрузить seed-данные: функция `LLM_CHAT`, system prompt, privacy policy, квоты, лимиты, admin config.
5. Развернуть временный **mock LLM provider** (WireMock/FastAPI) на staging хосте.
6. Настроить reverse proxy (есть доступ к конфигу) для WebSocket, sticky-session, timeout, reconnect.
7. Запустить `scripts/verify-llm-chat-staging.sh <staging-url> 20` — transport smoke.
8. Выполнить authenticated UI сценарии через CDP/Playwright (вход, чат, push, polling recovery, quota, fallback, cancel, owner isolation).
9. Нагрузочная проверка 20–50 UI сессий, reconnect, sticky-session.
10. Зафиксировать evidence в отчёте (без секретов).

### ❌ Требует ручного доступа / отдельного согласования:

| Действие | Почему |
|----------|--------|
| Создание/предоставление staging инфраструктуры (VM, K8s, DB, DNS, TLS) | Вне репозитория, инфраструктурная ответственность |
| Выдача доступа к reverse proxy конфигурации | Безопасность, разделение полномочий |
| Утверждение точных значений seed (system prompt, provider/model/region, квоты) | Бизнес-решение, не техническое |
| Предоставление sandbox API ключей провайдеров | Секреты, финансовая ответственность |
| Явное разрешение на запуск staging rehearsal | Процессуальный gate |
| Production deployment / merge в master | Только по отдельному распоряжению после acceptance |

---

## Резюме

**Staging-приёмка PR #230 возможна, но заблокирована отсутствием staging-инфраструктуры и утверждённых параметров seed.**

**Следующие шаги (требуют решения владельца):**
1. Предоставить staging URL / инфраструктуру (или подтвердить, что она существует вне репо).
2. Утвердить: system prompt + checksum, privacy policy version, разрешённые provider/model/region, общая квота, MAX_TOKENS, temperature.
3. Решить: использовать реальные sandbox credentials или развернуть mock provider.
4. Дать Hermes-1 доступ к reverse proxy конфигурации staging.
5. Дать явное согласие на запуск rehearsal.

После получения вышеуказанного — Hermes-1 выполнит полный цикл staging deployment, rehearsal, authenticated acceptance и выдаст evidence-отчёт.

**Никаких действий в production не выполнялось. Пароли и ключи не выводились. Сетевые настройки локальной машины не изменялись.**
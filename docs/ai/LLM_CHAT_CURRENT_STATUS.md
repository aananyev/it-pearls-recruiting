# Текущий статус плавающего LLM-чата HRM HuntTech

Дата: 2026-09-05

Это компактный источник текущего состояния. Подробные отчёты оставлены как evidence и не являются отдельным планом работ.

## Контекст и ограничения

- Репозиторий: `aananyev/it-pearls-recruiting`.
- PR: [#230](https://github.com/aananyev/it-pearls-recruiting/pull/230), ветка `feat/llm-chat`.
- Production не затрагивается; любые production-действия требуют отдельного распоряжения.
- Кандидатские/CV-данные чат не извлекает; PII-маскирование пользовательских вставок — TODO по распоряжению пользователя.
- Lookup usage по одному provider request ID не реализуется без подтверждённого API; используется `UNKNOWN_PENDING` и ручная reconciliation.

## Принятые решения

- Личный API имеет приоритет.
- Admin fallback выполняется только после ошибки личного API, при разрешённой policy и отдельном согласии пользователя.
- Профиль пользователя передаётся для персонализации без телефонов, паролей и технических секретов.
- Квота календарная месячная: общая по умолчанию, индивидуальный override имеет приоритет и хранит срок/причину.
- История бессрочная; доступ — владелец и администратор по permission.
- Миграции additive/idempotent; системный prompt и прочие seed-данные фиксируются в migration plan.

## Реализовано

- Backend chat service, conversation/message, quota ledger, requestId/idempotency, cancel и `UNKNOWN_PENDING`.
- Secure personal credentials с transitional legacy `API_KEY`.
- Floating UI, geometry settings, mobile sheet, streaming, Vaadin push и polling recovery 3 секунды.
- Permission-gated admin history и manual quota reconciliation.
- Mock routing tests, security-contract tests, migration/rollback plan и read-only staging transport smoke.

## Приёмка текущего этапа

- Текущий этап: этап 4 «Безопасность и данные», hardening-срез.
- Аналитик `Goodall`: `REWORK`; требует evidence шифрования/ротации, secret leakage, retention, consent/privacy version и runtime security.
- Тестировщик `Lorentz`: `REWORK`; staging/auth/load и legacy plaintext остаются P1, статические тесты не заменяют runtime acceptance.
- После замечаний выполнен полный локальный прогон security, foundation, schema, routing, context тестов и web-компиляции: `BUILD SUCCESSFUL`.
- Оба agent-сеанса после завершения проверки закрыты; активных субагентов нет.

## Следующий этап

Закрыть оставшийся security/data checklist этапа 4: доказать lifecycle ключей, отсутствие секретов в UI/log/error/audit, retention и consent/privacy version; затем перейти к authenticated staging/load этапа 5.

## Оставшиеся этапы roadmap

1. Этап 4 — завершить runtime security/data acceptance и remediation legacy plaintext keys.
2. Этап 5 — authenticated staging через proxy, push/recovery, provider sandbox, quota/fallback, concurrency, accessibility и regression.
3. Этап 6 — pre-production migration/rollback rehearsal, утверждённые seed-данные, backup window, acceptance и release decision.

После MVP: provider-specific usage lookup, PII-маскирование кандидатских вставок и архивирование истории — отдельные решения, не выполненные автоматически.

## Канонические документы

- `LLM_CHAT_IMPLEMENTATION_PLAN.md` — roadmap аналитика.
- `LLM_CHAT_RELEASE_READINESS_REPORT.md` — блокеры и шаги до выпуска.
- `LLM_CHAT_PRODUCTION_MIGRATION_PLAN.md` — changeSet, seed, rehearsal и rollback.
- `LLM_CHAT_PUSH_STAGE_VERIFICATION.md` — transport evidence и staging gate.

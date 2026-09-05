# Текущий статус плавающего LLM-чата HRM HuntTech

Дата: 2026-09-05

Это компактный источник текущего состояния. Подробные отчёты оставлены как evidence и не являются отдельным планом работ.

## Контекст и ограничения

- Репозиторий: `aananyev/it-pearls-recruiting`.
- PR: [#230](https://github.com/aananyev/it-pearls-recruiting/pull/230), ветка `feat/llm-chat`.
- Production не затрагивается; любые production-действия требуют отдельного распоряжения.
- Именование субагентов в коммуникации и отчетах строго по должностям; технические nicknames платформы не используются как имена.
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
- Admin-only legacy migration и server-side master-key rotation с временным previous key.
- Floating UI, geometry settings, mobile sheet, streaming, Vaadin push и polling recovery 3 секунды.
- Permission-gated admin history и manual quota reconciliation.
- Mock routing tests, security-contract tests, migration/rollback plan и read-only staging transport smoke.

## Приёмка текущего этапа

- Текущий этап: этап 4 «Безопасность и данные», hardening-срез.
- Аналитик: `REWORK`; требует evidence шифрования/ротации, secret leakage, retention, consent/privacy version и runtime security.
- Автоматизированный тестировщик: `FAIL` для полного acceptance gate; локальный кодовый срез прошёл, но staging/auth/load и runtime evidence отсутствуют.
- В текущем срезе добавлены versioned fallback consent и checkbox ExtSettingsWindow, admin-only legacy secret migration/master-key rotation, общий sanitizer ошибок/аудита и security-contract tests.
- Подготовлен `LLM_CHAT_STAGE4_RUNTIME_EVIDENCE_TEMPLATE.md` с synthetic staging fixture и матрицей security/data acceptance; реальные секреты и production не используются.
- Выполнен кодовый rework аудита: новые `AiCallLog` не сохраняют prompt/response, добавлены privacy/consent snapshots и additive/idempotent changeSet `260905-1`; исторический payload не удаляется без отдельного распоряжения.
- Независимый тестировщик по актуальному срезу не выдал verdict из-за исчерпания usage; его прежний отчет по старому commit исключён из приемки. Локальные тесты и компиляция текущего worktree прошли.
- Добавлен fail-closed гейт: `LLM_CHAT` не выполняет provider dispatch без настроенной версии privacy policy; отрицательный тест проходит.
- После замечаний выполнен полный локальный прогон security, foundation, schema, routing, context тестов и web-компиляции: `BUILD SUCCESSFUL`.
- В изолированной локальной песочнице временный mock OpenAI-compatible provider проверен без внешних API и production: sync JSON + usage, SSE streaming + usage + `[DONE]`, synthetic `503` и synthetic timeout — `PASS`; provider после прогона остановлен, временный файл удалён.
- Оба agent-сеанса после завершения проверки закрыты; активных субагентов нет.

## Локальная приёмка после запуска ветки

- Локальные targeted tests, Java-компиляция и deploy `app-core`/`app-web`/`app-web-toolkit` завершились `BUILD SUCCESSFUL`.
- В локальной PostgreSQL `hunttech` подтверждены AI-таблицы, активная административная конфигурация `DeepSeek (прод)` с моделью `deepseek-v4-flash`, privacy version `llm-chat-privacy-v1` и отсутствие записей чата до теста.
- В локальной PostgreSQL применены семь additive/idempotent changeSet чата; это локальная rehearsal-проверка, не production-перенос. Общая месячная квота осталась `NULL` и без распоряжения не заполнялась.
- Для локального пользователя без личного ключа подтверждено отсутствие `admin_fallback_consent`; реальный provider call не выполнялся, чтобы не обходить квоту и отдельное согласие.
- Собственная копия Tomcat на 8081 запускалась только локально и остановлена. Оставить её параллельно нельзя безопасно без изоляции DB-owned Telegram-настройки: при `telegram_bot_start=true` она конфликтует с уже работающим ботом (`409`). Общий экземпляр 8080 не перенастраивался намеренно.
- В UI shell 8080 ранее открывалась форма входа, но финальная проверка после reload получила `ERR_CONNECTION_REFUSED`; authenticated chat flow не подтвержден. Проверка 20–50 UI-сессий пользователем отменена.

## Следующий этап

Закрыть оставшийся security/data checklist этапа 4 runtime-evidence: доказать lifecycle ключей, отсутствие секретов в UI/log/error/audit, retention и consent/privacy version; затем перейти к authenticated staging этапа 5. Для локальной копии сначала нужна отдельная БД либо безопасный per-instance override Telegram-настройки.

## Оставшиеся этапы roadmap

1. Этап 4 — завершить runtime security/data acceptance и remediation legacy plaintext keys.
2. Этап 5 — authenticated staging через proxy, push/recovery, provider sandbox, quota/fallback, concurrency, accessibility и regression.
3. Этап 6 — pre-production migration/rollback rehearsal, утверждённые seed-данные, backup window, acceptance и release decision.

После MVP: provider-specific usage lookup, PII-маскирование кандидатских вставок и архивирование истории — отдельные решения, не выполненные автоматически.

## Канонические документы

- `LLM_CHAT_IMPLEMENTATION.md` — roadmap аналитика.
- `LLM_CHAT_RELEASE_READINESS_REPORT.md` — блокеры и шаги до выпуска.
- `LLM_CHAT_PRODUCTION_MIGRATION_PLAN.md` — changeSet, seed, rehearsal и rollback.
- `LLM_CHAT_PUSH_STAGE_VERIFICATION.md` — transport evidence и staging gate.

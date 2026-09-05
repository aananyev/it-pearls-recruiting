# Release readiness report: плавающий LLM-чат

Дата среза: 2026-09-05

Статус: NOT READY FOR PRODUCTION

Этот отчёт описывает готовность ветки и незакрытые действия. Он не является распоряжением на production-развёртывание. Production-перенос запрещён без отдельного явного согласования.

## Текущий этап roadmap

Этап 6 «Подготовка к выпуску»: release-readiness handoff. Перед началом этапа прочитаны roadmap, migration plan, push verification, integration test report и текущая реализация. В этом срезе подготовлены критерии передачи в staging, список блокеров и порядок дальнейших действий. Production не развёртывался.

## Кто работал на текущем этапе

- Основной агент: прочитал roadmap и сопутствующую документацию, сверил фактический статус PR, дополнил release-readiness отчёт, связал его с migration/rollback plan и зафиксировал следующий gate.
- Субагенты: на текущем этапе не привлекались. Аналитик, UI/UX-дизайнер и автоматизированный тестировщик не запускались, поскольку срез ограничен release-документацией и подготовкой staging handoff; тестовые и эксплуатационные действия в staging ещё невозможны без отдельного контура.

## Что уже выполнено

- Backend foundation, quota ledger, request idempotency и manual reconciliation добавлены additive-изменениями.
- Личный API имеет приоритет; admin fallback разрешается только после ошибки личного API и отдельного consent.
- Персональный контекст строится через общий builder; телефоны, пароли, API-ключи и candidate/CV entities не передаются.
- История scoped по владельцу; административная история и reconciliation permission-gated.
- Streaming использует Vaadin push с owner-scoped snapshot и polling recovery 3 секунды.
- Mock-интеграция personal/admin routing покрыта тестами.
- Локальный read-only transport smoke: push asset HTTP 200, WebSocket HTTP 101, 8/8 параллельных handshake.
- Migration plan с seed-данными, rehearsal, verification и rollback зафиксирован отдельно.

## Обязательные шаги до release

1. Развернуть approved commit из PR в отдельном staging с выключенным chat feature flag.
2. Запустить `scripts/verify-llm-chat-staging.sh <staging-app-url> 20` через фактический reverse proxy/балансировщик; зафиксировать asset 200, WebSocket 101 и результат concurrency.
3. Выполнить authenticated UI-сценарий: открыть floating chat, отправить сообщение через sandbox/mock provider, проверить push delta, завершение, историю и отсутствие повторного provider call.
4. Принудительно разорвать push-канал; проверить recovery polling 3 секунды, отсутствие дубля ответа и сохранение owner isolation.
5. Выполнить 20–50 одновременных UI-сессий, reconnect и проверку sticky-session/affinity и proxy timeouts.
6. Проверить сценарии quota: общая квота, индивидуальный override с `effectiveTo`/причиной, нулевая квота, конкурентные запросы, timeout, cancel и `UNKNOWN_PENDING`.
7. Проверить routing matrix: успешный личный API не расходует admin quota; fallback без consent запрещён; fallback с consent использует только разрешённые provider/model/region.
8. Выполнить security acceptance: чужая conversation недоступна, admin history доступна только по permission, ключи отсутствуют в UI/log/error/audit, prompt injection не отменяет privacy policy.
9. На pre-production копии прогнать migration rehearsal и rollback rehearsal по `LLM_CHAT_PRODUCTION_MIGRATION_PLAN.md`.
10. Идемпотентно загрузить и проверить seed: system prompt, prompt version/checksum, privacy policy version, лимиты, разрешённые AI-конфигурации и значения квот без секретов.
11. Зафиксировать migration log: backup identifier, changeSet, seed checksum, counts, verification queries, smoke evidence и решение rollout.
12. Получить явное решение о release и только после него назначить отдельное production-распоряжение.

## Открытые блокеры

- Нет staging URL, тестовой учётной записи и параметров reverse proxy/балансировщика.
- Нет утверждённых sandbox credentials для provider-интеграционного теста.
- Не зафиксированы финальные значения общей месячной квоты, system prompt/privacy policy version и provider/model/region для rollout.
- Provider-specific lookup usage по одному request ID не подтверждён; до отдельного решения используется ручная reconciliation, автоматический повторный provider call запрещён.
- PII-маскирование данных кандидатов оставлено TODO по распоряжению пользователя; текущий чат не извлекает candidate/CV entities.

## Что делать на следующем этапе

Следующий этап — закрыть authenticated staging/load gate этапа 5:

1. Получить staging URL, тестовую учётную запись, sandbox credentials и параметры reverse proxy/балансировщика.
2. Развернуть approved commit только в staging с выключенным feature flag.
3. Запустить transport smoke для asset/WebSocket, затем проверить authenticated push, recovery polling, quota, fallback, cancel, retry и owner isolation.
4. Провести нагрузочную проверку 20–50 UI-сессий, reconnect, sticky-session и proxy timeouts.
5. Повторно прогнать migration/rollback rehearsal и обновить этот отчёт evidence и решением о готовности.

## Краткий список оставшихся этапов roadmap

- Этап 5 «Интеграция и тестирование» — частично выполнен; остаются authenticated staging, нагрузка, security acceptance и регрессия shell/layout.
- Этап 6 «Подготовка к выпуску» — документация подготовлена, но остаются rehearsal, утверждение seed-значений, release decision и затем отдельное production-распоряжение.
- После MVP, не блокируя текущий PR: provider-specific usage lookup по request ID — только после подтверждения API; маскирование PII кандидатских данных — TODO по распоряжению пользователя; архивирование истории — отдельное решение при бессрочном хранении.

## Критерий закрытия отчёта

Статус можно изменить на READY FOR RELEASE только после выполнения всех обязательных шагов, устранения блокеров staging и миграционной rehearsal, а также явного решения о выпуске. До этого ветка может обновлять PR, но не production.

# Плавающий LLM-чат HRM HuntTech

## Назначение

Плавающий чат предоставляет пользователю интерфейс к существующему Java AI-сервису. Маршрутизация выполняется на middleware: сначала используется персональная конфигурация пользователя, затем административная — только при наличии отдельного согласия.

Кандидатские и CV-данные в MVP не загружаются и не передаются провайдеру. Профиль пользователя передаётся по существующему `UserAiContextService` без телефонов, паролей и AI-секретов.

## Реализованный контракт

- `LlmChatService.startConversation()` создаёт бессрочный диалог владельца.
- `sendMessage(conversationId, message, requestId)` проверяет владельца, резервирует месячную квоту и сохраняет USER/ASSISTANT сообщения.
- Повторный `requestId` не создаёт второй provider call или повторное списание.
- `cancelMessage()` реализует кооперативную отмену: ответ не сохраняется, usage учитывается при известном результате.
- `reconcileUnknown(requestId, actualTokens, providerCharged)` — административная операция закрытия `UNKNOWN_PENDING` без повторного вызова провайдера; требует отдельного specific permission `hunttech.ai.reconcileChatQuota`.
- `loadHistory()` доступен владельцу; `loadHistoryAsAdmin()` защищён specific permission `hunttech.ai.viewChatHistoryAdmin`.
- `LlmChatQuotaReconciliationBrowse` — permission-gated административный экран для ручной сверки; до проверки права не загружает ни одной строки.
- При обрыве streaming после provider usage callback резерв закрывается автоматически с `SYSTEM_PROVIDER_USAGE`; если известен только provider request ID, резерв остаётся `UNKNOWN_PENDING`.
- `startStreaming()` запускает запрос через middleware scheduler, а `pollStreaming()` возвращает owner-scoped накопленный snapshot; floating screen получает push-событие и использует polling с интервалом 3 секунды как recovery.
- OpenAI-compatible provider adapters поддерживают provider request ID, SSE streaming и прерывание активного HTTP-вызова по HRM `requestId`; legacy adapters автоматически отдают полный ответ одной дельтой.
- Streaming-задача переносит CUBA security context, не сохраняет partial assistant message и пишет итог в историю только после подтверждённого завершения.
- Floating UI сохраняет позицию и размер через штатные пользовательские screen settings CUBA; при ширине до 640 px диалог становится полноэкранным mobile sheet. Новых сущностей и changeSet для UI-настроек нет.
- Для live-ответа включён Vaadin push: core публикует UI-событие только с идентификаторами пользователя, диалога и запроса, а web-клиент получает актуальный owner-scoped snapshot. Polling с интервалом 3 секунды оставлен для восстановления при временной недоступности push.
- Lookup usage по одному `providerRequestId` отложен до подтверждения provider-specific API; решение и обязательные условия зафиксированы в `LLM_CHAT_PROVIDER_USAGE_LOOKUP_DECISION.md`.
- Интеграционная маршрутизация личного API и согласованного admin fallback покрыта mock-провайдерами; сценарии и ограничения зафиксированы в `LLM_CHAT_INTEGRATION_TEST_REPORT.md`.
- Read-only transport smoke для staging вынесен в `scripts/verify-llm-chat-staging.sh`; authenticated сценарии и нагрузка выполняются только в выделенном staging.
- План migration/rehearsal, seed-данных и rollback зафиксирован в `LLM_CHAT_PRODUCTION_MIGRATION_PLAN.md`; production-перенос без отдельного распоряжения запрещён.
- Текущая release readiness и все оставшиеся шаги зафиксированы в `LLM_CHAT_RELEASE_READINESS_REPORT.md`; статус остаётся `NOT READY FOR PRODUCTION`.
- Перед каждым новым этапом roadmap необходимо читать актуальный план и сопутствующие отчёты; текущий handoff и следующий staging gate описаны в release-readiness report.
- Security-contract проверки текущего среза находятся в `LlmChatSecurityContractTest`; runtime security acceptance выполняется только в staging.
- Компактный канонический статус, verdict аналитика/QA и актуальный порядок этапов находятся в `LLM_CHAT_CURRENT_STATUS.md`.

## Квота

Период — календарный месяц. Общая квота задаётся в `AiFunctionConfiguration.defaultMonthlyTokenQuota`; активный `LlmUserQuotaOverride` пользователя имеет приоритет и хранит дату окончания и причину. Для неизвестного исхода используется `UNKNOWN_PENDING`, который временно уменьшает доступный остаток.

## Секреты

Новые персональные ключи сохраняются в `API_KEY_ENCRYPTED` через middleware-шифрование. Legacy `API_KEY` оставлен переходным для совместимости и не копируется SQL-миграцией; приложение переводит его в ciphertext при сохранении или первом успешном использовании.

## Миграции

1. `260904-1-addAdminFallbackConsent` — отдельное согласие fallback, default `false`.
2. `260904-2-addLlmChatFoundation` — диалоги, сообщения, функция `LLM_CHAT` и системный prompt.
3. `260904-3-addLlmChatQuotaTables` — периоды, overrides и reservation ledger.
4. `260904-4-addUserAiEncryptedKey` — ciphertext-колонка персонального ключа.
5. `260904-5-addLlmChatRequestId` — request id и индекс сообщений.
6. `260904-6-addLlmChatReconciliationAudit` — nullable provider request ID для сообщения и резерва, а также кто и когда выполнил административную сверку.
7. `260905-1-addAiAuditSecuritySnapshots` — privacy/consent snapshot-поля, privacy policy seed и legacy audit marker без удаления исторического payload.

Liquibase и CUBA `updateDb` SQL находятся в `modules/core/db/`. Все шаги additive/idempotent; production-порядок и обязательные seed-данные описываются в отдельном migration plan.

## Ограничения

OpenAI-compatible provider adapters уже возвращают provider request ID, умеют SSE streaming и прерывание активного HTTP-вызова; нативные протоколы Anthropic/Gemini/YandexGPT/GigaChat используют безопасный sync fallback. В UI push включён как основной transport live-обновлений, а polling с интервалом 3 секунды оставлен recovery-механизмом без изменения схемы данных. In-memory snapshot удаляется через 10 минут после завершения; после перезапуска core незавершённый запрос восстанавливается как reservation/status и не запускается повторно. Автоматическая сверка выполняется только по фактическому usage callback, который прислал провайдер; один provider request ID без подтверждённого usage не даёт оснований для списания или освобождения и остаётся `UNKNOWN_PENDING` для ручной сверки.

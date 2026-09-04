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
- `startStreaming()` запускает запрос через middleware scheduler, а `pollStreaming()` возвращает owner-scoped накопленный snapshot; floating screen обновляет ответ timer-интервалом 500 мс.
- OpenAI-compatible provider adapters поддерживают provider request ID, SSE streaming и прерывание активного HTTP-вызова по HRM `requestId`; legacy adapters автоматически отдают полный ответ одной дельтой.
- Streaming-задача переносит CUBA security context, не сохраняет partial assistant message и пишет итог в историю только после подтверждённого завершения.

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

Liquibase и CUBA `updateDb` SQL находятся в `modules/core/db/`. Все шаги additive/idempotent; production-порядок и обязательные seed-данные описываются в отдельном migration plan.

## Ограничения

OpenAI-compatible provider adapters уже возвращают provider request ID, умеют SSE streaming и прерывание активного HTTP-вызова; нативные протоколы Anthropic/Gemini/YandexGPT/GigaChat используют безопасный sync fallback. В UI подключён polling facade, а не отдельный WebSocket/Vaadin push: это совместимый промежуточный transport без изменения схемы данных. In-memory snapshot удаляется через 10 минут после завершения; после перезапуска core незавершённый запрос восстанавливается как reservation/status и не запускается повторно. Автоматическая сверка по API провайдера ещё не реализована. До неё администратор сверяет `UNKNOWN_PENDING` через permission-gated экран: подтверждённое списание переводит резерв в `SETTLED`, отсутствие списания — в `RELEASED`.

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

Текущие provider adapters синхронные и пока не возвращают provider request ID. Жёсткая отмена HTTP-вызова и автоматическая сверка по API провайдера требуют отдельного адаптера. До его появления администратор сверяет `UNKNOWN_PENDING` через permission-gated экран: подтверждённое списание переводит резерв в `SETTLED`, отсутствие списания — в `RELEASED`.

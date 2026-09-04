# LlmChatService (`hunttech_LlmChatService`)

Middleware-фасад плавающего LLM-чата HRM HuntTech. Сервис владеет границами диалога, квотой, идемпотентностью и безопасным transport-контрактом web ↔ core; выбор провайдера и ключа остаётся в `AiExecutionService`.

## Бизнес-контекст (What & Why)

Пользователь общается с настроенной им LLM. Если личное подключение недоступно, административное подключение может быть использовано только после отдельного `adminFallbackConsent`. Профиль пользователя добавляется централизованным `UserAiContextService`; номера телефонов, пароли, AI-секреты и кандидатские/CV-данные в чатовый контекст не извлекаются.

## UI Context & Navigation

- `ExtMainScreen` открывает modeless `LlmChatScreen` через постоянный launcher.
- `LlmChatScreen` создаёт один диалог пользователя, показывает бессрочную историю и получает live-ответ через Vaadin push; polling timer 3 секунды остаётся резервом при временной недоступности push.
- Геометрия плавающего окна (позиция и размер) сохраняется в штатных пользовательских screen settings CUBA; новая таблица для этого не нужна.
- На экране до 640 px диалог превращается в полноэкранный mobile sheet, а сохранённые desktop-координаты не применяются.
- Удаление истории пользователю не предоставляется.

## Архитектура и размещение

- API: `modules/global/src/com/company/hunttech/service/LlmChatService.java`.
- DTO потокового состояния: `modules/global/src/com/company/hunttech/service/LlmChatStreamState.java`.
- Реализация: `modules/core/src/com/company/hunttech/service/LlmChatServiceBean.java`.
- Web proxy: `modules/web/src/com/company/hunttech/web-spring.xml`.
- Экран: `modules/web/src/com/company/hunttech/web/screens/llmchat/LlmChatScreen.java`.

## API и алгоритм

1. `startConversation()` создаёт `LlmChatConversation`, связанную с текущим пользователем.
2. `startStreaming(conversationId, message, requestId)` проверяет владельца, валидирует сообщение, резервирует месячную квоту и сохраняет USER-сообщение.
3. Запрос планируется штатным daemon `scheduler` с переносом CUBA `SecurityContext`; provider streaming дельты накапливаются в owner-scoped snapshot.
4. `pollStreaming()` повторно проверяет пользователя и conversationId и возвращает cumulative text. Push-событие передаёт только идентификаторы, а web-клиент сам запрашивает snapshot. Snapshot удаляется через 10 минут после завершения.
5. После подтверждённого результата quota reservation закрывается, ASSISTANT-сообщение сохраняется один раз. Если до обрыва пришёл фактический usage callback, резерв автоматически закрывается с audit actor `SYSTEM_PROVIDER_USAGE`; если пришёл только provider request ID, резерв переводится в `UNKNOWN_PENDING`, второй provider call не запускается.
6. `cancelMessage()` ставит `CANCEL_REQUESTED` и передаёт requestId в `AIProviderRegistry`. OpenAI-compatible адаптеры прерывают активное HTTP-соединение; sync-only адаптеры завершаются кооперативно.

Синхронные `sendMessage(...)` сохранены для совместимости и идемпотентных интеграций. Для provider без streaming execution layer отдаёт полный ответ одной дельтой.

## Security и доступ к истории

- `loadHistory()` — только владелец диалога.
- `loadHistoryAsAdmin()` — только при `hunttech.ai.viewChatHistoryAdmin`.
- Административная сверка неизвестного usage — только при `hunttech.ai.reconcileChatQuota`.
- В transport snapshot не передаются ключи, prompt payload провайдера или чужие диалоги.

## Тестирование

- `LlmChatFoundationContractTest` проверяет API, owner-scoped polling, timer, streaming routing, quota и privacy-контракты.
- `ScreenViewIntegrityTest` обязателен после изменения XML экрана.
- Проверка сборки: `:app-core:test`, `:app-core:compileJava`, `:app-web:compileJava`.

## Деплой и миграция

Этот срез streaming facade не требует новых таблиц или changeSet. Production-порядок, seed-промпты, квота, permissions и rollback описаны в `docs/ai/LLM_CHAT_IMPLEMENTATION.md` и отдельном production migration plan проекта. Перед rollout требуется smoke старых AI-функций и тестовая группа чата.

## Ограничения и следующие шаги

Vaadin push включён как основной transport live-обновлений; polling 3 секунды сохранён как recovery-механизм. Событие push не содержит partial AI text и не отменяет owner-scoped проверку в `pollStreaming()`. Lookup usage по одному provider request ID остаётся TODO до появления подтверждённых provider-specific API. Геометрия desktop-диалога сохраняется через CUBA settings, mobile sheet реализован CSS-режимом без изменения схемы.

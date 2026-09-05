# План миграции LLM-чата HRM HuntTech

Статус: подготовлен для review. Этот документ не является распоряжением на production-развёртывание. Любой production-перенос выполняется только отдельным явным согласованием.

## Ограничения

- Только additive-изменения; существующие AI-функции и экраны не останавливаются из-за новых таблиц и nullable-полей.
- До завершения staging-приёмки чат выключен feature flag’ом.
- Миграции и seed-операции идемпотентны.
- Rollback отключает чат и не удаляет историю, аудит или подтверждённый usage.
- Секреты не передаются в migration output, отчёты и логи.

## ChangeSet и порядок

1. `260904-1-addAdminFallbackConsent`: добавить `ADMIN_FALLBACK_CONSENT`, версию и дату согласия в `HUNTTECH_USER_AI_PROFILE`; существующим пользователям установить `FALSE`.
2. `260904-2-addLlmChatFoundation`: добавить conversation/message и seed функции `LLM_CHAT`.
3. `260904-3-addLlmChatQuotaTables`: добавить календарные периоды квоты, индивидуальные overrides с датой окончания и причиной, reservation ledger.
4. `260904-4-addUserAiEncryptedKey`: добавить nullable `API_KEY_ENCRYPTED`; plaintext SQL-backfill не выполнять.
5. `260904-5-addLlmChatRequestId`: добавить nullable `REQUEST_ID` и индекс сообщений.
6. `260904-6-addLlmChatReconciliationAudit`: добавить `PROVIDER_REQUEST_ID` и поля ручной сверки.
7. `260905-1-addAiAuditSecuritySnapshots`: добавить nullable `PRIVACY_POLICY_VERSION` в AI-функцию и три nullable snapshot-поля в `HUNTTECH_AI_CALL_LOG`.

После применения changeSet 260904-4 выполнить только в core/runtime с настроенным `hunttech.ai.encryptionKey` контролируемую операцию `AiCredentialService.migrateLegacyUserSecrets()` под правом `hunttech.ai.manageCorporateCredentials`. SQL не должен читать или шифровать plaintext API-ключи. Операция повторяема: уже очищенные `API_KEY` не выбираются, при ошибке незавершённые записи остаются для повторного запуска.

Ротация master-key выполняется отдельным окном: новый ключ задаётся в `hunttech.ai.encryptionKey`, прежний временно — в `hunttech.ai.previousEncryptionKey`; затем admin-only `AiCredentialService.rotateSecrets()` пере-шифровывает все ciphertext, после verification предыдущий ключ удаляется и выполняется повторная проверка доступа к AI. Значения ключей не попадают в migration log.

`260905-1-addAiAuditSecuritySnapshots` seed-ит для `LLM_CHAT` стабильную внутреннюю версию privacy policy `llm-chat-privacy-v1` только при пустом значении и заполняет новые audit snapshot-поля маркером `LEGACY_NOT_CAPTURED` для старых записей. Исторические `PROMPT_TEXT`/`RESPONSE_TEXT` не удаляются автоматически: их массовая очистка является необратимым изменением и требует отдельного явного распоряжения, backup и согласованного плана хранения.

Позиция общей квоты — `AiFunctionConfiguration.defaultMonthlyTokenQuota` для `LLM_CHAT`. Квота считается за календарный месяц; активный индивидуальный override пользователя имеет приоритет.

## Обязательные данные для загрузки

До запуска seed должны быть отдельно утверждены и записаны в migration log:

- точная версия системного prompt `LLM_CHAT` и его контрольная сумма;
- prompt template, `MAX_TOKENS`, temperature, execution/fallback policy;
- версия privacy policy и запрет передачи кандидатских/CV-данных;
- разрешённые provider/model/region из AI Control Plane;
- общая месячная квота и индивидуальные overrides, включая `effectiveTo`, причину и автора;
- административные AI-конфигурации без вывода ключей;
- feature flag в выключенном состоянии;
- права `hunttech.ai.viewChatHistoryAdmin` и `hunttech.ai.reconcileChatQuota` для согласованных административных ролей.

Если значение не утверждено, seed не должен подставлять догадки или затирать существующие настройки. Повторный seed не создаёт дубли и не перезаписывает ручные настройки пустыми значениями.

## Preflight и rehearsal

На копии production или выделенном pre-production выполнить:

1. Проверить текущий master changelog, наличие AI-таблиц и фактические counts.
2. Проверить backup и возможность восстановления.
3. Сохранить экспорт AI-конфигураций без секретов.
4. Выполнить dry-run инвентаризации legacy `API_KEY` без вывода значений; записать только count и идентификаторы конфигураций.
5. Прогнать все changeSet и seed повторно; counts и контрольные суммы не должны измениться.
6. Запустить admin-only legacy migration в staging, сохранить count до/после и checksum ciphertext без раскрытия ключей; повторный запуск должен дать `0` новых миграций.
7. Провести rotation rehearsal: новый/предыдущий server-side key, `rotateSecrets()`, проверка provider call, повторный запуск без изменений и удаление previous key после сверки.
8. Проверить rollback rehearsal: выключение feature flag, восстановление конфигурации и сохранение ledger.
9. Выполнить `scripts/verify-llm-chat-staging.sh` через реальный staging proxy.
10. Выполнить authenticated UI, provider sandbox, quota, fallback, cancel, retry и privacy smoke.

## Порядок rollout

1. Развернуть код с выключенным chat feature flag.
2. Применить changeSet в указанном порядке.
3. Идемпотентно загрузить функцию, системный prompt, privacy policy, разрешённые настройки и квоты.
4. Проверить permissions и отсутствие plaintext API keys в новых данных.
5. Выполнить verification queries и регрессию существующих AI-функций.
6. Включить чат только для тестовой группы.
7. После acceptance расширять охват поэтапно.

## Проверки после миграции

- Все changeSet отражены в master changelog.
- `adminFallbackConsent=false` у существующих профилей.
- Есть ровно одна квотная запись пользователя на календарный месяц.
- System prompt непустой, версия и checksum совпадают с migration log.
- Нет фиктивных quota periods, usage и provider request IDs.
- После legacy migration нет активных непустых `API_KEY`; ciphertext имеет формат `v1:<iv>:<ciphertext>`.
- У `LLM_CHAT` есть privacy policy version `llm-chat-privacy-v1`; новые audit-записи получают фактические snapshot-версии, старые явно помечены `LEGACY_NOT_CAPTURED`.
- Новые `AiCallLog` не содержат `PROMPT_TEXT` и `RESPONSE_TEXT`; существующий исторический payload не изменяется этим changeSet.
- До provider dispatch приложение проверяет наличие версии privacy policy; при пустой версии операция завершается fail-closed без внешнего вызова.
- После rotation rehearsal все активные credentials расшифровываются только текущим key; предыдущий key удалён из конфигурации после verification.
- Старые ключи не появляются в UI, исключениях, `AiCallLog` и server log; secret-like значения в диагностических ошибках заменены `[REDACTED]`.
- Повторный `requestId` не создаёт второй provider call или списание.
- `UNKNOWN_PENDING` закрывается только ручной сверкой без повторного вызова провайдера.
- История доступна только владельцу и разрешённому администратору.
- Старые `executeText`/`executeImage`, ExtSettingsWindow и AI Control Plane работают без изменения маршрутизации.

## Rollback

- При DDL/seed/runtime-ошибке выключить feature flag и остановить rollout.
- Не удалять новые таблицы, историю, аудит и подтверждённый usage автоматически.
- Восстановить предыдущие значения конфигурации из backup/versioned seed.
- Pending reservation оставить для ручной сверки; повторный provider call запрещён.
- При частичной legacy migration повторно запустить batch после устранения причины; не выполнять SQL-очистку plaintext без подтверждённого ciphertext.
- Фактическое списание подтвердить через `reconcileUnknown()` с audit trail.
- Удаление схемы допускается только отдельным утверждённым планом.

## Итоговый migration log

После согласованного переноса зафиксировать окно, backup identifier, commit, changeSet, seed prompts и checksum, значения квот без секретов, counts, verification queries, smoke/regression evidence, охват rollout, отклонения, решение rollback и ответственных. До такого распоряжения этот раздел не заполняется production-данными.

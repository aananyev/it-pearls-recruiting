# План миграции LLM-чата HRM HuntTech

Статус: подготовлен для review. Этот документ не является распоряжением на production-развёртывание. Любой production-перенос выполняется только отдельным явным согласованием.

## Ограничения

- Только additive-изменения; существующие AI-функции и экраны не останавливаются из-за новых таблиц и nullable-полей.
- До завершения staging-приёмки чат выключен feature flag’ом.
- Миграции и seed-операции идемпотентны.
- Rollback отключает чат и не удаляет историю, аудит или подтверждённый usage.
- Секреты не передаются в migration output, отчёты и логи.

## Production baseline и границы окна

- Этот документ — план и чек-лист, а не разрешение на production. Фактическое окно начинается только после отдельного распоряжения владельца.
- Источник кода — approved commit PR #230; перед окном зафиксировать SHA, ветку и версию артефакта. Локальные настройки, локальная БД и локальные тестовые credentials в production не переносятся.
- Рекомендуемая стартовая общая квота — `10 000` токенов на пользователя за календарный месяц. Финальное значение должно быть подтверждено в migration log перед seed; активный индивидуальный override имеет приоритет и обязан содержать `effectiveTo` и причину.
- Privacy policy для `LLM_CHAT` — `llm-chat-privacy-v1`. Для существующих пользователей `ADMIN_FALLBACK_CONSENT` остаётся `FALSE`; отдельное согласие не проставляется массово и не заменяется `EXTERNAL_PROCESSING_ALLOWED`.
- Provider/model/region берутся из уже разрешённых настроек AI Control Plane production. Секретный ключ не копируется из локальной среды; корпоративный ключ вводится или ротируется только через защищённый runtime/UI-процесс.
- В текущем PR отдельный feature flag для floating launcher не реализован: launcher добавлен в main screen без DB-флага. До production rollout нужно либо добавить и принять feature flag, либо выполнить согласованный rollout через права/ограниченный охват. Нельзя считать миграцию завершённой при простом включении схемы.

## Пошаговое production-окно

### 0. Предусловия и stop/go

1. Получить письменное распоряжение на production-окно, ответственных и канал аварийной остановки.
2. Утвердить commit, версию, provider/model/region, system prompt checksum, prompt template, temperature, `MAX_TOKENS`, квоту и права.
3. Иметь проверенный backup с идентификатором и подтверждённым restore point; запретить необратимые действия до проверки backup.
4. Подтвердить окно, в котором допустим короткий restart core/web, и наличие rollback-артефакта предыдущей версии.
5. Остановить rollout при любом расхождении схемы, seed-checksum, количества конфигураций или прав.

### 1. Preflight без изменений

Зафиксировать только безопасные метаданные:

- database/schema, master changelog и текущую версию приложения;
- наличие активной `LLM_CHAT` и её `privacyPolicyVersion` без вывода prompt и ключей;
- counts таблиц `HUNTTECH_AI_FUNCTION_CONFIGURATION`, `HUNTTECH_ADMIN_AI_CONFIGURATION`, `HUNTTECH_USER_AI_CONFIGURATION`, `HUNTTECH_USER_AI_FUNCTION_OVERRIDE`, `HUNTTECH_USER_AI_PROFILE`;
- count legacy `API_KEY` и count `API_KEY_ENCRYPTED`, без значений и без экспорта ciphertext;
- наличие текущих admin AI-конфигураций и разрешённых provider/model/region;
- наличие ролей/permissions `hunttech.ai.viewChatHistoryAdmin`, `hunttech.ai.reconcileChatQuota` и `hunttech.ai.manageCorporateCredentials`.

Если preflight показывает plaintext key, сначала выполнить отдельный controlled legacy migration через core/runtime; SQL-backfill ключей запрещён.

### 2. Backup и deploy с выключенным rollout

1. Создать backup базы и проверить возможность восстановления на pre-production копии.
2. Развернуть approved артефакты с floating launcher/чатом недоступными для production-пользователей согласно принятому rollout-механизму.
3. Не запускать provider call и не создавать тестовые conversation до завершения DDL и seed-проверок.
4. Проверить, что старые AI-функции, ExtSettingsWindow и AI Control Plane открываются штатно.

### 3. Применение schema changeSet

1. Применить штатный CUBA/Liquibase master changelog через core deployment; не выполнять SQL-файлы вручную в обход changelog.
2. Сохранить в migration log порядок и результат семи changeSet: `260904-1` … `260904-6`, затем `260905-1`.
3. Повторно запустить `updateDb` в pre-production rehearsal: ожидается отсутствие новых изменений, дублей и конфликтов.
4. При DDL-ошибке остановить rollout, не удалять частично созданные chat-таблицы вручную, восстановить предыдущую версию приложения и перейти к rollback-разделу.

### 4. Загрузка seed и reference data

Загрузить или проверить следующие данные, строго не перезаписывая ручные production-настройки пустыми значениями:

- `HUNTTECH_AI_FUNCTION_CONFIGURATION`: ровно одна активная функция `LLM_CHAT`; capability текстовой генерации; утверждённые system prompt и prompt template; `configurationVersion`; `includeUserContext=true`; `executionPolicy=USER_OVERRIDE_ALLOWED`; `fallbackPolicy=FALLBACK_TO_ADMIN`; утверждённые `temperature`, `maxTokens`, `defaultMonthlyTokenQuota=10 000` либо финальное значение из migration log; `privacyPolicyVersion=llm-chat-privacy-v1`.
- `HUNTTECH_ADMIN_AI_CONFIGURATION`: разрешённое активное корпоративное подключение, provider/model/base URL из AI Control Plane и secret только в `API_KEY_ENCRYPTED`. Если запись уже есть, сверить metadata и не дублировать её.
- `HUNTTECH_USER_AI_PROFILE`: существующим пользователям сохранить профиль и `EXTERNAL_PROCESSING_ALLOWED`; новые поля `ADMIN_FALLBACK_CONSENT`, версия и дата для существующих пользователей остаются `FALSE`/`NULL` до отдельного явного согласия.
- `HUNTTECH_USER_AI_CONFIGURATION` и `HUNTTECH_USER_AI_FUNCTION_OVERRIDE`: не создавать production personal credentials из локальных тестовых данных. Пользовательский ключ появляется только после самостоятельного защищённого ввода владельцем; override создаётся только для соответствующей пользовательской конфигурации.
- `HUNTTECH_LLM_CHAT_QUOTA_PERIOD`, `HUNTTECH_LLM_CHAT_QUOTA_RESERVATION`: не загружать фиктивные периоды, usage, request ID или reservations. Период создаётся сервисом при первом реальном запросе и относится к календарному месяцу.
- permissions: выдать только согласованным ролям admin history/reconciliation/credential-management; обычным пользователям эти specific permissions не назначать.

Контрольная сумма prompt считается от точного UTF-8 содержимого до загрузки; в migration log хранится checksum и версия, но не секреты и не персональные данные.

### 5. Безопасное lifecycle credentials

1. Проверить наличие server-side `hunttech.ai.encryptionKey` в секретном хранилище deployment без вывода значения.
2. Выполнить admin-only `AiCredentialService.migrateLegacyUserSecrets()` для оставшихся legacy user keys; записать только counts до/после.
3. При ротации задать новый current key и временный previous key через секретное хранилище, выполнить `AiCredentialService.rotateSecrets()`, проверить расшифровку и provider smoke, затем удалить previous key.
4. Повторный запуск migration/rotation должен дать нулевые изменения при неизменном наборе ciphertext.
5. Запретить копирование локального административного API-ключа, его ciphertext и тестовых personal configs в production.

### 6. Verification и ограниченный rollout

До включения для пользователей проверить:

1. `LLM_CHAT` active, prompt/template/version/checksum/privacy version совпадают с migration log.
2. Общая квота имеет утверждённое ненулевое значение; override с `effectiveTo`/причиной работает; duplicate period и duplicate request ID не создаются.
3. У существующих профилей fallback-consent не включился автоматически.
4. Ключи отсутствуют в UI, исключениях, audit и server log; новые `AiCallLog` не содержат prompt/response.
5. История доступна только владельцу и разрешённому администратору; удаление пользователем запрещено.
6. Личный API имеет приоритет; успешный personal call не уменьшает admin quota; fallback без отдельного consent запрещён.
7. Через разрешённый proxy выполнены asset/WebSocket smoke, authenticated sync/streaming/recovery, cancel/error и короткий regression связанных AI-экранов.
8. Включить чат сначала для согласованной тестовой группы; после smoke расширять охват поэтапно.

Нагрузочную проверку 20–50 UI-сессий в рамках этого проекта не выполнять: она отменена пользователем. Это не отменяет минимальную authenticated проверку и proxy/reconnect smoke.

## Verification queries для migration log

Запросы выполняются read-only и выводят только metadata/counts:

```sql
select code, is_active, configuration_version, privacy_policy_version,
       default_monthly_token_quota
from hunttech_ai_function_configuration
where code = 'LLM_CHAT';

select count(*) as active_admin_configs
from hunttech_admin_ai_configuration
where delete_ts is null and is_active = true;

select count(*) as legacy_user_keys
from hunttech_user_ai_configuration
where delete_ts is null and api_key is not null and trim(api_key) <> '';

select count(*) as encrypted_user_keys
from hunttech_user_ai_configuration
where delete_ts is null and api_key_encrypted is not null
  and trim(api_key_encrypted) <> '';

select count(*) as enabled_fallback_consents
from hunttech_user_ai_profile
where delete_ts is null and admin_fallback_consent = true;

select count(*) as chat_conversations
from hunttech_llm_chat_conversation
where delete_ts is null;
```

Значения prompt, API key, ciphertext, персональные профили, сообщения и response payload в migration log не копируются.

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

После согласованного переноса зафиксировать окно, backup identifier и результат restore check, commit/version, changeSet, seed prompts и checksum, значения квот без секретов, counts до/после, verification queries, credential lifecycle counts, smoke/regression evidence, охват rollout, отклонения, решение rollback и ответственных. До отдельного production-распоряжения этот раздел не заполняется production-данными.

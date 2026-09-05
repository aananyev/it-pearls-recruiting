# Release readiness report: плавающий LLM-чат

Дата среза: 2026-09-05

Статус: NOT READY FOR PRODUCTION

Этот отчёт описывает готовность ветки и незакрытые действия. Он не является распоряжением на production-развёртывание. Production-перенос запрещён без отдельного явного согласования.

## Текущий этап roadmap

Этап 4 «Безопасность и данные»: hardening-срез остаётся на приёмке `REWORK`. Перед началом этапа прочитаны roadmap, migration plan, push verification, integration test report, release handoff и текущая реализация. Локальные security-contract проверки добавлены, но полный security/data checklist не закрыт. Production не развёртывался.

## Кто работал на текущем этапе

- Основной агент: прочитал roadmap и сопутствующую документацию, устранил неполноту независимого test-run evidence, выполнил полный локальный прогон и обновил отчёт.
- Аналитик: проверил соответствие этапу 4 и полноту security/release gates; verdict `REWORK`.
- Автоматизированный тестировщик: проверил PR, тесты, миграции и риски staging; verdict `FAIL` для полного acceptance gate. Сеанс после отчёта закрыт.

## Что уже выполнено

- Backend foundation, quota ledger, request idempotency и manual reconciliation добавлены additive-изменениями.
- Личный API имеет приоритет; admin fallback разрешается только после ошибки личного API и отдельного consent.
- Персональный контекст строится через общий builder; телефоны, пароли, API-ключи и candidate/CV entities не передаются.
- История scoped по владельцу; административная история и reconciliation permission-gated.
- Streaming использует Vaadin push с owner-scoped snapshot и polling recovery 3 секунды.
- Mock-интеграция personal/admin routing покрыта тестами.
- Локальный read-only transport smoke: push asset HTTP 200, WebSocket HTTP 101, 8/8 параллельных handshake.
- Migration plan с seed-данными, rehearsal, verification и rollback зафиксирован отдельно.
- Versioned consent fallback: сервер проверяет флаг, версию и дату явного согласия; в ExtSettingsWindow добавлен отдельный checkbox, независимый от `externalProcessingAllowed`.
- `AiCredentialService.migrateLegacyUserSecrets()` выполняет admin-only перенос legacy `API_KEY` в AES-GCM ciphertext и очищает plaintext; SQL-backfill не используется.
- `AiCredentialService.rotateSecrets()` добавлен для controlled master-key rotation через текущий и временный предыдущий server-side key.
- `AiSecuritySanitizer` применяется к ошибкам, AI-аудиту и сообщениям server log; локальный contract test проверяет редактирование Authorization/secret-like значений.
- Для следующего runtime gate подготовлен `LLM_CHAT_STAGE4_RUNTIME_EVIDENCE_TEMPLATE.md`: синтетические пользователи/fixtures, migration и rotation rehearsal, secret leakage, consent/privacy, owner isolation, retention, quota и fallback checks.

## Результат независимой приёмки

- Аналитик потребовал не закрывать этап 4 до evidence по encryption/rotation, secret leakage, retention, privacy/consent и runtime security.
- Автоматизированный тестировщик подтвердил локальный PASS среза, но указал блокеры полного gate: authenticated staging/load, sandbox credentials, runtime secret leakage/retention checks и legacy migration evidence.
- Полный локальный прогон после QA замечания завершён успешно:

```text
GRADLE_USER_HOME=/private/tmp/hrm-pr230-gradle ./gradlew :app-core:test \\
  --tests com.company.hunttech.core.LlmChatSecurityContractTest \\
  --tests com.company.hunttech.core.LlmChatFoundationContractTest \\
  --tests com.company.hunttech.core.DatabaseSchemaReconciliationChangelogTest \\
  --tests com.company.hunttech.service.AiExecutionServiceBeanTest \\
  --tests com.company.hunttech.service.UserAiContextServiceBeanTest \\
  :app-core:compileJava :app-web:compileJava --no-daemon --console=plain
```

Результат: `BUILD SUCCESSFUL`. Локальный PASS не закрывает staging и runtime security gates.

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
- Evidence-шаблон подготовлен, но не может быть заполнен до выдачи staging URL, тестовых учётных записей и sandbox credentials.
- Выполнен additive rework аудита: новые `AiCallLog` не сохраняют prompt/response, добавлены snapshot-поля consent/privacy и changeSet `260905-1`; существующий исторический payload не очищается автоматически из-за необратимости.
- Массовая очистка исторических `PROMPT_TEXT`/`RESPONSE_TEXT` не выполнялась и требует отдельного распоряжения, поскольку противоречит безопасному бессрочному хранению без согласованной процедуры.
- Повторный запуск независимого тестировщика по текущему worktree завершился ошибкой лимита usage до отчёта; предыдущий отчет по старому commit не используется как приемка. Локальная приемка текущего worktree выполнена основным агентом.
- Добавлен и локально проверен fail-closed privacy gate: `LLM_CHAT` не обращается к провайдеру без версии privacy policy.

## Результат текущего security-среза

Локальные security-contract тесты проверяют запрет телефонов, паролей и API-ключей в builder, отсутствие загрузки candidate/CV entities, owner boundary для истории и streaming, отсутствие текста в push-событии и раздельные административные permission gates. Динамическая проверка через staging остаётся обязательной.

Проверка выполнена командой `./gradlew :app-core:test --tests com.company.hunttech.core.LlmChatSecurityContractTest --tests com.company.hunttech.core.LlmChatFoundationContractTest --tests com.company.hunttech.service.AiExecutionServiceBeanTest :app-core:compileJava :app-web:compileJava --no-daemon --console=plain`; результат: `BUILD SUCCESSFUL`.

## Что делать на следующем этапе

Сначала закрыть замечания аналитика по этапу 4, затем перейти к authenticated staging/load gate этапа 5:

1. Выполнить в staging encryption/rotation lifecycle, secret leakage, retention, consent/privacy version и legacy plaintext remediation rehearsal; локальный implementation slice уже добавлен, но runtime подтверждение отсутствует.
2. Получить staging URL, тестовую учётную запись, sandbox credentials и параметры reverse proxy/балансировщика.
3. Развернуть approved commit только в staging с выключенным feature flag.
4. Запустить transport smoke для asset/WebSocket, затем проверить authenticated push, recovery polling, quota, fallback, cancel, retry и owner isolation.
5. Провести нагрузочную проверку 20–50 UI-сессий, reconnect, sticky-session и proxy timeouts.
6. Повторно прогнать migration/rollback rehearsal и обновить этот отчёт evidence и решением о готовности.

## Краткий список оставшихся этапов roadmap

- Этап 4 «Безопасность и данные» — `REWORK`; остаются encryption/rotation evidence, secret leakage, retention, consent/privacy version, legacy plaintext remediation и runtime security.
- Этап 5 «Интеграция и тестирование» — частично выполнен; остаются authenticated staging, нагрузка и регрессия shell/layout.
- Этап 6 «Подготовка к выпуску» — документация подготовлена, но остаются rehearsal, утверждение seed-значений, release decision и затем отдельное production-распоряжение.
- После MVP, не блокируя текущий PR: provider-specific usage lookup по request ID — только после подтверждения API; маскирование PII кандидатских данных — TODO по распоряжению пользователя; архивирование истории — отдельное решение при бессрочном хранении.

## Критерий закрытия отчёта

Статус можно изменить на READY FOR RELEASE только после выполнения всех обязательных шагов, устранения блокеров staging и миграционной rehearsal, а также явного решения о выпуске. До этого ветка может обновлять PR, но не production.

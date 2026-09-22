# BL-2026-021 — обязательный владелец UserAiConfiguration

STATUS: WAITING_FOR_HERMES

- BASE_SHA: `d1f9c6d9846f2792615396166850b7d9cb6632eb`
- BRANCH: `agent/bl-2026-021-user-ai-configuration-owner`
- GOAL: исключить commit новой `UserAiConfiguration` с `USER_ID = null` во всех UI-путях создания.
- ALLOWED_SCOPE: экраны создания/редактирования `UserAiConfiguration`, их контрактные тесты и связанная документация.
- FORBIDDEN_SCOPE: изменение DB constraint, вывод/изменение API-ключей, использование `createdBy` как FK, deploy, production operations, несвязанный рефакторинг.
- ACCEPTANCE_CRITERIA: владелец назначен до commit; при невозможности определить владельца commit блокируется понятным сообщением; NOT NULL/FK сохраняются; альтернативные пути создания покрыты тестами.
- VERIFICATION_REQUIRED: профильные тесты, `ScreenViewIntegrityTest`, компиляция, `git diff --check`.

## Результат локальной проверки

- BL-2026-021 static contract: PASS.
- XML well-formedness: PASS.
- `git diff --check`: PASS.
- Secret-pattern scan изменённого diff: PASS.
- Gradle/profile test и `ScreenViewIntegrityTest`: не запущены, поскольку Gradle Wrapper 5.6.4 отсутствует в кэше, а доступ к `services.gradle.org` заблокирован средой до старта Gradle.
- Deploy: не выполнялся по прямому запрету.

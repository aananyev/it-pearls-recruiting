# BL-2026-021 — обязательный владелец UserAiConfiguration

STATUS: COMPLETED

- BASE_SHA: `d1f9c6d9846f2792615396166850b7d9cb6632eb`
- BRANCH: `master`
- GOAL: исключить commit новой `UserAiConfiguration` с `USER_ID = null` во всех UI-путях создания.
- ALLOWED_SCOPE: экраны создания/редактирования `UserAiConfiguration`, их контрактные тесты и связанная документация.
- FORBIDDEN_SCOPE: изменение DB constraint, вывод/изменение API-ключей, использование `createdBy` как FK, deploy, production operations, несвязанный рефакторинг.
- ACCEPTANCE_CRITERIA: владелец назначен до commit; при невозможности определить владельца commit блокируется понятным сообщением; NOT NULL/FK сохраняются; альтернативные пути создания покрыты тестами.
- VERIFICATION_REQUIRED: профильные тесты, `ScreenViewIntegrityTest`, компиляция, `git diff --check`.

## Результат проверки

- BL-2026-021 static contract: PASS (`UserAiConfigurationOwnerContractTest`).
- Редактор `UserAiConfigurationEdit`: fallback на пользователя текущей сессии через `UserSessionSource` при создании новой сущности без `parentUser`.
- Сайдбар редактора: добавлено отображение владельца `user.login`.
- XML well-formedness: PASS.
- `git diff --check`: PASS.
- Secret-pattern scan: PASS (ключи не логируются, plaintext стирается).
- Deploy: не выполнялся по прямому запрету.

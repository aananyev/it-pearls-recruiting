# AdminAiConfiguration

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`AdminAiConfiguration` хранит корпоративные AI-подключения для функций, финансируемых и контролируемых администратором HRM HuntTech. Сущность физически отделяет корпоративные credentials от персонального `UserAiConfiguration`.

### UI Context & Navigation

Администрирование выполняется только через `hunttech_AdminAiConfiguration.browse/edit` в «Управление AI». Сущность помечена `@SystemLevel`; browse-view не содержит ciphertext. Обычным ролям доступ к entity/screens не должен предоставляться.

### Behavior Summary

- ввод нового ключа → plaintext отправляется middleware `AiCredentialService` → AES-GCM ciphertext сохраняется в entity;
- повторное открытие Edit → plaintext не возвращается в Web Client → пустое поле означает сохранить прежний ключ;
- тест подключения → UI передаёт только UUID записи → core расшифровывает ключ непосредственно перед provider-вызовом;
- ошибка теста → сохраняется безопасный класс ошибки → provider message/secret не сохраняются;
- execution → resolver получает активное подключение → secret расшифровывается только в core.

## Модель данных

Entity: `hunttech_AdminAiConfiguration`  
Table: `HUNTTECH_ADMIN_AI_CONFIGURATION`.

Поля: `name`, `providerCode`, `apiKeyEncrypted`, `defaultModelName`, `baseApiUrl`, `active`, `priority`, `lastTestStatus`, `lastTestAt`, `lastError`.

## Безопасность

`apiKeyEncrypted` не входит в `admin-ai-configuration-browse-view`. UI никогда не предоставляет функцию просмотра существующего ключа. Шифрование использует `hunttech.ai.encryptionKey`; реальное значение не хранится в Git и должно быть не короче 32 символов. Middleware дополнительно проверяет screen permission перед шифрованием/тестом корпоративного credential.

`baseApiUrl` на первом этапе является metadata для будущих OpenAI-compatible gateway adapters; текущие vendor adapters продолжают владеть своими endpoint-контрактами.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Создана системная сущность корпоративного AI-подключения с AES-GCM credential storage |

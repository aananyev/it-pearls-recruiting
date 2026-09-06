# AiCredentialService

## Business & Context Intro

### Назначение и бизнес-смысл (What & Why)

`AiCredentialService` изолирует корпоративные API credentials от Web Client. UI может передать новый plaintext для шифрования или UUID подключения для теста, но не имеет операции decrypt/read-secret.

### UI Context & Navigation

Сервис используется только экранами `hunttech_AdminAiConfiguration.browse/edit`. Операции дополнительно защищены middleware-проверкой `Security.isScreenPermitted()`.

### Behavior Summary

- сохранить новый ключ → permission check → AES-GCM encrypt → ciphertext возвращается для commit;
- ключ не введён при редактировании → прежний ciphertext сохраняется;
- тест → UI передаёт UUID → core загружает secret-view → decrypt → короткий provider request;
- успех → `SUCCESS`/timestamp → safe browse reload;
- ошибка → `FAILED` + класс ошибки → secret/provider request details не сохраняются.
- legacy migration → admin-only batch шифрует исторический `API_KEY`, очищает plaintext и возвращает только количество обработанных записей; повторный запуск идемпотентен.

## API

```java
String encryptAdminSecret(String plainText);
String encryptUserSecret(String plainText);
int migrateLegacyUserSecrets();
int rotateSecrets();
void testAdminConnection(UUID configurationId);
```

В интерфейсе намеренно отсутствует метод получения или расшифровки секрета.
`migrateLegacyUserSecrets()` не возвращает ключи и не принимает их от Web Client; middleware загружает legacy-значения только внутри core и требует `hunttech.ai.manageCorporateCredentials`.

## Криптография

`AiSecretCipher`: AES/GCM/NoPadding, случайный 12-byte IV на каждое шифрование, 128-bit GCM tag. Key material берётся из server-side `hunttech.ai.encryptionKey`, преобразуется SHA-256 в AES key. Формат ciphertext versioned: `v1:<iv>:<ciphertext>`.

До завершения контролируемой миграции сохраняется обратная совместимость с legacy `API_KEY`: первое использование может выполнить одноразовое шифрование и очистку записи. Для release migration запускается admin-only batch, после сверки количество plaintext-записей должно быть равно нулю. Для master-key rotation временно задаются текущий и предыдущий server-side ключи, затем `rotateSecrets()` повторно шифрует ciphertext и возвращает только count; после проверки предыдущий ключ удаляется из конфигурации окружения.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Зарегистрирован в web-прокси (web-spring.xml, remoteServices) — без этого AdminAiConfigurationBrowse/Edit падали с DevelopmentException; views AI Control Plane добавлены в cuba.viewsConfig (app.properties/web-app.properties) |
| 2026-08-12 | Создан middleware credential service и AES-GCM secret layer |

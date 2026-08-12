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

## API

```java
String encryptAdminSecret(String plainText);
void testAdminConnection(UUID configurationId);
```

В интерфейсе намеренно отсутствует метод получения или расшифровки секрета.

## Криптография

`AiSecretCipher`: AES/GCM/NoPadding, случайный 12-byte IV на каждое шифрование, 128-bit GCM tag. Key material берётся из server-side `hunttech.ai.encryptionKey`, преобразуется SHA-256 в AES key. Формат ciphertext versioned: `v1:<iv>:<ciphertext>`.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-08-12 | Зарегистрирован в web-прокси (web-spring.xml, remoteServices) — без этого AdminAiConfigurationBrowse/Edit падали с DevelopmentException; views AI Control Plane добавлены в cuba.viewsConfig (app.properties/web-app.properties) |
| 2026-08-12 | Создан middleware credential service и AES-GCM secret layer |

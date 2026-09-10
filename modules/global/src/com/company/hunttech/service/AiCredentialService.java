package com.company.hunttech.service;

import java.util.UUID;

/**
 * Безопасный middleware-фасад управления корпоративными AI credentials.
 *
 * Интерфейс намеренно не предоставляет decrypt/get-secret: Web Client может только
 * передать новый plaintext для шифрования или попросить core протестировать запись по id.
 */
public interface AiCredentialService {
    String NAME = "hunttech_AiCredentialService";

    String encryptAdminSecret(String plainText);

    /**
     * Encrypts a personal secret. The service never exposes a decrypt operation
     * to the web client and does not persist the supplied plaintext.
     */
    String encryptUserSecret(String plainText);

    /**
     * Переносит legacy API_KEY пользователей в ciphertext и очищает plaintext.
     * Операция доступна только администратору и идемпотентна.
     */
    int migrateLegacyUserSecrets();

    /** Re-encrypts existing credentials after a server-side master-key rotation. */
    int rotateSecrets();

    void testAdminConnection(UUID configurationId);

    /**
     * Проверяет подключение к AI-провайдеру с заданными параметрами.
     *
     * <p>Может вызываться как для корпоративных настроек администратора, так и для
     * персональных настроек произвольного пользователя системы. Если передан новый
     * незашифрованный ключ (plainApiKey), проверка выполняется с ним (без сохранения в БД).
     * Если plainApiKey не задан, сервис расшифровывает encryptedApiKey.</p>
     *
     * @param providerCode    код провайдера (из AiProviderCatalog)
     * @param modelName       имя модели (null или пустое — модель по умолчанию провайдера)
     * @param plainApiKey     новый введённый API-ключ в открытом виде (или null)
     * @param encryptedApiKey ранее сохранённый зашифрованный ключ (или null)
     * @param baseApiUrl      базовый URL API (опционально, или null)
     * @return результат проверки со статусом, сообщением и расширенной диагностикой причин неудачи
     */
    AiConnectionTestResult testConnection(String providerCode, String modelName,
                                          String plainApiKey, String encryptedApiKey,
                                          String baseApiUrl);
}

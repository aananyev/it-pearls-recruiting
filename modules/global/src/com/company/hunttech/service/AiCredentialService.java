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

    void testAdminConnection(UUID configurationId);
}

package com.company.hunttech.core.ai;

import com.company.hunttech.config.HunttechAiSecurityConfig;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DevelopmentException;
import org.springframework.stereotype.Component;

import javax.inject.Inject;

/**
 * Middleware-only доступ к шифрованию корпоративных AI credentials.
 *
 * Ключ шифрования читается из серверной конфигурации только в момент операции.
 * Пустая настройка блокирует работу с корпоративными секретами предсказуемой ошибкой.
 */
@Component
public class AiSecretService {
    @Inject
    private Configuration configuration;

    private final AiSecretCipher cipher = new AiSecretCipher();

    public String encrypt(String plainText) {
        return cipher.encrypt(plainText, requireEncryptionKey());
    }

    public String decrypt(String encryptedText) {
        return cipher.decrypt(encryptedText, requireEncryptionKey());
    }

    /**
     * Keeps ciphertext unchanged when it already uses the current key; otherwise
     * decrypts it with the temporary previous key and encrypts it with the current key.
     */
    public String rotate(String encryptedText) {
        String currentKey = requireEncryptionKey();
        try {
            cipher.decrypt(encryptedText, currentKey);
            return encryptedText;
        } catch (RuntimeException currentKeyFailure) {
            String previousKey = configuration.getConfig(HunttechAiSecurityConfig.class)
                    .getPreviousEncryptionKey();
            if (previousKey == null || previousKey.trim().length() < 32) {
                throw new DevelopmentException(
                        "Не настроен предыдущий ключ шифрования для ротации AI credentials.");
            }
            String plainText = cipher.decrypt(encryptedText, previousKey);
            return cipher.encrypt(plainText, currentKey);
        }
    }

    private String requireEncryptionKey() {
        String key = configuration.getConfig(HunttechAiSecurityConfig.class).getEncryptionKey();
        if (key == null || key.trim().length() < 32) {
            throw new DevelopmentException(
                    "Не настроен hunttech.ai.encryptionKey: требуется секрет не короче 32 символов.");
        }
        return key;
    }
}

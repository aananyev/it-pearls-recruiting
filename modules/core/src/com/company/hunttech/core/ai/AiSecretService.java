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

    private String requireEncryptionKey() {
        String key = configuration.getConfig(HunttechAiSecurityConfig.class).getEncryptionKey();
        if (key == null || key.trim().length() < 32) {
            throw new DevelopmentException(
                    "Не настроен hunttech.ai.encryptionKey: требуется секрет не короче 32 символов.");
        }
        return key;
    }
}

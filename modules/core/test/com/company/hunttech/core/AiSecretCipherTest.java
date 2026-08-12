package com.company.hunttech.core;

import com.company.hunttech.core.ai.AiSecretCipher;
import com.haulmont.cuba.core.global.DevelopmentException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * Регрессия криптографического контракта корпоративных AI credentials.
 * Тест не использует реальных секретов или внешние AI API.
 */
public class AiSecretCipherTest {
    private static final String KEY = "0123456789abcdef0123456789abcdef";

    @Test
    public void encryptDecryptRoundTrip() {
        AiSecretCipher cipher = new AiSecretCipher();
        String encrypted = cipher.encrypt("corporate-secret", KEY);

        assertEquals("corporate-secret", cipher.decrypt(encrypted, KEY));
    }

    @Test
    public void randomIvMakesCiphertextDifferent() {
        AiSecretCipher cipher = new AiSecretCipher();

        String first = cipher.encrypt("same-secret", KEY);
        String second = cipher.encrypt("same-secret", KEY);

        assertNotEquals("AES-GCM должен использовать новый IV для каждой записи", first, second);
    }

    @Test(expected = DevelopmentException.class)
    public void wrongKeyCannotDecrypt() {
        AiSecretCipher cipher = new AiSecretCipher();
        String encrypted = cipher.encrypt("corporate-secret", KEY);

        cipher.decrypt(encrypted, "abcdef0123456789abcdef0123456789");
    }
}

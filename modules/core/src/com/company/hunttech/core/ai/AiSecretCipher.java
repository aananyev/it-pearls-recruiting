package com.company.hunttech.core.ai;

import com.haulmont.cuba.core.global.DevelopmentException;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-GCM codec для корпоративных AI credentials.
 *
 * Класс не знает о CUBA Configuration и удобен для изолированного тестирования.
 * В шифротекст включается случайный IV; plaintext и key material никогда не логируются.
 */
public class AiSecretCipher {
    private static final String PREFIX = "v1";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private final SecureRandom secureRandom = new SecureRandom();

    public String encrypt(String plainText, String keyMaterial) {
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, createKey(keyMaterial), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return PREFIX + ":" + Base64.getEncoder().encodeToString(iv)
                    + ":" + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new DevelopmentException("Не удалось зашифровать корпоративный AI credential.", e);
        }
    }

    public String decrypt(String token, String keyMaterial) {
        try {
            String[] parts = token.split(":", 3);
            if (parts.length != 3 || !PREFIX.equals(parts[0])) {
                throw new IllegalArgumentException("Unsupported secret format");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] encrypted = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, createKey(keyMaterial), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new DevelopmentException("Не удалось расшифровать корпоративный AI credential.", e);
        }
    }

    private SecretKeySpec createKey(String keyMaterial) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return new SecretKeySpec(digest.digest(keyMaterial.getBytes(StandardCharsets.UTF_8)), "AES");
    }
}

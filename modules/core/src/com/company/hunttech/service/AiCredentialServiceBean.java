package com.company.hunttech.service;

import com.company.hunttech.core.ai.AIProvider;
import com.company.hunttech.core.ai.AIProviderRegistry;
import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.Security;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Реализация защищённых административных операций с корпоративными AI credentials.
 *
 * Specific permission проверяется на middleware, поэтому один только screen grant
 * не позволяет вызвать шифрование/тест корпоративного credential через сервис.
 */
@Service(AiCredentialService.NAME)
public class AiCredentialServiceBean implements AiCredentialService {
    public static final String MANAGE_CORPORATE_CREDENTIALS_PERMISSION =
            "hunttech.ai.manageCorporateCredentials";

    @Inject
    private DataManager dataManager;
    @Inject
    private Security security;
    @Inject
    private AIProviderRegistry aiProviderRegistry;
    @Inject
    private AiSecretService aiSecretService;

    @Override
    public String encryptAdminSecret(String plainText) {
        requireAdminPermission();
        if (!isConfigured(plainText)) {
            throw new DevelopmentException("Новый API-ключ не задан.");
        }
        return aiSecretService.encrypt(plainText);
    }

    @Override
    public String encryptUserSecret(String plainText) {
        if (!isConfigured(plainText)) {
            throw new DevelopmentException("Новый персональный API-ключ не задан.");
        }
        return aiSecretService.encrypt(plainText);
    }

    @Override
    public int migrateLegacyUserSecrets() {
        requireAdminPermission();
        List<UserAiConfiguration> legacyConfigurations = dataManager.load(UserAiConfiguration.class)
                .query("select e from hunttech_UserAiConfiguration e "
                        + "where e.apiKey is not null and trim(e.apiKey) <> ''")
                .view("user-ai-configuration-ai-execution-view")
                .list();
        int migrated = 0;
        for (UserAiConfiguration configuration : legacyConfigurations) {
            String legacySecret = configuration.getApiKey();
            if (!isConfigured(legacySecret)) {
                continue;
            }
            configuration.setApiKeyEncrypted(aiSecretService.encrypt(legacySecret));
            configuration.setApiKey(null);
            dataManager.commit(configuration);
            migrated++;
        }
        return migrated;
    }

    @Override
    public int rotateSecrets() {
        requireAdminPermission();
        int rotated = 0;
        List<UserAiConfiguration> userConfigurations = dataManager.load(UserAiConfiguration.class)
                .query("select e from hunttech_UserAiConfiguration e "
                        + "where e.apiKeyEncrypted is not null and trim(e.apiKeyEncrypted) <> ''")
                .view("user-ai-configuration-ai-execution-view")
                .list();
        for (UserAiConfiguration configuration : userConfigurations) {
            String current = configuration.getApiKeyEncrypted();
            String rotatedValue = aiSecretService.rotate(current);
            if (!rotatedValue.equals(current)) {
                configuration.setApiKeyEncrypted(rotatedValue);
                dataManager.commit(configuration);
                rotated++;
            }
        }

        List<AdminAiConfiguration> adminConfigurations = dataManager.load(AdminAiConfiguration.class)
                .query("select e from hunttech_AdminAiConfiguration e "
                        + "where e.apiKeyEncrypted is not null and trim(e.apiKeyEncrypted) <> ''")
                .view("admin-ai-configuration-secret-view")
                .list();
        for (AdminAiConfiguration configuration : adminConfigurations) {
            String current = configuration.getApiKeyEncrypted();
            String rotatedValue = aiSecretService.rotate(current);
            if (!rotatedValue.equals(current)) {
                configuration.setApiKeyEncrypted(rotatedValue);
                dataManager.commit(configuration);
                rotated++;
            }
        }
        return rotated;
    }

    @Override
    public void testAdminConnection(UUID configurationId) {
        requireAdminPermission();
        if (configurationId == null) {
            throw new DevelopmentException("Не выбрано корпоративное AI-подключение.");
        }

        AdminAiConfiguration configuration = dataManager.load(AdminAiConfiguration.class)
                .id(configurationId)
                .view("admin-ai-configuration-secret-view")
                .one();
        try {
            validateConfiguration(configuration);
            AIProvider provider = aiProviderRegistry.getProvider(configuration.getProviderCode());
            String response = provider.generateText(
                    "Ответь одним словом: ok",
                    "Тест корпоративного подключения HRM HuntTech.",
                    aiSecretService.decrypt(configuration.getApiKeyEncrypted()),
                    configuration.getDefaultModelName(),
                    Collections.<String, Object>singletonMap("temperature", 0.0));
            if (!isConfigured(response)) {
                throw new DevelopmentException("AI-провайдер вернул пустой ответ.");
            }
            configuration.setLastTestStatus("SUCCESS");
            configuration.setLastTestAt(new Date());
            configuration.setLastError(null);
            dataManager.commit(configuration);
        } catch (RuntimeException e) {
            configuration.setLastTestStatus("FAILED");
            configuration.setLastTestAt(new Date());
            // Не сохраняем provider message: сторонняя библиотека может включить request details.
            configuration.setLastError("Ошибка проверки: " + e.getClass().getSimpleName());
            dataManager.commit(configuration);
            throw new DevelopmentException("Корпоративное AI-подключение не прошло проверку.", e);
        }
    }

    private void validateConfiguration(AdminAiConfiguration configuration) {
        if (!Boolean.TRUE.equals(configuration.getActive())) {
            throw new DevelopmentException("Корпоративное AI-подключение отключено.");
        }
        if (!isConfigured(configuration.getProviderCode())) {
            throw new DevelopmentException("Не указан провайдер AI.");
        }
        if (!isConfigured(configuration.getApiKeyEncrypted())) {
            throw new DevelopmentException("Корпоративный API-ключ не настроен.");
        }
    }

    private void requireAdminPermission() {
        if (!security.isSpecificPermitted(MANAGE_CORPORATE_CREDENTIALS_PERMISSION)) {
            throw new DevelopmentException("Нет права управления корпоративными AI-подключениями.");
        }
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

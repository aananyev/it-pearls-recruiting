package com.company.hunttech.service;

import com.company.hunttech.core.ai.AIProvider;
import com.company.hunttech.core.ai.AIProviderRegistry;
import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Security;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AiCredentialServiceBeanTest {
    private DataManager dataManager;
    private Security security;
    private AiSecretService aiSecretService;
    private AIProviderRegistry aiProviderRegistry;
    private AiCredentialServiceBean service;

    @Before
    public void setUp() {
        dataManager = mock(DataManager.class);
        security = mock(Security.class);
        aiSecretService = mock(AiSecretService.class);
        aiProviderRegistry = mock(AIProviderRegistry.class);
        service = new AiCredentialServiceBean();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "dataManager", dataManager);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "security", security);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "aiSecretService", aiSecretService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "aiProviderRegistry", aiProviderRegistry);
    }

    @Test(expected = com.haulmont.cuba.core.global.DevelopmentException.class)
    public void migrationRequiresAdminPermission() {
        when(security.isSpecificPermitted(AiCredentialServiceBean.MANAGE_CORPORATE_CREDENTIALS_PERMISSION))
                .thenReturn(false);

        service.migrateLegacyUserSecrets();

        verify(dataManager, never()).load(UserAiConfiguration.class);
    }

    @Test
    public void migrationEncryptsAndClearsLegacyValue() {
        when(security.isSpecificPermitted(AiCredentialServiceBean.MANAGE_CORPORATE_CREDENTIALS_PERMISSION))
                .thenReturn(true);
        UserAiConfiguration configuration = new UserAiConfiguration();
        configuration.setApiKey("legacy-secret");
        FluentLoader loader = mock(FluentLoader.class, RETURNS_DEEP_STUBS);
        when(dataManager.load(UserAiConfiguration.class)).thenReturn(loader);
        when(loader.query(anyString()).view(anyString()).list())
                .thenReturn(Collections.singletonList(configuration));
        when(aiSecretService.encrypt("legacy-secret")).thenReturn("v1:ciphertext");

        assertEquals(1, service.migrateLegacyUserSecrets());

        assertEquals("v1:ciphertext", configuration.getApiKeyEncrypted());
        assertNull(configuration.getApiKey());
        verify(dataManager).commit(configuration);
    }

    @Test
    public void rotationCoversUserAndAdminCredentials() {
        when(security.isSpecificPermitted(AiCredentialServiceBean.MANAGE_CORPORATE_CREDENTIALS_PERMISSION))
                .thenReturn(true);
        UserAiConfiguration userConfiguration = new UserAiConfiguration();
        userConfiguration.setApiKeyEncrypted("user-old");
        AdminAiConfiguration adminConfiguration = new AdminAiConfiguration();
        adminConfiguration.setApiKeyEncrypted("admin-old");

        FluentLoader userLoader = mock(FluentLoader.class, RETURNS_DEEP_STUBS);
        FluentLoader adminLoader = mock(FluentLoader.class, RETURNS_DEEP_STUBS);
        when(dataManager.load(UserAiConfiguration.class)).thenReturn(userLoader);
        when(dataManager.load(AdminAiConfiguration.class)).thenReturn(adminLoader);
        when(userLoader.query(anyString()).view(anyString()).list())
                .thenReturn(Collections.singletonList(userConfiguration));
        when(adminLoader.query(anyString()).view(anyString()).list())
                .thenReturn(Collections.singletonList(adminConfiguration));
        when(aiSecretService.rotate("user-old")).thenReturn("user-new");
        when(aiSecretService.rotate("admin-old")).thenReturn("admin-new");

        assertEquals(2, service.rotateSecrets());

        assertEquals("user-new", userConfiguration.getApiKeyEncrypted());
        assertEquals("admin-new", adminConfiguration.getApiKeyEncrypted());
    }

    @Test
    public void testConnectionSuccessWithPlainApiKey() {
        AIProvider provider = mock(AIProvider.class);
        when(aiProviderRegistry.getProvider("openai")).thenReturn(provider);
        when(provider.generateText(anyString(), anyString(), eq("sk-plain-key"), eq("gpt-4o"), anyMap()))
                .thenReturn("ok");

        AiConnectionTestResult result = service.testConnection("openai", "gpt-4o", "sk-plain-key", null, null);

        assertTrue(result.isSuccess());
        assertEquals("SUCCESS", result.getStatus());
        assertNotNull(result.getLatencyMs());
        assertTrue(result.getMessage().contains("gpt-4o"));
        assertTrue(result.getMessage().contains("ok"));
    }

    @Test
    public void testConnectionMissingApiKeyReturnsInformativeFailure() {
        AiConnectionTestResult result = service.testConnection("openai", "gpt-4o", null, null, null);

        assertFalse(result.isSuccess());
        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getMessage().contains("API-ключ не задан"));
        assertTrue(result.getDetailedError().contains("API-ключ для подключения к AI-провайдеру отсутствует"));
    }

    @Test
    public void testConnectionHttp401UnauthorizedDiagnostics() {
        AIProvider provider = mock(AIProvider.class);
        when(aiProviderRegistry.getProvider("openai")).thenReturn(provider);
        when(provider.generateText(anyString(), anyString(), anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("HTTP 401: Incorrect API key provided"));

        AiConnectionTestResult result = service.testConnection("openai", "gpt-4o", "sk-invalid", null, null);

        assertFalse(result.isSuccess());
        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getMessage().contains("401"));
        assertTrue(result.getMessage().contains("неверный или отозванный API-ключ"));
        assertTrue(result.getDetailedError().contains("Проверьте статус ключа"));
    }

    @Test
    public void testConnectionHttp429QuotaExceededDiagnostics() {
        AIProvider provider = mock(AIProvider.class);
        when(aiProviderRegistry.getProvider("openai")).thenReturn(provider);
        when(provider.generateText(anyString(), anyString(), anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("HTTP 429: You exceeded your current quota, please check your plan and billing details."));

        AiConnectionTestResult result = service.testConnection("openai", "gpt-4o", "sk-valid", null, null);

        assertFalse(result.isSuccess());
        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getMessage().contains("429"));
        assertTrue(result.getDetailedError().contains("баланс/квота"));
        assertTrue(result.getDetailedError().contains("Insufficient Quota"));
    }

    @Test
    public void testConnectionTimeoutDiagnostics() {
        AIProvider provider = mock(AIProvider.class);
        when(aiProviderRegistry.getProvider("anthropic")).thenReturn(provider);
        when(provider.generateText(anyString(), anyString(), anyString(), anyString(), anyMap()))
                .thenThrow(new RuntimeException("java.net.SocketTimeoutException: Read timed out"));

        AiConnectionTestResult result = service.testConnection("anthropic", "claude-sonnet-4-6", "sk-ant", null, null);

        assertFalse(result.isSuccess());
        assertEquals("FAILED", result.getStatus());
        assertTrue(result.getMessage().contains("таймаут"));
        assertTrue(result.getDetailedError().contains("таймаут или сетевой сбой"));
    }

    @Test
    public void testConnectionDecryptsEncryptedKeyWhenPlainEmpty() {
        when(aiSecretService.decrypt("enc:saved-secret")).thenReturn("decrypted-api-key");
        AIProvider provider = mock(AIProvider.class);
        when(aiProviderRegistry.getProvider("gigachat")).thenReturn(provider);
        when(provider.generateText(anyString(), anyString(), eq("decrypted-api-key"), anyString(), anyMap()))
                .thenReturn("ok");

        AiConnectionTestResult result = service.testConnection("gigachat", "GigaChat", "", "enc:saved-secret", null);

        assertTrue(result.isSuccess());
        assertEquals("SUCCESS", result.getStatus());
    }
}

package com.company.hunttech.service;

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
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AiCredentialServiceBeanTest {
    private DataManager dataManager;
    private Security security;
    private AiSecretService aiSecretService;
    private AiCredentialServiceBean service;

    @Before
    public void setUp() {
        dataManager = mock(DataManager.class);
        security = mock(Security.class);
        aiSecretService = mock(AiSecretService.class);
        service = new AiCredentialServiceBean();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "dataManager", dataManager);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "security", security);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "aiSecretService", aiSecretService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "aiProviderRegistry", mock(AIProviderRegistry.class));
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
}

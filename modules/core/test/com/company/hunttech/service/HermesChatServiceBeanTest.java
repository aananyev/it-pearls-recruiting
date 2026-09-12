package com.company.hunttech.service;

import com.company.hunttech.config.HunttechHermesConfig;
import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.global.UserSession;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тестирование логики выбора моделей и цепочки fallback в HermesChatServiceBean.
 * Сценарий: сначала подключается модель из пользовательских настроек (UserAiConfiguration),
 * если ни одна не отвечает — административные модели (AdminAiConfiguration), затем дефолт контейнера.
 */
public class HermesChatServiceBeanTest {

    private HermesChatServiceBean service;
    private DataManager dataManager;
    private Metadata metadata;
    private UserSessionSource userSessionSource;
    private Configuration configuration;
    private HunttechHermesConfig hermesConfig;
    private AiSecretService aiSecretService;

    @Before
    public void setUp() {
        service = new HermesChatServiceBean();
        dataManager = mock(DataManager.class);
        metadata = mock(Metadata.class);
        userSessionSource = mock(UserSessionSource.class);
        configuration = mock(Configuration.class);
        hermesConfig = mock(HunttechHermesConfig.class);
        aiSecretService = mock(AiSecretService.class);

        when(configuration.getConfig(HunttechHermesConfig.class)).thenReturn(hermesConfig);
        when(hermesConfig.getProfile()).thenReturn("hrm-viewer");
        when(hermesConfig.getContainerName()).thenReturn("hermes-hrm-viewer");

        ReflectionTestUtils.setField(service, "dataManager", dataManager);
        ReflectionTestUtils.setField(service, "metadata", metadata);
        ReflectionTestUtils.setField(service, "userSessionSource", userSessionSource);
        ReflectionTestUtils.setField(service, "configuration", configuration);
        ReflectionTestUtils.setField(service, "aiSecretService", aiSecretService);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCandidateResolutionOrderUserThenAdminThenDefault() throws Exception {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        user.setLogin("test-recruiter");

        // Mock UserAiConfiguration
        UserAiConfiguration userConfig = new UserAiConfiguration();
        userConfig.setUser(user);
        userConfig.setProviderCode("deepseek");
        userConfig.setDefaultModelName("deepseek-chat");
        userConfig.setApiKey("sk-user-key");
        userConfig.setIsActive(true);

        FluentLoader<UserAiConfiguration, UUID> userFluentLoader = mock(FluentLoader.class);
        FluentLoader.ByQuery<UserAiConfiguration, UUID> userLoader = mock(FluentLoader.ByQuery.class);
        when(dataManager.load(UserAiConfiguration.class)).thenReturn(userFluentLoader);
        when(userFluentLoader.query(anyString())).thenReturn(userLoader);
        when(userLoader.parameter(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(userLoader);
        when(userLoader.list()).thenReturn(Collections.singletonList(userConfig));

        // Mock AdminAiConfiguration
        AdminAiConfiguration adminConfig = new AdminAiConfiguration();
        adminConfig.setName("Corporate DeepSeek");
        adminConfig.setProviderCode("deepseek");
        adminConfig.setDefaultModelName("deepseek-v4-flash");
        adminConfig.setApiKeyEncrypted("enc:admin-key");
        adminConfig.setActive(true);

        when(aiSecretService.decrypt("enc:admin-key")).thenReturn("sk-admin-decrypted-key");

        FluentLoader<AdminAiConfiguration, UUID> adminFluentLoader = mock(FluentLoader.class);
        FluentLoader.ByQuery<AdminAiConfiguration, UUID> adminLoader = mock(FluentLoader.ByQuery.class);
        when(dataManager.load(AdminAiConfiguration.class)).thenReturn(adminFluentLoader);
        when(adminFluentLoader.query(anyString())).thenReturn(adminLoader);
        when(adminLoader.list()).thenReturn(Collections.singletonList(adminConfig));

        Method resolveMethod = HermesChatServiceBean.class.getDeclaredMethod("resolveExecutionCandidates", ExtUser.class);
        resolveMethod.setAccessible(true);
        List<HermesChatServiceBean.HermesExecutionCandidate> candidates =
                (List<HermesChatServiceBean.HermesExecutionCandidate>) resolveMethod.invoke(service, user);

        assertNotNull(candidates);
        assertEquals(3, candidates.size());

        // 1. Сначала пользовательская модель
        assertEquals("USER", candidates.get(0).getSource());
        assertEquals("deepseek", candidates.get(0).getProviderCode());
        assertEquals("deepseek-chat", candidates.get(0).getModelName());
        assertEquals("sk-user-key", candidates.get(0).getApiKey());

        // 2. Затем административная модель
        assertEquals("ADMIN", candidates.get(1).getSource());
        assertEquals("deepseek", candidates.get(1).getProviderCode());
        assertEquals("deepseek-v4-flash", candidates.get(1).getModelName());
        assertEquals("sk-admin-decrypted-key", candidates.get(1).getApiKey());

        // 3. Последняя — резервная конфигурация контейнера
        assertEquals("CONTAINER_DEFAULT", candidates.get(2).getSource());
    }

    @Test
    public void testIsErrorResponseDetection() {
        // Ошибочные ответы должны быть распознаны
        org.junit.Assert.assertTrue(service.isErrorResponse("HTTP 403: { \"success\": false, \"error\": \"Access denied by security policy.\" }"));
        org.junit.Assert.assertTrue(service.isErrorResponse("HTTP 401: Unauthorized"));
        org.junit.Assert.assertTrue(service.isErrorResponse("HTTP 429: Too Many Requests"));
        org.junit.Assert.assertTrue(service.isErrorResponse("HTTP 500: Internal Server Error"));
        org.junit.Assert.assertTrue(service.isErrorResponse("{ \"success\": false, \"error\": \"Model quota exceeded\" }"));
        org.junit.Assert.assertTrue(service.isErrorResponse("{\"success\":false,\"error\":\"Model quota exceeded\"}"));
        org.junit.Assert.assertTrue(service.isErrorResponse("AuthenticationError: Incorrect API key provided"));
        org.junit.Assert.assertTrue(service.isErrorResponse("Error: No API key found for provider openrouter"));

        // Корректные ответы ассистента НЕ должны быть помечены как ошибки
        org.junit.Assert.assertFalse(service.isErrorResponse("Привет! Я готов помочь вам с HRM системой."));
        org.junit.Assert.assertFalse(service.isErrorResponse("Сегодня назначено 3 собеседования."));
        org.junit.Assert.assertFalse(service.isErrorResponse("В компании действует strict access denied by security policy регламент для защиты данных."));
        org.junit.Assert.assertFalse(service.isErrorResponse(null));
        org.junit.Assert.assertFalse(service.isErrorResponse(""));
    }
}

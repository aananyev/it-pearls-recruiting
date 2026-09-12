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
    private UserAiContextService userAiContextService;

    @Before
    public void setUp() {
        service = new HermesChatServiceBean();
        dataManager = mock(DataManager.class);
        metadata = mock(Metadata.class);
        userSessionSource = mock(UserSessionSource.class);
        configuration = mock(Configuration.class);
        hermesConfig = mock(HunttechHermesConfig.class);
        aiSecretService = mock(AiSecretService.class);
        userAiContextService = mock(UserAiContextService.class);

        when(configuration.getConfig(HunttechHermesConfig.class)).thenReturn(hermesConfig);
        when(hermesConfig.getProfile()).thenReturn("hrm-viewer");
        when(hermesConfig.getContainerName()).thenReturn("hermes-hrm-viewer");

        ReflectionTestUtils.setField(service, "dataManager", dataManager);
        ReflectionTestUtils.setField(service, "metadata", metadata);
        ReflectionTestUtils.setField(service, "userSessionSource", userSessionSource);
        ReflectionTestUtils.setField(service, "configuration", configuration);
        ReflectionTestUtils.setField(service, "aiSecretService", aiSecretService);
        ReflectionTestUtils.setField(service, "userAiContextService", userAiContextService);
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

    @Test
    public void testBuildHermesUserPrompt_withUserAiContextService() {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        user.setLogin("alan");
        user.setName("Алексей Ананьев");
        user.setEmail("alan@hunttech.ru");

        com.company.hunttech.service.dto.AiUserContext userCtx = new com.company.hunttech.service.dto.AiUserContext();
        userCtx.setActive(true);
        userCtx.getProfileData().put("userName", "Алексей Ананьев");
        userCtx.getProfileData().put("currentPosition", "Руководитель группы подбора");
        userCtx.getProfileData().put("preferredLanguage", "RUSSIAN");
        userCtx.getProfileData().put("communicationStyle", "BUSINESS");
        userCtx.getProfileData().put("responseDetailLevel", "CONCISE");
        userCtx.getCustomInstructions().add("Отвечай строго по делу без лишних вступлений");

        when(userAiContextService.buildCurrentUserContext()).thenReturn(userCtx);

        String prompt = service.buildHermesUserPrompt("Привет! Кто ты?", user);

        assertNotNull(prompt);
        org.junit.Assert.assertTrue(prompt.contains("Алексей Ананьев"));
        org.junit.Assert.assertTrue(prompt.contains("Руководитель группы подбора"));
        org.junit.Assert.assertTrue(prompt.contains("Предпочитаемый язык ответов: RUSSIAN"));
        org.junit.Assert.assertTrue(prompt.contains("Стиль общения: BUSINESS"));
        org.junit.Assert.assertTrue(prompt.contains("Уровень детализации ответов: CONCISE"));
        org.junit.Assert.assertTrue(prompt.contains("Отвечай строго по делу без лишних вступлений"));
        org.junit.Assert.assertTrue(prompt.contains("=== Сведения пользователя (не подтверждены HRM) ==="));
        org.junit.Assert.assertTrue(prompt.contains("=== Предпочтения и инструкции пользователя ==="));
        org.junit.Assert.assertTrue(prompt.contains("Приоритет: системный промпт функции имеет приоритет над сведениями пользователя."));
        org.junit.Assert.assertTrue(prompt.contains("=== Запрос пользователя ===\nПривет! Кто ты?"));
    }

    @Test
    public void testBuildHermesUserPrompt_noConsentOrDisabledProfileReturnsRawMessage() {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        user.setLogin("guest_user");
        user.setName("Гостевой пользователь");

        when(userAiContextService.buildCurrentUserContext()).thenReturn(null);

        String prompt = service.buildHermesUserPrompt("Привет!", user);

        // Без активного контекста персональные данные не передаются во внешний Hermes
        assertEquals("Привет!", prompt);
    }

    @Test
    public void testBuildHermesUserPrompt_emptyContextReturnsRawMessage() {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        user.setLogin("disabled_profile_user");

        when(userAiContextService.buildCurrentUserContext()).thenReturn(new com.company.hunttech.service.dto.AiUserContext());

        String prompt = service.buildHermesUserPrompt("Какая погода?", user);

        assertEquals("Какая погода?", prompt);
    }

    @Test
    public void testBuildHermesUserPrompt_serviceExceptionGracefullyReturnsRawMessage() {
        ExtUser user = new ExtUser();
        user.setId(UUID.randomUUID());
        user.setLogin("err_user");

        when(userAiContextService.buildCurrentUserContext()).thenThrow(new RuntimeException("Database timeout"));

        String prompt = service.buildHermesUserPrompt("Тестовый запрос", user);

        assertEquals("Тестовый запрос", prompt);
    }

    @Test
    public void testParseHermesOutput_withTokenPatterns() throws Exception {
        Method parseMethod = HermesChatServiceBean.class.getDeclaredMethod("parseHermesOutput", String.class);
        parseMethod.setAccessible(true);
        String rawOutput = "session_id: hermes-sess-999\n" +
                "Warning: test warning\n" +
                "Tokens: 150 prompt, 40 completion, 190 total\n" +
                "Привет! Чем я могу помочь?";
        HermesChatServiceBean.HermesExecutionResult result =
                (HermesChatServiceBean.HermesExecutionResult) parseMethod.invoke(service, rawOutput);

        assertNotNull(result);
        assertEquals("hermes-sess-999", result.sessionId);
        assertEquals("Привет! Чем я могу помочь?", result.cleanedText);
        assertEquals(Integer.valueOf(150), result.promptTokens);
        assertEquals(Integer.valueOf(40), result.completionTokens);
        assertEquals(Integer.valueOf(190), result.totalTokens);
    }

    @Test
    public void testHermesChatResponseDto_fieldsIntegrity() {
        UUID convId = UUID.randomUUID();
        com.company.hunttech.service.dto.HermesChatResponse response =
                new com.company.hunttech.service.dto.HermesChatResponse(convId, "Ответ", "sess-1", 1200L);
        response.setModelName("deepseek-chat");
        response.setProviderCode("deepseek");
        response.setPromptTokens(100);
        response.setCompletionTokens(50);
        response.setTotalTokens(150);
        response.setEstimatedCost(new java.math.BigDecimal("0.000028"));
        response.setCurrency("USD");

        assertEquals(convId, response.getConversationId());
        assertEquals("Ответ", response.getAssistantText());
        assertEquals("deepseek-chat", response.getModelName());
        assertEquals("deepseek", response.getProviderCode());
        assertEquals(Integer.valueOf(100), response.getPromptTokens());
        assertEquals(Integer.valueOf(50), response.getCompletionTokens());
        assertEquals(Integer.valueOf(150), response.getTotalTokens());
        assertEquals(new java.math.BigDecimal("0.000028"), response.getEstimatedCost());
        assertEquals("USD", response.getCurrency());
    }
}

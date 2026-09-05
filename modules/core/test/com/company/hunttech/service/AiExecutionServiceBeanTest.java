package com.company.hunttech.service;

import com.company.hunttech.core.ai.AIProvider;
import com.company.hunttech.core.ai.AIProviderRegistry;
import com.company.hunttech.core.ai.AiSecretService;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.UserAiProfile;
import com.company.hunttech.service.AiConsentPolicy;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.entity.ai.AiCallLog;
import com.company.hunttech.entity.ai.AiCapability;
import com.company.hunttech.entity.ai.AiExecutionPolicy;
import com.company.hunttech.entity.ai.AiFallbackPolicy;
import com.company.hunttech.entity.ai.AiFunctionConfiguration;
import com.company.hunttech.entity.ai.UserAiFunctionOverride;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.LoadContext;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.entity.User;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Сценарии 1–10 плана персонализации (docs/architecture/SettingWindow_AboutMe_AI_Personalization_Plan.md §9.1):
 * гейты контекста, приоритет промпта функции, IMAGE-изоляция, лимиты, аудит AiCallLog.
 */
public class AiExecutionServiceBeanTest {

    private static final String FUNCTION_CODE = "VACANCY_TEXT";
    private static final String BASE_SYSTEM_PROMPT = "Ты — ассистент рекрутера. Пиши вакансии.";
    private static final String CONTEXT_MARKER = "=== Сведения пользователя (не подтверждены HRM) ===";
    private static final String INSTRUCTIONS_MARKER = "=== Предпочтения и инструкции пользователя ===";

    private AiExecutionServiceBean service;
    private DataManager dataManager;
    private Metadata metadata;
    private UserSessionSource userSessionSource;
    private AIProviderRegistry providerRegistry;
    private AiSecretService aiSecretService;
    private UserAiContextService userAiContextService;
    private AIProvider provider;

    private AiFunctionConfiguration function;
    private User user;

    @Before
    public void setUp() {
        dataManager = mock(DataManager.class);
        metadata = mock(Metadata.class);
        userSessionSource = mock(UserSessionSource.class);
        providerRegistry = mock(AIProviderRegistry.class);
        aiSecretService = mock(AiSecretService.class);
        userAiContextService = mock(UserAiContextService.class);
        provider = mock(AIProvider.class);

        service = new AiExecutionServiceBean();
        org.springframework.test.util.ReflectionTestUtils.setField(service, "dataManager", dataManager);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "metadata", metadata);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "userSessionSource", userSessionSource);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "aiProviderRegistry", providerRegistry);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "aiSecretService", aiSecretService);
        org.springframework.test.util.ReflectionTestUtils.setField(service, "userAiContextService", userAiContextService);

        function = new AiFunctionConfiguration();
        function.setCode(FUNCTION_CODE);
        function.setName("Тестовая функция");
        function.setCapability(AiCapability.TEXT_GENERATION);
        function.setExecutionPolicy(AiExecutionPolicy.ADMIN_ONLY);
        function.setFallbackPolicy(AiFallbackPolicy.NO_FALLBACK);
        function.setPromptTemplate("Опиши вакансию ${vacancyName}");
        function.setSystemPrompt(BASE_SYSTEM_PROMPT);
        function.setIncludeUserContext(true);
        function.setPrivacyPolicyVersion(AiConsentPolicy.LLM_CHAT_PRIVACY_POLICY_VERSION);

        AdminAiConfiguration admin = new AdminAiConfiguration();
        admin.setProviderCode("openai");
        admin.setDefaultModelName("gpt-test");
        admin.setActive(true);
        admin.setApiKeyEncrypted("secret");
        function.setAdminConfiguration(admin);

        user = new User();
        com.haulmont.cuba.security.global.UserSession userSession =
                mock(com.haulmont.cuba.security.global.UserSession.class);
        when(userSession.getUser()).thenReturn(user);
        when(userSessionSource.getUserSession()).thenReturn(userSession);
        when(dataManager.load(AiFunctionConfiguration.class))
                .thenReturn(mock(FluentLoader.class, org.mockito.Mockito.RETURNS_DEEP_STUBS));
        FluentLoader deepLoader = (FluentLoader) dataManager.load(AiFunctionConfiguration.class);
        when(deepLoader.query(anyString()).parameter(anyString(), any()).view(anyString()).optional())
                .thenReturn(java.util.Optional.of(function));
        when(dataManager.load(UserAiFunctionOverride.class))
                .thenReturn(mock(FluentLoader.class, org.mockito.Mockito.RETURNS_DEEP_STUBS));
        FluentLoader overrideLoader = (FluentLoader) dataManager.load(UserAiFunctionOverride.class);
        when(overrideLoader.query(anyString()).parameter(anyString(), any()).parameter(anyString(), any())
                .view(anyString()).optional())
                .thenReturn(java.util.Optional.empty());
        when(dataManager.loadList(any(LoadContext.class)))
                .thenReturn(Collections.singletonList(function));
        when(providerRegistry.getProvider("openai")).thenReturn(provider);
        when(aiSecretService.decrypt("secret")).thenReturn("plain-key");
        when(provider.executeTextWithTokens(anyString(), anyString(), anyString(), anyString(), any()))
                .thenReturn(com.company.hunttech.core.ai.AiProviderResponse.ofText(
                        "Сгенерированный текст вакансии", 100, 50, 150));
        when(metadata.create(AiCallLog.class)).thenAnswer(inv -> new AiCallLog());
    }

    private void stubProfileContext(com.company.hunttech.service.dto.AiUserContext context) {
        when(userAiContextService.buildCurrentUserContext()).thenReturn(context);
    }

    private com.company.hunttech.service.dto.AiUserContext activeContext() {
        com.company.hunttech.service.dto.AiUserContext ctx = new com.company.hunttech.service.dto.AiUserContext();
        ctx.setActive(true);
        ctx.getProfileData().put("currentPosition", "Руководитель отдела рекрутинга");
        ctx.getProfileData().put("hiringGeographies", "Россия, СНГ");
        ctx.getCustomInstructions().add("Отвечай кратко и по делу");
        return ctx;
    }

    private String executedSystemPrompt() {
        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        verify(provider).executeTextWithTokens(anyString(), systemCaptor.capture(), anyString(), anyString(), any());
        return systemCaptor.getValue();
    }

    @Test
    public void scenario1_activeProfileAndFlagTrue_appendsMarkedContextBlock() {
        stubProfileContext(activeContext());

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        String systemPrompt = executedSystemPrompt();
        assertTrue(systemPrompt.startsWith(BASE_SYSTEM_PROMPT));
        assertTrue(systemPrompt.contains(CONTEXT_MARKER));
        assertTrue(systemPrompt.contains("currentPosition: Руководитель отдела рекрутинга"));
        assertTrue(systemPrompt.contains(INSTRUCTIONS_MARKER));
        assertTrue(systemPrompt.contains("- Отвечай кратко и по делу"));
        assertTrue(systemPrompt.contains("Приоритет: системный промпт функции имеет приоритет"));
    }

    @Test
    public void scenario2_disabledProfile_keepsOriginalSystemPrompt() {
        stubProfileContext(new com.company.hunttech.service.dto.AiUserContext());

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        assertEquals(BASE_SYSTEM_PROMPT, executedSystemPrompt());
    }

    @Test
    public void scenario3_noConsent_keepsOriginalSystemPrompt() {
        // Пустой контекст возвращается и при отсутствии согласия (гейт в UserAiContextBuilder).
        stubProfileContext(new com.company.hunttech.service.dto.AiUserContext());

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        assertEquals(BASE_SYSTEM_PROMPT, executedSystemPrompt());
    }

    @Test
    public void scenario4_imageFunction_neverReceivesUserContext() {
        function.setCapability(AiCapability.IMAGE_GENERATION);
        stubProfileContext(activeContext());

        byte[] imageBytes = new byte[]{1, 2, 3};
        when(provider.generateImage(anyString(), anyString(), anyString(), anyString(), any(), any(), anyString()))
                .thenReturn(imageBytes);

        service.executeImage("LOGO_FUNCTION", Collections.singletonMap("vacancyName", "Java"),
                imageBytes, "image/png");

        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        verify(provider).generateImage(anyString(), systemCaptor.capture(), anyString(), anyString(), any(), any(), anyString());
        assertEquals(BASE_SYSTEM_PROMPT, systemCaptor.getValue());
    }

    @Test
    public void scenario5_activeButEmptyProfile_keepsOriginalSystemPrompt() {
        com.company.hunttech.service.dto.AiUserContext ctx = new com.company.hunttech.service.dto.AiUserContext();
        ctx.setActive(true);
        stubProfileContext(ctx);

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        assertEquals(BASE_SYSTEM_PROMPT, executedSystemPrompt());
    }

    @Test
    public void scenario6_flagFalse_keepsOriginalSystemPromptEvenWithActiveProfile() {
        function.setIncludeUserContext(false);
        stubProfileContext(activeContext());

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        assertEquals(BASE_SYSTEM_PROMPT, executedSystemPrompt());
        // Контекст вообще не запрашивался: лишнего JPQL нет.
        verify(userAiContextService, never()).buildCurrentUserContext();
    }

    @Test
    public void scenario7_limitBoundsContextBlock() {
        // Лимит применяется в UserAiContextBuilder; проверяем сквозной сценарий:
        // контекст, построенный с бюджетом, даёт блок, уложившийся в жёсткий предел.
        UserAiProfile profile = new UserAiProfile();
        profile.setProfileEnabled(true);
        profile.setExternalProcessingAllowed(true);
        profile.setCurrentPosition("Руководитель отдела рекрутинга");
        profile.setCustomAiInstructions("Отвечай кратко и по делу");
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            big.append("слово ");
        }
        profile.setAboutMe(big.toString());
        stubProfileContext(UserAiContextBuilder.buildContext(profile, 4000));

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        String systemPrompt = executedSystemPrompt();
        int blockCodePoints = systemPrompt.codePointCount(0, systemPrompt.length())
                - BASE_SYSTEM_PROMPT.codePointCount(0, BASE_SYSTEM_PROMPT.length());
        // Блок обязан уложиться в жёсткий верхний предел контекста (16000).
        assertTrue(blockCodePoints <= 16000);
        assertTrue(systemPrompt.contains(CONTEXT_MARKER));
    }

    @Test
    public void scenario8_markersAndOrder_functionPromptFirstContextAfter() {
        stubProfileContext(activeContext());

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        String systemPrompt = executedSystemPrompt();
        assertTrue(systemPrompt.indexOf(BASE_SYSTEM_PROMPT) < systemPrompt.indexOf(CONTEXT_MARKER));
        assertTrue(systemPrompt.indexOf(CONTEXT_MARKER) < systemPrompt.indexOf(INSTRUCTIONS_MARKER));
    }

    @Test
    public void scenario9_nullFlag_resolvesByCapability() {
        function.setIncludeUserContext(null);
        stubProfileContext(activeContext());

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        // TEXT_GENERATION при NULL-флаге → контекст включается.
        assertTrue(executedSystemPrompt().contains(CONTEXT_MARKER));
    }

    @Test
    public void scenario10_auditFields_recordedOnSuccess() {
        stubProfileContext(activeContext());

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        ArgumentCaptor<CommitContext> commitCaptor = ArgumentCaptor.forClass(CommitContext.class);
        verify(dataManager).commit(commitCaptor.capture());
        AiCallLog callLog = (AiCallLog) commitCaptor.getValue().getCommitInstances().iterator().next();
        assertEquals(Boolean.TRUE, callLog.getContextIncluded());
        assertTrue(callLog.getContextCodePoints() != null && callLog.getContextCodePoints() > 0);
        assertNull(callLog.getPromptText());
        assertNull(callLog.getResponseText());
        assertEquals(AiConsentPolicy.LLM_CHAT_PRIVACY_POLICY_VERSION,
                callLog.getPrivacyPolicyVersionSnapshot());
    }

    @Test
    public void scenario10b_noContext_recordsNullAuditFlags() {
        function.setIncludeUserContext(false);

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        ArgumentCaptor<CommitContext> commitCaptor = ArgumentCaptor.forClass(CommitContext.class);
        verify(dataManager).commit(commitCaptor.capture());
        AiCallLog callLog = (AiCallLog) commitCaptor.getValue().getCommitInstances().iterator().next();
        // Флаг оценивался, но контекст не передавался: false, размер не фиксируется.
        assertEquals(Boolean.FALSE, callLog.getContextIncluded());
        assertNull(callLog.getContextCodePoints());
    }

    @Test
    public void contextBuildFailure_degradesToOriginalPrompt() {
        when(userAiContextService.buildCurrentUserContext()).thenThrow(new RuntimeException("db down"));

        service.executeText(FUNCTION_CODE, Collections.singletonMap("vacancyName", "Java"));

        assertEquals(BASE_SYSTEM_PROMPT, executedSystemPrompt());
    }

    @Test
    public void llmChat_prefersPersonalApiAndDoesNotCallAdminApi() {
        function.setCode("LLM_CHAT");
        function.setPromptTemplate("${message}");
        function.setExecutionPolicy(AiExecutionPolicy.USER_OVERRIDE_ALLOWED);
        function.setFallbackPolicy(AiFallbackPolicy.NO_FALLBACK);

        UserAiConfiguration personal = new UserAiConfiguration();
        personal.setUser(user);
        personal.setProviderCode("personal-provider");
        personal.setApiKeyEncrypted("personal-secret");
        personal.setDefaultModelName("personal-model");
        personal.setIsActive(true);
        UserAiFunctionOverride override = new UserAiFunctionOverride();
        override.setUser(user);
        override.setAiFunction(function);
        override.setUserAiConfiguration(personal);
        override.setEnabled(true);

        FluentLoader overrideLoader = (FluentLoader) dataManager.load(UserAiFunctionOverride.class);
        when(overrideLoader.query(anyString()).parameter(anyString(), any()).parameter(anyString(), any())
                .view(anyString()).optional())
                .thenReturn(java.util.Optional.of(override));
        when(providerRegistry.getProvider("personal-provider")).thenReturn(provider);
        when(aiSecretService.decrypt("personal-secret")).thenReturn("personal-key");

        AiExecutionResult result = service.executeText("LLM_CHAT", Collections.singletonMap("message", "Привет"));

        assertEquals(AiCredentialOwner.USER, result.getCredentialOwner());
        verify(provider).executeTextWithTokens(anyString(), anyString(), eq("personal-key"), eq("personal-model"), any());
        verify(aiSecretService, never()).decrypt("secret");
    }

    @Test
    public void llmChatWithoutPrivacyPolicyDoesNotCallProvider() {
        function.setCode("LLM_CHAT");
        function.setPrivacyPolicyVersion(null);
        function.setPromptTemplate("${message}");

        boolean rejected = false;
        try {
            service.executeText("LLM_CHAT", Collections.singletonMap("message", "Привет"));
        } catch (DevelopmentException expected) {
            rejected = true;
            assertTrue(expected.getMessage().contains("privacy policy"));
        }

        assertTrue("LLM_CHAT must fail closed without privacy policy version", rejected);
        verify(provider, never()).executeTextWithTokens(anyString(), anyString(), anyString(), anyString(), any());
    }

    @Test
    public void llmChat_fallsBackToAdminOnlyAfterPersonalFailureAndConsent() {
        function.setCode("LLM_CHAT");
        function.setPromptTemplate("${message}");
        function.setExecutionPolicy(AiExecutionPolicy.USER_OVERRIDE_ALLOWED);
        function.setFallbackPolicy(AiFallbackPolicy.FALLBACK_TO_ADMIN);

        UserAiConfiguration personal = new UserAiConfiguration();
        personal.setUser(user);
        personal.setProviderCode("personal-provider");
        personal.setApiKeyEncrypted("personal-secret");
        personal.setDefaultModelName("personal-model");
        personal.setIsActive(true);
        UserAiFunctionOverride override = new UserAiFunctionOverride();
        override.setUser(user);
        override.setAiFunction(function);
        override.setUserAiConfiguration(personal);
        override.setEnabled(true);
        stubOverride(override);

        UserAiProfile profile = new UserAiProfile();
        profile.setAdminFallbackConsent(true);
        profile.setAdminFallbackConsentVersion(AiConsentPolicy.ADMIN_FALLBACK_VERSION);
        profile.setAdminFallbackConsentAt(new java.util.Date());
        stubProfile(profile);
        AdminAiConfiguration admin = function.getAdminConfiguration();
        admin.setProviderCode("admin-provider");
        admin.setApiKeyEncrypted("admin-secret");
        function.setAdminConfiguration(admin);

        AIProvider adminProvider = mock(AIProvider.class);
        when(providerRegistry.getProvider("personal-provider")).thenReturn(provider);
        when(providerRegistry.getProvider("admin-provider")).thenReturn(adminProvider);
        when(aiSecretService.decrypt("personal-secret")).thenReturn("personal-key");
        when(aiSecretService.decrypt("admin-secret")).thenReturn("admin-key");
        when(provider.executeTextWithTokens(anyString(), anyString(), eq("personal-key"), eq("personal-model"), any()))
                .thenThrow(new RuntimeException("personal provider unavailable"));
        when(adminProvider.executeTextWithTokens(anyString(), anyString(), eq("admin-key"), eq("gpt-test"), any()))
                .thenReturn(com.company.hunttech.core.ai.AiProviderResponse.ofText("admin response", 10, 5, 15));

        AiExecutionResult result = service.executeText("LLM_CHAT", Collections.singletonMap("message", "Привет"));

        assertEquals(AiCredentialOwner.ADMIN, result.getCredentialOwner());
        verify(provider).executeTextWithTokens(anyString(), anyString(), eq("personal-key"), eq("personal-model"), any());
        verify(adminProvider).executeTextWithTokens(anyString(), anyString(), eq("admin-key"), eq("gpt-test"), any());
    }

    @Test
    public void llmChat_doesNotFallbackWithoutSeparateConsent() {
        function.setCode("LLM_CHAT");
        function.setPromptTemplate("${message}");
        function.setExecutionPolicy(AiExecutionPolicy.USER_OVERRIDE_ALLOWED);
        function.setFallbackPolicy(AiFallbackPolicy.FALLBACK_TO_ADMIN);

        UserAiConfiguration personal = new UserAiConfiguration();
        personal.setUser(user);
        personal.setProviderCode("personal-provider");
        personal.setApiKeyEncrypted("personal-secret");
        personal.setDefaultModelName("personal-model");
        personal.setIsActive(true);
        UserAiFunctionOverride override = new UserAiFunctionOverride();
        override.setUser(user);
        override.setAiFunction(function);
        override.setUserAiConfiguration(personal);
        override.setEnabled(true);
        stubOverride(override);
        UserAiProfile profile = new UserAiProfile();
        profile.setAdminFallbackConsent(false);
        stubProfile(profile);

        when(providerRegistry.getProvider("personal-provider")).thenReturn(provider);
        when(aiSecretService.decrypt("personal-secret")).thenReturn("personal-key");
        when(provider.executeTextWithTokens(anyString(), anyString(), eq("personal-key"), eq("personal-model"), any()))
                .thenThrow(new RuntimeException("personal provider unavailable"));

        boolean rejected = false;
        try {
            service.executeText("LLM_CHAT", Collections.singletonMap("message", "Привет"));
        } catch (DevelopmentException expected) {
            rejected = true;
            assertTrue(expected.getMessage().contains("отдельное согласие"));
        }
        assertTrue("Admin fallback must require separate consent", rejected);
        verify(provider).executeTextWithTokens(anyString(), anyString(), eq("personal-key"), eq("personal-model"), any());
        verify(providerRegistry, never()).getProvider("admin-provider");
    }

    private void stubOverride(UserAiFunctionOverride override) {
        FluentLoader overrideLoader = (FluentLoader) dataManager.load(UserAiFunctionOverride.class);
        when(overrideLoader.query(anyString()).parameter(anyString(), any()).parameter(anyString(), any())
                .view(anyString()).optional())
                .thenReturn(java.util.Optional.of(override));
    }

    private void stubProfile(UserAiProfile profile) {
        FluentLoader profileLoader = mock(FluentLoader.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
        when(dataManager.load(UserAiProfile.class)).thenReturn(profileLoader);
        when(profileLoader.query(anyString()).parameter(anyString(), any()).view(anyString()).optional())
                .thenReturn(java.util.Optional.of(profile));
    }
}

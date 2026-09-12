package com.company.hunttech.service;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.UserAiConfiguration;
import com.company.hunttech.entity.ai.AdminAiConfiguration;
import com.company.hunttech.entity.ai.LlmChatConversation;
import com.company.hunttech.entity.ai.LlmChatMessage;
import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LlmChatServiceBeanTest {

    @InjectMocks
    private LlmChatServiceBean service;

    @Mock
    private DataManager dataManager;

    private ExtUser user;
    private LlmChatConversation conversation;

    @Before
    public void setUp() {
        user = new ExtUser();
        user.setId(UUID.randomUUID());
        user.setLogin("testUser");

        conversation = new LlmChatConversation();
        conversation.setId(UUID.randomUUID());
        conversation.setUser(user);
        conversation.setStatus("ACTIVE");
    }

    @SuppressWarnings("unchecked")
    private <T extends Entity<UUID>> FluentLoader.ByQuery<T, UUID> mockQueryLoader(Class<T> entityClass) {
        FluentLoader<T, UUID> fluentLoader = mock(FluentLoader.class);
        FluentLoader.ByQuery<T, UUID> queryLoader = mock(FluentLoader.ByQuery.class);
        when(dataManager.load(entityClass)).thenReturn(fluentLoader);
        when(fluentLoader.query(anyString())).thenReturn(queryLoader);
        when(queryLoader.parameter(anyString(), any())).thenReturn(queryLoader);
        when(queryLoader.view(anyString())).thenReturn(queryLoader);
        return queryLoader;
    }

    @Test
    public void testEstimateTokens() {
        assertEquals(0, LlmChatServiceBean.estimateTokens(null));
        assertEquals(0, LlmChatServiceBean.estimateTokens(""));
        assertTrue(LlmChatServiceBean.estimateTokens("Привет") > 0);
        assertTrue(LlmChatServiceBean.estimateTokens("Длинный вопрос о кандидатах на позицию Java Developer") > 5);
    }

    @Test
    public void testResolveEffectiveMaxContextTokens_FromUserConfig() {
        UserAiConfiguration userConfig = new UserAiConfiguration();
        userConfig.setMaxContextTokens(5000);

        FluentLoader.ByQuery<UserAiConfiguration, UUID> queryLoader = mockQueryLoader(UserAiConfiguration.class);
        when(queryLoader.list()).thenReturn(Collections.singletonList(userConfig));

        int limit = service.resolveEffectiveMaxContextTokens(user);
        assertEquals(5000, limit);
    }

    @Test
    public void testResolveEffectiveMaxContextTokens_CapsAt10000() {
        UserAiConfiguration userConfig = new UserAiConfiguration();
        userConfig.setMaxContextTokens(50000); // Превышает 10000

        FluentLoader.ByQuery<UserAiConfiguration, UUID> queryLoader = mockQueryLoader(UserAiConfiguration.class);
        when(queryLoader.list()).thenReturn(Collections.singletonList(userConfig));

        int limit = service.resolveEffectiveMaxContextTokens(user);
        assertEquals(10000, limit);
    }

    @Test
    public void testResolveEffectiveMaxContextTokens_FallsBackToAdminConfig() {
        // User config not found
        FluentLoader.ByQuery<UserAiConfiguration, UUID> userQueryLoader = mockQueryLoader(UserAiConfiguration.class);
        when(userQueryLoader.list()).thenReturn(Collections.emptyList());

        // Admin config found with 8000
        AdminAiConfiguration adminConfig = new AdminAiConfiguration();
        adminConfig.setMaxContextTokens(8000);

        FluentLoader.ByQuery<AdminAiConfiguration, UUID> adminQueryLoader = mockQueryLoader(AdminAiConfiguration.class);
        when(adminQueryLoader.list()).thenReturn(Collections.singletonList(adminConfig));

        int limit = service.resolveEffectiveMaxContextTokens(user);
        assertEquals(8000, limit);
    }

    @Test
    public void testResolveEffectiveMaxContextTokens_DefaultWhenNoConfigs() {
        FluentLoader.ByQuery<UserAiConfiguration, UUID> userQueryLoader = mockQueryLoader(UserAiConfiguration.class);
        when(userQueryLoader.list()).thenReturn(Collections.emptyList());

        FluentLoader.ByQuery<AdminAiConfiguration, UUID> adminQueryLoader = mockQueryLoader(AdminAiConfiguration.class);
        when(adminQueryLoader.list()).thenReturn(Collections.emptyList());

        int limit = service.resolveEffectiveMaxContextTokens(user);
        assertEquals(10000, limit);
    }

    @Test
    public void testBuildMessageWithHistory_EmptyHistory() {
        FluentLoader.ByQuery<LlmChatMessage, UUID> msgLoader = mockQueryLoader(LlmChatMessage.class);
        when(msgLoader.list()).thenReturn(Collections.emptyList());

        String result = service.buildMessageWithHistory(conversation, user, 1, "Привет");
        assertEquals("Привет", result);
    }

    @Test
    public void testBuildMessageWithHistory_WithHistoryWithinLimit() {
        LlmChatMessage msg1 = new LlmChatMessage();
        msg1.setRole("USER");
        msg1.setContent("Сколько было собеседований вчера?");
        msg1.setSequenceNo(1);

        LlmChatMessage msg2 = new LlmChatMessage();
        msg2.setRole("ASSISTANT");
        msg2.setContent("Вчера было 3 собеседования.");
        msg2.setSequenceNo(2);

        FluentLoader.ByQuery<LlmChatMessage, UUID> msgLoader = mockQueryLoader(LlmChatMessage.class);
        when(msgLoader.list()).thenReturn(Arrays.asList(msg1, msg2));

        String result = service.buildMessageWithHistory(conversation, user, 3, "А кто из них был аналитиком?");
        assertTrue(result.contains("[История диалога]"));
        assertTrue(result.contains("Пользователь: Сколько было собеседований вчера?"));
        assertTrue(result.contains("Ассистент: Вчера было 3 собеседования."));
        assertTrue(result.contains("[Текущий запрос пользователя]"));
        assertTrue(result.contains("А кто из них был аналитиком?"));
    }

    @Test
    public void testBuildMessageWithHistory_ExceedsLimit_ResetsContext() {
        // Mock user config with small limit (100 tokens)
        UserAiConfiguration userConfig = new UserAiConfiguration();
        userConfig.setMaxContextTokens(100);

        FluentLoader.ByQuery<UserAiConfiguration, UUID> userQueryLoader = mockQueryLoader(UserAiConfiguration.class);
        when(userQueryLoader.list()).thenReturn(Collections.singletonList(userConfig));

        // Create massive history that easily exceeds 100 tokens
        StringBuilder bigText = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            bigText.append("Очень длинный подробный текст ответа ассистента со множеством параметров, кандидатов и цифр. ");
        }

        LlmChatMessage msg1 = new LlmChatMessage();
        msg1.setRole("USER");
        msg1.setContent("Расскажи обо всех кандидатах подробно");
        msg1.setSequenceNo(1);

        LlmChatMessage msg2 = new LlmChatMessage();
        msg2.setRole("ASSISTANT");
        msg2.setContent(bigText.toString());
        msg2.setSequenceNo(2);

        FluentLoader.ByQuery<LlmChatMessage, UUID> msgLoader = mockQueryLoader(LlmChatMessage.class);
        when(msgLoader.list()).thenReturn(Arrays.asList(msg1, msg2));

        String result = service.buildMessageWithHistory(conversation, user, 3, "Новый вопрос");

        // Context reached limit, so it was reset/cleared!
        assertEquals("Новый вопрос", result);
        assertEquals(Integer.valueOf(3), conversation.getContextResetSequenceNo());
        verify(dataManager).commit(conversation);
    }
}

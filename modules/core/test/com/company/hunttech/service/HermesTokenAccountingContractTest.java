package com.company.hunttech.service;

import com.company.hunttech.config.HunttechHermesConfig;
import com.company.hunttech.config.HunttechHermesManagerConfig;
import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.ai.LlmChatConversation;
import com.company.hunttech.service.dto.HermesChatResponse;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.DevelopmentException;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.security.global.UserSession;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class HermesTokenAccountingContractTest {

    private static final String QUOTA_EXHAUSTED_MSG =
            "Закончились доступные токены ИИ. Пожалуйста, обратитесь к администратору системы для пополнения квоты.";

    private UserAiQuotaService userAiQuotaService;
    private ExtUser currentUser;
    private DataManager dataManager;
    private Metadata metadata;
    private UserSessionSource userSessionSource;
    private Configuration configuration;
    private Security security;

    @Before
    public void setUp() {
        userAiQuotaService = mock(UserAiQuotaService.class);
        dataManager = mock(DataManager.class);
        metadata = mock(Metadata.class);
        userSessionSource = mock(UserSessionSource.class);
        configuration = mock(Configuration.class);
        security = mock(Security.class);

        currentUser = new ExtUser();
        currentUser.setId(UUID.randomUUID());
        currentUser.setLogin("test_recruiter");
        currentUser.setName("Тестовый Рекрутер");

        UserSession userSession = mock(UserSession.class);
        when(userSession.getUser()).thenReturn(currentUser);
        when(userSessionSource.getUserSession()).thenReturn(userSession);
    }

    @Test
    public void testHermesViewer_blocksOnExhaustedQuota() {
        HermesChatServiceBean viewerService = new HermesChatServiceBean();
        HunttechHermesConfig hermesConfig = mock(HunttechHermesConfig.class);
        when(hermesConfig.getProfile()).thenReturn("hrm-viewer");
        when(configuration.getConfig(HunttechHermesConfig.class)).thenReturn(hermesConfig);

        ReflectionTestUtils.setField(viewerService, "userAiQuotaService", userAiQuotaService);
        ReflectionTestUtils.setField(viewerService, "dataManager", dataManager);
        ReflectionTestUtils.setField(viewerService, "metadata", metadata);
        ReflectionTestUtils.setField(viewerService, "userSessionSource", userSessionSource);
        ReflectionTestUtils.setField(viewerService, "configuration", configuration);

        // Квота исчерпана
        doThrow(new DevelopmentException(QUOTA_EXHAUSTED_MSG))
                .when(userAiQuotaService).checkQuotaAvailable(eq(currentUser.getId()), anyInt());

        UUID convId = UUID.randomUUID();
        HermesChatResponse response = viewerService.sendHermesMessage(convId, "Привет, покажи кандидатов");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains(QUOTA_EXHAUSTED_MSG));

        // Проверяем, что checkQuotaAvailable вызывался
        verify(userAiQuotaService).checkQuotaAvailable(eq(currentUser.getId()), anyInt());
        // Проверяем, что списание не происходило
        verify(userAiQuotaService, never()).recordTokenConsumption(any(UUID.class), anyInt());
    }

    @Test
    public void testHermesOperator_blocksOnExhaustedQuota() {
        HermesManagerChatServiceBean operatorService = new HermesManagerChatServiceBean();
        HunttechHermesManagerConfig managerConfig = mock(HunttechHermesManagerConfig.class);
        when(managerConfig.getProfile()).thenReturn("hrm-operator");
        when(configuration.getConfig(HunttechHermesManagerConfig.class)).thenReturn(managerConfig);
        when(security.isSpecificPermitted(HermesManagerChatServiceBean.PERMISSION_MANAGER_HERMES_WRITE)).thenReturn(true);

        ReflectionTestUtils.setField(operatorService, "userAiQuotaService", userAiQuotaService);
        ReflectionTestUtils.setField(operatorService, "dataManager", dataManager);
        ReflectionTestUtils.setField(operatorService, "metadata", metadata);
        ReflectionTestUtils.setField(operatorService, "userSessionSource", userSessionSource);
        ReflectionTestUtils.setField(operatorService, "configuration", configuration);
        ReflectionTestUtils.setField(operatorService, "security", security);

        // Квота исчерпана
        doThrow(new DevelopmentException(QUOTA_EXHAUSTED_MSG))
                .when(userAiQuotaService).checkQuotaAvailable(eq(currentUser.getId()), anyInt());

        UUID convId = UUID.randomUUID();
        HermesChatResponse response = operatorService.sendManagerHermesMessage(convId, "Создай вакансию Java Developer");

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertTrue(response.getErrorMessage().contains(QUOTA_EXHAUSTED_MSG));

        // Проверяем, что checkQuotaAvailable вызывался
        verify(userAiQuotaService).checkQuotaAvailable(eq(currentUser.getId()), anyInt());
        // Проверяем, что списание не происходило
        verify(userAiQuotaService, never()).recordTokenConsumption(any(UUID.class), anyInt());
    }

    @Test
    public void testEstimateTokens() {
        assertEquals(0, HermesManagerChatServiceBean.estimateTokens(null));
        assertEquals(0, HermesManagerChatServiceBean.estimateTokens("   "));
        assertEquals(1, HermesManagerChatServiceBean.estimateTokens("Hi"));
        assertEquals(4, HermesManagerChatServiceBean.estimateTokens("Hello world text"));
    }
}

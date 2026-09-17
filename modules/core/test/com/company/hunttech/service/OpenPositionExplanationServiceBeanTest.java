package com.company.hunttech.service;

import com.company.hunttech.config.HunttechHermesConfig;
import com.company.hunttech.entity.Grade;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.OpenPositionSkill;
import com.company.hunttech.entity.Project;
import com.company.hunttech.entity.ai.AiCapability;
import com.company.hunttech.entity.ai.OpenPositionAiExplanationLog;
import com.company.hunttech.service.dto.OpenPositionExplanationResult;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.core.global.View;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Юнит-тестирование OpenPositionExplanationServiceBean:
 * 1. Проверка стандартного разбора требований через AiExecutionService.
 * 2. Проверка житейского объяснения через Hermes hrm-viewer.
 * 3. Проверка создания записей аудита OpenPositionAiExplanationLog.
 */
public class OpenPositionExplanationServiceBeanTest {

    private OpenPositionExplanationServiceBean service;
    private DataManager dataManager;
    private Metadata metadata;
    private UserSessionSource userSessionSource;
    private AiExecutionService aiExecutionService;
    private Configuration configuration;
    private HunttechHermesConfig hermesConfig;

    private OpenPosition testPosition;
    private UUID testPositionId;
    private User testUser;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() {
        service = new OpenPositionExplanationServiceBean();
        dataManager = mock(DataManager.class);
        metadata = mock(Metadata.class);
        userSessionSource = mock(UserSessionSource.class);
        aiExecutionService = mock(AiExecutionService.class);
        configuration = mock(Configuration.class);
        hermesConfig = mock(HunttechHermesConfig.class);

        ReflectionTestUtils.setField(service, "dataManager", dataManager);
        ReflectionTestUtils.setField(service, "metadata", metadata);
        ReflectionTestUtils.setField(service, "userSessionSource", userSessionSource);
        ReflectionTestUtils.setField(service, "aiExecutionService", aiExecutionService);
        ReflectionTestUtils.setField(service, "configuration", configuration);

        when(configuration.getConfig(HunttechHermesConfig.class)).thenReturn(hermesConfig);
        when(hermesConfig.getViewerContainerName()).thenReturn("hermes-hrm-viewer");
        when(hermesConfig.getViewerProfile()).thenReturn("hrm-viewer");
        when(hermesConfig.getViewerTimeoutSeconds()).thenReturn(60);
        when(hermesConfig.getSshEnabled()).thenReturn(false);

        testPositionId = UUID.randomUUID();
        testPosition = new OpenPosition();
        testPosition.setId(testPositionId);
        testPosition.setVacansyName("Senior Java Разработчик");
        testPosition.setComment("Разработка высоконагруженных микросервисов на Spring Boot, Kafka, PostgreSQL.");
        testPosition.setSearchMap("Ключевые слова: Java 17, Spring Boot 3, Kafka, Docker. Доноры: Тинькофф, Сбер.");
        testPosition.setInterviewChecklist("Must-have: Опыт от 5 лет, знание JVM internals, многопоточность.");

        Grade grade = new Grade();
        grade.setGradeName("Senior");
        testPosition.setGrade(grade);

        Project project = new Project();
        project.setProjectName("Банковский Процессинг");
        testPosition.setProjectName(project);

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setLogin("alan");
        testUser.setName("Alan Ananyev");

        UserSession userSession = mock(UserSession.class);
        when(userSession.getUser()).thenReturn(testUser);
        when(userSessionSource.checkCurrentUserSession()).thenReturn(true);
        when(userSessionSource.getUserSession()).thenReturn(userSession);

        FluentLoader.ById<OpenPosition, UUID> byIdLoader = mock(FluentLoader.ById.class);
        FluentLoader<OpenPosition, UUID> entityLoader = mock(FluentLoader.class);
        when(dataManager.load(OpenPosition.class)).thenReturn(entityLoader);
        when(entityLoader.id(testPositionId)).thenReturn(byIdLoader);
        when(byIdLoader.view(anyString())).thenReturn(byIdLoader);
        when(byIdLoader.one()).thenReturn(testPosition);

        FluentLoader.ByQuery<OpenPositionSkill, UUID> skillQueryLoader = mock(FluentLoader.ByQuery.class);
        FluentLoader<OpenPositionSkill, UUID> posSkillLoader = mock(FluentLoader.class);
        when(dataManager.load(OpenPositionSkill.class)).thenReturn(posSkillLoader);
        when(posSkillLoader.query(anyString())).thenReturn(skillQueryLoader);
        when(skillQueryLoader.parameter(anyString(), any())).thenReturn(skillQueryLoader);
        when(skillQueryLoader.view(anyString())).thenReturn(skillQueryLoader);
        when(skillQueryLoader.list()).thenReturn(Collections.emptyList());

        when(metadata.create(OpenPositionAiExplanationLog.class)).thenAnswer(inv -> {
            OpenPositionAiExplanationLog log = new OpenPositionAiExplanationLog();
            log.setId(UUID.randomUUID());
            return log;
        });

        when(dataManager.commit(any(OpenPositionAiExplanationLog.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    public void testExplainRequirementsSuccess() {
        AiExecutionResult mockAiResult = AiExecutionResult.textResult(
                "VACANCY_EXPLAIN_REQUIREMENTS",
                "Умный анализ и объяснение требований вакансии",
                AiCapability.TEXT_GENERATION,
                "deepseek/deepseek-chat",
                "deepseek",
                AiCredentialOwner.ADMIN,
                "Суть вакансии: разработка ядра платежной системы на Java 17.",
                500,
                300,
                800,
                "req-123"
        );

        when(aiExecutionService.executeText(eq("VACANCY_EXPLAIN_REQUIREMENTS"), anyMap()))
                .thenReturn(mockAiResult);

        OpenPositionExplanationResult result = service.explainRequirements(testPositionId);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals("STANDARD", result.getExplanationType());
        assertEquals("AI_EXECUTION_SERVICE", result.getServiceType());
        assertEquals("deepseek/deepseek-chat", result.getModelName());
        assertEquals("deepseek", result.getProviderCode());
        assertEquals("Суть вакансии: разработка ядра платежной системы на Java 17.", result.getExplanationText());
        assertEquals(Integer.valueOf(800), result.getTotalTokens());
        assertNotNull(result.getLogId());

        verify(aiExecutionService).executeText(eq("VACANCY_EXPLAIN_REQUIREMENTS"), anyMap());
        verify(dataManager).commit(any(OpenPositionAiExplanationLog.class));
    }

    @Test
    public void testExplainRequirementsNotFound() {
        UUID unknownId = UUID.randomUUID();
        FluentLoader.ById<OpenPosition, UUID> missingByIdLoader = mock(FluentLoader.ById.class);
        FluentLoader<OpenPosition, UUID> missingLoader = mock(FluentLoader.class);
        when(dataManager.load(OpenPosition.class)).thenReturn(missingLoader);
        when(missingLoader.id(unknownId)).thenReturn(missingByIdLoader);
        when(missingByIdLoader.view(anyString())).thenReturn(missingByIdLoader);
        when(missingByIdLoader.one()).thenThrow(new IllegalStateException("Entity not found"));

        OpenPositionExplanationResult result = service.explainRequirements(unknownId);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertEquals("Вакансия не найдена", result.getErrorMessage());
    }

    @Test
    public void testBuildHermesSimplifiedPromptContainsKeywords() {
        Map<String, Object> context = (Map<String, Object>) ReflectionTestUtils.invokeMethod(
                service, "buildExplanationContext", testPosition, "Предыдущий черновой текст"
        );

        assertNotNull(context);
        assertEquals("Senior Java Разработчик", context.get("vacancyName"));
        assertEquals("Senior", context.get("grade"));
        assertEquals("Банковский Процессинг", context.get("projectName"));
        assertEquals("Предыдущий черновой текст", context.get("previousExplanation"));

        String prompt = (String) ReflectionTestUtils.invokeMethod(service, "buildHermesSimplifiedPrompt", context);
        assertNotNull(prompt);
        assertTrue(prompt.contains("Житейская аналогия роли"));
        assertTrue(prompt.contains("Технологический стек на пальцах"));
        assertTrue(prompt.contains("Контекст из индустрии и интернета"));
        assertTrue(prompt.contains("Памятка рекрутеру на скрининге"));
        assertTrue(prompt.contains("Senior Java Разработчик"));
        assertTrue(prompt.contains("Предыдущий черновой текст"));
    }
}

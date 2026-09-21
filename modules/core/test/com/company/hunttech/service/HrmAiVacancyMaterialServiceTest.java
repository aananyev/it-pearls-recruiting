package com.company.hunttech.service;

import com.company.hunttech.entity.ai.AiCapability;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HrmAiVacancyMaterialServiceTest {

    private HrmAiServiceBean service;
    private AiExecutionService aiExecutionService;

    @Before
    public void setUp() throws Exception {
        service = new HrmAiServiceBean();
        aiExecutionService = mock(AiExecutionService.class);
        Field field = HrmAiServiceBean.class.getDeclaredField("aiExecutionService");
        field.setAccessible(true);
        field.set(service, aiExecutionService);
    }

    @Test
    public void checklistUsesDedicatedFunctionAndReturnsExecutionMetadata() {
        assertMaterialRoute(VacancyMaterialType.CHECKLIST, HrmAiService.FUNCTION_VACANCY_CHECKLIST);
    }

    @Test
    public void searchMapUsesDedicatedFunctionAndReturnsExecutionMetadata() {
        assertMaterialRoute(VacancyMaterialType.SEARCH_MAP, HrmAiService.FUNCTION_VACANCY_SEARCH_MAP);
    }

    @Test
    public void interviewPlanUsesDedicatedFunctionAndReturnsExecutionMetadata() {
        assertMaterialRoute(VacancyMaterialType.INTERVIEW_PLAN, HrmAiService.FUNCTION_VACANCY_INTERVIEW_PLAN);
    }

    @Test
    public void smartVacancyCreationAlwaysUsesSameTypedServiceForAllMaterials() throws Exception {
        SmartOpenPositionIngestServiceBean smartService = new SmartOpenPositionIngestServiceBean();
        HrmAiService sharedService = mock(HrmAiService.class);
        Field field = SmartOpenPositionIngestServiceBean.class.getDeclaredField("hrmAiService");
        field.setAccessible(true);
        field.set(smartService, sharedService);

        SmartOpenPositionParsedData data = new SmartOpenPositionParsedData();
        data.setVacansyName("Java developer");
        data.setRawText("Java vacancy source");
        data.setComment("1. Роль " + "x".repeat(220));
        data.setCommentEn("x".repeat(120));
        data.setInterviewChecklist("parsed checklist " + "x".repeat(120));
        data.setSearchMap("parsed search map " + "x".repeat(120));
        data.setInterviewPlan("parsed interview plan " + "x".repeat(120));
        data.setTelegramPost("ready");
        when(sharedService.generateVacancyMaterial(eq(VacancyMaterialType.CHECKLIST), anyMap()))
                .thenReturn(AiExecutionResult.textResult(
                        HrmAiService.FUNCTION_VACANCY_CHECKLIST, "Checklist",
                        AiCapability.TEXT_GENERATION, "model", "provider",
                        AiCredentialOwner.ADMIN, "AI checklist"));
        when(sharedService.generateVacancyMaterial(eq(VacancyMaterialType.SEARCH_MAP), anyMap()))
                .thenReturn(aiResult(HrmAiService.FUNCTION_VACANCY_SEARCH_MAP, "AI search map"));
        when(sharedService.generateVacancyMaterial(eq(VacancyMaterialType.INTERVIEW_PLAN), anyMap()))
                .thenReturn(aiResult(HrmAiService.FUNCTION_VACANCY_INTERVIEW_PLAN, "AI interview plan"));

        Method method = SmartOpenPositionIngestServiceBean.class.getDeclaredMethod(
                "ensureFourArtifacts", SmartOpenPositionParsedData.class, String.class);
        method.setAccessible(true);
        method.invoke(smartService, data, data.getRawText());

        assertEquals("AI checklist", data.getInterviewChecklist());
        assertEquals("AI checklist", data.getExercise());
        assertEquals("AI search map", data.getSearchMap());
        assertEquals("AI search map", data.getMemoForInterview());
        assertEquals("AI interview plan", data.getInterviewPlan());
        assertEquals("AI interview plan", data.getTemplateLetter());
        verify(sharedService).generateVacancyMaterial(eq(VacancyMaterialType.CHECKLIST), anyMap());
        verify(sharedService).generateVacancyMaterial(eq(VacancyMaterialType.SEARCH_MAP), anyMap());
        verify(sharedService).generateVacancyMaterial(eq(VacancyMaterialType.INTERVIEW_PLAN), anyMap());
    }

    @Test
    public void smartVacancyCreationPreservesParsedMaterialsWhenDedicatedAiFails() throws Exception {
        SmartOpenPositionIngestServiceBean smartService = new SmartOpenPositionIngestServiceBean();
        HrmAiService sharedService = mock(HrmAiService.class);
        inject(smartService, "hrmAiService", sharedService);
        SmartOpenPositionParsedData data = completeParsedData();
        String parsedChecklist = data.getInterviewChecklist();
        String parsedSearchMap = data.getSearchMap();
        String parsedInterviewPlan = data.getInterviewPlan();
        when(sharedService.generateVacancyMaterial(org.mockito.ArgumentMatchers.any(), anyMap()))
                .thenThrow(new com.haulmont.cuba.core.global.DevelopmentException("missing prompt"));

        invokeEnsureArtifacts(smartService, data);

        assertEquals(parsedChecklist, data.getInterviewChecklist());
        assertEquals(parsedChecklist, data.getExercise());
        assertEquals(parsedSearchMap, data.getSearchMap());
        assertEquals(parsedSearchMap, data.getMemoForInterview());
        assertEquals(parsedInterviewPlan, data.getInterviewPlan());
        assertEquals(parsedInterviewPlan, data.getTemplateLetter());
        verify(sharedService, times(3)).generateVacancyMaterial(
                org.mockito.ArgumentMatchers.any(), anyMap());
    }

    @Test(expected = com.haulmont.cuba.core.global.DevelopmentException.class)
    public void missingDescriptionIsRejectedBeforeAiExecution() {
        service.generateVacancyMaterial(VacancyMaterialType.CHECKLIST, new LinkedHashMap<>());
    }

    @Test
    public void missingPromptExceptionIsPreservedForUiHandling() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("description", "Java vacancy");
        com.haulmont.cuba.core.global.DevelopmentException expected =
                new com.haulmont.cuba.core.global.DevelopmentException("missing prompt");
        when(aiExecutionService.executeText(HrmAiService.FUNCTION_VACANCY_CHECKLIST, context))
                .thenThrow(expected);

        try {
            service.generateVacancyMaterial(VacancyMaterialType.CHECKLIST, context);
            org.junit.Assert.fail("DevelopmentException expected");
        } catch (com.haulmont.cuba.core.global.DevelopmentException actual) {
            assertSame(expected, actual);
        }
    }

    private void assertMaterialRoute(VacancyMaterialType type, String expectedFunctionCode) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("description", "Java vacancy");
        AiExecutionResult expected = AiExecutionResult.textResult(
                expectedFunctionCode, type.name(), AiCapability.TEXT_GENERATION,
                "model", "provider", AiCredentialOwner.ADMIN, "generated");
        when(aiExecutionService.executeText(eq(expectedFunctionCode), eq(context))).thenReturn(expected);

        AiExecutionResult actual = service.generateVacancyMaterial(type, context);

        assertSame(expected, actual);
        assertEquals(expectedFunctionCode, actual.getFunctionCode());
        verify(aiExecutionService).executeText(expectedFunctionCode, context);
    }

    private AiExecutionResult aiResult(String functionCode, String text) {
        return AiExecutionResult.textResult(functionCode, functionCode,
                AiCapability.TEXT_GENERATION, "model", "provider", AiCredentialOwner.ADMIN, text);
    }

    private SmartOpenPositionParsedData completeParsedData() {
        SmartOpenPositionParsedData data = new SmartOpenPositionParsedData();
        data.setVacansyName("Java developer");
        data.setRawText("Java vacancy source");
        data.setComment("1. Роль " + "x".repeat(220));
        data.setCommentEn("x".repeat(120));
        data.setInterviewChecklist("parsed checklist " + "x".repeat(120));
        data.setSearchMap("parsed search map " + "x".repeat(120));
        data.setInterviewPlan("parsed interview plan " + "x".repeat(120));
        data.setTelegramPost("ready");
        return data;
    }

    private void invokeEnsureArtifacts(SmartOpenPositionIngestServiceBean service,
                                       SmartOpenPositionParsedData data) throws Exception {
        Method method = SmartOpenPositionIngestServiceBean.class.getDeclaredMethod(
                "ensureFourArtifacts", SmartOpenPositionParsedData.class, String.class);
        method.setAccessible(true);
        method.invoke(service, data, data.getRawText());
    }

    private void inject(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}

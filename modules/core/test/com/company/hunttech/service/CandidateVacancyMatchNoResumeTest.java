package com.company.hunttech.service;

import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.CandidateSkill;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.ai.AiCapability;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Regression coverage for the intended profile-and-skills-only candidate fallback. */
public class CandidateVacancyMatchNoResumeTest {

    private CandidateVacancyMatchAiServiceBean service;
    private DataManager dataManager;
    private AiExecutionService aiExecutionService;

    @Before
    public void setUp() {
        service = new CandidateVacancyMatchAiServiceBean();
        dataManager = mock(DataManager.class);
        aiExecutionService = mock(AiExecutionService.class);
        ReflectionTestUtils.setField(service, "dataManager", dataManager);
        ReflectionTestUtils.setField(service, "aiExecutionService", aiExecutionService);
    }

    @Test
    public void emptyResumeListProducesExplicitNoResumeContext() throws Exception {
        Method method = CandidateVacancyMatchAiServiceBean.class
                .getDeclaredMethod("buildCandidateResumeText", java.util.List.class);
        method.setAccessible(true);

        final String context;
        try {
            context = (String) method.invoke(new CandidateVacancyMatchAiServiceBean(), Collections.<CandidateCV>emptyList());
        } catch (InvocationTargetException e) {
            fail("An empty CV list must produce an explicit no-resume context, but threw " + e.getCause());
            return;
        }

        assertTrue("AI context must explicitly identify the absence of a readable resume",
                context.toLowerCase().contains("резюме") &&
                        (context.toLowerCase().contains("отсутствует") || context.toLowerCase().contains("не предоставлено")));
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void vacancyMatchDispatchesAiForCandidateWithoutReadableResume() {
        UUID vacancyId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();
        OpenPosition vacancy = new OpenPosition();
        vacancy.setId(vacancyId);
        vacancy.setOpenClose(false);
        vacancy.setVacansyName("Java Developer");

        JobCandidate candidate = new JobCandidate();
        candidate.setId(candidateId);
        candidate.setFullName("Resume-less test candidate");

        FluentLoader<OpenPosition, UUID> vacancyLoader = mock(FluentLoader.class);
        FluentLoader.ById<OpenPosition, UUID> vacancyById = mock(FluentLoader.ById.class);
        when(dataManager.load(OpenPosition.class)).thenReturn(vacancyLoader);
        when(vacancyLoader.id(vacancyId)).thenReturn(vacancyById);
        when(vacancyById.view(any(Consumer.class))).thenReturn(vacancyById);
        when(vacancyById.optional()).thenReturn(Optional.of(vacancy));

        FluentLoader<JobCandidate, UUID> candidateLoader = mock(FluentLoader.class);
        FluentLoader.ByQuery<JobCandidate, UUID> resumePoolQuery = mock(FluentLoader.ByQuery.class);
        FluentLoader.ByQuery<JobCandidate, UUID> fallbackPoolQuery = mock(FluentLoader.ByQuery.class);
        when(dataManager.load(JobCandidate.class)).thenReturn(candidateLoader);
        when(candidateLoader.query("select distinct e from hunttech_JobCandidate e where (e.blocked = false or e.blocked is null) and exists (select cv from hunttech_CandidateCV cv where cv.candidate.id = e.id and cv.textCV is not null and length(trim(cv.textCV)) > 0) order by e.updateTs desc"))
                .thenReturn(resumePoolQuery);
        when(candidateLoader.query("select e from hunttech_JobCandidate e where e.blocked = false or e.blocked is null order by e.updateTs desc"))
                .thenReturn(fallbackPoolQuery);
        when(resumePoolQuery.view("jobCandidate-full-view")).thenReturn(resumePoolQuery);
        when(resumePoolQuery.maxResults(25)).thenReturn(resumePoolQuery);
        when(resumePoolQuery.list()).thenReturn(Collections.<JobCandidate>emptyList());
        when(fallbackPoolQuery.view("jobCandidate-full-view")).thenReturn(fallbackPoolQuery);
        when(fallbackPoolQuery.maxResults(25)).thenReturn(fallbackPoolQuery);
        when(fallbackPoolQuery.list()).thenReturn(Collections.singletonList(candidate));

        FluentLoader<CandidateCV, UUID> cvLoader = mock(FluentLoader.class);
        FluentLoader.ByQuery<CandidateCV, UUID> cvQuery = mock(FluentLoader.ByQuery.class);
        when(dataManager.load(CandidateCV.class)).thenReturn(cvLoader);
        when(cvLoader.query(anyString())).thenReturn(cvQuery);
        when(cvQuery.parameter(eq("candId"), eq(candidateId))).thenReturn(cvQuery);
        when(cvQuery.view(any(Consumer.class))).thenReturn(cvQuery);
        when(cvQuery.list()).thenReturn(Collections.<CandidateCV>emptyList());

        FluentLoader<CandidateSkill, UUID> skillLoader = mock(FluentLoader.class);
        FluentLoader.ByQuery<CandidateSkill, UUID> skillQuery = mock(FluentLoader.ByQuery.class);
        when(dataManager.load(CandidateSkill.class)).thenReturn(skillLoader);
        when(skillLoader.query(anyString())).thenReturn(skillQuery);
        when(skillQuery.parameter(eq("candId"), eq(candidateId))).thenReturn(skillQuery);
        when(skillQuery.view(any(Consumer.class))).thenReturn(skillQuery);
        when(skillQuery.list()).thenReturn(Collections.<CandidateSkill>emptyList());

        when(aiExecutionService.executeText(eq(CandidateVacancyMatchAiService.FUNCTION_CODE), any()))
                .thenReturn(AiExecutionResult.textResult(
                        CandidateVacancyMatchAiService.FUNCTION_CODE, "Vacancy match",
                        AiCapability.TEXT_GENERATION, "test-model", "test-provider",
                        AiCredentialOwner.ADMIN, "{}"));

        service.matchCandidatesForVacancy(vacancyId, operationId);

        org.mockito.ArgumentCaptor<java.util.Map<String, Object>> context = org.mockito.ArgumentCaptor.forClass(java.util.Map.class);
        verify(aiExecutionService).executeText(eq(CandidateVacancyMatchAiService.FUNCTION_CODE), context.capture());
        String resumeText = (String) context.getValue().get("candidateResumeText");
        assertTrue("AI must be invoked with an explicit profile-and-skills-only marker",
                resumeText != null && resumeText.contains("Резюме с распознанным текстом отсутствует"));
        assertTrue("AI request ID must be correlated with the UI operation ID",
                String.valueOf(context.getValue().get("requestId")).startsWith(operationId.toString()));
    }
}

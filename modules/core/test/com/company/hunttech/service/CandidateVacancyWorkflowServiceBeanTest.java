package com.company.hunttech.service;

import com.company.hunttech.entity.*;
import com.company.hunttech.service.dto.TakeIntoWorkResult;
import com.haulmont.cuba.core.global.*;
import com.haulmont.cuba.security.entity.User;
import com.haulmont.cuba.security.global.UserSession;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class CandidateVacancyWorkflowServiceBeanTest {

    private CandidateVacancyWorkflowServiceBean service;
    private DataManager dataManager;
    private Metadata metadata;
    private UserSessionSource userSessionSource;
    private AiExecutionService aiExecutionService;
    private TimeSource timeSource;

    private User testUser;
    private UUID candidateId;
    private UUID vacancyId;
    private JobCandidate testCandidate;
    private OpenPosition testVacancy;

    @Before
    public void setUp() {
        service = new CandidateVacancyWorkflowServiceBean();
        dataManager = mock(DataManager.class);
        metadata = mock(Metadata.class);
        userSessionSource = mock(UserSessionSource.class);
        aiExecutionService = mock(AiExecutionService.class);
        timeSource = mock(TimeSource.class);

        ReflectionTestUtils.setField(service, "dataManager", dataManager);
        ReflectionTestUtils.setField(service, "metadata", metadata);
        ReflectionTestUtils.setField(service, "userSessionSource", userSessionSource);
        ReflectionTestUtils.setField(service, "aiExecutionService", aiExecutionService);
        ReflectionTestUtils.setField(service, "timeSource", timeSource);

        testUser = new User();
        testUser.setLogin("test_recruiter");
        testUser.setName("Тестовый Рекрутер");

        UserSession userSession = mock(UserSession.class);
        when(userSession.getUser()).thenReturn(testUser);
        when(userSessionSource.getUserSession()).thenReturn(userSession);

        when(timeSource.currentTimestamp()).thenReturn(new Date());

        candidateId = UUID.randomUUID();
        vacancyId = UUID.randomUUID();

        testCandidate = new JobCandidate();
        testCandidate.setId(candidateId);
        testCandidate.setFirstName("Иван");
        testCandidate.setSecondName("Иванов");

        testVacancy = new OpenPosition();
        testVacancy.setId(vacancyId);
        testVacancy.setVacansyName("Senior Java Developer");
    }

    @Test
    public void testTakeIntoWork_NullArgs() {
        TakeIntoWorkResult res = service.takeIntoWork(null, vacancyId, 85, null);
        assertFalse(res.isSuccess());
        assertNotNull(res.getMessage());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPrepareInteractionDraft() {
        FluentLoader.ById<JobCandidate, UUID> candidateLoader = mock(FluentLoader.ById.class);
        FluentLoader<JobCandidate, UUID> fluentCandidate = mock(FluentLoader.class);
        when(dataManager.load(JobCandidate.class)).thenReturn(fluentCandidate);
        when(fluentCandidate.id(candidateId)).thenReturn(candidateLoader);
        when(candidateLoader.view(anyString())).thenReturn(candidateLoader);
        when(candidateLoader.optional()).thenReturn(Optional.of(testCandidate));

        FluentLoader.ById<OpenPosition, UUID> vacancyLoader = mock(FluentLoader.ById.class);
        FluentLoader<OpenPosition, UUID> fluentVacancy = mock(FluentLoader.class);
        when(dataManager.load(OpenPosition.class)).thenReturn(fluentVacancy);
        when(fluentVacancy.id(vacancyId)).thenReturn(vacancyLoader);
        when(vacancyLoader.view(anyString())).thenReturn(vacancyLoader);
        when(vacancyLoader.optional()).thenReturn(Optional.of(testVacancy));

        FluentLoader.ByQuery<Iteraction, UUID> startTypeLoader = mock(FluentLoader.ByQuery.class);
        FluentLoader<Iteraction, UUID> fluentIteraction = mock(FluentLoader.class);
        when(dataManager.load(Iteraction.class)).thenReturn(fluentIteraction);
        when(fluentIteraction.query(anyString())).thenReturn(startTypeLoader);
        when(startTypeLoader.optional()).thenReturn(Optional.empty());
        when(startTypeLoader.list()).thenReturn(Collections.emptyList());

        when(metadata.create(IteractionList.class)).thenReturn(new IteractionList());

        IteractionList draft = service.prepareInteractionDraft(candidateId, vacancyId, 90,
                Arrays.asList("Java", "Spring Boot"), Collections.singletonList("Kubernetes"));

        assertNotNull(draft);
        assertEquals(testCandidate, draft.getCandidate());
        assertEquals(testVacancy, draft.getVacancy());
        assertNotNull(draft.getComment());
        assertTrue(draft.getComment().contains("90%"));
        assertTrue(draft.getComment().contains("Java, Spring Boot"));
        assertTrue(draft.getComment().contains("Kubernetes"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGenerateOutreachDraft_FallbackWhenAiNull() {
        FluentLoader.ById<JobCandidate, UUID> candidateLoader = mock(FluentLoader.ById.class);
        FluentLoader<JobCandidate, UUID> fluentCandidate = mock(FluentLoader.class);
        when(dataManager.load(JobCandidate.class)).thenReturn(fluentCandidate);
        when(fluentCandidate.id(candidateId)).thenReturn(candidateLoader);
        when(candidateLoader.view(anyString())).thenReturn(candidateLoader);
        when(candidateLoader.optional()).thenReturn(Optional.of(testCandidate));

        FluentLoader.ById<OpenPosition, UUID> vacancyLoader = mock(FluentLoader.ById.class);
        FluentLoader<OpenPosition, UUID> fluentVacancy = mock(FluentLoader.class);
        when(dataManager.load(OpenPosition.class)).thenReturn(fluentVacancy);
        when(fluentVacancy.id(vacancyId)).thenReturn(vacancyLoader);
        when(vacancyLoader.view(anyString())).thenReturn(vacancyLoader);
        when(vacancyLoader.optional()).thenReturn(Optional.of(testVacancy));

        when(aiExecutionService.executeText(eq(CandidateVacancyWorkflowService.FUNCTION_OUTREACH_DRAFT), anyMap()))
                .thenReturn(null);

        String draft = service.generateOutreachDraft(candidateId, vacancyId,
                Collections.singletonList("Опыт со Spring"), Collections.singletonList("Java"));

        assertNotNull(draft);
        assertTrue(draft.contains("Здравствуйте, Иван"));
        assertTrue(draft.contains("Senior Java Developer"));
        assertFalse(draft.contains("score"));
        assertFalse(draft.contains("AI"));
    }
}

package com.company.hunttech.service;

import com.company.hunttech.dto.integration.CandidateCompositeCreateRequestDto;
import com.company.hunttech.dto.integration.CandidateCompositeResponseDto;
import com.company.hunttech.dto.integration.CandidateCvCreateRequestDto;
import com.company.hunttech.dto.integration.CandidateCvResponseDto;
import com.company.hunttech.dto.integration.CompanyCreateRequestDto;
import com.company.hunttech.dto.integration.CompanyResponseDto;
import com.company.hunttech.dto.integration.InteractionCreateRequestDto;
import com.company.hunttech.dto.integration.InteractionResponseDto;
import com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto;
import com.company.hunttech.dto.integration.ProjectVacancyResponseDto;
import com.company.hunttech.entity.CandidateCV;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.Iteraction;
import com.company.hunttech.entity.IteractionList;
import com.company.hunttech.entity.JobCandidate;
import com.company.hunttech.entity.OpenPosition;
import com.company.hunttech.entity.Project;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ExternalIntegrationServiceBeanTest {

    private ExternalIntegrationServiceBean service;
    private DataManager mockDataManager;
    private Metadata mockMetadata;
    private HrmAiService mockHrmAiService;

    @Before
    public void setUp() throws Exception {
        service = new ExternalIntegrationServiceBean();
        mockDataManager = mock(DataManager.class, Mockito.RETURNS_DEEP_STUBS);
        mockMetadata = mock(Metadata.class);
        mockHrmAiService = mock(HrmAiService.class);

        injectField(service, "dataManager", mockDataManager);
        injectField(service, "metadata", mockMetadata);
        injectField(service, "hrmAiService", mockHrmAiService);
    }

    @Test
    public void testCreateCompanySuccess() {
        Company newCompany = new Company();
        UUID companyId = UUID.randomUUID();
        newCompany.setId(companyId);

        when(mockMetadata.create(Company.class)).thenReturn(newCompany);
        when(mockDataManager.load(Company.class).query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.empty());

        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("ООО Рога и Копыта");
        request.setInn("7701234567");
        request.setWebsite("https://example.com");
        request.setExternalId("ext-comp-101");
        request.setCorrelationId("corr-comp-1");

        CompanyResponseDto response = service.createCompany(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(companyId.toString(), response.getCompanyId());
        assertEquals("ext-comp-101", response.getExternalId());
        assertEquals("ООО Рога и Копыта", response.getCompanyName());
        assertEquals(CompanyResponseDto.STATUS_CREATED, response.getStatus());
        assertEquals("corr-comp-1", response.getCorrelationId());
        assertEquals("https://example.com", newCompany.getWebsite());

        verify(mockDataManager).commit(newCompany);
    }

    @Test
    public void testCreateCompanyIdempotencyKeyReturnsCachedResponse() {
        Company newCompany = new Company();
        UUID companyId = UUID.randomUUID();
        newCompany.setId(companyId);

        when(mockMetadata.create(Company.class)).thenReturn(newCompany);
        when(mockDataManager.load(Company.class).query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.empty());

        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("Компания Идемпотентность");
        request.setIdempotencyKey("idem-key-100");
        request.setExternalId("ext-100");

        CompanyResponseDto firstResponse = service.createCompany(request);
        assertNotNull(firstResponse);
        assertTrue(firstResponse.isSuccess());
        assertEquals(CompanyResponseDto.STATUS_CREATED, firstResponse.getStatus());

        // Второй запрос с тем же idempotencyKey
        CompanyResponseDto secondResponse = service.createCompany(request);
        assertNotNull(secondResponse);
        assertTrue(secondResponse.isSuccess());
        assertEquals(firstResponse.getCompanyId(), secondResponse.getCompanyId());

        // commit должен быть вызван только один раз
        verify(mockDataManager, times(1)).commit(any(Company.class));
    }

    @Test
    public void testCreateCompanyDeduplicationByInn() {
        Company existing = new Company();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);
        existing.setComanyName("Существующая компания");
        existing.setInn("7701234567");

        when(mockDataManager.load(Company.class).query(contains("e.inn = :inn")).parameter(eq("inn"), eq("7701234567")).optional())
                .thenReturn(Optional.of(existing));

        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("Новое Название");
        request.setInn("7701234567");
        request.setExternalId("ext-dup-inn");
        request.setCorrelationId("corr-dup-1");

        CompanyResponseDto response = service.createCompany(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(existingId.toString(), response.getCompanyId());
        assertEquals("ext-dup-inn", response.getExternalId());
        assertEquals(CompanyResponseDto.STATUS_EXISTING_FOUND, response.getStatus());

        verify(mockDataManager, never()).commit(any(Company.class));
    }

    @Test
    public void testCreateCompanyDeduplicationByName() {
        Company existing = new Company();
        UUID existingId = UUID.randomUUID();
        existing.setId(existingId);
        existing.setComanyName("ООО Вектор");

        when(mockDataManager.load(Company.class).query(contains("lower(e.comanyName) = :name")).parameter(eq("name"), eq("ооо вектор")).optional())
                .thenReturn(Optional.of(existing));

        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("ООО Вектор");
        request.setExternalId("ext-dup-name");

        CompanyResponseDto response = service.createCompany(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(existingId.toString(), response.getCompanyId());
        assertEquals(CompanyResponseDto.STATUS_EXISTING_FOUND, response.getStatus());
        verify(mockDataManager, never()).commit(any(Company.class));
    }

    @Test
    public void testCreateCompanyValidationFailureEmptyName() {
        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("   ");
        request.setCorrelationId("corr-fail-name");

        CompanyResponseDto response = service.createCompany(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertEquals("corr-fail-name", response.getCorrelationId());
        assertNotNull(response.getErrorDetails());
        assertEquals("VALIDATION_ERROR", response.getErrorDetails().getErrorCode());
        assertTrue(response.getMessage().contains("Наименование компании обязательно"));
    }

    @Test
    public void testCreateCompanyValidationFailureInvalidInn() {
        CompanyCreateRequestDto request = new CompanyCreateRequestDto();
        request.setCompanyName("Компания");
        request.setInn("12345"); // Неверная длина

        CompanyResponseDto response = service.createCompany(request);

        assertNotNull(response);
        assertFalse(response.isSuccess());
        assertNotNull(response.getErrorDetails());
        assertEquals("VALIDATION_ERROR", response.getErrorDetails().getErrorCode());
        assertTrue(response.getMessage().contains("ИНН должен содержать 10 или 12 цифр"));
    }

    @Test
    public void testCreateProjectAndVacancySuccessNewProject() {
        com.company.hunttech.entity.Project newProject = new com.company.hunttech.entity.Project();
        UUID projectId = UUID.randomUUID();
        newProject.setId(projectId);

        com.company.hunttech.entity.OpenPosition newVacancy = new com.company.hunttech.entity.OpenPosition();
        UUID vacancyId = UUID.randomUUID();
        newVacancy.setId(vacancyId);

        when(mockMetadata.create(com.company.hunttech.entity.Project.class)).thenReturn(newProject);
        when(mockMetadata.create(com.company.hunttech.entity.OpenPosition.class)).thenReturn(newVacancy);

        when(mockDataManager.load(com.company.hunttech.entity.Project.class).query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.empty());

        com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto request = new com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto();
        request.setProjectName("Проект Альфа");
        request.setProjectDescription("Описание проекта Альфа");
        request.setVacancyName("Java Senior Developer");
        request.setExternalId("ext-vac-001");
        request.setCorrelationId("corr-vac-1");
        request.setSalaryMin(new java.math.BigDecimal("300000"));
        request.setSalaryMax(new java.math.BigDecimal("400000"));

        com.company.hunttech.dto.integration.ProjectVacancyResponseDto response = service.createProjectAndVacancy(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(projectId.toString(), response.getProjectId());
        assertEquals(vacancyId.toString(), response.getVacancyId());
        assertEquals("ext-vac-001", response.getExternalId());
        assertEquals("CREATED", response.getStatus());
        assertEquals("corr-vac-1", response.getCorrelationId());

        assertEquals("Java Senior Developer", newVacancy.getVacansyName());
        assertEquals(newProject, newVacancy.getProjectName());
        assertEquals(new java.math.BigDecimal("300000"), newVacancy.getSalaryMin());
        assertEquals(new java.math.BigDecimal("400000"), newVacancy.getSalaryMax());
        assertFalse(newVacancy.getOpenClose());
        assertFalse(newVacancy.getSignDraft());

        verify(mockDataManager).commit(any(com.haulmont.cuba.core.global.CommitContext.class));
    }

    @Test
    public void testCreateProjectAndVacancyWithAiArtifacts() {
        com.company.hunttech.entity.Project newProject = new com.company.hunttech.entity.Project();
        UUID projectId = UUID.randomUUID();
        newProject.setId(projectId);

        com.company.hunttech.entity.OpenPosition newVacancy = new com.company.hunttech.entity.OpenPosition();
        UUID vacancyId = UUID.randomUUID();
        newVacancy.setId(vacancyId);

        when(mockMetadata.create(com.company.hunttech.entity.Project.class)).thenReturn(newProject);
        when(mockMetadata.create(com.company.hunttech.entity.OpenPosition.class)).thenReturn(newVacancy);

        when(mockDataManager.load(com.company.hunttech.entity.Project.class).query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.empty());

        String rawComment = "Требуется Java разработчик со знанием Spring, PostgreSQL и Liquibase.";
        String standardized = "### Описание вакансии\nJava разработчик уровня Senior.";
        String checklist = "- [ ] Опыт с Java 11+\n- [ ] Опыт с Spring Boot";
        String searchMap = "1. Компании-доноры: FinTech\n2. Ключевые слова: Java, Spring";
        String interviewPlan = "1. Знакомство (5 мин)\n2. Технический блок (40 мин)";

        when(mockHrmAiService.standardizeVacancyDescription(rawComment)).thenReturn(standardized);
        when(mockHrmAiService.generateChecklist(standardized)).thenReturn(checklist);
        when(mockHrmAiService.generateSearchMap(standardized)).thenReturn(searchMap);
        when(mockHrmAiService.generateInterviewPlan(standardized)).thenReturn(interviewPlan);

        com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto request = new com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto();
        request.setProjectName("AI Проект");
        request.setVacancyName("Senior Java Dev");
        request.setComment(rawComment);
        request.setExternalId("ext-ai-001");

        com.company.hunttech.dto.integration.ProjectVacancyResponseDto response = service.createProjectAndVacancy(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());

        // Проверка записи оригинала вакансии
        assertEquals(rawComment, newVacancy.getRawDescription());

        // Проверка стандартизированного описания
        assertEquals(standardized, newVacancy.getComment());

        // Проверка чеклиста (в обоих полях)
        assertEquals(checklist, newVacancy.getInterviewChecklist());
        assertEquals(checklist, newVacancy.getExercise());
        assertTrue(Boolean.TRUE.equals(newVacancy.getNeedExercise()));

        // Проверка карты поиска (в обоих полях)
        assertEquals(searchMap, newVacancy.getSearchMap());
        assertEquals(searchMap, newVacancy.getMemoForInterview());
        assertTrue(Boolean.TRUE.equals(newVacancy.getNeedMemoForInterview()));

        // Проверка плана интервью (в обоих полях)
        assertEquals(interviewPlan, newVacancy.getInterviewPlan());
        assertEquals(interviewPlan, newVacancy.getTemplateLetter());
        assertTrue(Boolean.TRUE.equals(newVacancy.getNeedLetter()));

        verify(mockDataManager).commit(any(com.haulmont.cuba.core.global.CommitContext.class));
    }

    @Test
    public void testCreateProjectAndVacancySuccessExistingProject() {
        com.company.hunttech.entity.Project existingProject = new com.company.hunttech.entity.Project();
        UUID projectId = UUID.randomUUID();
        existingProject.setId(projectId);
        existingProject.setProjectName("Существующий проект");

        com.company.hunttech.entity.OpenPosition newVacancy = new com.company.hunttech.entity.OpenPosition();
        UUID vacancyId = UUID.randomUUID();
        newVacancy.setId(vacancyId);

        when(mockMetadata.create(com.company.hunttech.entity.OpenPosition.class)).thenReturn(newVacancy);
        when(mockDataManager.load(com.company.hunttech.entity.Project.class).id(eq(projectId)).optional())
                .thenReturn(Optional.of(existingProject));

        com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto request = new com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto();
        request.setExistingProjectId(projectId.toString());
        request.setVacancyName("QA Automation Engineer");
        request.setExternalId("ext-vac-002");

        com.company.hunttech.dto.integration.ProjectVacancyResponseDto response = service.createProjectAndVacancy(request);

        assertNotNull(response);
        assertTrue(response.isSuccess());
        assertEquals(projectId.toString(), response.getProjectId());
        assertEquals(vacancyId.toString(), response.getVacancyId());
        assertEquals(existingProject, newVacancy.getProjectName());

        verify(mockDataManager).commit(any(com.haulmont.cuba.core.global.CommitContext.class));
    }

    @Test
    public void testCreateProjectAndVacancyIdempotencyKeyCached() {
        com.company.hunttech.entity.Project existingProject = new com.company.hunttech.entity.Project();
        UUID projectId = UUID.randomUUID();
        existingProject.setId(projectId);

        com.company.hunttech.entity.OpenPosition newVacancy = new com.company.hunttech.entity.OpenPosition();
        UUID vacancyId = UUID.randomUUID();
        newVacancy.setId(vacancyId);

        when(mockMetadata.create(com.company.hunttech.entity.OpenPosition.class)).thenReturn(newVacancy);
        when(mockDataManager.load(com.company.hunttech.entity.Project.class).id(eq(projectId)).optional())
                .thenReturn(Optional.of(existingProject));

        com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto request = new com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto();
        request.setExistingProjectId(projectId.toString());
        request.setVacancyName("Frontend Tech Lead");
        request.setIdempotencyKey("idem-vac-key-1");

        com.company.hunttech.dto.integration.ProjectVacancyResponseDto firstResponse = service.createProjectAndVacancy(request);
        assertNotNull(firstResponse);
        assertTrue(firstResponse.isSuccess());

        com.company.hunttech.dto.integration.ProjectVacancyResponseDto secondResponse = service.createProjectAndVacancy(request);
        assertNotNull(secondResponse);
        assertTrue(secondResponse.isSuccess());
        assertEquals(firstResponse.getVacancyId(), secondResponse.getVacancyId());

        // commit Context вызывается только один раз
        verify(mockDataManager, times(1)).commit(any(com.haulmont.cuba.core.global.CommitContext.class));
    }

    @Test
    public void testCreateProjectAndVacancyValidationFailures() {
        // 1. Пустой vacancyName
        com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto req1 = new com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto();
        req1.setProjectName("Проект");
        com.company.hunttech.dto.integration.ProjectVacancyResponseDto resp1 = service.createProjectAndVacancy(req1);
        assertNotNull(resp1);
        assertFalse(resp1.isSuccess());
        assertTrue(resp1.getMessage().contains("vacancyName"));

        // 2. Нет ни existingProjectId, ни projectName
        com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto req2 = new com.company.hunttech.dto.integration.ProjectVacancyCreateRequestDto();
        req2.setVacancyName("Разработчик");
        com.company.hunttech.dto.integration.ProjectVacancyResponseDto resp2 = service.createProjectAndVacancy(req2);
        assertNotNull(resp2);
        assertFalse(resp2.isSuccess());
        assertTrue(resp2.getMessage().contains("existingProjectId или projectName"));
    }

    @Test
    public void testCreateCandidateCvSuccess() {
        JobCandidate candidate = new JobCandidate();
        UUID candidateId = UUID.randomUUID();
        candidate.setId(candidateId);

        CandidateCV newCv = new CandidateCV();
        UUID cvId = UUID.randomUUID();
        newCv.setId(cvId);

        when(mockDataManager.load(JobCandidate.class).id(eq(candidateId)).optional()).thenReturn(Optional.of(candidate));
        when(mockMetadata.create(CandidateCV.class)).thenReturn(newCv);

        CandidateCvCreateRequestDto req = new CandidateCvCreateRequestDto();
        req.setCandidateId(candidateId.toString());
        req.setTextCv("Опыт работы 7 лет, Java, Spring, Kubernetes");
        req.setResumeUrl("https://hh.ru/resume/12345");
        req.setCoverLetter("Здравствуйте! Прошу рассмотреть мое резюме.");
        req.setExternalId("ext-cv-001");
        req.setCorrelationId("corr-cv-1");

        CandidateCvResponseDto resp = service.createCandidateCV(req);

        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertEquals(cvId.toString(), resp.getCandidateCvId());
        assertEquals(candidateId.toString(), resp.getCandidateId());
        assertEquals("ext-cv-001", resp.getExternalId());
        assertEquals(CandidateCvResponseDto.STATUS_CREATED, resp.getStatus());

        assertEquals(candidate, newCv.getCandidate());
        assertEquals("Опыт работы 7 лет, Java, Spring, Kubernetes", newCv.getTextCV());
        assertEquals("https://hh.ru/resume/12345", newCv.getLinkOriginalCv());

        verify(mockDataManager).commit(newCv);
    }

    @Test
    public void testCreateInteractionSuccess() {
        JobCandidate candidate = new JobCandidate();
        UUID candidateId = UUID.randomUUID();
        candidate.setId(candidateId);

        OpenPosition vacancy = new OpenPosition();
        UUID vacancyId = UUID.randomUUID();
        vacancy.setId(vacancyId);

        Iteraction interactionType = new Iteraction();
        UUID typeId = UUID.randomUUID();
        interactionType.setId(typeId);

        IteractionList newInteraction = new IteractionList();
        UUID interactionId = UUID.randomUUID();
        newInteraction.setId(interactionId);

        when(mockDataManager.load(JobCandidate.class).id(eq(candidateId)).optional()).thenReturn(Optional.of(candidate));
        when(mockDataManager.load(OpenPosition.class).id(eq(vacancyId)).optional()).thenReturn(Optional.of(vacancy));
        when(mockDataManager.load(Iteraction.class).id(eq(typeId)).optional()).thenReturn(Optional.of(interactionType));
        when(mockDataManager.loadValue(contains("select max(e.numberIteraction)"), eq(java.math.BigDecimal.class)).optional())
                .thenReturn(Optional.of(new java.math.BigDecimal("41")));
        when(mockMetadata.create(IteractionList.class)).thenReturn(newInteraction);

        InteractionCreateRequestDto req = new InteractionCreateRequestDto();
        req.setCandidateId(candidateId.toString());
        req.setVacancyId(vacancyId.toString());
        req.setInteractionTypeId(typeId.toString());
        req.setComment("Кандидат подтвердил прохождение интервью");
        req.setCommunicationMethod("Telegram");
        req.setRating(5);
        req.setExternalId("ext-act-1");
        req.setCorrelationId("corr-act-1");

        InteractionResponseDto resp = service.createInteraction(req);

        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertEquals(interactionId.toString(), resp.getInteractionId());
        assertEquals(candidateId.toString(), resp.getCandidateId());
        assertEquals(vacancyId.toString(), resp.getVacancyId());
        assertEquals(new java.math.BigDecimal("42"), resp.getNumberInteraction());
        assertEquals("CREATED", resp.getStatus());

        assertEquals(candidate, newInteraction.getCandidate());
        assertEquals(vacancy, newInteraction.getVacancy());
        assertEquals("Telegram", newInteraction.getCommunicationMethod());
        assertEquals(Integer.valueOf(5), newInteraction.getRating());

        verify(mockDataManager).commit(newInteraction);
    }

    @Test
    public void testCreateCandidateWithDetailsNewCandidateWithCvAndInteraction() {
        JobCandidate newCandidate = new JobCandidate();
        UUID candidateId = UUID.randomUUID();
        newCandidate.setId(candidateId);

        CandidateCV newCv = new CandidateCV();
        UUID cvId = UUID.randomUUID();
        newCv.setId(cvId);

        IteractionList newInteraction = new IteractionList();
        UUID interactionId = UUID.randomUUID();
        newInteraction.setId(interactionId);

        OpenPosition vacancy = new OpenPosition();
        UUID vacancyId = UUID.randomUUID();
        vacancy.setId(vacancyId);

        when(mockMetadata.create(JobCandidate.class)).thenReturn(newCandidate);
        when(mockMetadata.create(CandidateCV.class)).thenReturn(newCv);
        when(mockMetadata.create(IteractionList.class)).thenReturn(newInteraction);

        // Кандидат не найден при дедупликации
        when(mockDataManager.load(JobCandidate.class).query(anyString()).parameter(anyString(), any()).optional())
                .thenReturn(Optional.empty());
        when(mockDataManager.load(OpenPosition.class).id(eq(vacancyId)).optional())
                .thenReturn(Optional.of(vacancy));

        CandidateCompositeCreateRequestDto req = new CandidateCompositeCreateRequestDto();
        req.setFirstName("Иван");
        req.setSecondName("Иванов");
        req.setMiddleName("Иванович");
        req.setPhone("+7 999 123-45-67");
        req.setEmail("ivanov@example.com");
        req.setCvText("Резюме Senior Java Developer");
        req.setVacancyId(vacancyId.toString());
        req.setInteractionComment("Первичный контакт через API");
        req.setExternalId("ext-comp-cand-1");
        req.setCorrelationId("corr-comp-cand-1");

        CandidateCompositeResponseDto resp = service.createCandidateWithDetails(req);

        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertEquals(candidateId.toString(), resp.getCandidateId());
        assertEquals(cvId.toString(), resp.getCandidateCvId());
        assertEquals(interactionId.toString(), resp.getInteractionId());
        assertEquals(CandidateCompositeResponseDto.STATUS_CREATED, resp.getStatus());
        assertEquals("ext-comp-cand-1", resp.getExternalId());

        assertEquals("Иван", newCandidate.getFirstName());
        assertEquals("Иванов", newCandidate.getSecondName());
        assertEquals("Иванов Иван Иванович", newCandidate.getFullName());

        verify(mockDataManager).commit(any(com.haulmont.cuba.core.global.CommitContext.class));
    }

    @Test
    public void testCreateCandidateWithDetailsDeduplicationByPhone() {
        JobCandidate existingCandidate = new JobCandidate();
        UUID candidateId = UUID.randomUUID();
        existingCandidate.setId(candidateId);
        existingCandidate.setFirstName("Петр");
        existingCandidate.setSecondName("Петров");

        CandidateCV newCv = new CandidateCV();
        UUID cvId = UUID.randomUUID();
        newCv.setId(cvId);

        when(mockMetadata.create(CandidateCV.class)).thenReturn(newCv);

        // Найден по номеру телефона
        when(mockDataManager.load(JobCandidate.class).query(contains("c.phone = :p")).parameter(eq("p"), anyString()).optional())
                .thenReturn(Optional.of(existingCandidate));


        CandidateCompositeCreateRequestDto req = new CandidateCompositeCreateRequestDto();
        req.setFirstName("Петр");
        req.setSecondName("Петров");
        req.setPhone("+7 (999) 777-88-99");
        req.setCvText("Обновленное резюме Петра");
        req.setExternalId("ext-dup-phone");

        CandidateCompositeResponseDto resp = service.createCandidateWithDetails(req);

        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertEquals(candidateId.toString(), resp.getCandidateId());
        assertEquals(cvId.toString(), resp.getCandidateCvId());
        assertEquals(CandidateCompositeResponseDto.STATUS_EXISTING_FOUND, resp.getStatus());

        // Проверяем, что новое резюме привязано к существующему кандидату
        assertEquals(existingCandidate, newCv.getCandidate());
        verify(mockDataManager).commit(any(com.haulmont.cuba.core.global.CommitContext.class));
    }

    private static void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}



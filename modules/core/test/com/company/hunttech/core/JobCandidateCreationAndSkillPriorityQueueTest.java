package com.company.hunttech.core;

import com.company.hunttech.config.HunttechSkillsEnrichmentConfig;
import com.company.hunttech.entity.*;
import com.company.hunttech.listeners.CandidateCvChangedListener;
import com.company.hunttech.service.CandidateSkillEnrichmentService;
import com.company.hunttech.service.CandidateSkillEnrichmentServiceBean;
import com.company.hunttech.service.CandidateSkillsEnrichmentWorker;
import com.company.hunttech.service.SkillAnalysisResult;
import com.company.hunttech.service.SkillAnalysisService;
import com.company.hunttech.service.dto.CandidateSkillsScanResult;
import com.haulmont.chile.core.annotations.Composition;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Query;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.app.events.AttributeChanges;
import com.haulmont.cuba.core.app.events.EntityChangedEvent;
import com.haulmont.cuba.core.entity.annotation.PublishEntityChangedEvents;
import com.haulmont.cuba.core.entity.contracts.Id;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.security.app.Authentication;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Сквозной интеграционный и контрактный автотест создания нового кандидата,
 * привязки резюме и проверки приоритетной постановки в очередь сервиса
 * «Фоновое определение навыков».
 */
public class JobCandidateCreationAndSkillPriorityQueueTest {

    private CandidateSkillEnrichmentServiceBean service;
    private CandidateCvChangedListener listener;
    private CandidateSkillsEnrichmentWorker worker;

    private DataManager mockDataManager;
    private Metadata mockMetadata;
    private Configuration mockConfiguration;
    private HunttechSkillsEnrichmentConfig mockConfig;
    private Authentication mockAuthentication;
    private Persistence mockPersistence;
    private SkillAnalysisService mockSkillAnalysisService;

    @Before
    public void setUp() throws Exception {
        service = new CandidateSkillEnrichmentServiceBean();
        listener = new CandidateCvChangedListener();
        worker = new CandidateSkillsEnrichmentWorker();

        mockDataManager = mock(DataManager.class, Mockito.RETURNS_DEEP_STUBS);
        mockMetadata = mock(Metadata.class);
        mockConfiguration = mock(Configuration.class);
        mockConfig = mock(HunttechSkillsEnrichmentConfig.class);
        mockAuthentication = mock(Authentication.class);
        mockPersistence = mock(Persistence.class, Mockito.RETURNS_DEEP_STUBS);
        mockSkillAnalysisService = mock(SkillAnalysisService.class);

        when(mockConfiguration.getConfig(HunttechSkillsEnrichmentConfig.class)).thenReturn(mockConfig);
        when(mockConfig.getMaxRetries()).thenReturn(3);
        when(mockConfig.getDelayBetweenRequestsSec()).thenReturn(0);
        when(mockConfig.getEnabled()).thenReturn(true);
        when(mockConfig.getAiFunctionCode()).thenReturn("SKILLS_EXTRACT_BACKGROUND");
        when(mockConfig.getBatchFetchSize()).thenReturn(10);
        when(mockConfig.getMaxCandidatesPerHour()).thenReturn(100);

        // Инъекция в CandidateSkillEnrichmentServiceBean
        injectField(service, "dataManager", mockDataManager);
        injectField(service, "metadata", mockMetadata);
        injectField(service, "configuration", mockConfiguration);
        injectField(service, "persistence", mockPersistence);
        injectField(service, "skillAnalysisService", mockSkillAnalysisService);

        // Инъекция в CandidateCvChangedListener
        injectField(listener, "enrichmentService", service);
        injectField(listener, "authentication", mockAuthentication);

        // Инъекция в CandidateSkillsEnrichmentWorker
        injectField(worker, "dataManager", mockDataManager);
        injectField(worker, "metadata", mockMetadata);
        injectField(worker, "configuration", mockConfiguration);
        injectField(worker, "persistence", mockPersistence);
        injectField(worker, "authentication", mockAuthentication);
        injectField(worker, "enrichmentService", service);

        worker = Mockito.spy(worker);

        // Перекрестная ссылка service -> worker
        injectField(service, "enrichmentWorker", worker);
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testJobCandidateAndCandidateCv_EntityAnnotations_AndModelIntegrity() throws Exception {
        // 1. Проверка наличия @PublishEntityChangedEvents для генерации событий CUBA
        assertTrue("JobCandidate должен иметь аннотацию @PublishEntityChangedEvents",
                JobCandidate.class.isAnnotationPresent(PublishEntityChangedEvents.class));
        assertTrue("CandidateCV должен иметь аннотацию @PublishEntityChangedEvents",
                CandidateCV.class.isAnnotationPresent(PublishEntityChangedEvents.class));

        // 2. Проверка композиции резюме в JobCandidate
        Field candidateCvField = JobCandidate.class.getDeclaredField("candidateCv");
        assertTrue("candidateCv в JobCandidate должно быть помечено @Composition",
                candidateCvField.isAnnotationPresent(Composition.class));

        // 3. Проверка приоритета в CandidateCvSkillAnalysis
        CandidateCvSkillAnalysis analysis = new CandidateCvSkillAnalysis();
        assertEquals("По умолчанию приоритет должен быть 0",
                Integer.valueOf(CandidateSkillEnrichmentService.PRIORITY_DEFAULT), analysis.getPriority());
        analysis.setPriority(CandidateSkillEnrichmentService.PRIORITY_HIGH);
        assertEquals("Приоритет должен установиться в 100",
                Integer.valueOf(100), analysis.getPriority());
    }

    @Test
    public void testRecruiterCreatesCandidate_WithCvText_ListenerTriggersPriorityQueue100() {
        // Создание нового кандидата и резюме с текстом рекрутером
        UUID candidateId = UUID.randomUUID();
        JobCandidate candidate = new JobCandidate();
        candidate.setId(candidateId);
        candidate.setFirstName("Алексей");
        candidate.setSecondName("Иванов");
        candidate.setFullName("Алексей Иванов");

        UUID cvId = UUID.randomUUID();
        CandidateCV cv = new CandidateCV();
        cv.setId(cvId);
        cv.setCandidate(candidate);
        cv.setTextCV("Опыт: Java 17, Spring Boot 2.7, PostgreSQL 14, Docker, Kafka, Redis");

        // Мокирование загрузки CV
        FluentLoader.ById<CandidateCV, UUID> cvLoader = mock(FluentLoader.ById.class);
        when(mockDataManager.load(CandidateCV.class).id(cvId)).thenReturn(cvLoader);
        when(cvLoader.view("candidateCV-llm-view")).thenReturn(cvLoader);
        when(cvLoader.optional()).thenReturn(Optional.of(cv));

        // Мокирование поиска существующей записи анализа (новая запись)
        FluentLoader.ByQuery<CandidateCvSkillAnalysis, UUID> analysisLoader = mock(FluentLoader.ByQuery.class);
        when(mockDataManager.load(CandidateCvSkillAnalysis.class).query(anyString())).thenReturn(analysisLoader);
        when(analysisLoader.parameter(eq("cvId"), eq(cvId))).thenReturn(analysisLoader);
        when(analysisLoader.view("candidateCvSkillAnalysis-browse-view")).thenReturn(analysisLoader);
        when(analysisLoader.optional()).thenReturn(Optional.empty());

        CandidateCvSkillAnalysis createdAnalysis = new CandidateCvSkillAnalysis();
        when(mockMetadata.create(CandidateCvSkillAnalysis.class)).thenReturn(createdAnalysis);

        // Имитируем событие EntityChangedEvent<CandidateCV, UUID> типа CREATED от CUBA
        EntityChangedEvent<CandidateCV, UUID> event = mock(EntityChangedEvent.class);
        when(event.getType()).thenReturn(EntityChangedEvent.Type.CREATED);
        Id<CandidateCV, UUID> entityId = Id.of(cvId, CandidateCV.class);
        when(event.getEntityId()).thenReturn(entityId);

        // Вызов слушателя событий
        listener.onCandidateCvAfterCommit(event);

        // Проверяем вызовы аутентификации
        verify(mockAuthentication).begin();
        verify(mockAuthentication).end();

        // Проверяем, что создана запись с наивысшим приоритетом 100 и статусом NOT_ANALYZED
        assertEquals("Статус новой задачи должен быть NOT_ANALYZED",
                CandidateCvAnalysisStatus.NOT_ANALYZED, createdAnalysis.getStatus());
        assertEquals("Приоритет созданной задачи должен быть PRIORITY_HIGH (100)",
                Integer.valueOf(CandidateSkillEnrichmentService.PRIORITY_HIGH), createdAnalysis.getPriority());
        assertEquals(candidate, createdAnalysis.getCandidate());
        assertEquals(cv, createdAnalysis.getCandidateCv());
        assertNotNull("Хэш резюме должен быть рассчитан", createdAnalysis.getCvContentHash());

        // Проверяем сохранение задачи в DataManager и вызов немедленного пробуждения воркера
        verify(mockDataManager).commit(createdAnalysis);
        verify(worker).triggerImmediateProcessing();
    }

    @Test
    public void testRecruiterCreatesCandidate_WithoutCvText_GracefullySkipped() {
        // Создание кандидата с пустым текстом резюме
        UUID candidateId = UUID.randomUUID();
        JobCandidate candidate = new JobCandidate();
        candidate.setId(candidateId);
        candidate.setFullName("Сергей Смирнов");

        UUID cvId = UUID.randomUUID();
        CandidateCV cv = new CandidateCV();
        cv.setId(cvId);
        cv.setCandidate(candidate);
        cv.setTextCV("   "); // Только пробелы

        FluentLoader.ById<CandidateCV, UUID> cvLoader = mock(FluentLoader.ById.class);
        when(mockDataManager.load(CandidateCV.class).id(cvId)).thenReturn(cvLoader);
        when(cvLoader.view("candidateCV-llm-view")).thenReturn(cvLoader);
        when(cvLoader.optional()).thenReturn(Optional.of(cv));

        FluentLoader.ByQuery<CandidateCvSkillAnalysis, UUID> analysisLoader = mock(FluentLoader.ByQuery.class);
        when(mockDataManager.load(CandidateCvSkillAnalysis.class).query(anyString())).thenReturn(analysisLoader);
        when(analysisLoader.parameter(eq("cvId"), eq(cvId))).thenReturn(analysisLoader);
        when(analysisLoader.view("candidateCvSkillAnalysis-browse-view")).thenReturn(analysisLoader);
        when(analysisLoader.optional()).thenReturn(Optional.empty());

        CandidateCvSkillAnalysis createdAnalysis = new CandidateCvSkillAnalysis();
        when(mockMetadata.create(CandidateCvSkillAnalysis.class)).thenReturn(createdAnalysis);

        EntityChangedEvent<CandidateCV, UUID> event = mock(EntityChangedEvent.class);
        when(event.getType()).thenReturn(EntityChangedEvent.Type.CREATED);
        when(event.getEntityId()).thenReturn(Id.of(cvId, CandidateCV.class));

        listener.onCandidateCvAfterCommit(event);

        // Задача должна быть помечена SKIPPED с понятным пояснением, а воркер НЕ должен триггериться
        assertEquals(CandidateCvAnalysisStatus.SKIPPED, createdAnalysis.getStatus());
        assertEquals("Текст резюме пуст", createdAnalysis.getLastError());
        verify(mockDataManager).commit(createdAnalysis);
        verify(worker, never()).triggerImmediateProcessing();
    }

    @Test
    public void testRecruiterUpdatesExistingCandidate_NewCvText_PromotedToPriority100() {
        UUID candidateId = UUID.randomUUID();
        JobCandidate candidate = new JobCandidate();
        candidate.setId(candidateId);
        candidate.setFullName("Дмитрий Кузнецов");

        UUID cvId = UUID.randomUUID();
        CandidateCV cv = new CandidateCV();
        cv.setId(cvId);
        cv.setCandidate(candidate);
        cv.setTextCV("Обновленный опыт: Go, Kubernetes, Helm, CI/CD, Prometheus");

        FluentLoader.ById<CandidateCV, UUID> cvLoader = mock(FluentLoader.ById.class);
        when(mockDataManager.load(CandidateCV.class).id(cvId)).thenReturn(cvLoader);
        when(cvLoader.view("candidateCV-llm-view")).thenReturn(cvLoader);
        when(cvLoader.optional()).thenReturn(Optional.of(cv));

        // Ранее проанализированная запись со статусом FRESH и приоритетом 0
        CandidateCvSkillAnalysis existingAnalysis = new CandidateCvSkillAnalysis();
        existingAnalysis.setId(UUID.randomUUID());
        existingAnalysis.setCandidate(candidate);
        existingAnalysis.setCandidateCv(cv);
        existingAnalysis.setStatus(CandidateCvAnalysisStatus.FRESH);
        existingAnalysis.setPriority(CandidateSkillEnrichmentService.PRIORITY_DEFAULT);

        FluentLoader.ByQuery<CandidateCvSkillAnalysis, UUID> analysisLoader = mock(FluentLoader.ByQuery.class);
        when(mockDataManager.load(CandidateCvSkillAnalysis.class).query(anyString())).thenReturn(analysisLoader);
        when(analysisLoader.parameter(eq("cvId"), eq(cvId))).thenReturn(analysisLoader);
        when(analysisLoader.view("candidateCvSkillAnalysis-browse-view")).thenReturn(analysisLoader);
        when(analysisLoader.optional()).thenReturn(Optional.of(existingAnalysis));

        // Имитируем событие UPDATED с изменением поля textCV
        EntityChangedEvent<CandidateCV, UUID> event = mock(EntityChangedEvent.class);
        when(event.getType()).thenReturn(EntityChangedEvent.Type.UPDATED);
        when(event.getEntityId()).thenReturn(Id.of(cvId, CandidateCV.class));
        AttributeChanges changes = mock(AttributeChanges.class);
        when(changes.isChanged("textCV")).thenReturn(true);
        when(event.getChanges()).thenReturn(changes);

        listener.onCandidateCvAfterCommit(event);

        // Проверяем, что существующая запись переведена в NOT_ANALYZED и поднята до приоритета 100
        assertEquals(CandidateCvAnalysisStatus.NOT_ANALYZED, existingAnalysis.getStatus());
        assertEquals(Integer.valueOf(100), existingAnalysis.getPriority());
        assertEquals(Integer.valueOf(0), existingAnalysis.getRetryCount());
        assertNull(existingAnalysis.getLastError());
        assertNotNull(existingAnalysis.getCvContentHash());

        verify(mockDataManager).commit(existingAnalysis);
    }

    @Test
    public void testRecruiterUpdatesOtherCandidateField_DoesNotReEnqueue() {
        UUID cvId = UUID.randomUUID();

        EntityChangedEvent<CandidateCV, UUID> event = mock(EntityChangedEvent.class);
        when(event.getType()).thenReturn(EntityChangedEvent.Type.UPDATED);
        when(event.getEntityId()).thenReturn(Id.of(cvId, CandidateCV.class));
        AttributeChanges changes = mock(AttributeChanges.class);
        when(changes.isChanged("textCV")).thenReturn(false); // Изменилось другое поле, например letter
        when(event.getChanges()).thenReturn(changes);

        listener.onCandidateCvAfterCommit(event);

        // dataManager.load(CandidateCV.class) не должен даже вызываться
        verify(mockDataManager, never()).load(CandidateCV.class);
    }

    @Test
    public void testPriorityWorkerSelection_UrgentRecruiterCandidatePickedFirst() throws Exception {
        UUID regularCandidateId = UUID.randomUUID();
        UUID regularCvId = UUID.randomUUID();
        UUID urgentCandidateId = UUID.randomUUID();
        UUID urgentCvId = UUID.randomUUID();

        // Мокирование выборки срочных задач (Шаг 0 в selectAndLockNextCandidateCv)
        EntityManager em = mock(EntityManager.class);
        Transaction tx = mock(Transaction.class);
        when(mockPersistence.createTransaction()).thenReturn(tx);
        when(mockPersistence.getEntityManager()).thenReturn(em);

        Query step0Query = mock(Query.class);
        when(em.createQuery(contains("coalesce(a.priority, 0) >= :highPriority"))).thenReturn(step0Query);
        when(step0Query.setParameter(eq("naStatus"), eq(CandidateCvAnalysisStatus.NOT_ANALYZED.getId()))).thenReturn(step0Query);
        when(step0Query.setParameter(eq("highPriority"), eq(CandidateSkillEnrichmentService.PRIORITY_HIGH))).thenReturn(step0Query);
        when(step0Query.setMaxResults(anyInt())).thenReturn(step0Query);

        // Возвращаем задачу рекрутера с приоритетом 100
        UUID urgentAnalysisId = UUID.randomUUID();
        List<Object[]> priorityRows = Collections.singletonList(
                new Object[]{urgentAnalysisId, urgentCandidateId, urgentCvId, "Алексей Иванов (Срочный)"}
        );
        when(step0Query.getResultList()).thenReturn(priorityRows);

        // Мокирование атомарного апдейта статуса в PROCESSING
        Query updateQuery = mock(Query.class);
        when(em.createQuery(startsWith("update hunttech_CandidateCvSkillAnalysis a set a.status = :procStatus"))).thenReturn(updateQuery);
        when(updateQuery.setParameter(eq("procStatus"), eq(CandidateCvAnalysisStatus.PROCESSING.getId()))).thenReturn(updateQuery);
        when(updateQuery.setParameter(eq("now"), any(Date.class))).thenReturn(updateQuery);
        when(updateQuery.setParameter(eq("id"), eq(urgentAnalysisId))).thenReturn(updateQuery);
        when(updateQuery.setParameter(eq("expectedStatus"), eq(CandidateCvAnalysisStatus.NOT_ANALYZED.getId()))).thenReturn(updateQuery);
        when(updateQuery.executeUpdate()).thenReturn(1);

        // Вызов приватного метода selectAndLockNextCandidateCv через рефлексию
        Method selectMethod = CandidateSkillsEnrichmentWorker.class.getDeclaredMethod("selectAndLockNextCandidateCv");
        selectMethod.setAccessible(true);
        Object nextTask = selectMethod.invoke(worker);

        assertNotNull("Срочная задача рекрутера должна быть выбрана воркером", nextTask);
        Field taskCvIdField = nextTask.getClass().getDeclaredField("candidateCvId");
        taskCvIdField.setAccessible(true);
        assertEquals("Воркер должен взять CV срочного кандидата", urgentCvId, taskCvIdField.get(nextTask));

        Field statusField = nextTask.getClass().getDeclaredField("initialStatus");
        statusField.setAccessible(true);
        assertEquals("Начальный статус задачи рекрутера - NOT_ANALYZED",
                CandidateCvAnalysisStatus.NOT_ANALYZED, statusField.get(nextTask));
    }

    @Test
    public void testEndToEndWorkerCycle_CandidateEnriched_AndPriorityResetToZero() {
        UUID candidateId = UUID.randomUUID();
        JobCandidate candidate = new JobCandidate();
        candidate.setId(candidateId);
        candidate.setFullName("Екатерина Попова");

        UUID cvId = UUID.randomUUID();
        CandidateCV cv = new CandidateCV();
        cv.setId(cvId);
        cv.setCandidate(candidate);
        cv.setTextCV("Разработка на Java, Spring Boot, Hibernate, SQL.");

        // Подготовка существующей записи анализа с приоритетом 100
        CandidateCvSkillAnalysis analysis = new CandidateCvSkillAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setCandidate(candidate);
        analysis.setCandidateCv(cv);
        analysis.setStatus(CandidateCvAnalysisStatus.PROCESSING);
        analysis.setPriority(CandidateSkillEnrichmentService.PRIORITY_HIGH); // 100

        FluentLoader.ByQuery<CandidateCvSkillAnalysis, UUID> analysisLoader = mock(FluentLoader.ByQuery.class);
        when(mockDataManager.load(CandidateCvSkillAnalysis.class).query(anyString())).thenReturn(analysisLoader);
        when(analysisLoader.parameter(eq("cvId"), eq(cvId))).thenReturn(analysisLoader);
        when(analysisLoader.view("candidateCvSkillAnalysis-browse-view")).thenReturn(analysisLoader);
        when(analysisLoader.optional()).thenReturn(Optional.of(analysis));

        // Мокирование ответа AI-сервиса с найденными навыками
        SkillTree javaSkill = new SkillTree();
        javaSkill.setId(UUID.randomUUID());
        javaSkill.setSkillName("Java");

        SkillTree springSkill = new SkillTree();
        springSkill.setId(UUID.randomUUID());
        springSkill.setSkillName("Spring Boot");

        SkillAnalysisResult mainResult = SkillAnalysisResult.of(Arrays.asList(javaSkill, springSkill), null);
        SkillAnalysisResult emptyResult = SkillAnalysisResult.of(Collections.emptyList(), null);

        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), eq(SkillAnalysisService.LEVEL_MAIN), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(mainResult);
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), eq(SkillAnalysisService.LEVEL_SECONDARY), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(emptyResult);
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), eq(SkillAnalysisService.LEVEL_TERTIARY), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(emptyResult);

        // Мокирование поиска существующих навыков кандидата (пустой список)
        FluentLoader.ByQuery<CandidateSkill, UUID> skillLoader = mock(FluentLoader.ByQuery.class);
        when(mockDataManager.load(CandidateSkill.class).query(anyString())).thenReturn(skillLoader);
        when(skillLoader.parameter(eq("candidateId"), eq(candidateId))).thenReturn(skillLoader);
        when(skillLoader.view(anyString())).thenReturn(skillLoader);
        when(skillLoader.list()).thenReturn(Collections.emptyList());

        when(mockMetadata.create(CandidateSkill.class)).thenAnswer(inv -> new CandidateSkill());

        // Запуск обогащения навыков
        CandidateSkillsScanResult scanResult = service.scanAndEnrich(candidate, cv, "SKILLS_EXTRACT_BACKGROUND", true);

        // Проверяем успешное извлечение навыков
        assertEquals(2, scanResult.getTotalDetected());
        assertEquals(2, scanResult.getAddedSkills().size());

        // Проверяем, что статус стал FRESH, а приоритет сброшен обратно в PRIORITY_DEFAULT (0)
        assertEquals("После успешного анализа статус должен быть FRESH",
                CandidateCvAnalysisStatus.FRESH, analysis.getStatus());
        assertEquals("После успешного анализа приоритет должен быть сброшен в 0",
                Integer.valueOf(CandidateSkillEnrichmentService.PRIORITY_DEFAULT), analysis.getPriority());
        assertNull("Ошибок не должно быть", analysis.getLastError());
        assertEquals(Integer.valueOf(2), analysis.getSkillsFoundCount());

        verify(mockDataManager).commit(analysis);
    }

    @Test
    public void testCandidateSkillsEnrichmentMonitoring_PrioritySortingContract() throws Exception {
        // Проверка XML дескриптора дашборда мониторинга на предмет правильной сортировки очереди
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/candidateskillsenrichment/candidate-skills-enrichment-monitoring.xml");
        assertTrue("XML дашборда мониторинга должен существовать", xmlFile.exists());
        String xmlContent = new String(Files.readAllBytes(xmlFile.toPath()));

        // Проверяем сортировку: активная очередь (статусы 10=NOT_ANALYZED, 20=PROCESSING) вверху,
        // затем по убыванию приоритета (coalesce(e.priority, 0) desc), затем по дате
        assertTrue("Запрос analysesDl должен содержать приоритезацию активной очереди и приоритета",
                xmlContent.contains("order by (case when e.status in (10, 20) then 0 else 1 end), coalesce(e.priority, 0) desc"));
        assertTrue("Таблица должна содержать колонку priority",
                xmlContent.contains("<column id=\"priority\""));
    }

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.exists()) return file;
        File parent = new File("../" + path);
        if (parent.exists()) return parent;
        if (path.startsWith("modules/core/")) {
            File sub = new File(path.substring("modules/core/".length()));
            if (sub.exists()) return sub;
        }
        if (path.startsWith("modules/web/")) {
            File sub = new File("../web/" + path.substring("modules/web/".length()));
            if (sub.exists()) return sub;
            File directSub = new File(path.substring("modules/web/".length()));
            if (directSub.exists()) return directSub;
        }
        return file;
    }
}

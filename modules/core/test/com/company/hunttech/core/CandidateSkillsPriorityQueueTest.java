package com.company.hunttech.core;

import com.company.hunttech.config.HunttechSkillsEnrichmentConfig;
import com.company.hunttech.entity.*;
import com.company.hunttech.service.CandidateSkillEnrichmentService;
import com.company.hunttech.service.CandidateSkillEnrichmentServiceBean;
import com.company.hunttech.service.CandidateSkillsEnrichmentWorker;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.FluentLoader;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Модульные и контрактные тесты приоритетной очереди сервиса «Фоновое определение навыков».
 */
public class CandidateSkillsPriorityQueueTest {

    private CandidateSkillEnrichmentServiceBean service;
    private DataManager mockDataManager;
    private Metadata mockMetadata;
    private Configuration mockConfiguration;
    private HunttechSkillsEnrichmentConfig mockConfig;
    private CandidateSkillsEnrichmentWorker mockWorker;

    @Before
    public void setUp() throws Exception {
        service = new CandidateSkillEnrichmentServiceBean();
        mockDataManager = mock(DataManager.class, Mockito.RETURNS_DEEP_STUBS);
        mockMetadata = mock(Metadata.class);
        mockConfiguration = mock(Configuration.class);
        mockConfig = mock(HunttechSkillsEnrichmentConfig.class);
        mockWorker = mock(CandidateSkillsEnrichmentWorker.class);

        when(mockConfiguration.getConfig(HunttechSkillsEnrichmentConfig.class)).thenReturn(mockConfig);
        when(mockConfig.getMaxRetries()).thenReturn(4);
        when(mockConfig.getDelayBetweenRequestsSec()).thenReturn(15);
        when(mockConfig.getEnabled()).thenReturn(true);
        when(mockConfig.getAiFunctionCode()).thenReturn("SKILLS_EXTRACT_BACKGROUND");

        injectField(service, "dataManager", mockDataManager);
        injectField(service, "metadata", mockMetadata);
        injectField(service, "configuration", mockConfiguration);
        injectField(service, "enrichmentWorker", mockWorker);
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testPriorityConstants() {
        assertEquals(0, CandidateSkillEnrichmentService.PRIORITY_DEFAULT);
        assertEquals(100, CandidateSkillEnrichmentService.PRIORITY_HIGH);
    }

    @Test
    public void testCandidateCvSkillAnalysis_PriorityProperty() {
        CandidateCvSkillAnalysis analysis = new CandidateCvSkillAnalysis();
        assertEquals(Integer.valueOf(0), analysis.getPriority());

        analysis.setPriority(100);
        assertEquals(Integer.valueOf(100), analysis.getPriority());
    }

    @Test
    public void testEnqueueCandidateCvPriority_CreatesNewAnalysisRecordWithPriority100AndTriggersWorker() {
        UUID cvId = UUID.randomUUID();
        JobCandidate candidate = new JobCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Петр Петров");

        CandidateCV cv = new CandidateCV();
        cv.setId(cvId);
        cv.setCandidate(candidate);
        cv.setTextCV("Опыт: Java, Spring Boot, PostgreSQL, Docker.");

        FluentLoader.ById<CandidateCV, UUID> cvLoader = mock(FluentLoader.ById.class);
        when(mockDataManager.load(CandidateCV.class).id(cvId)).thenReturn(cvLoader);
        when(cvLoader.view("candidateCV-llm-view")).thenReturn(cvLoader);
        when(cvLoader.optional()).thenReturn(Optional.of(cv));

        // Поиск существующего анализа возвращает пустой Optional
        FluentLoader.ByQuery<CandidateCvSkillAnalysis, UUID> analysisLoader = mock(FluentLoader.ByQuery.class);
        when(mockDataManager.load(CandidateCvSkillAnalysis.class).query(anyString())).thenReturn(analysisLoader);
        when(analysisLoader.parameter(eq("cvId"), eq(cvId))).thenReturn(analysisLoader);
        when(analysisLoader.view("candidateCvSkillAnalysis-browse-view")).thenReturn(analysisLoader);
        when(analysisLoader.optional()).thenReturn(Optional.empty());

        CandidateCvSkillAnalysis createdAnalysis = new CandidateCvSkillAnalysis();
        when(mockMetadata.create(CandidateCvSkillAnalysis.class)).thenReturn(createdAnalysis);

        // Вызов постановки в приоритетную очередь
        service.enqueueCandidateCvPriority(cvId);

        // Проверяем статус и приоритет
        assertEquals(CandidateCvAnalysisStatus.NOT_ANALYZED, createdAnalysis.getStatus());
        assertEquals(Integer.valueOf(100), createdAnalysis.getPriority());
        assertEquals(candidate, createdAnalysis.getCandidate());
        assertEquals(cv, createdAnalysis.getCandidateCv());
        assertNotNull(createdAnalysis.getCvContentHash());

        // Проверяем сохранение и триггер воркера
        verify(mockDataManager).commit(createdAnalysis);
        verify(mockWorker).triggerImmediateProcessing();
    }

    @Test
    public void testDatabaseMigrationFilesExist() throws Exception {
        File sqlFile = resolveFile("modules/core/db/update/postgres/26/260921-2-addPriorityToCandidateCvSkillAnalysis.sql");
        assertTrue("SQL миграция должна существовать", sqlFile.exists());
        String sql = new String(Files.readAllBytes(sqlFile.toPath()));
        assertTrue("SQL должен добавлять колонку PRIORITY", sql.contains("PRIORITY integer DEFAULT 0"));
        assertTrue("SQL должен создавать индекс", sql.contains("IDX_CAND_CV_SKILL_ANALYSIS_PRIORITY"));

        File xmlFile = resolveFile("modules/core/db/changelog/260921-2-addPriorityToCandidateCvSkillAnalysis.xml");
        assertTrue("Changelog XML должен существовать", xmlFile.exists());

        File masterFile = resolveFile("modules/core/db/changelog/db.changelog-master.xml");
        assertTrue("db.changelog-master.xml должен существовать", masterFile.exists());
        String master = new String(Files.readAllBytes(masterFile.toPath()));
        assertTrue("Мастер-лог должен включать 260921-2-addPriorityToCandidateCvSkillAnalysis.xml",
                master.contains("260921-2-addPriorityToCandidateCvSkillAnalysis.xml"));
    }

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.exists()) return file;
        if (path.startsWith("modules/core/")) {
            File sub = new File(path.substring("modules/core/".length()));
            if (sub.exists()) return sub;
        }
        return file;
    }
}

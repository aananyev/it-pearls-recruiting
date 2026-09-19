package com.company.hunttech.core;

import com.company.hunttech.config.HunttechSkillsEnrichmentConfig;
import com.company.hunttech.entity.*;
import com.company.hunttech.service.*;
import com.company.hunttech.service.dto.CandidateSkillsEnrichmentKpiDto;
import com.company.hunttech.service.dto.CandidateSkillsScanResult;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Модульные и контрактные тесты подсистемы Candidate Skills Enrichment.
 */
public class CandidateSkillEnrichmentServiceTest {

    private CandidateSkillEnrichmentServiceBean service;
    private SkillAnalysisService mockSkillAnalysisService;
    private DataManager mockDataManager;
    private Metadata mockMetadata;
    private Persistence mockPersistence;
    private Configuration mockConfiguration;
    private HunttechSkillsEnrichmentConfig mockConfig;

    @Before
    public void setUp() throws Exception {
        service = new CandidateSkillEnrichmentServiceBean();
        mockSkillAnalysisService = mock(SkillAnalysisService.class);
        mockDataManager = mock(DataManager.class, Mockito.RETURNS_DEEP_STUBS);
        mockMetadata = mock(Metadata.class);
        mockPersistence = mock(Persistence.class);
        mockConfiguration = mock(Configuration.class);
        mockConfig = mock(HunttechSkillsEnrichmentConfig.class);

        when(mockConfiguration.getConfig(HunttechSkillsEnrichmentConfig.class)).thenReturn(mockConfig);
        when(mockConfig.getMaxRetries()).thenReturn(4);
        when(mockConfig.getDelayBetweenRequestsSec()).thenReturn(15);
        when(mockConfig.getEnabled()).thenReturn(true);
        when(mockConfig.getAiFunctionCode()).thenReturn("SKILLS_EXTRACT_BACKGROUND");

        injectField(service, "skillAnalysisService", mockSkillAnalysisService);
        injectField(service, "dataManager", mockDataManager);
        injectField(service, "metadata", mockMetadata);
        injectField(service, "persistence", mockPersistence);
        injectField(service, "configuration", mockConfiguration);
    }

    private void injectField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    public void testHashCalculation_NormalizedAndConsistent() {
        String html1 = "<p>Опыт работы: <b>Java</b>, Spring Boot, PostgreSQL.</p>";
        String html2 = "   Опыт работы:   Java,   Spring   Boot, PostgreSQL.  ";

        String hash1 = service.calculateNormalizedCvHash(html1);
        String hash2 = service.calculateNormalizedCvHash(html2);

        assertNotNull("Хэш не должен быть null", hash1);
        assertEquals("Хэш для семантически одинакового текста должен совпадать", hash1, hash2);
    }

    @Test
    public void testHashCalculation_EmptyOrWhitespaceReturnsNull() {
        assertNull(service.calculateNormalizedCvHash(null));
        assertNull(service.calculateNormalizedCvHash("   "));
        assertNull(service.calculateNormalizedCvHash("   <br/>  <p></p>  "));
    }

    @Test
    public void testScanAndEnrich_EmptyCv_DoesNotCallAiAndMarksSkipped() {
        JobCandidate candidate = new JobCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Иван Иванов");

        CandidateCV cv = new CandidateCV();
        cv.setId(UUID.randomUUID());
        cv.setTextCV("");

        CandidateCvSkillAnalysis mockAnalysis = new CandidateCvSkillAnalysis();
        when(mockMetadata.create(CandidateCvSkillAnalysis.class)).thenReturn(mockAnalysis);

        CandidateSkillsScanResult result = service.scanAndEnrich(candidate, cv, null, true);

        assertFalse("Результат для пустого CV должен быть неуспешным", result.isSuccess());
        verify(mockSkillAnalysisService, never()).analyzeWithFunction(anyString(), anyString(), anyString(), anyBoolean());
        verify(mockDataManager).commit(mockAnalysis);
        assertEquals("Статус должен быть SKIPPED", CandidateCvAnalysisStatus.SKIPPED, mockAnalysis.getStatus());
    }

    @Test
    public void testScanAndEnrich_ProviderError_GoesToRetryWithBackoff() {
        JobCandidate candidate = new JobCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Петр Петров");

        CandidateCV cv = new CandidateCV();
        cv.setId(UUID.randomUUID());
        cv.setTextCV("Опыт: Java, Kafka");

        CandidateCvSkillAnalysis mockAnalysis = new CandidateCvSkillAnalysis();
        when(mockMetadata.create(CandidateCvSkillAnalysis.class)).thenReturn(mockAnalysis);

        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), anyString(), anyString(), eq(false)))
                .thenThrow(new RuntimeException("429 Too Many Requests: Rate limit reached"));

        CandidateSkillsScanResult result = service.scanAndEnrich(candidate, cv, "SKILLS_EXTRACT_BACKGROUND", true);

        assertFalse(result.isSuccess());
        assertEquals("Статус должен стать RETRY", CandidateCvAnalysisStatus.RETRY, mockAnalysis.getStatus());
        assertEquals("Попытка должна стать 1", Integer.valueOf(1), mockAnalysis.getRetryCount());
        assertNotNull("Должно быть выставлено время следующего повтора", mockAnalysis.getNextRetryAt());
        assertTrue("Время повтора должно быть в будущем", mockAnalysis.getNextRetryAt().after(new Date()));
    }

    @Test
    public void testBackoffIntervals_Exponential() throws Exception {
        java.lang.reflect.Method method = CandidateSkillEnrichmentServiceBean.class.getDeclaredMethod("calculateBackoffMinutes", int.class);
        method.setAccessible(true);

        long b1 = (long) method.invoke(service, 1);
        long b2 = (long) method.invoke(service, 2);
        long b3 = (long) method.invoke(service, 3);
        long b4 = (long) method.invoke(service, 4);

        assertEquals("Попытка 1: задержка 1 минута", 1L, b1);
        assertEquals("Попытка 2: задержка 5 минут", 5L, b2);
        assertEquals("Попытка 3: задержка 15 минут", 15L, b3);
        assertEquals("Попытка 4+: задержка 60 минут", 60L, b4);
    }
}

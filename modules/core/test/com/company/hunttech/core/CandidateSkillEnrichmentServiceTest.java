package com.company.hunttech.core;

import com.company.hunttech.config.HunttechSkillsEnrichmentConfig;
import com.company.hunttech.entity.ai.AiCapability;
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
        verify(mockSkillAnalysisService, never()).analyzeWithFunction(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean());
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
        mockAnalysis.setProviderCode("previous-provider");
        mockAnalysis.setModelName("previous-model");
        mockAnalysis.setExecutionSource(CandidateCvSkillAnalysis.EXECUTION_SOURCE_AI);
        when(mockDataManager.load(CandidateCvSkillAnalysis.class)
                .query(anyString())
                .parameter("cvId", cv.getId())
                .view("candidateCvSkillAnalysis-browse-view")
                .optional()).thenReturn(Optional.of(mockAnalysis));

        when(mockConfig.getFreeOnly()).thenReturn(true);
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), anyString(), anyString(), eq(false), eq(true)))
                .thenThrow(new RuntimeException("429 Too Many Requests: Rate limit reached"));

        CandidateSkillsScanResult result = service.scanAndEnrich(candidate, cv, "SKILLS_EXTRACT_BACKGROUND", true);

        assertFalse(result.isSuccess());
        assertEquals("Статус должен стать RETRY", CandidateCvAnalysisStatus.RETRY, mockAnalysis.getStatus());
        assertEquals("Попытка должна стать 1", Integer.valueOf(1), mockAnalysis.getRetryCount());
        assertNotNull("Должно быть выставлено время следующего повтора", mockAnalysis.getNextRetryAt());
        assertTrue("Время повтора должно быть в будущем", mockAnalysis.getNextRetryAt().after(new Date()));
        assertEquals("Ошибка не должна затирать provider успешного вызова", "previous-provider", mockAnalysis.getProviderCode());
        assertEquals("Ошибка не должна затирать model успешного вызова", "previous-model", mockAnalysis.getModelName());
        assertEquals(CandidateCvSkillAnalysis.EXECUTION_SOURCE_AI, mockAnalysis.getExecutionSource());
    }

    @Test
    public void testScanAndEnrich_FinalErrorPreservesPreviousSuccessfulMetadata() {
        JobCandidate candidate = new JobCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Сергей Орлов");
        CandidateCV cv = new CandidateCV();
        cv.setId(UUID.randomUUID());
        cv.setTextCV("Опыт: Java, Kafka");
        CandidateCvSkillAnalysis existingAnalysis = new CandidateCvSkillAnalysis();
        existingAnalysis.setProviderCode("previous-provider");
        existingAnalysis.setModelName("previous-model");
        existingAnalysis.setExecutionSource(CandidateCvSkillAnalysis.EXECUTION_SOURCE_AI);
        when(mockDataManager.load(CandidateCvSkillAnalysis.class)
                .query(anyString())
                .parameter("cvId", cv.getId())
                .view("candidateCvSkillAnalysis-browse-view")
                .optional()).thenReturn(Optional.of(existingAnalysis));
        when(mockConfig.getFreeOnly()).thenReturn(true);
        when(mockConfig.getMaxRetries()).thenReturn(1);
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), anyString(), anyString(), eq(false), eq(true)))
                .thenThrow(new RuntimeException("Provider unavailable"));

        CandidateSkillsScanResult result = service.scanAndEnrich(candidate, cv, "SKILLS_EXTRACT_BACKGROUND", true);

        assertFalse(result.isSuccess());
        assertEquals(CandidateCvAnalysisStatus.ERROR, existingAnalysis.getStatus());
        assertEquals("previous-provider", existingAnalysis.getProviderCode());
        assertEquals("previous-model", existingAnalysis.getModelName());
        assertEquals(CandidateCvSkillAnalysis.EXECUTION_SOURCE_AI, existingAnalysis.getExecutionSource());
    }

    @Test
    public void testScanAndEnrich_ForceScanBypassesFreshHashGuard() {
        JobCandidate candidate = new JobCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Анна Иванова");

        CandidateCV cv = new CandidateCV();
        cv.setId(UUID.randomUUID());
        cv.setTextCV("Java, Spring, PostgreSQL");

        CandidateCvSkillAnalysis existingAnalysis = new CandidateCvSkillAnalysis();
        existingAnalysis.setStatus(CandidateCvAnalysisStatus.FRESH);
        existingAnalysis.setCvContentHash(service.calculateNormalizedCvHash(cv.getTextCV()));
        existingAnalysis.setSkillsConfigurationVersion(1);
        when(mockDataManager.load(CandidateCvSkillAnalysis.class)
                .query(anyString())
                .parameter("cvId", cv.getId())
                .view("candidateCvSkillAnalysis-browse-view")
                .optional()).thenReturn(Optional.of(existingAnalysis));
        when(mockDataManager.load(CandidateSkill.class)
                .query(anyString())
                .parameter("candidateId", candidate.getId())
                .view("candidateSkill-view")
                .list()).thenReturn(Collections.emptyList());

        SkillAnalysisResult noSkills = SkillAnalysisResult.of(Collections.emptyList(), null);
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(noSkills);

        CandidateSkillsScanResult result = service.scanAndEnrich(candidate, cv, null, false, true);

        assertTrue("Принудительная актуализация должна пройти стандартный pipeline", result.isSuccess());
        verify(mockSkillAnalysisService, times(4))
                .analyzeWithFunction(anyString(), anyString(), eq(SkillAnalysisService.FUNCTION_SKILLS_EXTRACT), eq(true), eq(false));
    }

    @Test
    public void testScanAndEnrich_SavesActualDeepSeekExecutionMetadata() {
        JobCandidate candidate = new JobCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Мария Смирнова");
        CandidateCV cv = new CandidateCV();
        cv.setId(UUID.randomUUID());
        cv.setTextCV("Java, Spring, PostgreSQL");
        CandidateCvSkillAnalysis mockAnalysis = new CandidateCvSkillAnalysis();
        when(mockMetadata.create(CandidateCvSkillAnalysis.class)).thenReturn(mockAnalysis);
        when(mockDataManager.load(CandidateSkill.class).query(anyString()).parameter("candidateId", candidate.getId()).view("candidateSkill-view").list())
                .thenReturn(Collections.emptyList());
        when(mockDataManager.load(CandidateCvSkillAnalysis.class).query(anyString()).parameter("cvId", cv.getId()).view("candidateCvSkillAnalysis-browse-view").optional())
                .thenReturn(Optional.empty());
        AiExecutionResult execution = AiExecutionResult.textResult("SKILLS_EXTRACT", "Skills", AiCapability.TEXT_GENERATION,
                "test-model", "test-provider", AiCredentialOwner.ADMIN, "[]", 1, 2, 3);
        SkillAnalysisResult aiResult = SkillAnalysisResult.of(Collections.emptyList(), execution);
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(aiResult);
        CandidateSkillsScanResult result = service.scanAndEnrich(candidate, cv, "SKILLS_EXTRACT", false);
        assertTrue(result.isSuccess());
        assertEquals(CandidateCvSkillAnalysis.EXECUTION_SOURCE_AI, mockAnalysis.getExecutionSource());
        assertEquals("test-provider", mockAnalysis.getProviderCode());
        assertEquals("test-model", mockAnalysis.getModelName());
        verify(mockDataManager, atLeastOnce()).commit(mockAnalysis);
    }

    @Test
    public void testScanAndEnrich_PersistsPartialAiMetadataWithoutInventingMissingValue() {
        JobCandidate candidate = new JobCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Ольга Соколова");
        CandidateCV cv = new CandidateCV();
        cv.setId(UUID.randomUUID());
        cv.setTextCV("Java, Spring, PostgreSQL");
        CandidateCvSkillAnalysis mockAnalysis = new CandidateCvSkillAnalysis();
        when(mockMetadata.create(CandidateCvSkillAnalysis.class)).thenReturn(mockAnalysis);
        when(mockDataManager.load(CandidateSkill.class).query(anyString()).parameter("candidateId", candidate.getId()).view("candidateSkill-view").list())
                .thenReturn(Collections.emptyList());
        when(mockDataManager.load(CandidateCvSkillAnalysis.class).query(anyString()).parameter("cvId", cv.getId()).view("candidateCvSkillAnalysis-browse-view").optional())
                .thenReturn(Optional.empty());
        AiExecutionResult execution = AiExecutionResult.textResult("SKILLS_EXTRACT", "Skills", AiCapability.TEXT_GENERATION,
                "  ", " actual-provider ", AiCredentialOwner.ADMIN, "[]", 1, 2, 3);
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(SkillAnalysisResult.of(Collections.emptyList(), execution));

        CandidateSkillsScanResult result = service.scanAndEnrich(candidate, cv, "SKILLS_EXTRACT", false);

        assertTrue(result.isSuccess());
        assertEquals(CandidateCvSkillAnalysis.EXECUTION_SOURCE_AI_METADATA_INCOMPLETE, mockAnalysis.getExecutionSource());
        assertEquals("actual-provider", mockAnalysis.getProviderCode());
        assertNull("Отсутствующая модель не должна подменяться текущей конфигурацией", mockAnalysis.getModelName());
    }

    @Test
    public void testScanAndEnrich_DictionaryFallbackDoesNotInventAiMetadata() {
        JobCandidate candidate = new JobCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Анна Петрова");
        CandidateCV cv = new CandidateCV();
        cv.setId(UUID.randomUUID());
        cv.setTextCV("Java, Spring, PostgreSQL");
        CandidateCvSkillAnalysis mockAnalysis = new CandidateCvSkillAnalysis();
        when(mockMetadata.create(CandidateCvSkillAnalysis.class)).thenReturn(mockAnalysis);
        when(mockDataManager.load(CandidateSkill.class).query(anyString()).parameter("candidateId", candidate.getId()).view("candidateSkill-view").list())
                .thenReturn(Collections.emptyList());
        when(mockDataManager.load(CandidateCvSkillAnalysis.class).query(anyString()).parameter("cvId", cv.getId()).view("candidateCvSkillAnalysis-browse-view").optional())
                .thenReturn(Optional.empty());
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(SkillAnalysisResult.of(Collections.emptyList(), null));

        CandidateSkillsScanResult result = service.scanAndEnrich(candidate, cv, "SKILLS_EXTRACT", false);

        assertTrue(result.isSuccess());
        assertEquals(CandidateCvSkillAnalysis.EXECUTION_SOURCE_DICTIONARY_FALLBACK, mockAnalysis.getExecutionSource());
        assertNull(mockAnalysis.getProviderCode());
        assertNull(mockAnalysis.getModelName());
        verify(mockDataManager, atLeastOnce()).commit(mockAnalysis);
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

    @Test
    public void testScanAndEnrich_SavesActualAiExecutionMetadata() {
        JobCandidate candidate = new JobCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Мария Смирнова");
        CandidateCV cv = new CandidateCV();
        cv.setId(UUID.randomUUID());
        cv.setTextCV("Java, Spring, PostgreSQL");

        CandidateCvSkillAnalysis analysis = new CandidateCvSkillAnalysis();
        when(mockMetadata.create(CandidateCvSkillAnalysis.class)).thenReturn(analysis);
        when(mockDataManager.load(CandidateSkill.class).query(anyString()).parameter("candidateId", candidate.getId())
                .view("candidateSkill-view").list()).thenReturn(Collections.emptyList());
        when(mockDataManager.load(CandidateCvSkillAnalysis.class).query(anyString()).parameter("cvId", cv.getId())
                .view("candidateCvSkillAnalysis-browse-view").optional()).thenReturn(Optional.empty());

        AiExecutionResult execution = AiExecutionResult.textResult("SKILLS_EXTRACT", "Skills", AiCapability.TEXT_GENERATION,
                "deepseek-v4-flash", "deepseek", AiCredentialOwner.ADMIN, "[]", 1, 2, 3);
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), anyString(), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(SkillAnalysisResult.of(Collections.emptyList(), execution));

        CandidateSkillsScanResult result = service.scanAndEnrich(candidate, cv, "SKILLS_EXTRACT", false);

        assertTrue(result.isSuccess());
        assertEquals(CandidateCvSkillAnalysis.EXECUTION_SOURCE_AI, analysis.getExecutionSource());
        assertEquals("deepseek", analysis.getProviderCode());
        assertEquals("deepseek-v4-flash", analysis.getModelName());
        verify(mockDataManager, atLeastOnce()).commit(analysis);
    }

    @Test
    public void testFreeOnlyToggle() {
        when(mockConfig.getFreeOnly()).thenReturn(true);
        assertTrue("Должен возвращать true из конфигурации", service.isFreeOnly());

        service.setFreeOnly(false);
        verify(mockConfig).setFreeOnly(false);
    }

    @Test
    public void testReprocessCv_ResetsContentHash() {
        UUID cvId = UUID.randomUUID();
        CandidateCvSkillAnalysis mockAnalysis = new CandidateCvSkillAnalysis();
        mockAnalysis.setStatus(CandidateCvAnalysisStatus.FRESH);
        mockAnalysis.setCvContentHash("existing-hash");

        when(mockDataManager.load(CandidateCvSkillAnalysis.class)
                .query(anyString())
                .parameter("cvId", cvId)
                .view("candidateCvSkillAnalysis-browse-view")
                .optional()).thenReturn(Optional.of(mockAnalysis));

        service.reprocessCv(cvId);

        assertEquals("Статус должен быть NOT_ANALYZED", CandidateCvAnalysisStatus.NOT_ANALYZED, mockAnalysis.getStatus());
        assertEquals("Приоритет должен быть HIGH", Integer.valueOf(CandidateSkillEnrichmentService.PRIORITY_HIGH), mockAnalysis.getPriority());
        assertNull("cvContentHash должен быть сброшен в null для форсированного повторного анализа", mockAnalysis.getCvContentHash());
        verify(mockDataManager).commit(mockAnalysis);
    }

    @Test
    public void testScanAndEnrich_SumsTokensAcrossAllLevels() {
        JobCandidate candidate = new JobCandidate();
        candidate.setId(UUID.randomUUID());
        candidate.setFullName("Алексей Тестов");
        CandidateCV cv = new CandidateCV();
        cv.setId(UUID.randomUUID());
        cv.setTextCV("Java, Spring, SQL");

        CandidateCvSkillAnalysis analysis = new CandidateCvSkillAnalysis();
        when(mockMetadata.create(CandidateCvSkillAnalysis.class)).thenReturn(analysis);
        when(mockDataManager.load(CandidateSkill.class).query(anyString()).parameter("candidateId", candidate.getId())
                .view("candidateSkill-view").list()).thenReturn(Collections.emptyList());
        when(mockDataManager.load(CandidateCvSkillAnalysis.class).query(anyString()).parameter("cvId", cv.getId())
                .view("candidateCvSkillAnalysis-browse-view").optional()).thenReturn(Optional.empty());

        AiExecutionResult exec1 = AiExecutionResult.textResult("SKILLS_EXTRACT", "Skills", AiCapability.TEXT_GENERATION,
                "deepseek-v4-flash", "deepseek", AiCredentialOwner.ADMIN, "[]", 100, 50, 150);
        AiExecutionResult exec2 = AiExecutionResult.textResult("SKILLS_EXTRACT", "Skills", AiCapability.TEXT_GENERATION,
                "deepseek-v4-flash", "deepseek", AiCredentialOwner.ADMIN, "[]", 200, 80, 280);
        AiExecutionResult exec3 = AiExecutionResult.textResult("SKILLS_EXTRACT", "Skills", AiCapability.TEXT_GENERATION,
                "deepseek-v4-flash", "deepseek", AiCredentialOwner.ADMIN, "[]", 150, 60, 210);

        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), eq(SkillAnalysisService.LEVEL_MAIN), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(SkillAnalysisResult.of(Collections.emptyList(), exec1));
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), eq(SkillAnalysisService.LEVEL_SECONDARY), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(SkillAnalysisResult.of(Collections.emptyList(), exec2));
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), eq(SkillAnalysisService.LEVEL_TERTIARY), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(SkillAnalysisResult.of(Collections.emptyList(), exec3));
        when(mockSkillAnalysisService.analyzeWithFunction(anyString(), eq(SkillAnalysisService.LEVEL_ALL), anyString(), anyBoolean(), anyBoolean()))
                .thenReturn(SkillAnalysisResult.of(Collections.emptyList(), null));

        CandidateSkillsScanResult result = service.scanAndEnrich(candidate, cv, "SKILLS_EXTRACT", false);

        assertTrue(result.isSuccess());
        assertEquals("Провайдер должен совпадать с ответом AI", "deepseek", analysis.getProviderCode());
        assertEquals("Модель должна совпадать с ответом AI", "deepseek-v4-flash", analysis.getModelName());
        assertEquals("Prompt tokens должны суммироваться (100 + 200 + 150)", Integer.valueOf(450), analysis.getPromptTokens());
        assertEquals("Completion tokens должны суммироваться (50 + 80 + 60)", Integer.valueOf(190), analysis.getCompletionTokens());
        assertEquals("Total tokens должны суммироваться (150 + 280 + 210)", Integer.valueOf(640), analysis.getTotalTokens());
    }
}

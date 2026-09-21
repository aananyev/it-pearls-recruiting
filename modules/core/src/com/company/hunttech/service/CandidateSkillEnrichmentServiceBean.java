package com.company.hunttech.service;

import com.company.hunttech.config.HunttechSkillsEnrichmentConfig;
import com.company.hunttech.entity.*;
import com.company.hunttech.service.dto.CandidateSkillsEnrichmentKpiDto;
import com.company.hunttech.service.dto.CandidateSkillsScanResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haulmont.cuba.core.EntityManager;
import com.haulmont.cuba.core.Persistence;
import com.haulmont.cuba.core.Transaction;
import com.haulmont.cuba.core.global.CommitContext;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service(CandidateSkillEnrichmentService.NAME)
public class CandidateSkillEnrichmentServiceBean implements CandidateSkillEnrichmentService {

    private static final Logger log = LoggerFactory.getLogger(CandidateSkillEnrichmentServiceBean.class);

    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private Persistence persistence;
    @Inject
    private Configuration configuration;
    @Inject
    private SkillAnalysisService skillAnalysisService;
    @Inject
    private CandidateSkillsEnrichmentWorker enrichmentWorker;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public CandidateSkillsScanResult scanAndEnrich(JobCandidate candidate, CandidateCV cv, String aiFunctionCode, boolean isBackground) {
        long startTime = System.currentTimeMillis();
        CandidateSkillsScanResult result = new CandidateSkillsScanResult();

        if (candidate == null || cv == null) {
            result.setSuccess(false);
            result.setRawError("Кандидат или резюме не заданы");
            return result;
        }

        String rawText = cv.getTextCV();
        if (rawText == null || rawText.trim().isEmpty()) {
            markCvAnalysisStatus(candidate, cv, CandidateCvAnalysisStatus.SKIPPED, "Текст резюме пуст", null, 0);
            result.setSuccess(false);
            result.setRawError("Текст резюме пуст");
            return result;
        }

        String cleanText;
        try {
            cleanText = Jsoup.parse(rawText).text();
        } catch (Exception e) {
            cleanText = rawText;
        }

        if (cleanText == null || cleanText.trim().isEmpty()) {
            markCvAnalysisStatus(candidate, cv, CandidateCvAnalysisStatus.SKIPPED, "Текст резюме после очистки разметки пуст", null, 0);
            result.setSuccess(false);
            result.setRawError("Текст резюме после очистки HTML-разметки пуст");
            return result;
        }

        String contentHash = calculateNormalizedCvHash(rawText);
        String effectiveFunctionCode = (aiFunctionCode != null && !aiFunctionCode.trim().isEmpty())
                ? aiFunctionCode.trim()
                : (isBackground ? SkillAnalysisService.FUNCTION_SKILLS_EXTRACT_BACKGROUND : SkillAnalysisService.FUNCTION_SKILLS_EXTRACT);

        Integer configVersion = loadFunctionVersion(effectiveFunctionCode);

        // 1. Проверяем, не является ли CV уже актуальным (FRESH)
        CandidateCvSkillAnalysis existingAnalysis = loadAnalysisRecord(cv.getId());
        if (existingAnalysis != null && existingAnalysis.getStatus() == CandidateCvAnalysisStatus.FRESH) {
            if (Objects.equals(existingAnalysis.getCvContentHash(), contentHash)
                    && Objects.equals(existingAnalysis.getSkillsConfigurationVersion(), configVersion)) {
                log.info("CV кандидата {} (CV ID: {}) уже проанализировано актуальной версией AI (хэш совпадает), повторный вызов пропущен",
                        candidate.getFullName(), cv.getId());
                result.setSuccess(true);
                result.setDurationMs(System.currentTimeMillis() - startTime);
                return result;
            } else {
                log.info("Содержимое CV кандидата {} (CV ID: {}) изменилось или устарела версия AI -> перевод в STALE",
                        candidate.getFullName(), cv.getId());
                existingAnalysis.setStatus(CandidateCvAnalysisStatus.STALE);
                dataManager.commit(existingAnalysis);
            }
        }

        // 2. Вызов AI через SkillAnalysisService (ВНЕ транзакции БД!)
        boolean freeOnly = isBackground && configuration.getConfig(HunttechSkillsEnrichmentConfig.class).getFreeOnly();
        boolean allowFallback = !isBackground || !freeOnly; // В фоне при FREE_ONLY строгий запрет fallback на словарный поиск
        SkillAnalysisResult mainResult = null;
        SkillAnalysisResult secondaryResult = null;
        SkillAnalysisResult tertiaryResult = null;
        SkillAnalysisResult allResult = null;
        AiExecutionResult aiExecution = null;

        try {
            mainResult = skillAnalysisService.analyzeWithFunction(cleanText, SkillAnalysisService.LEVEL_MAIN, effectiveFunctionCode, allowFallback, freeOnly);
            secondaryResult = skillAnalysisService.analyzeWithFunction(cleanText, SkillAnalysisService.LEVEL_SECONDARY, effectiveFunctionCode, allowFallback, freeOnly);
            tertiaryResult = skillAnalysisService.analyzeWithFunction(cleanText, SkillAnalysisService.LEVEL_TERTIARY, effectiveFunctionCode, allowFallback, freeOnly);

            List<SkillTree> mainSkills = mainResult.getSkills() != null ? mainResult.getSkills() : Collections.emptyList();
            List<SkillTree> secondarySkills = secondaryResult.getSkills() != null ? secondaryResult.getSkills() : Collections.emptyList();
            List<SkillTree> tertiarySkills = tertiaryResult.getSkills() != null ? tertiaryResult.getSkills() : Collections.emptyList();

            if (mainSkills.isEmpty() && secondarySkills.isEmpty() && tertiarySkills.isEmpty()) {
                allResult = skillAnalysisService.analyzeWithFunction(cleanText, SkillAnalysisService.LEVEL_ALL, effectiveFunctionCode, allowFallback, freeOnly);
                mainSkills = allResult.getSkills() != null ? allResult.getSkills() : Collections.emptyList();
            }

            aiExecution = firstNonNullExecution(mainResult, secondaryResult, tertiaryResult, allResult);
            result.setAiExecution(aiExecution);

            // 3. Расчет дельты изменений и сохранение CandidateSkill (в короткой транзакции)
            applySkillsDelta(candidate, cv, mainSkills, secondarySkills, tertiarySkills, isBackground, result);

            result.setSuccess(true);
            result.setDurationMs(System.currentTimeMillis() - startTime);

            // 4. Сохранение статуса FRESH и аудита в CandidateCvSkillAnalysis
            saveSuccessAnalysisRecord(candidate, cv, contentHash, configVersion, effectiveFunctionCode, aiExecution, result);

            log.info("Успешно завершен AI-анализ навыков кандидата {} (CV ID: {}): обнаружено={}, добавлено={}, обновлено={}",
                    candidate.getFullName(), cv.getId(), result.getTotalDetected(), result.getAddedSkills().size(), result.getUpdatedSkills().size());

            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setRawError(e.getMessage());
            result.setDurationMs(duration);

            handleAnalysisError(candidate, cv, contentHash, configVersion, effectiveFunctionCode, e, duration);
            return result;
        }
    }

    private void applySkillsDelta(JobCandidate candidate, CandidateCV cv,
                                  List<SkillTree> mainSkills,
                                  List<SkillTree> secondarySkills,
                                  List<SkillTree> tertiarySkills,
                                  boolean isBackground,
                                  CandidateSkillsScanResult result) {

        List<CandidateSkill> existingSkills = dataManager.load(CandidateSkill.class)
                .query("select e from hunttech_CandidateSkill e where e.candidate.id = :candidateId")
                .parameter("candidateId", candidate.getId())
                .view("candidateSkill-view")
                .list();

        Map<UUID, CandidateSkill> existingMap = new HashMap<>();
        for (CandidateSkill cs : existingSkills) {
            if (cs.getSkill() != null) {
                existingMap.put(cs.getSkill().getId(), cs);
            }
        }

        List<CandidateSkill> toSave = new ArrayList<>();
        Set<UUID> processedIds = new HashSet<>();

        // 1. Обработка основных навыков (MAIN)
        for (SkillTree st : mainSkills) {
            if (st == null || !processedIds.add(st.getId())) continue;
            CandidateSkill existing = existingMap.get(st.getId());
            if (existing == null) {
                CandidateSkill cs = metadata.create(CandidateSkill.class);
                cs.setCandidate(candidate);
                cs.setSkill(st);
                cs.setPriority(CandidateSkillPriority.MAIN);
                cs.setSourceType(isBackground ? "AI_BG" : "AI");
                cs.setSourceCv(cv);
                toSave.add(cs);
                result.getAddedSkills().add(new CandidateSkillsScanResult.SkillDeltaItem(st.getSkillName(), CandidateSkillPriority.MAIN));
            } else {
                if (existing.getPriority() == CandidateSkillPriority.MAIN) {
                    result.getUnchangedSkills().add(new CandidateSkillsScanResult.SkillDeltaItem(st.getSkillName(), CandidateSkillPriority.MAIN));
                } else {
                    CandidateSkillPriority oldPriority = existing.getPriority();
                    existing.setPriority(CandidateSkillPriority.MAIN);
                    toSave.add(existing);
                    result.getUpdatedSkills().add(new CandidateSkillsScanResult.SkillDeltaItem(
                            st.getSkillName(), CandidateSkillPriority.MAIN, oldPriority, "Повышен до основного"));
                }
            }
        }

        // 2. Обработка второстепенных навыков (SECONDARY)
        for (SkillTree st : secondarySkills) {
            if (st == null || !processedIds.add(st.getId())) continue;
            CandidateSkill existing = existingMap.get(st.getId());
            if (existing == null) {
                CandidateSkill cs = metadata.create(CandidateSkill.class);
                cs.setCandidate(candidate);
                cs.setSkill(st);
                cs.setPriority(CandidateSkillPriority.SECONDARY);
                cs.setSourceType(isBackground ? "AI_BG" : "AI");
                cs.setSourceCv(cv);
                toSave.add(cs);
                result.getAddedSkills().add(new CandidateSkillsScanResult.SkillDeltaItem(st.getSkillName(), CandidateSkillPriority.SECONDARY));
            } else {
                if (existing.getPriority() == CandidateSkillPriority.MAIN) {
                    // Уже был MAIN — не понижаем!
                    result.getUnchangedSkills().add(new CandidateSkillsScanResult.SkillDeltaItem(st.getSkillName(), CandidateSkillPriority.MAIN));
                } else if (existing.getPriority() == CandidateSkillPriority.SECONDARY) {
                    result.getUnchangedSkills().add(new CandidateSkillsScanResult.SkillDeltaItem(st.getSkillName(), CandidateSkillPriority.SECONDARY));
                } else {
                    CandidateSkillPriority oldPriority = existing.getPriority();
                    existing.setPriority(CandidateSkillPriority.SECONDARY);
                    toSave.add(existing);
                    result.getUpdatedSkills().add(new CandidateSkillsScanResult.SkillDeltaItem(
                            st.getSkillName(), CandidateSkillPriority.SECONDARY, oldPriority, "Повышен до второстепенного"));
                }
            }
        }

        // 3. Обработка третьестепенных навыков (TERTIARY)
        for (SkillTree st : tertiarySkills) {
            if (st == null || !processedIds.add(st.getId())) continue;
            CandidateSkill existing = existingMap.get(st.getId());
            if (existing == null) {
                CandidateSkill cs = metadata.create(CandidateSkill.class);
                cs.setCandidate(candidate);
                cs.setSkill(st);
                cs.setPriority(CandidateSkillPriority.TERTIARY);
                cs.setSourceType(isBackground ? "AI_BG" : "AI");
                cs.setSourceCv(cv);
                toSave.add(cs);
                result.getAddedSkills().add(new CandidateSkillsScanResult.SkillDeltaItem(st.getSkillName(), CandidateSkillPriority.TERTIARY));
            } else {
                result.getUnchangedSkills().add(new CandidateSkillsScanResult.SkillDeltaItem(st.getSkillName(), existing.getPriority()));
            }
        }

        result.setTotalDetected(mainSkills.size() + secondarySkills.size() + tertiarySkills.size());

        if (!toSave.isEmpty()) {
            dataManager.commit(new CommitContext(toSave));
        }
    }

    private void saveSuccessAnalysisRecord(JobCandidate candidate, CandidateCV cv, String contentHash,
                                           Integer configVersion, String functionCode,
                                           AiExecutionResult aiExecution,
                                           CandidateSkillsScanResult scanResult) {
        CandidateCvSkillAnalysis analysis = loadAnalysisRecord(cv.getId());
        if (analysis == null) {
            analysis = metadata.create(CandidateCvSkillAnalysis.class);
            analysis.setCandidate(candidate);
            analysis.setCandidateCv(cv);
        }

        analysis.setStatus(CandidateCvAnalysisStatus.FRESH);
        analysis.setCvContentHash(contentHash);
        analysis.setSkillsAnalyzedAt(new Date());
        analysis.setProcessingFinishedAt(new Date());
        analysis.setDurationMs(scanResult.getDurationMs());
        analysis.setSkillsConfigurationVersion(configVersion);
        analysis.setRetryCount(0);
        analysis.setNextRetryAt(null);
        analysis.setLastError(null);
        analysis.setPriority(CandidateSkillEnrichmentService.PRIORITY_DEFAULT);
        analysis.setAiFunctionCode(functionCode);

        if (aiExecution != null) {
            analysis.setProviderCode(aiExecution.getProviderCode());
            analysis.setModelName(aiExecution.getModelName());
            analysis.setPromptTokens(aiExecution.getPromptTokens());
            analysis.setCompletionTokens(aiExecution.getCompletionTokens());
            analysis.setTotalTokens(aiExecution.getTotalTokens());
        }

        analysis.setSkillsFoundCount(scanResult.getTotalDetected());
        analysis.setSkillsAddedCount(scanResult.getAddedSkills().size());
        analysis.setSkillsUpdatedCount(scanResult.getUpdatedSkills().size());
        analysis.setSkillsUnchangedCount(scanResult.getUnchangedSkills().size());
        analysis.setSkillsSkippedCount(scanResult.getSkippedSkills().size());

        try {
            analysis.setDeltaDetailsJson(objectMapper.writeValueAsString(scanResult));
        } catch (Exception e) {
            log.warn("Не удалось сериализовать deltaDetailsJson: {}", e.getMessage());
        }

        dataManager.commit(analysis);
    }

    private void handleAnalysisError(JobCandidate candidate, CandidateCV cv, String contentHash,
                                     Integer configVersion, String functionCode,
                                     Exception error, long durationMs) {
        CandidateCvSkillAnalysis analysis = loadAnalysisRecord(cv.getId());
        if (analysis == null) {
            analysis = metadata.create(CandidateCvSkillAnalysis.class);
            analysis.setCandidate(candidate);
            analysis.setCandidateCv(cv);
        }

        int maxRetries = Math.max(1, configuration.getConfig(HunttechSkillsEnrichmentConfig.class).getMaxRetries());
        int currentRetries = analysis.getRetryCount() != null ? analysis.getRetryCount() + 1 : 1;
        analysis.setRetryCount(currentRetries);
        analysis.setDurationMs(durationMs);
        analysis.setCvContentHash(contentHash);
        analysis.setSkillsConfigurationVersion(configVersion);
        analysis.setAiFunctionCode(functionCode);
        analysis.setProcessingFinishedAt(new Date());

        String errorMsg = error.getMessage() != null ? error.getMessage() : error.toString();
        if (errorMsg.length() > 1950) {
            errorMsg = errorMsg.substring(0, 1950);
        }
        analysis.setLastError(errorMsg);

        if (currentRetries < maxRetries) {
            analysis.setStatus(CandidateCvAnalysisStatus.RETRY);
            // Exponential backoff: 1 min, 5 min, 15 min, 60 min
            long backoffMinutes = calculateBackoffMinutes(currentRetries);
            Date nextRetry = new Date(System.currentTimeMillis() + backoffMinutes * 60_000L);
            analysis.setNextRetryAt(nextRetry);
            log.warn("Ошибка фонового AI-анализа навыков кандидата {} (попытка {} из {}), следующий повтор через {} мин: {}",
                    candidate.getFullName(), currentRetries, maxRetries, backoffMinutes, errorMsg);
        } else {
            analysis.setStatus(CandidateCvAnalysisStatus.ERROR);
            analysis.setNextRetryAt(null);
            log.error("Превышен лимит попыток ({}) фонового AI-анализа навыков кандидата {}: {}",
                    maxRetries, candidate.getFullName(), errorMsg);
        }

        dataManager.commit(analysis);
    }

    private long calculateBackoffMinutes(int retryCount) {
        switch (retryCount) {
            case 1: return 1;
            case 2: return 5;
            case 3: return 15;
            default: return 60;
        }
    }

    private void markCvAnalysisStatus(JobCandidate candidate, CandidateCV cv,
                                      CandidateCvAnalysisStatus status, String errorMsg,
                                      String hash, long durationMs) {
        CandidateCvSkillAnalysis analysis = loadAnalysisRecord(cv.getId());
        if (analysis == null) {
            analysis = metadata.create(CandidateCvSkillAnalysis.class);
            analysis.setCandidate(candidate);
            analysis.setCandidateCv(cv);
        }
        analysis.setStatus(status);
        analysis.setLastError(errorMsg);
        analysis.setCvContentHash(hash);
        analysis.setDurationMs(durationMs);
        analysis.setProcessingFinishedAt(new Date());
        dataManager.commit(analysis);
    }

    private CandidateCvSkillAnalysis loadAnalysisRecord(UUID cvId) {
        if (cvId == null) return null;
        return dataManager.load(CandidateCvSkillAnalysis.class)
                .query("select e from hunttech_CandidateCvSkillAnalysis e where e.candidateCv.id = :cvId")
                .parameter("cvId", cvId)
                .view("candidateCvSkillAnalysis-browse-view")
                .optional()
                .orElse(null);
    }

    private Integer loadFunctionVersion(String functionCode) {
        try {
            return dataManager.loadValue(
                    "select e.configurationVersion from hunttech_AiFunctionConfiguration e " +
                            "where e.code = :code and e.active = true", Integer.class)
                    .parameter("code", functionCode)
                    .optional()
                    .orElse(1);
        } catch (Exception e) {
            return 1;
        }
    }

    private AiExecutionResult firstNonNullExecution(SkillAnalysisResult... results) {
        if (results == null) return null;
        for (SkillAnalysisResult r : results) {
            if (r != null && r.getAiExecution() != null) {
                return r.getAiExecution();
            }
        }
        return null;
    }

    @Override
    public String calculateNormalizedCvHash(String rawCvText) {
        if (rawCvText == null || rawCvText.trim().isEmpty()) {
            return null;
        }
        String clean;
        try {
            clean = Jsoup.parse(rawCvText).text();
        } catch (Exception e) {
            clean = rawCvText;
        }
        String normalized = clean.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    @Override
    public void enqueueCandidateCvPriority(UUID candidateCvId) {
        enqueueCandidateCv(candidateCvId, PRIORITY_HIGH);
    }

    @Override
    public void enqueueCandidateCv(UUID candidateCvId, int priority) {
        if (candidateCvId == null) return;

        CandidateCV cv = dataManager.load(CandidateCV.class)
                .id(candidateCvId)
                .view("candidateCV-llm-view")
                .optional()
                .orElse(null);

        if (cv == null || cv.getCandidate() == null) {
            log.warn("Не удалось поставить CV ID: {} в очередь фонового анализа навыков: CV или кандидат не найден", candidateCvId);
            return;
        }

        String rawText = cv.getTextCV();
        if (rawText == null || rawText.trim().isEmpty()) {
            markCvAnalysisStatus(cv.getCandidate(), cv, CandidateCvAnalysisStatus.SKIPPED, "Текст резюме пуст", null, 0);
            log.info("CV ID: {} помечено SKIPPED (пустой текст)", candidateCvId);
            return;
        }

        String currentHash = calculateNormalizedCvHash(rawText);
        CandidateCvSkillAnalysis analysis = loadAnalysisRecord(candidateCvId);

        if (analysis == null) {
            analysis = metadata.create(CandidateCvSkillAnalysis.class);
            analysis.setCandidate(cv.getCandidate());
            analysis.setCandidateCv(cv);
            analysis.setStatus(CandidateCvAnalysisStatus.NOT_ANALYZED);
            analysis.setPriority(priority);
            analysis.setRetryCount(0);
            analysis.setNextRetryAt(new Date());
            analysis.setLastError(null);
            analysis.setCvContentHash(currentHash);
            dataManager.commit(analysis);
            log.info("CV ID: {} кандидата {} успешно поставлено в очередь фонового анализа (приоритет: {})",
                    candidateCvId, cv.getCandidate().getFullName(), priority);
        } else {
            // Если уже FRESH и текст не менялся, повторный анализ не требуется
            if (analysis.getStatus() == CandidateCvAnalysisStatus.FRESH
                    && Objects.equals(analysis.getCvContentHash(), currentHash)) {
                log.info("CV ID: {} кандидата {} уже имеет актуальный статус FRESH с тем же хэшем, пропуск",
                        candidateCvId, cv.getCandidate().getFullName());
                return;
            }

            // Иначе переводим в NOT_ANALYZED с заданным приоритетом
            analysis.setStatus(CandidateCvAnalysisStatus.NOT_ANALYZED);
            analysis.setPriority(priority);
            analysis.setRetryCount(0);
            analysis.setNextRetryAt(new Date());
            analysis.setLastError(null);
            analysis.setCvContentHash(currentHash);
            dataManager.commit(analysis);
            log.info("CV ID: {} кандидата {} обновлено в очереди фонового анализа (статус: NOT_ANALYZED, приоритет: {})",
                    candidateCvId, cv.getCandidate().getFullName(), priority);
        }

        // Немедленное пробуждение воркера для скорейшего определения навыков
        if (enrichmentWorker != null) {
            enrichmentWorker.triggerImmediateProcessing();
        }
    }

    @Override
    public void reprocessCv(UUID candidateCvId) {
        if (candidateCvId == null) return;
        CandidateCvSkillAnalysis analysis = loadAnalysisRecord(candidateCvId);
        if (analysis != null) {
            analysis.setStatus(CandidateCvAnalysisStatus.NOT_ANALYZED);
            analysis.setPriority(PRIORITY_HIGH);
            analysis.setRetryCount(0);
            analysis.setNextRetryAt(new Date());
            analysis.setLastError(null);
            dataManager.commit(analysis);
            log.info("CV ID: {} вручную отправлено на повторный AI-анализ с приоритетом {}", candidateCvId, PRIORITY_HIGH);
            if (enrichmentWorker != null) {
                enrichmentWorker.triggerImmediateProcessing();
            }
        }
    }

    @Override
    public CandidateSkillsEnrichmentKpiDto getKpiMetrics() {
        CandidateSkillsEnrichmentKpiDto kpi = new CandidateSkillsEnrichmentKpiDto();
        HunttechSkillsEnrichmentConfig cfg = configuration.getConfig(HunttechSkillsEnrichmentConfig.class);

        kpi.setWorkerEnabled(cfg.getEnabled());
        kpi.setWorkerStatus(cfg.getEnabled() ? "РАБОТАЕТ" : "ОСТАНОВЛЕН");

        try (Transaction tx = persistence.createTransaction()) {
            EntityManager em = persistence.getEntityManager();

            // 1. Всего пригодно CV (непустой textCV)
            Number totalCv = (Number) em.createQuery(
                    "select count(e) from hunttech_CandidateCV e where e.textCV is not null and length(trim(e.textCV)) > 0")
                    .getSingleResult();
            kpi.setTotalEligibleCvCount(totalCv != null ? totalCv.longValue() : 0);

            // 2. Статусы по таблице CandidateCvSkillAnalysis
            List<Object[]> statusCounts = em.createQuery(
                    "select e.status, count(e) from hunttech_CandidateCvSkillAnalysis e group by e.status")
                    .getResultList();

            long fresh = 0, stale = 0, retry = 0, error = 0, skipped = 0, processing = 0;
            for (Object[] row : statusCounts) {
                Integer stId = (Integer) row[0];
                long cnt = ((Number) row[1]).longValue();
                CandidateCvAnalysisStatus st = CandidateCvAnalysisStatus.fromId(stId);
                if (st == CandidateCvAnalysisStatus.FRESH) fresh += cnt;
                else if (st == CandidateCvAnalysisStatus.STALE) stale += cnt;
                else if (st == CandidateCvAnalysisStatus.RETRY) retry += cnt;
                else if (st == CandidateCvAnalysisStatus.ERROR) error += cnt;
                else if (st == CandidateCvAnalysisStatus.SKIPPED) skipped += cnt;
                else if (st == CandidateCvAnalysisStatus.PROCESSING) processing += cnt;
            }

            kpi.setFreshCount(fresh);
            kpi.setStaleCount(stale);
            kpi.setRetryCount(retry);
            kpi.setErrorCount(error);
            kpi.setSkippedCount(skipped);
            kpi.setProcessingCount(processing);

            // Не проанализировано: пригодные CV (непустой textCV), для которых нет свежего анализа
            Number notAnalyzed = (Number) em.createQuery(
                    "select count(cv) from hunttech_CandidateCV cv " +
                            "where cv.textCV is not null and length(trim(cv.textCV)) > 0 " +
                            "and not exists (" +
                            "    select 1 from hunttech_CandidateCvSkillAnalysis a " +
                            "    where a.candidateCv.id = cv.id and a.status in :trackedStatuses" +
                            ")")
                    .setParameter("trackedStatuses", Arrays.asList(
                            CandidateCvAnalysisStatus.FRESH.getId(),
                            CandidateCvAnalysisStatus.PROCESSING.getId(),
                            CandidateCvAnalysisStatus.STALE.getId(),
                            CandidateCvAnalysisStatus.RETRY.getId(),
                            CandidateCvAnalysisStatus.ERROR.getId(),
                            CandidateCvAnalysisStatus.SKIPPED.getId()
                    ))
                    .getSingleResult();
            kpi.setNotAnalyzedCount(notAnalyzed != null ? notAnalyzed.longValue() : 0);

            // 3. Временные срезы (обработано сегодня, 24ч, 7д)
            Date now = new Date();
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            Date startOfToday = cal.getTime();

            Date past24h = new Date(now.getTime() - 24L * 3600_000L);
            Date past7d = new Date(now.getTime() - 7L * 24L * 3600_000L);

            Number processedToday = (Number) em.createQuery(
                    "select count(e) from hunttech_CandidateCvSkillAnalysis e " +
                            "where e.skillsAnalyzedAt >= :today and e.status = :fresh")
                    .setParameter("today", startOfToday)
                    .setParameter("fresh", CandidateCvAnalysisStatus.FRESH.getId())
                    .getSingleResult();
            kpi.setProcessedToday(processedToday != null ? processedToday.longValue() : 0);

            Number processed24h = (Number) em.createQuery(
                    "select count(e) from hunttech_CandidateCvSkillAnalysis e " +
                            "where e.skillsAnalyzedAt >= :past24h and e.status = :fresh")
                    .setParameter("past24h", past24h)
                    .setParameter("fresh", CandidateCvAnalysisStatus.FRESH.getId())
                    .getSingleResult();
            kpi.setProcessed24h(processed24h != null ? processed24h.longValue() : 0);

            Number processed7d = (Number) em.createQuery(
                    "select count(e) from hunttech_CandidateCvSkillAnalysis e " +
                            "where e.skillsAnalyzedAt >= :past7d and e.status = :fresh")
                    .setParameter("past7d", past7d)
                    .setParameter("fresh", CandidateCvAnalysisStatus.FRESH.getId())
                    .getSingleResult();
            kpi.setProcessed7d(processed7d != null ? processed7d.longValue() : 0);

            // 4. Токены и AI запросы за сегодня
            Number aiReqToday = (Number) em.createQuery(
                    "select count(e) from hunttech_CandidateCvSkillAnalysis e " +
                            "where e.processingFinishedAt >= :today and e.aiFunctionCode is not null")
                    .setParameter("today", startOfToday)
                    .getSingleResult();
            kpi.setAiRequestsToday(aiReqToday != null ? aiReqToday.longValue() : 0);

            Object[] tokensToday = (Object[]) em.createQuery(
                    "select sum(e.promptTokens), sum(e.completionTokens), sum(e.totalTokens) " +
                            "from hunttech_CandidateCvSkillAnalysis e " +
                            "where e.processingFinishedAt >= :today")
                    .setParameter("today", startOfToday)
                    .getSingleResult();

            if (tokensToday != null) {
                kpi.setPromptTokensToday(tokensToday[0] != null ? ((Number) tokensToday[0]).longValue() : 0);
                kpi.setCompletionTokensToday(tokensToday[1] != null ? ((Number) tokensToday[1]).longValue() : 0);
                kpi.setTotalTokensToday(tokensToday[2] != null ? ((Number) tokensToday[2]).longValue() : 0);
            }

            // Детекция платных вызовов (ТЗ п. 29)
            Number paidCalls = (Number) em.createQuery(
                    "select count(e) from hunttech_CandidateCvSkillAnalysis e " +
                            "where e.estimatedCost is not null and e.estimatedCost > 0 and e.processingFinishedAt >= :today")
                    .setParameter("today", startOfToday)
                    .getSingleResult();
            kpi.setPaidRequestDetected(paidCalls != null && paidCalls.longValue() > 0);

            // 5. Скорость и расчетный ETA
            double speedPerHour = kpi.getProcessed24h() / 24.0;
            kpi.setProcessingSpeedPerHour(Math.round(speedPerHour * 10.0) / 10.0);

            long remainingQueue = (notAnalyzed != null ? notAnalyzed.longValue() : 0L) + stale + retry;
            if (speedPerHour > 0.1 && remainingQueue > 0) {
                double hoursRemaining = remainingQueue / speedPerHour;
                if (hoursRemaining < 24) {
                    kpi.setEstimatedEtaText(String.format("~%.1f ч.", hoursRemaining));
                } else {
                    double days = hoursRemaining / 24.0;
                    kpi.setEstimatedEtaText(String.format("~%.1f дн.", days));
                }
            } else if (remainingQueue == 0) {
                kpi.setEstimatedEtaText("Очередь пуста (100%)");
            } else {
                kpi.setEstimatedEtaText("Недостаточно данных для ETA");
            }

            // 6. Сводка по провайдерам и моделям
            List<Object[]> providerRows = em.createQuery(
                    "select e.providerCode, e.modelName, count(e), sum(e.totalTokens) " +
                            "from hunttech_CandidateCvSkillAnalysis e " +
                            "where e.providerCode is not null " +
                            "group by e.providerCode, e.modelName")
                    .getResultList();

            for (Object[] r : providerRows) {
                String prov = (String) r[0];
                String mod = (String) r[1];
                long reqs = r[2] != null ? ((Number) r[2]).longValue() : 0;
                long toks = r[3] != null ? ((Number) r[3]).longValue() : 0;
                kpi.getProviderModelStats().add(new CandidateSkillsEnrichmentKpiDto.ProviderModelStatDto(
                        prov, mod, reqs, reqs, 0, toks, BigDecimal.ZERO));
            }

            tx.commit();
        } catch (Exception e) {
            log.warn("Ошибка расчета KPI фонового анализа навыков: {}", e.getMessage());
        }

        return kpi;
    }

    @Override
    public void setWorkerEnabled(boolean enabled) {
        HunttechSkillsEnrichmentConfig cfg = configuration.getConfig(HunttechSkillsEnrichmentConfig.class);
        cfg.setEnabled(enabled);
        log.info("Фоновая служба обогащения навыков переключена: enabled={}", enabled);
    }

    @Override
    public boolean isWorkerEnabled() {
        return configuration.getConfig(HunttechSkillsEnrichmentConfig.class).getEnabled();
    }

    @Override
    public void runWorkerCycleNow() {
        if (enrichmentWorker != null) {
            enrichmentWorker.runWorkerTickSafe();
        }
    }

    @Override
    public boolean isFreeOnly() {
        return configuration.getConfig(HunttechSkillsEnrichmentConfig.class).getFreeOnly();
    }

    @Override
    public void setFreeOnly(boolean freeOnly) {
        HunttechSkillsEnrichmentConfig cfg = configuration.getConfig(HunttechSkillsEnrichmentConfig.class);
        cfg.setFreeOnly(freeOnly);
        log.info("Режим «Только бесплатные нейросети» переключен: freeOnly={}", freeOnly);
    }
}

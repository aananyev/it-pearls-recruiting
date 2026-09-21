package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Состояние и телеметрия фонового AI-анализа навыков конкретного резюме кандидата.
 * <p>
 * Хранит текущий статус обработки, хеш нормализованного текста резюме (SHA-256),
 * версию AI-конфигурации, метрики времени, токенов, стоимости, а также компактную
 * дельту изменений навыков (ADDED, UPDATED, UNCHANGED, SKIPPED).
 */
@Table(name = "HUNTTECH_CANDIDATE_CV_SKILL_ANALYSIS", indexes = {
        @Index(name = "IDX_CAND_CV_SKILL_ANALYSIS_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_CAND_CV_SKILL_ANALYSIS_CAND", columnList = "CANDIDATE_ID"),
        @Index(name = "IDX_CAND_CV_SKILL_ANALYSIS_NEXT_RETRY", columnList = "NEXT_RETRY_AT"),
        @Index(name = "IDX_CAND_CV_SKILL_ANALYSIS_STARTED", columnList = "PROCESSING_STARTED_AT"),
        @Index(name = "IDX_CAND_CV_SKILL_ANALYSIS_PRIORITY", columnList = "STATUS, PRIORITY, CREATE_TS")
}, uniqueConstraints = {
        @UniqueConstraint(name = "IDX_CAND_CV_SKILL_ANALYSIS_UNQ_CV", columnNames = {"CANDIDATE_CV_ID", "DELETE_TS"})
})
@Entity(name = "hunttech_CandidateCvSkillAnalysis")
@NamePattern("%s (%s)|candidate,status")
public class CandidateCvSkillAnalysis extends StandardEntity {
    private static final long serialVersionUID = 4819284729104820194L;

    /** Кандидат */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_ID")
    protected JobCandidate candidate;

    /** Анализируемое резюме кандидата */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CANDIDATE_CV_ID")
    protected CandidateCV candidateCv;

    /** SHA-256 хеш нормализованного текста резюме для контроля изменений */
    @Column(name = "CV_CONTENT_HASH", length = 64)
    protected String cvContentHash;

    /** Текущий статус анализа (NOT_ANALYZED, PROCESSING, FRESH, STALE, RETRY, ERROR, SKIPPED) */
    @NotNull
    @Column(name = "STATUS", nullable = false)
    protected Integer status;

    /** Время последнего успешного анализа навыков */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "SKILLS_ANALYZED_AT")
    protected Date skillsAnalyzedAt;

    /** Время начала последней обработки */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PROCESSING_STARTED_AT")
    protected Date processingStartedAt;

    /** Время завершения последней обработки */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "PROCESSING_FINISHED_AT")
    protected Date processingFinishedAt;

    /** Длительность обработки в миллисекундах */
    @Column(name = "DURATION_MS")
    protected Long durationMs;

    /** Версия системной конфигурации AI-функции на момент выполнения анализа */
    @Column(name = "SKILLS_CONFIGURATION_VERSION")
    protected Integer skillsConfigurationVersion;

    /** Количество выполненных попыток (для backoff и RETRY) */
    @Column(name = "RETRY_COUNT")
    protected Integer retryCount = 0;

    /** Запланированное время следующего повтора при ошибке */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "NEXT_RETRY_AT")
    protected Date nextRetryAt;

    /** Сообщение об ошибке (техническая причина последнего сбоя) */
    @Column(name = "LAST_ERROR", length = 2000)
    protected String lastError;

    /** Код AI-провайдера (например g4f, openai, openrouter) */
    @Column(name = "PROVIDER_CODE", length = 64)
    protected String providerCode;

    /** Имя AI-модели */
    @Column(name = "MODEL_NAME", length = 128)
    protected String modelName;

    /** Использованная AI-функция */
    @Column(name = "AI_FUNCTION_CODE", length = 64)
    protected String aiFunctionCode;

    /** Токены промпта */
    @Column(name = "PROMPT_TOKENS")
    protected Integer promptTokens;

    /** Токены ответа */
    @Column(name = "COMPLETION_TOKENS")
    protected Integer completionTokens;

    /** Всего токенов */
    @Column(name = "TOTAL_TOKENS")
    protected Integer totalTokens;

    /** Расчетная стоимость вызова */
    @Column(name = "ESTIMATED_COST", precision = 19, scale = 6)
    protected BigDecimal estimatedCost;

    /** Общее количество навыков, обнаруженных в резюме */
    @Column(name = "SKILLS_FOUND_COUNT")
    protected Integer skillsFoundCount = 0;

    /** Количество новых навыков, добавленных кандидату */
    @Column(name = "SKILLS_ADDED_COUNT")
    protected Integer skillsAddedCount = 0;

    /** Количество навыков, у которых обновлен приоритет */
    @Column(name = "SKILLS_UPDATED_COUNT")
    protected Integer skillsUpdatedCount = 0;

    /** Количество навыков, которые уже присутствовали без изменений */
    @Column(name = "SKILLS_UNCHANGED_COUNT")
    protected Integer skillsUnchangedCount = 0;

    /** Количество пропущенных навыков */
    @Column(name = "SKILLS_SKIPPED_COUNT")
    protected Integer skillsSkippedCount = 0;

    /** Структурированный JSON детализации изменений для диалога «Что именно сделал алгоритм» */
    @Lob
    @Column(name = "DELTA_DETAILS_JSON")
    protected String deltaDetailsJson;

    /** Приоритет в очереди обработки (0 = обычный фоновый, 100 = высокий приоритет по действию рекрутера) */
    @Column(name = "PRIORITY")
    protected Integer priority = 0;

    public JobCandidate getCandidate() {
        return candidate;
    }

    public void setCandidate(JobCandidate candidate) {
        this.candidate = candidate;
    }

    public CandidateCV getCandidateCv() {
        return candidateCv;
    }

    public void setCandidateCv(CandidateCV candidateCv) {
        this.candidateCv = candidateCv;
    }

    public String getCvContentHash() {
        return cvContentHash;
    }

    public void setCvContentHash(String cvContentHash) {
        this.cvContentHash = cvContentHash;
    }

    public CandidateCvAnalysisStatus getStatus() {
        return status == null ? null : CandidateCvAnalysisStatus.fromId(status);
    }

    public void setStatus(CandidateCvAnalysisStatus status) {
        this.status = status == null ? null : status.getId();
    }

    public Date getSkillsAnalyzedAt() {
        return skillsAnalyzedAt;
    }

    public void setSkillsAnalyzedAt(Date skillsAnalyzedAt) {
        this.skillsAnalyzedAt = skillsAnalyzedAt;
    }

    public Date getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(Date processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public Date getProcessingFinishedAt() {
        return processingFinishedAt;
    }

    public void setProcessingFinishedAt(Date processingFinishedAt) {
        this.processingFinishedAt = processingFinishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Integer getSkillsConfigurationVersion() {
        return skillsConfigurationVersion;
    }

    public void setSkillsConfigurationVersion(Integer skillsConfigurationVersion) {
        this.skillsConfigurationVersion = skillsConfigurationVersion;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Date getNextRetryAt() {
        return nextRetryAt;
    }

    public void setNextRetryAt(Date nextRetryAt) {
        this.nextRetryAt = nextRetryAt;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getAiFunctionCode() {
        return aiFunctionCode;
    }

    public void setAiFunctionCode(String aiFunctionCode) {
        this.aiFunctionCode = aiFunctionCode;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public Integer getSkillsFoundCount() {
        return skillsFoundCount;
    }

    public void setSkillsFoundCount(Integer skillsFoundCount) {
        this.skillsFoundCount = skillsFoundCount;
    }

    public Integer getSkillsAddedCount() {
        return skillsAddedCount;
    }

    public void setSkillsAddedCount(Integer skillsAddedCount) {
        this.skillsAddedCount = skillsAddedCount;
    }

    public Integer getSkillsUpdatedCount() {
        return skillsUpdatedCount;
    }

    public void setSkillsUpdatedCount(Integer skillsUpdatedCount) {
        this.skillsUpdatedCount = skillsUpdatedCount;
    }

    public Integer getSkillsUnchangedCount() {
        return skillsUnchangedCount;
    }

    public void setSkillsUnchangedCount(Integer skillsUnchangedCount) {
        this.skillsUnchangedCount = skillsUnchangedCount;
    }

    public Integer getSkillsSkippedCount() {
        return skillsSkippedCount;
    }

    public void setSkillsSkippedCount(Integer skillsSkippedCount) {
        this.skillsSkippedCount = skillsSkippedCount;
    }

    public String getDeltaDetailsJson() {
        return deltaDetailsJson;
    }

    public void setDeltaDetailsJson(String deltaDetailsJson) {
        this.deltaDetailsJson = deltaDetailsJson;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}

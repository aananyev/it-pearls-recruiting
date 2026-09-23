package com.company.hunttech.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Состояние и телеметрия фонового AI-анализа контактных данных и фото конкретного резюме кандидата.
 * <p>
 * Хранит текущий статус обработки, хеш нормализованного текста резюме (SHA-256),
 * версию AI-конфигурации, метрики времени, токенов, стоимости, флаг извлечения фото,
 * а также сводку извлеченных контактов и компактный JSON дельты.
 */
@Table(name = "HUNTTECH_CAND_CV_CONTACT_ANALYSIS", indexes = {
        @Index(name = "IDX_CAND_CV_CNT_STATUS", columnList = "STATUS"),
        @Index(name = "IDX_CAND_CV_CNT_CAND", columnList = "CANDIDATE_ID"),
        @Index(name = "IDX_CAND_CV_CNT_NEXT_RETRY", columnList = "NEXT_RETRY_AT"),
        @Index(name = "IDX_CAND_CV_CNT_STARTED", columnList = "PROCESSING_STARTED_AT"),
        @Index(name = "IDX_CAND_CV_CNT_PRIORITY", columnList = "STATUS, PRIORITY, CREATE_TS")
}, uniqueConstraints = {
        @UniqueConstraint(name = "IDX_CAND_CV_CNT_UNQ_CV", columnNames = {"CANDIDATE_CV_ID", "DELETE_TS"})
})
@Entity(name = "hunttech_CandidateCvContactAnalysis")
@NamePattern("%s (%s)|candidate,status")
public class CandidateCvContactAnalysis extends StandardEntity {
    private static final long serialVersionUID = 5829104820194819284L;

    public static final String EXECUTION_SOURCE_AI = "AI";
    public static final String EXECUTION_SOURCE_AI_METADATA_INCOMPLETE = "AI_METADATA_INCOMPLETE";
    public static final String EXECUTION_SOURCE_RULE_FALLBACK = "RULE_FALLBACK";

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

    /** SHA-256 хеш текста резюме для контроля изменений */
    @Column(name = "CV_CONTENT_HASH", length = 64)
    protected String cvContentHash;

    /** Текущий статус анализа (NOT_ANALYZED, PROCESSING, FRESH, STALE, RETRY, ERROR, SKIPPED) */
    @NotNull
    @Column(name = "STATUS", nullable = false)
    protected Integer status;

    /** Время последнего успешного анализа контактов */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CONTACTS_ANALYZED_AT")
    protected Date contactsAnalyzedAt;

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
    @Column(name = "CONTACTS_CONFIG_VERSION")
    protected Integer contactsConfigurationVersion;

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

    /** Код AI-провайдера (например bai, g4f, openai, openrouter) */
    @Column(name = "PROVIDER_CODE", length = 64)
    protected String providerCode;

    /** Имя AI-модели */
    @Column(name = "MODEL_NAME", length = 128)
    protected String modelName;

    /** Фактический источник результата */
    @Column(name = "EXECUTION_SOURCE", length = 32)
    protected String executionSource;

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

    /** Общее количество найденных контактов */
    @Column(name = "CONTACTS_FOUND_COUNT")
    protected Integer contactsFoundCount = 0;

    /** Количество обновленных контактов в карточке кандидата */
    @Column(name = "CONTACTS_UPDATED_COUNT")
    protected Integer contactsUpdatedCount = 0;

    /** Признак извлечения фотографии кандидата */
    @Column(name = "PHOTO_EXTRACTED")
    protected Boolean photoExtracted = false;

    /** Извлеченный телефон (для быстрого отображения в таблице) */
    @Column(name = "EXTRACTED_PHONE", length = 64)
    protected String extractedPhone;

    /** Извлеченный email */
    @Column(name = "EXTRACTED_EMAIL", length = 128)
    protected String extractedEmail;

    /** Извлеченный Telegram */
    @Column(name = "EXTRACTED_TELEGRAM", length = 64)
    protected String extractedTelegram;

    /** Извлеченный город */
    @Column(name = "EXTRACTED_CITY", length = 128)
    protected String extractedCity;

    /** Структурированный JSON детализации изменений контактов */
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

    public Date getContactsAnalyzedAt() {
        return contactsAnalyzedAt;
    }

    public void setContactsAnalyzedAt(Date contactsAnalyzedAt) {
        this.contactsAnalyzedAt = contactsAnalyzedAt;
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

    public Integer getContactsConfigurationVersion() {
        return contactsConfigurationVersion;
    }

    public void setContactsConfigurationVersion(Integer contactsConfigurationVersion) {
        this.contactsConfigurationVersion = contactsConfigurationVersion;
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

    public String getExecutionSource() {
        return executionSource;
    }

    public void setExecutionSource(String executionSource) {
        this.executionSource = executionSource;
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

    public Integer getContactsFoundCount() {
        return contactsFoundCount;
    }

    public void setContactsFoundCount(Integer contactsFoundCount) {
        this.contactsFoundCount = contactsFoundCount;
    }

    public Integer getContactsUpdatedCount() {
        return contactsUpdatedCount;
    }

    public void setContactsUpdatedCount(Integer contactsUpdatedCount) {
        this.contactsUpdatedCount = contactsUpdatedCount;
    }

    public Boolean getPhotoExtracted() {
        return photoExtracted;
    }

    public void setPhotoExtracted(Boolean photoExtracted) {
        this.photoExtracted = photoExtracted;
    }

    public String getExtractedPhone() {
        return extractedPhone;
    }

    public void setExtractedPhone(String extractedPhone) {
        this.extractedPhone = extractedPhone;
    }

    public String getExtractedEmail() {
        return extractedEmail;
    }

    public void setExtractedEmail(String extractedEmail) {
        this.extractedEmail = extractedEmail;
    }

    public String getExtractedTelegram() {
        return extractedTelegram;
    }

    public void setExtractedTelegram(String extractedTelegram) {
        this.extractedTelegram = extractedTelegram;
    }

    public String getExtractedCity() {
        return extractedCity;
    }

    public void setExtractedCity(String extractedCity) {
        this.extractedCity = extractedCity;
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

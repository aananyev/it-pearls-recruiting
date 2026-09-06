package com.company.hunttech.entity.ai;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.security.entity.User;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Журнал выполнения вызовов к AI (LLM / генерация изображений).
 * <p>
 * Фиксирует все вызовы к AI с метаданными: пользователь, время, длительность,
 * провайдер, модель, токены, стоимость, вызывающий экран/сервис и статус.
 * Payload prompt/response не является техническим аудитом и для новых записей не сохраняется.
 */
@Table(name = "HUNTTECH_AI_CALL_LOG", indexes = {
        @Index(name = "IDX_HUNTTECH_AI_CALL_LOG_USER", columnList = "USER_ID"),
        @Index(name = "IDX_HUNTTECH_AI_CALL_LOG_CALL_TIME", columnList = "CALL_TIME"),
        @Index(name = "IDX_HUNTTECH_AI_CALL_LOG_FUNCTION", columnList = "FUNCTION_CODE"),
        @Index(name = "IDX_HUNTTECH_AI_CALL_LOG_STATUS", columnList = "STATUS")
})
@Entity(name = "hunttech_AiCallLog")
@NamePattern("%s %s|functionCode,callTime")
public class AiCallLog extends StandardEntity {
    private static final long serialVersionUID = 7291834928174928172L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private User user;

    @Column(name = "USER_LOGIN", length = 128)
    private String userLogin;

    @Column(name = "USER_NAME", length = 255)
    private String userName;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CALL_TIME")
    private Date callTime;

    @Column(name = "DURATION_MS")
    private Long durationMs;

    @Column(name = "FUNCTION_CODE", length = 64)
    private String functionCode;

    @Column(name = "FUNCTION_NAME", length = 255)
    private String functionName;

    @Column(name = "CAPABILITY", length = 32)
    private String capability;

    @Column(name = "PROVIDER_CODE", length = 32)
    private String providerCode;

    @Column(name = "MODEL_NAME", length = 128)
    private String modelName;

    @Column(name = "CREDENTIAL_OWNER", length = 32)
    private String credentialOwner;

    @Column(name = "PROMPT_TOKENS")
    private Integer promptTokens;

    @Column(name = "COMPLETION_TOKENS")
    private Integer completionTokens;

    @Column(name = "TOTAL_TOKENS")
    private Integer totalTokens;

    @Column(name = "ESTIMATED_COST", precision = 19, scale = 6)
    private BigDecimal estimatedCost;

    @Column(name = "CURRENCY", length = 8)
    private String currency;

    @Lob
    @Column(name = "PROMPT_TEXT")
    private String promptText;

    @Lob
    @Column(name = "RESPONSE_TEXT")
    private String responseText;

    @Column(name = "CALLER_SOURCE", length = 255)
    private String callerSource;

    @Column(name = "STATUS", length = 32)
    private String status;

    @Lob
    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    /**
     * Аудит персонализации: контекст «Обо мне» (UserAiProfile) был фактически добавлен
     * в system prompt этого вызова. Контент блока в лог не пишется (приватность).
     */
    @Column(name = "CONTEXT_INCLUDED")
    private Boolean contextIncluded;

    /** Размер добавленного блока в code points (диагностика стоимости вызова). */
    @Column(name = "CONTEXT_CODE_POINTS")
    private Integer contextCodePoints;

    /** Policy version effective when this operation was authorized. */
    @Column(name = "PRIVACY_POLICY_VERSION_SNAPSHOT", length = 64)
    private String privacyPolicyVersionSnapshot;

    /** User external-processing consent version captured at dispatch time. */
    @Column(name = "EXTERNAL_PROCESSING_CONSENT_VERSION_SNAPSHOT", length = 64)
    private String externalProcessingConsentVersionSnapshot;

    /** Separate admin-fallback consent version; null when admin fallback was not used. */
    @Column(name = "ADMIN_FALLBACK_CONSENT_VERSION_SNAPSHOT", length = 64)
    private String adminFallbackConsentVersionSnapshot;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Date getCallTime() {
        return callTime;
    }

    public void setCallTime(Date callTime) {
        this.callTime = callTime;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getFunctionCode() {
        return functionCode;
    }

    public void setFunctionCode(String functionCode) {
        this.functionCode = functionCode;
    }

    public String getFunctionName() {
        return functionName;
    }

    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }

    public String getCapability() {
        return capability;
    }

    public void setCapability(String capability) {
        this.capability = capability;
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

    public String getCredentialOwner() {
        return credentialOwner;
    }

    public void setCredentialOwner(String credentialOwner) {
        this.credentialOwner = credentialOwner;
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getPromptText() {
        return promptText;
    }

    public void setPromptText(String promptText) {
        this.promptText = promptText;
    }

    public String getResponseText() {
        return responseText;
    }

    public void setResponseText(String responseText) {
        this.responseText = responseText;
    }

    public String getCallerSource() {
        return callerSource;
    }

    public void setCallerSource(String callerSource) {
        this.callerSource = callerSource;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Boolean getContextIncluded() {
        return contextIncluded;
    }

    public void setContextIncluded(Boolean contextIncluded) {
        this.contextIncluded = contextIncluded;
    }

    public Integer getContextCodePoints() {
        return contextCodePoints;
    }

    public void setContextCodePoints(Integer contextCodePoints) {
        this.contextCodePoints = contextCodePoints;
    }

    public String getPrivacyPolicyVersionSnapshot() {
        return privacyPolicyVersionSnapshot;
    }

    public void setPrivacyPolicyVersionSnapshot(String privacyPolicyVersionSnapshot) {
        this.privacyPolicyVersionSnapshot = privacyPolicyVersionSnapshot;
    }

    public String getExternalProcessingConsentVersionSnapshot() {
        return externalProcessingConsentVersionSnapshot;
    }

    public void setExternalProcessingConsentVersionSnapshot(String externalProcessingConsentVersionSnapshot) {
        this.externalProcessingConsentVersionSnapshot = externalProcessingConsentVersionSnapshot;
    }

    public String getAdminFallbackConsentVersionSnapshot() {
        return adminFallbackConsentVersionSnapshot;
    }

    public void setAdminFallbackConsentVersionSnapshot(String adminFallbackConsentVersionSnapshot) {
        this.adminFallbackConsentVersionSnapshot = adminFallbackConsentVersionSnapshot;
    }
}

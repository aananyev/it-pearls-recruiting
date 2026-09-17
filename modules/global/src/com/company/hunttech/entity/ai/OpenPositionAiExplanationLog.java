package com.company.hunttech.entity.ai;

import com.company.hunttech.entity.OpenPosition;
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
import java.util.Date;

/**
 * Журнал генерации объяснений требований вакансий через AI (стандартный сервис и Hermes hrm-viewer).
 * Фиксирует инициатора, дату, полный текст ответа, модель, провайдер и служебные метаданные.
 */
@Table(name = "HUNTTECH_OP_AI_EXPLANATION_LOG", indexes = {
        @Index(name = "IDX_HUNTTECH_OP_AI_EXP_LOG_OP", columnList = "OPEN_POSITION_ID"),
        @Index(name = "IDX_HUNTTECH_OP_AI_EXP_LOG_USER", columnList = "USER_ID"),
        @Index(name = "IDX_HUNTTECH_OP_AI_EXP_LOG_TIME", columnList = "CALL_TIME"),
        @Index(name = "IDX_HUNTTECH_OP_AI_EXP_LOG_TYPE", columnList = "EXPLANATION_TYPE")
})
@Entity(name = "hunttech_OpenPositionAiExplanationLog")
@NamePattern("%s %s|explanationType,callTime")
public class OpenPositionAiExplanationLog extends StandardEntity {
    private static final long serialVersionUID = -4819283748291049281L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "OPEN_POSITION_ID")
    private OpenPosition openPosition;

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

    @Column(name = "EXPLANATION_TYPE", length = 32, nullable = false)
    private String explanationType; // STANDARD, SIMPLIFIED_ANALOGY

    @Column(name = "SERVICE_TYPE", length = 32, nullable = false)
    private String serviceType; // AI_EXECUTION_SERVICE, HERMES_HRM_VIEWER

    @Column(name = "MODEL_NAME", length = 128)
    private String modelName;

    @Column(name = "PROVIDER_CODE", length = 64)
    private String providerCode;

    @Column(name = "DURATION_MS")
    private Long durationMs;

    @Column(name = "PROMPT_TOKENS")
    private Integer promptTokens;

    @Column(name = "COMPLETION_TOKENS")
    private Integer completionTokens;

    @Column(name = "TOTAL_TOKENS")
    private Integer totalTokens;

    @Lob
    @Column(name = "REQUEST_CONTEXT")
    private String requestContext;

    @Lob
    @Column(name = "RESPONSE_CONTENT")
    private String responseContent;

    @Lob
    @Column(name = "TECHNICAL_INFO")
    private String technicalInfo;

    @Column(name = "STATUS", length = 32)
    private String status; // SUCCESS, ERROR

    @Column(name = "ERROR_MESSAGE", length = 1000)
    private String errorMessage;

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

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

    public String getExplanationType() {
        return explanationType;
    }

    public void setExplanationType(String explanationType) {
        this.explanationType = explanationType;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
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

    public String getRequestContext() {
        return requestContext;
    }

    public void setRequestContext(String requestContext) {
        this.requestContext = requestContext;
    }

    public String getResponseContent() {
        return responseContent;
    }

    public void setResponseContent(String responseContent) {
        this.responseContent = responseContent;
    }

    public String getTechnicalInfo() {
        return technicalInfo;
    }

    public void setTechnicalInfo(String technicalInfo) {
        this.technicalInfo = technicalInfo;
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
}

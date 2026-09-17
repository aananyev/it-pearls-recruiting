package com.company.hunttech.service.dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * DTO результата генерации объяснения требований вакансии.
 */
public class OpenPositionExplanationResult implements Serializable {
    private static final long serialVersionUID = 718293049182391234L;

    private boolean success;
    private String explanationText;
    private String explanationType; // STANDARD, SIMPLIFIED_ANALOGY
    private String serviceType; // AI_EXECUTION_SERVICE, HERMES_HRM_VIEWER
    private String modelName;
    private String providerCode;
    private Long durationMs;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private UUID logId;
    private String errorMessage;

    public OpenPositionExplanationResult() {
    }

    public static OpenPositionExplanationResult success(String explanationText,
                                                        String explanationType,
                                                        String serviceType,
                                                        String modelName,
                                                        String providerCode,
                                                        Long durationMs,
                                                        Integer promptTokens,
                                                        Integer completionTokens,
                                                        Integer totalTokens,
                                                        UUID logId) {
        OpenPositionExplanationResult result = new OpenPositionExplanationResult();
        result.setSuccess(true);
        result.setExplanationText(explanationText);
        result.setExplanationType(explanationType);
        result.setServiceType(serviceType);
        result.setModelName(modelName);
        result.setProviderCode(providerCode);
        result.setDurationMs(durationMs);
        result.setPromptTokens(promptTokens);
        result.setCompletionTokens(completionTokens);
        result.setTotalTokens(totalTokens);
        result.setLogId(logId);
        return result;
    }

    public static OpenPositionExplanationResult error(String errorMessage,
                                                      String explanationType,
                                                      String serviceType,
                                                      Long durationMs) {
        OpenPositionExplanationResult result = new OpenPositionExplanationResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setExplanationType(explanationType);
        result.setServiceType(serviceType);
        result.setDurationMs(durationMs);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getExplanationText() {
        return explanationText;
    }

    public void setExplanationText(String explanationText) {
        this.explanationText = explanationText;
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

    public UUID getLogId() {
        return logId;
    }

    public void setLogId(UUID logId) {
        this.logId = logId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}

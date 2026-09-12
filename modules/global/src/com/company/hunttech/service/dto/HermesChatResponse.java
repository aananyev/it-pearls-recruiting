package com.company.hunttech.service.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ответ от Hermes Agent.
 */
public class HermesChatResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID conversationId;
    private String assistantText;
    private boolean success;
    private String errorMessage;
    private String hermesSessionId;
    private long executionTimeMs;

    private String modelName;
    private String providerCode;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal estimatedCost;
    private String currency;

    public HermesChatResponse() {
    }

    public HermesChatResponse(UUID conversationId, String assistantText, String hermesSessionId, long executionTimeMs) {
        this.conversationId = conversationId;
        this.assistantText = assistantText;
        this.success = true;
        this.hermesSessionId = hermesSessionId;
        this.executionTimeMs = executionTimeMs;
    }

    public static HermesChatResponse error(UUID conversationId, String errorMessage, long executionTimeMs) {
        HermesChatResponse response = new HermesChatResponse();
        response.setConversationId(conversationId);
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setExecutionTimeMs(executionTimeMs);
        return response;
    }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }

    public String getAssistantText() { return assistantText; }
    public void setAssistantText(String assistantText) { this.assistantText = assistantText; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getHermesSessionId() { return hermesSessionId; }
    public void setHermesSessionId(String hermesSessionId) { this.hermesSessionId = hermesSessionId; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }

    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }

    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }

    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }

    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
}

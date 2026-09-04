package com.company.hunttech.service;

import java.io.Serializable;
import java.util.UUID;

/** Safe synchronous facade response; streaming deltas are not exposed by the current UI facade. */
public class LlmChatResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID conversationId;
    private String assistantText;
    private String providerCode;
    private String modelName;
    private AiCredentialOwner credentialOwner;

    public LlmChatResponse() {
    }

    public LlmChatResponse(UUID conversationId, String assistantText, String providerCode,
                           String modelName, AiCredentialOwner credentialOwner) {
        this.conversationId = conversationId;
        this.assistantText = assistantText;
        this.providerCode = providerCode;
        this.modelName = modelName;
        this.credentialOwner = credentialOwner;
    }

    public UUID getConversationId() { return conversationId; }
    public void setConversationId(UUID conversationId) { this.conversationId = conversationId; }
    public String getAssistantText() { return assistantText; }
    public void setAssistantText(String assistantText) { this.assistantText = assistantText; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public AiCredentialOwner getCredentialOwner() { return credentialOwner; }
    public void setCredentialOwner(AiCredentialOwner credentialOwner) { this.credentialOwner = credentialOwner; }
}

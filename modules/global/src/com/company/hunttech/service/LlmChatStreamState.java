package com.company.hunttech.service;

import java.io.Serializable;
import java.util.UUID;

/**
 * Owner-scoped snapshot of a chat streaming request returned to the web tier.
 * The text is cumulative so a missed polling tick cannot lose already received
 * deltas.
 */
public class LlmChatStreamState implements Serializable {
    private static final long serialVersionUID = 734981230498123L;

    private UUID conversationId;
    private String requestId;
    private String text;
    private String status;
    private String errorMessage;
    private boolean completed;

    public LlmChatStreamState() {
    }

    public LlmChatStreamState(UUID conversationId, String requestId, String text,
                              String status, String errorMessage, boolean completed) {
        this.conversationId = conversationId;
        this.requestId = requestId;
        this.text = text;
        this.status = status;
        this.errorMessage = errorMessage;
        this.completed = completed;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getText() {
        return text;
    }

    public String getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public boolean isCompleted() {
        return completed;
    }
}

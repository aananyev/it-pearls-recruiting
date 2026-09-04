package com.company.hunttech.service;

import com.company.hunttech.entity.ai.LlmChatMessage;

import java.util.List;
import java.util.UUID;

/** Middleware facade for the floating HRM chat. */
public interface LlmChatService {
    String NAME = "hunttech_LlmChatService";
    String VIEW_CHAT_HISTORY_ADMIN_PERMISSION = "hunttech.ai.viewChatHistoryAdmin";

    UUID startConversation();

    LlmChatResponse sendMessage(UUID conversationId, String message);

    LlmChatResponse sendMessage(UUID conversationId, String message, String requestId);

    /**
     * Requests cooperative cancellation of an in-flight request. The existing
     * synchronous provider contract cannot interrupt an already-open HTTP call;
     * middleware therefore suppresses persistence of its result and settles
     * usage when the provider returns.
     */
    void cancelMessage(UUID conversationId, String requestId);

    List<LlmChatMessage> loadHistory(UUID conversationId);

    /**
     * Administrative read path. The middleware checks the specific permission;
     * ordinary users must use loadHistory(), which remains owner-scoped.
     */
    List<LlmChatMessage> loadHistoryAsAdmin(UUID conversationId);
}

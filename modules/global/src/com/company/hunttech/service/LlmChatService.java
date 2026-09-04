package com.company.hunttech.service;

import com.company.hunttech.entity.ai.LlmChatMessage;

import java.util.List;
import java.util.UUID;

/** Middleware facade for the floating HRM chat. */
public interface LlmChatService {
    String NAME = "hunttech_LlmChatService";
    String VIEW_CHAT_HISTORY_ADMIN_PERMISSION = "hunttech.ai.viewChatHistoryAdmin";
    String RECONCILE_CHAT_QUOTA_PERMISSION = "hunttech.ai.reconcileChatQuota";

    UUID startConversation();

    LlmChatResponse sendMessage(UUID conversationId, String message);

    LlmChatResponse sendMessage(UUID conversationId, String message, String requestId);

    /**
     * Requests cancellation of an in-flight request. Providers with an
     * interruptible adapter close their active HTTP connection; legacy
     * synchronous adapters still use cooperative cancellation and settle usage
     * when the provider returns.
     */
    void cancelMessage(UUID conversationId, String requestId);

    /**
     * Closes an UNKNOWN_PENDING reservation after an administrator verifies
     * whether the provider charged the request and its actual token usage.
     */
    void reconcileUnknown(String requestId, Integer actualTokens, boolean providerCharged);

    List<LlmChatMessage> loadHistory(UUID conversationId);

    /**
     * Administrative read path. The middleware checks the specific permission;
     * ordinary users must use loadHistory(), which remains owner-scoped.
     */
    List<LlmChatMessage> loadHistoryAsAdmin(UUID conversationId);
}

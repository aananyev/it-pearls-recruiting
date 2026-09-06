package com.company.hunttech;

import com.haulmont.addon.globalevents.GlobalApplicationEvent;
import com.haulmont.addon.globalevents.GlobalUiEvent;

import java.util.UUID;

/**
 * UI-only signal that a chat stream snapshot changed.
 *
 * <p>The event intentionally carries identifiers only. The web client reads
 * the owner-scoped snapshot through LlmChatService, so partial AI text is not
 * broadcast as an event payload to other UI sessions.</p>
 */
public class LlmChatStreamEvent extends GlobalApplicationEvent implements GlobalUiEvent {
    private final UUID userId;
    private final UUID conversationId;
    private final String requestId;
    private final boolean completed;

    public LlmChatStreamEvent(Object source, UUID userId, UUID conversationId,
                              String requestId, boolean completed) {
        super(source);
        this.userId = userId;
        this.conversationId = conversationId;
        this.requestId = requestId;
        this.completed = completed;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isCompleted() {
        return completed;
    }
}

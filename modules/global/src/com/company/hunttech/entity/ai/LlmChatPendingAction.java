package com.company.hunttech.entity.ai;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.UUID;

@Table(name = "HUNTTECH_LLM_CHAT_PENDING_ACTION", indexes = {
        @Index(name = "IDX_HUNTTECH_PENDING_ACTION_USER_CONV", columnList = "USER_ID, CONVERSATION_ID, STATUS, EXPIRES_AT")
})
@Entity(name = "hunttech_LlmChatPendingAction")
@NamePattern("%s|actionType")
public class LlmChatPendingAction extends StandardEntity {
    private static final long serialVersionUID = 1L;

    @NotNull
    @Column(name = "USER_ID", nullable = false)
    private UUID userId;

    @NotNull
    @Column(name = "CONVERSATION_ID", nullable = false)
    private UUID conversationId;

    @NotNull
    @Column(name = "ACTION_TYPE", nullable = false, length = 64)
    private String actionType;

    @NotNull
    @Column(name = "STATUS", nullable = false, length = 32)
    private String status = "PENDING";

    @NotNull
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "EXPIRES_AT", nullable = false)
    private Date expiresAt;

    @Lob
    @Column(name = "STATE_DATA")
    private String stateData;

    @Column(name = "REQUEST_ID", length = 64)
    private String requestId;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public void setConversationId(UUID conversationId) {
        this.conversationId = conversationId;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStateData() {
        return stateData;
    }

    public void setStateData(String stateData) {
        this.stateData = stateData;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}

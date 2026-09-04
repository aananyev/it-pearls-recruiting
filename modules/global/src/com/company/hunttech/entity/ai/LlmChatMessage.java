package com.company.hunttech.entity.ai;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Table(name = "HUNTTECH_LLM_CHAT_MESSAGE")
@Entity(name = "hunttech_LlmChatMessage")
public class LlmChatMessage extends StandardEntity {
    private static final long serialVersionUID = 1L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CONVERSATION_ID", nullable = false)
    private LlmChatConversation conversation;

    @NotNull
    @Column(name = "ROLE", nullable = false, length = 16)
    private String role;

    @NotNull
    @Lob
    @Column(name = "CONTENT", nullable = false)
    private String content;

    @NotNull
    @Column(name = "SEQUENCE_NO", nullable = false)
    private Integer sequenceNo;

    @Column(name = "REQUEST_ID", length = 64)
    private String requestId;

    @NotNull
    @Column(name = "STATUS", nullable = false, length = 32)
    private String status = "COMPLETED";

    @Column(name = "PROVIDER_CODE", length = 64)
    private String providerCode;

    @Column(name = "MODEL_NAME", length = 128)
    private String modelName;

    @Column(name = "CREDENTIAL_OWNER", length = 16)
    private String credentialOwner;

    @Column(name = "TOTAL_TOKENS")
    private Integer totalTokens;

    public LlmChatConversation getConversation() { return conversation; }
    public void setConversation(LlmChatConversation conversation) { this.conversation = conversation; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getSequenceNo() { return sequenceNo; }
    public void setSequenceNo(Integer sequenceNo) { this.sequenceNo = sequenceNo; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getCredentialOwner() { return credentialOwner; }
    public void setCredentialOwner(String credentialOwner) { this.credentialOwner = credentialOwner; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
}

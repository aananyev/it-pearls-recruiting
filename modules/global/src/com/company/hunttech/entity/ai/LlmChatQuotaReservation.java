package com.company.hunttech.entity.ai;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Table(name = "HUNTTECH_LLM_CHAT_QUOTA_RESERVATION")
@Entity(name = "hunttech_LlmChatQuotaReservation")
public class LlmChatQuotaReservation extends StandardEntity {
    private static final long serialVersionUID = 1L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PERIOD_ID", nullable = false)
    private LlmChatQuotaPeriod period;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CONVERSATION_ID")
    private LlmChatConversation conversation;

    @NotNull
    @Column(name = "REQUEST_ID", nullable = false, length = 64, unique = true)
    private String requestId;

    @NotNull
    @Column(name = "RESERVED_TOKENS", nullable = false)
    private Integer reservedTokens;

    @Column(name = "SETTLED_TOKENS")
    private Integer settledTokens;

    @NotNull
    @Column(name = "STATUS", nullable = false, length = 32)
    private String status = "RESERVED";

    public LlmChatQuotaPeriod getPeriod() { return period; }
    public void setPeriod(LlmChatQuotaPeriod period) { this.period = period; }
    public LlmChatConversation getConversation() { return conversation; }
    public void setConversation(LlmChatConversation conversation) { this.conversation = conversation; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Integer getReservedTokens() { return reservedTokens; }
    public void setReservedTokens(Integer reservedTokens) { this.reservedTokens = reservedTokens; }
    public Integer getSettledTokens() { return settledTokens; }
    public void setSettledTokens(Integer settledTokens) { this.settledTokens = settledTokens; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

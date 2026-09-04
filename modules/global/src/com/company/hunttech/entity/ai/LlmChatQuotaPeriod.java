package com.company.hunttech.entity.ai;

import com.company.hunttech.entity.ExtUser;
import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Table(name = "HUNTTECH_LLM_CHAT_QUOTA_PERIOD")
@Entity(name = "hunttech_LlmChatQuotaPeriod")
public class LlmChatQuotaPeriod extends StandardEntity {
    private static final long serialVersionUID = 1L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private ExtUser user;

    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "PERIOD_START", nullable = false)
    private Date periodStart;

    @NotNull
    @Column(name = "QUOTA_TOKENS", nullable = false)
    private Integer quotaTokens;

    @NotNull
    @Column(name = "RESERVED_TOKENS", nullable = false)
    private Integer reservedTokens = 0;

    @NotNull
    @Column(name = "CONSUMED_TOKENS", nullable = false)
    private Integer consumedTokens = 0;

    @NotNull
    @Column(name = "PENDING_TOKENS", nullable = false)
    private Integer pendingTokens = 0;

    public ExtUser getUser() { return user; }
    public void setUser(ExtUser user) { this.user = user; }
    public Date getPeriodStart() { return periodStart; }
    public void setPeriodStart(Date periodStart) { this.periodStart = periodStart; }
    public Integer getQuotaTokens() { return quotaTokens; }
    public void setQuotaTokens(Integer quotaTokens) { this.quotaTokens = quotaTokens; }
    public Integer getReservedTokens() { return reservedTokens; }
    public void setReservedTokens(Integer reservedTokens) { this.reservedTokens = reservedTokens; }
    public Integer getConsumedTokens() { return consumedTokens; }
    public void setConsumedTokens(Integer consumedTokens) { this.consumedTokens = consumedTokens; }
    public Integer getPendingTokens() { return pendingTokens; }
    public void setPendingTokens(Integer pendingTokens) { this.pendingTokens = pendingTokens; }
}

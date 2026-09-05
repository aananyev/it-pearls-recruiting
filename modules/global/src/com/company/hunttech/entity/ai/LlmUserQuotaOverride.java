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

@Table(name = "HUNTTECH_LLM_USER_QUOTA_OVERRIDE")
@Entity(name = "hunttech_LlmUserQuotaOverride")
public class LlmUserQuotaOverride extends StandardEntity {
    private static final long serialVersionUID = 1L;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "USER_ID", nullable = false)
    private ExtUser user;

    @NotNull
    @Column(name = "MONTHLY_QUOTA_TOKENS", nullable = false)
    private Integer monthlyQuotaTokens;

    @NotNull
    @Temporal(TemporalType.DATE)
    @Column(name = "EFFECTIVE_FROM", nullable = false)
    private Date effectiveFrom;

    @Temporal(TemporalType.DATE)
    @Column(name = "EFFECTIVE_TO")
    private Date effectiveTo;

    @NotNull
    @Column(name = "REASON", nullable = false, length = 2000)
    private String reason;

    public ExtUser getUser() { return user; }
    public void setUser(ExtUser user) { this.user = user; }
    public Integer getMonthlyQuotaTokens() { return monthlyQuotaTokens; }
    public void setMonthlyQuotaTokens(Integer monthlyQuotaTokens) { this.monthlyQuotaTokens = monthlyQuotaTokens; }
    public Date getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(Date effectiveFrom) { this.effectiveFrom = effectiveFrom; }
    public Date getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(Date effectiveTo) { this.effectiveTo = effectiveTo; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}

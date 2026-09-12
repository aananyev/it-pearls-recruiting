package com.company.hunttech.service.dto.ai;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.UUID;

public class ActiveUserQuotaSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private UUID userId;
    private String userLogin;
    private String userName;
    private boolean active;
    private Integer allocatedTokens;
    private boolean unlimited;
    private boolean customOverride;
    private int consumedTokens;
    private Integer remainingTokens;
    private int totalCalls;
    private BigDecimal estimatedCost;
    private int errorCount;
    private Date lastCallTime;

    public ActiveUserQuotaSummary() {
        this.estimatedCost = BigDecimal.ZERO;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserLogin() {
        return userLogin;
    }

    public void setUserLogin(String userLogin) {
        this.userLogin = userLogin;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getAllocatedTokens() {
        return allocatedTokens;
    }

    public void setAllocatedTokens(Integer allocatedTokens) {
        this.allocatedTokens = allocatedTokens;
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    public void setUnlimited(boolean unlimited) {
        this.unlimited = unlimited;
    }

    public boolean isCustomOverride() {
        return customOverride;
    }

    public void setCustomOverride(boolean customOverride) {
        this.customOverride = customOverride;
    }

    public int getConsumedTokens() {
        return consumedTokens;
    }

    public void setConsumedTokens(int consumedTokens) {
        this.consumedTokens = consumedTokens;
    }

    public Integer getRemainingTokens() {
        return remainingTokens;
    }

    public void setRemainingTokens(Integer remainingTokens) {
        this.remainingTokens = remainingTokens;
    }

    public int getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(int totalCalls) {
        this.totalCalls = totalCalls;
    }

    public BigDecimal getEstimatedCost() {
        return estimatedCost != null ? estimatedCost : BigDecimal.ZERO;
    }

    public void setEstimatedCost(BigDecimal estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public void setErrorCount(int errorCount) {
        this.errorCount = errorCount;
    }

    public Date getLastCallTime() {
        return lastCallTime;
    }

    public void setLastCallTime(Date lastCallTime) {
        this.lastCallTime = lastCallTime;
    }
}

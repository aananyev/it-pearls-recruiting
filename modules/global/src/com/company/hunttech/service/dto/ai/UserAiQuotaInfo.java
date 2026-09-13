package com.company.hunttech.service.dto.ai;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class UserAiQuotaInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer allocatedTokens;
    private int extraTokens;
    private int consumedTokens;
    private int reservedTokens;
    private int pendingTokens;
    private int totalUsedTokens;
    private Integer remainingTokens;
    private boolean unlimited;
    private boolean customOverride;

    public UserAiQuotaInfo() {
    }

    public UserAiQuotaInfo(Integer allocatedTokens, int consumedTokens, int reservedTokens,
                           int pendingTokens, boolean customOverride) {
        this(allocatedTokens, 0, consumedTokens, reservedTokens, pendingTokens, customOverride);
    }

    public UserAiQuotaInfo(Integer allocatedTokens, int extraTokens, int consumedTokens,
                           int reservedTokens, int pendingTokens, boolean customOverride) {
        this.allocatedTokens = allocatedTokens;
        this.extraTokens = Math.max(0, extraTokens);
        this.consumedTokens = Math.max(0, consumedTokens);
        this.reservedTokens = Math.max(0, reservedTokens);
        this.pendingTokens = Math.max(0, pendingTokens);
        this.totalUsedTokens = this.consumedTokens + this.reservedTokens + this.pendingTokens;
        this.customOverride = customOverride;
        recalculate();
    }

    public static boolean isUnlimitedQuota(Integer tokens) {
        return tokens != null && (tokens == -1 || tokens == Integer.MAX_VALUE);
    }

    private void recalculate() {
        this.totalUsedTokens = this.consumedTokens + this.reservedTokens + this.pendingTokens;
        this.unlimited = isUnlimitedQuota(this.allocatedTokens);
        if (this.unlimited) {
            this.remainingTokens = null;
        } else if (this.allocatedTokens == null) {
            this.remainingTokens = Math.max(0, this.extraTokens - this.totalUsedTokens);
        } else {
            long effectiveLimit = (long) this.allocatedTokens + this.extraTokens;
            long remaining = effectiveLimit - this.totalUsedTokens;
            this.remainingTokens = (int) Math.max(0, Math.min(Integer.MAX_VALUE, remaining));
        }
    }

    public Integer getAllocatedTokens() {
        return allocatedTokens;
    }

    public void setAllocatedTokens(Integer allocatedTokens) {
        this.allocatedTokens = allocatedTokens;
        recalculate();
    }

    public int getConsumedTokens() {
        return consumedTokens;
    }

    public void setConsumedTokens(int consumedTokens) {
        this.consumedTokens = consumedTokens;
        recalculate();
    }

    public int getReservedTokens() {
        return reservedTokens;
    }

    public void setReservedTokens(int reservedTokens) {
        this.reservedTokens = reservedTokens;
        recalculate();
    }

    public int getPendingTokens() {
        return pendingTokens;
    }

    public void setPendingTokens(int pendingTokens) {
        this.pendingTokens = pendingTokens;
        recalculate();
    }

    public int getTotalUsedTokens() {
        return totalUsedTokens;
    }

    public void setTotalUsedTokens(int totalUsedTokens) {
        this.totalUsedTokens = totalUsedTokens;
    }

    public Integer getRemainingTokens() {
        return remainingTokens;
    }

    public Integer getAvailableTokens() {
        return remainingTokens;
    }

    public void setRemainingTokens(Integer remainingTokens) {
        this.remainingTokens = remainingTokens;
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

    public int getExtraTokens() {
        return extraTokens;
    }

    public void setExtraTokens(int extraTokens) {
        this.extraTokens = Math.max(0, extraTokens);
        recalculate();
    }

    public int getEffectiveLimit() {
        if (unlimited) {
            return -1;
        }
        long eff = (long) (allocatedTokens != null ? allocatedTokens : 0) + extraTokens;
        return (int) Math.min((long) Integer.MAX_VALUE, eff);
    }

    public String formatAllocated() {
        if (unlimited) {
            return "Безлимитно";
        }
        if (allocatedTokens == null) {
            return "0";
        }
        return formatNumber(allocatedTokens);
    }

    public String formatExtra() {
        return formatNumber(extraTokens);
    }

    public String formatConsumed() {
        return formatNumber(consumedTokens);
    }

    public String formatTotalUsed() {
        return formatNumber(totalUsedTokens);
    }

    public String formatRemaining() {
        if (unlimited) {
            return "Безлимитно";
        }
        if (remainingTokens == null) {
            return "—";
        }
        return formatNumber(remainingTokens);
    }

    public double getPercentUsed() {
        int eff = getEffectiveLimit();
        if (unlimited || eff <= 0) {
            return 0.0;
        }
        return Math.min(100.0, (totalUsedTokens * 100.0) / eff);
    }

    public double getPercentRemaining() {
        if (unlimited) {
            return 100.0;
        }
        int eff = getEffectiveLimit();
        if (eff <= 0 || remainingTokens == null) {
            return 0.0;
        }
        return Math.max(0.0, (remainingTokens * 100.0) / eff);
    }

    public String getStatusBadgeHtml() {
        if (unlimited) {
            return "<span style='background: rgba(16, 185, 129, 0.15); color: #059669; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px;'>Безлимитно</span>";
        }
        if (allocatedTokens == null && extraTokens <= 0) {
            return "<span style='background: rgba(148, 163, 184, 0.15); color: #64748b; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px;'>Не настроено</span>";
        }
        double rem = getPercentRemaining();
        if (remainingTokens != null && remainingTokens <= 0) {
            return "<span style='background: rgba(239, 68, 68, 0.15); color: #dc2626; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px;'>Исчерпано</span>";
        } else if (rem < 20.0) {
            return "<span style='background: rgba(245, 158, 11, 0.15); color: #d97706; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px;'>Мало (" + String.format(Locale.US, "%.1f%%", rem) + ")</span>";
        } else {
            return "<span style='background: rgba(16, 185, 129, 0.15); color: #059669; padding: 2px 8px; border-radius: 4px; font-weight: 600; font-size: 11px;'>Норма (" + String.format(Locale.US, "%.1f%%", rem) + ")</span>";
        }
    }

    private static String formatNumber(long number) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("ru", "RU"));
        symbols.setGroupingSeparator(' ');
        DecimalFormat format = new DecimalFormat("#,###", symbols);
        return format.format(number);
    }
}

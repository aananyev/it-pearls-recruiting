package com.company.itpearls.core;

import java.io.Serializable;

public class TelegramSendResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String failureReason;

    private TelegramSendResult(boolean success, String failureReason) {
        this.success = success;
        this.failureReason = failureReason;
    }

    public static TelegramSendResult success() {
        return new TelegramSendResult(true, null);
    }

    public static TelegramSendResult failure(String failureReason) {
        return new TelegramSendResult(false, failureReason);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }
}

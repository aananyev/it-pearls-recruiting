package com.company.hunttech.service;

import com.company.hunttech.entity.OpenPosition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Результат умного создания/загрузки вакансии.
 */
public class SmartOpenPositionIngestResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private OpenPosition openPosition;
    private List<String> warnings = new ArrayList<>();

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OpenPosition getOpenPosition() {
        return openPosition;
    }

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings != null ? warnings : new ArrayList<>();
    }
}

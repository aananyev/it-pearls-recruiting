package com.company.hunttech.dto.integration;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Унифицированный формат ответа об ошибке для внешних интеграций.
 */
public class ApiErrorResponseDto implements Serializable {
    private static final long serialVersionUID = 9812736412345678L;

    private boolean success = false;
    private String errorCode;
    private String message;
    private String correlationId;
    private Date timestamp;
    private List<Map<String, String>> details = new ArrayList<>();

    public ApiErrorResponseDto() {
        this.timestamp = new Date();
    }

    public ApiErrorResponseDto(String errorCode, String message, String correlationId) {
        this.success = false;
        this.errorCode = errorCode;
        this.message = message;
        this.correlationId = correlationId;
        this.timestamp = new Date();
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public List<Map<String, String>> getDetails() {
        return details;
    }

    public void setDetails(List<Map<String, String>> details) {
        this.details = details != null ? details : new ArrayList<>();
    }
}

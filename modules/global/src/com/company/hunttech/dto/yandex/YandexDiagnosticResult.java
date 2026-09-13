package com.company.hunttech.dto.yandex;

import java.io.Serializable;

public class YandexDiagnosticResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String serviceType;
    private boolean success;
    private int httpStatusCode;
    private String message;
    private String details;

    public YandexDiagnosticResult() {
    }

    public YandexDiagnosticResult(String serviceType, boolean success, int httpStatusCode, String message, String details) {
        this.serviceType = serviceType;
        this.success = success;
        this.httpStatusCode = httpStatusCode;
        this.message = message;
        this.details = details;
    }

    public static YandexDiagnosticResult ok(String serviceType, String message) {
        return new YandexDiagnosticResult(serviceType, true, 200, message, null);
    }

    public static YandexDiagnosticResult error(String serviceType, int statusCode, String message, String details) {
        return new YandexDiagnosticResult(serviceType, false, statusCode, message, details);
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(int httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}

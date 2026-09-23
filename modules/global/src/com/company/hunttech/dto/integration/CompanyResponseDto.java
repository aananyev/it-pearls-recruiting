package com.company.hunttech.dto.integration;

import java.io.Serializable;

/**
 * Ответ на создание / поиск компании во внешнем API (BL-2026-027).
 */
public class CompanyResponseDto implements Serializable {
    private static final long serialVersionUID = 6271829304152637L;

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_EXISTING_FOUND = "EXISTING_FOUND";

    private boolean success;
    private String companyId;
    private String externalId;
    private String companyName;
    private String status;
    private String correlationId;
    private String message;
    private ApiErrorResponseDto errorDetails;

    public CompanyResponseDto() {
    }

    public static CompanyResponseDto ok(String companyId, String externalId, String companyName, String status, String correlationId) {
        CompanyResponseDto dto = new CompanyResponseDto();
        dto.setSuccess(true);
        dto.setCompanyId(companyId);
        dto.setExternalId(externalId);
        dto.setCompanyName(companyName);
        dto.setStatus(status);
        dto.setCorrelationId(correlationId);
        return dto;
    }

    public static CompanyResponseDto error(String message, String correlationId, String errorCode) {
        CompanyResponseDto dto = new CompanyResponseDto();
        dto.setSuccess(false);
        dto.setMessage(message);
        dto.setCorrelationId(correlationId);
        dto.setErrorDetails(new ApiErrorResponseDto(errorCode, message, correlationId));
        return dto;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public ApiErrorResponseDto getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(ApiErrorResponseDto errorDetails) {
        this.errorDetails = errorDetails;
    }
}

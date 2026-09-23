package com.company.hunttech.dto.integration;

import java.io.Serializable;

/**
 * Ответ на создание резюме кандидата (BL-2026-028).
 */
public class CandidateCvResponseDto implements Serializable {
    private static final long serialVersionUID = 5918237461928374L;

    public static final String STATUS_CREATED = "CREATED";

    private boolean success;
    private String candidateCvId;
    private String candidateId;
    private String externalId;
    private String status;
    private String correlationId;
    private String message;
    private ApiErrorResponseDto errorDetails;

    public CandidateCvResponseDto() {
    }

    public static CandidateCvResponseDto ok(String candidateCvId, String candidateId, String externalId, String status, String correlationId) {
        CandidateCvResponseDto dto = new CandidateCvResponseDto();
        dto.setSuccess(true);
        dto.setCandidateCvId(candidateCvId);
        dto.setCandidateId(candidateId);
        dto.setExternalId(externalId);
        dto.setStatus(status);
        dto.setCorrelationId(correlationId);
        return dto;
    }

    public static CandidateCvResponseDto error(String message, String correlationId, String errorCode) {
        CandidateCvResponseDto dto = new CandidateCvResponseDto();
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

    public String getCandidateCvId() {
        return candidateCvId;
    }

    public void setCandidateCvId(String candidateCvId) {
        this.candidateCvId = candidateCvId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
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

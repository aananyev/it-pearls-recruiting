package com.company.hunttech.dto.integration;

import java.io.Serializable;

/**
 * Ответ на комплексное создание кандидата, резюме и взаимодействия (BL-2026-026).
 */
public class CandidateCompositeResponseDto implements Serializable {
    private static final long serialVersionUID = 8918237461928378L;

    public static final String STATUS_CREATED = "CREATED";
    public static final String STATUS_EXISTING_FOUND = "EXISTING_FOUND";

    private boolean success;
    private String candidateId;
    private String candidateCvId;
    private String interactionId;
    private String externalId;
    private String status;
    private String correlationId;
    private String message;
    private ApiErrorResponseDto errorDetails;

    public CandidateCompositeResponseDto() {
    }

    public static CandidateCompositeResponseDto ok(String candidateId, String candidateCvId, String interactionId,
                                                   String externalId, String status, String correlationId) {
        CandidateCompositeResponseDto dto = new CandidateCompositeResponseDto();
        dto.setSuccess(true);
        dto.setCandidateId(candidateId);
        dto.setCandidateCvId(candidateCvId);
        dto.setInteractionId(interactionId);
        dto.setExternalId(externalId);
        dto.setStatus(status);
        dto.setCorrelationId(correlationId);
        return dto;
    }

    public static CandidateCompositeResponseDto error(String message, String correlationId, String errorCode) {
        CandidateCompositeResponseDto dto = new CandidateCompositeResponseDto();
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

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateCvId() {
        return candidateCvId;
    }

    public void setCandidateCvId(String candidateCvId) {
        this.candidateCvId = candidateCvId;
    }

    public String getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(String interactionId) {
        this.interactionId = interactionId;
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

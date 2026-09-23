package com.company.hunttech.dto.integration;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Ответ на создание взаимодействия (BL-2026-028).
 */
public class InteractionResponseDto implements Serializable {
    private static final long serialVersionUID = 6918237461928376L;

    public static final String STATUS_CREATED = "CREATED";

    private boolean success;
    private String interactionId;
    private String candidateId;
    private String vacancyId;
    private BigDecimal numberInteraction;
    private String externalId;
    private String status;
    private String correlationId;
    private String message;
    private ApiErrorResponseDto errorDetails;

    public InteractionResponseDto() {
    }

    public static InteractionResponseDto ok(String interactionId, String candidateId, String vacancyId,
                                            BigDecimal numberInteraction, String externalId, String status, String correlationId) {
        InteractionResponseDto dto = new InteractionResponseDto();
        dto.setSuccess(true);
        dto.setInteractionId(interactionId);
        dto.setCandidateId(candidateId);
        dto.setVacancyId(vacancyId);
        dto.setNumberInteraction(numberInteraction);
        dto.setExternalId(externalId);
        dto.setStatus(status);
        dto.setCorrelationId(correlationId);
        return dto;
    }

    public static InteractionResponseDto error(String message, String correlationId, String errorCode) {
        InteractionResponseDto dto = new InteractionResponseDto();
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

    public String getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(String interactionId) {
        this.interactionId = interactionId;
    }

    public String getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(String candidateId) {
        this.candidateId = candidateId;
    }

    public String getVacancyId() {
        return vacancyId;
    }

    public void setVacancyId(String vacancyId) {
        this.vacancyId = vacancyId;
    }

    public BigDecimal getNumberInteraction() {
        return numberInteraction;
    }

    public void setNumberInteraction(BigDecimal numberInteraction) {
        this.numberInteraction = numberInteraction;
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

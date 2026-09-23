package com.company.hunttech.dto.integration;

import java.io.Serializable;

/**
 * Ответ на создание проекта и вакансии во внешнем API (BL-2026-025).
 */
public class ProjectVacancyResponseDto implements Serializable {
    private static final long serialVersionUID = 7281930415263741L;

    public static final String STATUS_CREATED = "CREATED";

    private boolean success;
    private String projectId;
    private String vacancyId;
    private String externalId;
    private String status;
    private String correlationId;
    private String message;
    private ApiErrorResponseDto errorDetails;

    public ProjectVacancyResponseDto() {
    }

    public static ProjectVacancyResponseDto ok(String projectId, String vacancyId, String externalId, String status, String correlationId) {
        ProjectVacancyResponseDto dto = new ProjectVacancyResponseDto();
        dto.setSuccess(true);
        dto.setProjectId(projectId);
        dto.setVacancyId(vacancyId);
        dto.setExternalId(externalId);
        dto.setStatus(status);
        dto.setCorrelationId(correlationId);
        return dto;
    }

    public static ProjectVacancyResponseDto error(String message, String correlationId, String errorCode) {
        ProjectVacancyResponseDto dto = new ProjectVacancyResponseDto();
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

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getVacancyId() {
        return vacancyId;
    }

    public void setVacancyId(String vacancyId) {
        this.vacancyId = vacancyId;
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

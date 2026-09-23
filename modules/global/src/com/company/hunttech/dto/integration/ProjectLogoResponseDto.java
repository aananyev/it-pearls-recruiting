package com.company.hunttech.dto.integration;

import java.io.Serializable;

public class ProjectLogoResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String projectId;
    private String projectName;
    private Integer logoSizeBytes;
    private String status;
    private String message;
    private String correlationId;
    private ApiErrorResponseDto errorDetails;

    public ProjectLogoResponseDto() {
    }

    public static ProjectLogoResponseDto success(String projectId, String projectName, int logoSizeBytes, String correlationId) {
        ProjectLogoResponseDto dto = new ProjectLogoResponseDto();
        dto.setSuccess(true);
        dto.setProjectId(projectId);
        dto.setProjectName(projectName);
        dto.setLogoSizeBytes(logoSizeBytes);
        dto.setStatus("UPLOADED");
        dto.setCorrelationId(correlationId);
        return dto;
    }

    public static ProjectLogoResponseDto error(String message, String correlationId, String errorCode) {
        ProjectLogoResponseDto dto = new ProjectLogoResponseDto();
        dto.setSuccess(false);
        dto.setMessage(message);
        dto.setStatus("ERROR");
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

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public Integer getLogoSizeBytes() {
        return logoSizeBytes;
    }

    public void setLogoSizeBytes(Integer logoSizeBytes) {
        this.logoSizeBytes = logoSizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public ApiErrorResponseDto getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(ApiErrorResponseDto errorDetails) {
        this.errorDetails = errorDetails;
    }
}

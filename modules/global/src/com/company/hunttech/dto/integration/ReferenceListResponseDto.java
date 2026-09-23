package com.company.hunttech.dto.integration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Ответ со списком элементов справочника.
 */
public class ReferenceListResponseDto implements Serializable {
    private static final long serialVersionUID = 7182930415263748L;

    private boolean success = true;
    private int totalCount;
    private List<ReferenceItemDto> items = new ArrayList<>();
    private String correlationId;
    private String errorMessage;
    private ApiErrorResponseDto errorDetails;

    public ReferenceListResponseDto() {
    }

    public static ReferenceListResponseDto ok(List<ReferenceItemDto> items, int totalCount, String correlationId) {
        ReferenceListResponseDto response = new ReferenceListResponseDto();
        response.setSuccess(true);
        response.setItems(items != null ? items : new ArrayList<>());
        response.setTotalCount(totalCount);
        response.setCorrelationId(correlationId);
        return response;
    }

    public static ReferenceListResponseDto error(String errorMessage, String correlationId) {
        return error(errorMessage, correlationId, "SYSTEM_ERROR");
    }

    public static ReferenceListResponseDto error(String errorMessage, String correlationId, String errorCode) {
        ReferenceListResponseDto response = new ReferenceListResponseDto();
        response.setSuccess(false);
        response.setErrorMessage(errorMessage);
        response.setCorrelationId(correlationId);
        response.setErrorDetails(new ApiErrorResponseDto(errorCode, errorMessage, correlationId));
        return response;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<ReferenceItemDto> getItems() {
        return items;
    }

    public void setItems(List<ReferenceItemDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public ApiErrorResponseDto getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(ApiErrorResponseDto errorDetails) {
        this.errorDetails = errorDetails;
    }
}

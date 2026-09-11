package com.company.hunttech.service;

import java.io.Serializable;

/**
 * Результат проверки подключения к AI-провайдеру (корпоративному или персональному).
 *
 * Передаётся между core- и web-модулями при тестировании настроек подключения к API.
 * Содержит статус, краткое сообщение, время отклика, а также подробную расшифровку
 * причин неудачи с рекомендациями по их устранению.
 */
public class AiConnectionTestResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String status;
    private String message;
    private String detailedError;
    private String rawError;
    private Long latencyMs;
    private String providerCode;
    private String modelName;

    public AiConnectionTestResult() {
    }

    public static AiConnectionTestResult success(String providerCode, String modelName,
                                                long latencyMs, String sampleResponse) {
        AiConnectionTestResult result = new AiConnectionTestResult();
        result.setSuccess(true);
        result.setStatus("SUCCESS");
        result.setProviderCode(providerCode);
        result.setModelName(modelName);
        result.setLatencyMs(latencyMs);
        String msg = String.format("Подключение успешно установлено. Провайдер: %s, Модель: %s, Время ответа: %d мс.",
                providerCode != null ? providerCode : "default",
                modelName != null ? modelName : "default",
                latencyMs);
        if (sampleResponse != null && !sampleResponse.trim().isEmpty()) {
            msg += " Ответ: «" + sampleResponse.trim() + "»";
        }
        result.setMessage(msg);
        return result;
    }

    public static AiConnectionTestResult fail(String providerCode, String modelName,
                                             String message, String detailedError,
                                             String rawError) {
        AiConnectionTestResult result = new AiConnectionTestResult();
        result.setSuccess(false);
        result.setStatus("FAILED");
        result.setProviderCode(providerCode);
        result.setModelName(modelName);
        result.setMessage(message);
        result.setDetailedError(detailedError);
        result.setRawError(rawError);
        return result;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
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

    public String getDetailedError() {
        return detailedError;
    }

    public void setDetailedError(String detailedError) {
        this.detailedError = detailedError;
    }

    public String getRawError() {
        return rawError;
    }

    public void setRawError(String rawError) {
        this.rawError = rawError;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public void setProviderCode(String providerCode) {
        this.providerCode = providerCode;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}

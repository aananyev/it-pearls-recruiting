package com.company.hunttech.service;

import com.company.hunttech.entity.ai.AiCapability;

import java.io.Serializable;

/**
 * Результат выполнения AI-функции: payload + метаданные для пользовательской нотификации.
 *
 * <p>Контракт (docs/architecture/HRM_HuntTech_AI_User_Notification_Contract.md): каждый
 * успешный реальный AI-вызов через {@link AiExecutionService} возвращает не только
 * сгенерированный текст/изображение, но и метаданные — какую модель какой провайдер
 * выполнил операцию и кто является собственником использованного API
 * ({@link AiCredentialOwner}: корпоративное подключение администратора или личное
 * подключение пользователя). Экраны обязаны показать исчезающую нотификацию с этими
 * данными (стандартные CUBA Notifications, см. web-утилиту AiOperationNotifier).</p>
 *
 * <p>Для текстовых capability заполняется {@link #getText()}, для IMAGE_GENERATION —
 * {@link #getImage()}; второе поле в каждом случае равно {@code null}.</p>
 */
public class AiExecutionResult implements Serializable {

    private final String functionCode;
    private final String functionName;
    private final AiCapability capability;
    private final String text;
    private final byte[] image;
    private final String modelName;
    private final String providerCode;
    private final AiCredentialOwner credentialOwner;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final Integer totalTokens;
    private final String providerRequestId;

    private AiExecutionResult(String functionCode, String functionName, AiCapability capability,
                              String modelName, String providerCode, AiCredentialOwner credentialOwner,
                              String text, byte[] image, Integer promptTokens,
                              Integer completionTokens, Integer totalTokens,
                              String providerRequestId) {
        this.functionCode = functionCode;
        this.functionName = functionName;
        this.capability = capability;
        this.modelName = modelName;
        this.providerCode = providerCode;
        this.credentialOwner = credentialOwner;
        this.text = text;
        this.image = image;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.providerRequestId = providerRequestId;
    }

    /**
     * Создаёт результат текстовой AI-функции (TEXT_GENERATION и др.).
     */
    public static AiExecutionResult textResult(String functionCode, String functionName,
                                               AiCapability capability, String modelName,
                                               String providerCode, AiCredentialOwner credentialOwner,
                                               String text) {
        return new AiExecutionResult(functionCode, functionName, capability,
                modelName, providerCode, credentialOwner, text, null, null, null, null, null);
    }

    /** Provider usage, when the adapter reported it, for quota settlement. */
    public static AiExecutionResult textResult(String functionCode, String functionName,
                                               AiCapability capability, String modelName,
                                               String providerCode, AiCredentialOwner credentialOwner,
                                               String text, Integer promptTokens,
                                               Integer completionTokens, Integer totalTokens) {
        return new AiExecutionResult(functionCode, functionName, capability,
                modelName, providerCode, credentialOwner, text, null,
                promptTokens, completionTokens, totalTokens, null);
    }

    /** Text result with provider-native request identifier when the adapter exposes one. */
    public static AiExecutionResult textResult(String functionCode, String functionName,
                                               AiCapability capability, String modelName,
                                               String providerCode, AiCredentialOwner credentialOwner,
                                               String text, Integer promptTokens,
                                               Integer completionTokens, Integer totalTokens,
                                               String providerRequestId) {
        return new AiExecutionResult(functionCode, functionName, capability,
                modelName, providerCode, credentialOwner, text, null,
                promptTokens, completionTokens, totalTokens, providerRequestId);
    }

    /**
     * Создаёт результат IMAGE_GENERATION-функции.
     */
    public static AiExecutionResult imageResult(String functionCode, String functionName,
                                                AiCapability capability, String modelName,
                                                String providerCode, AiCredentialOwner credentialOwner,
                                                byte[] image) {
        return new AiExecutionResult(functionCode, functionName, capability,
                modelName, providerCode, credentialOwner, null, image, null, null, null, null);
    }

    /** Стабильный код выполненной AI-функции (например {@code PROJECT_SHORT_DESCRIPTION_GENERATE}). */
    public String getFunctionCode() {
        return functionCode;
    }

    /** Отображаемое имя AI-функции из конфигурации (для нотификации «что делала»). */
    public String getFunctionName() {
        return functionName;
    }

    /** Capability выполненной функции. */
    public AiCapability getCapability() {
        return capability;
    }

    /** Сгенерированный текст (для текстовых capability) или {@code null} для IMAGE_GENERATION. */
    public String getText() {
        return text;
    }

    /** Сгенерированное/обработанное изображение (для IMAGE_GENERATION) или {@code null} для текста. */
    public byte[] getImage() {
        return image;
    }

    /** Модель, которая реально выполнила операцию (эффективная, с учётом override/fallback). */
    public String getModelName() {
        return modelName;
    }

    /** Код провайдера, через который выполнялась операция (deepseek, openai, …). */
    public String getProviderCode() {
        return providerCode;
    }

    /** Собственник использованного API: корпоративное подключение или личное пользователя. */
    public AiCredentialOwner getCredentialOwner() {
        return credentialOwner;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public String getProviderRequestId() {
        return providerRequestId;
    }
}

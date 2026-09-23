package com.company.hunttech.core.ai;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Расширенный результат выполнения вызова провайдером AI, включающий
 * сгенерированный контент, статистику токенов и стоимость.
 */
public class AiProviderResponse implements Serializable {
    private static final long serialVersionUID = 192837461928374L;

    private final String text;
    private final byte[] image;
    private final Integer promptTokens;
    private final Integer completionTokens;
    private final Integer totalTokens;
    private final BigDecimal cost;
    private final String currency;
    private final String providerRequestId;
    private final String actualModel;

    public AiProviderResponse(String text, byte[] image, Integer promptTokens,
                              Integer completionTokens, Integer totalTokens,
                              BigDecimal cost, String currency) {
        this(text, image, promptTokens, completionTokens, totalTokens, cost, currency, null, null);
    }

    public AiProviderResponse(String text, byte[] image, Integer promptTokens,
                              Integer completionTokens, Integer totalTokens,
                              BigDecimal cost, String currency, String providerRequestId) {
        this(text, image, promptTokens, completionTokens, totalTokens, cost, currency, providerRequestId, null);
    }

    public AiProviderResponse(String text, byte[] image, Integer promptTokens,
                              Integer completionTokens, Integer totalTokens,
                              BigDecimal cost, String currency, String providerRequestId,
                              String actualModel) {
        this.text = text;
        this.image = image;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.cost = cost;
        this.currency = currency;
        this.providerRequestId = providerRequestId;
        this.actualModel = actualModel;
    }

    public static AiProviderResponse ofText(String text, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        return new AiProviderResponse(text, null, promptTokens, completionTokens, totalTokens, null, null, null, null);
    }

    public static AiProviderResponse ofTextWithCost(String text, Integer promptTokens, Integer completionTokens,
                                                    Integer totalTokens, BigDecimal cost, String currency) {
        return new AiProviderResponse(text, null, promptTokens, completionTokens, totalTokens, cost, currency, null, null);
    }

    public static AiProviderResponse ofText(String text, Integer promptTokens, Integer completionTokens,
                                            Integer totalTokens, String providerRequestId) {
        return new AiProviderResponse(text, null, promptTokens, completionTokens, totalTokens,
                null, null, providerRequestId, null);
    }

    public static AiProviderResponse ofText(String text, Integer promptTokens, Integer completionTokens,
                                            Integer totalTokens, String providerRequestId, String actualModel) {
        return new AiProviderResponse(text, null, promptTokens, completionTokens, totalTokens,
                null, null, providerRequestId, actualModel);
    }

    public static AiProviderResponse ofImage(byte[] image, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        return new AiProviderResponse(null, image, promptTokens, completionTokens, totalTokens, null, null, null, null);
    }

    public String getText() {
        return text;
    }

    public byte[] getImage() {
        return image;
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

    public BigDecimal getCost() {
        return cost;
    }

    public String getCurrency() {
        return currency;
    }

    public String getProviderRequestId() {
        return providerRequestId;
    }

    public String getActualModel() {
        return actualModel;
    }
}

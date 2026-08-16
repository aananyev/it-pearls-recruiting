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

    public AiProviderResponse(String text, byte[] image, Integer promptTokens,
                              Integer completionTokens, Integer totalTokens,
                              BigDecimal cost, String currency) {
        this.text = text;
        this.image = image;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.cost = cost;
        this.currency = currency;
    }

    public static AiProviderResponse ofText(String text, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        return new AiProviderResponse(text, null, promptTokens, completionTokens, totalTokens, null, null);
    }

    public static AiProviderResponse ofTextWithCost(String text, Integer promptTokens, Integer completionTokens,
                                                    Integer totalTokens, BigDecimal cost, String currency) {
        return new AiProviderResponse(text, null, promptTokens, completionTokens, totalTokens, cost, currency);
    }

    public static AiProviderResponse ofImage(byte[] image, Integer promptTokens, Integer completionTokens, Integer totalTokens) {
        return new AiProviderResponse(null, image, promptTokens, completionTokens, totalTokens, null, null);
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
}

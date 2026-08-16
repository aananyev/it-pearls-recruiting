package com.company.hunttech.core.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Калькулятор расчётной стоимости AI-запросов на основе тарифов провайдеров и моделей.
 */
public class AiCostCalculator {

    private static final BigDecimal ONE_MILLION = BigDecimal.valueOf(1_000_000);

    public static class CostResult {
        private final BigDecimal cost;
        private final String currency;

        public CostResult(BigDecimal cost, String currency) {
            this.cost = cost;
            this.currency = currency;
        }

        public BigDecimal getCost() {
            return cost;
        }

        public String getCurrency() {
            return currency;
        }
    }

    /**
     * Вычисляет стоимость на основе токенов и модели.
     *
     * @param providerCode     код провайдера (openai, deepseek, anthropic, gigachat, yandexgpt, ...)
     * @param modelName        название модели
     * @param promptTokens     кол-во входных токенов
     * @param completionTokens кол-во выходных токенов
     * @return CostResult с рассчитанной стоимостью и валютой
     */
    public static CostResult calculateCost(String providerCode, String modelName,
                                          Integer promptTokens, Integer completionTokens) {
        if (promptTokens == null) promptTokens = 0;
        if (completionTokens == null) completionTokens = 0;
        if (promptTokens == 0 && completionTokens == 0) {
            return new CostResult(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP), resolveCurrency(providerCode));
        }

        String model = modelName != null ? modelName.toLowerCase().trim() : "";
        String provider = providerCode != null ? providerCode.toLowerCase().trim() : "";

        // Ставки за 1 миллион токенов (USD или RUB)
        BigDecimal pricePromptPer1M = BigDecimal.valueOf(1.0);
        BigDecimal priceCompletionPer1M = BigDecimal.valueOf(3.0);
        String currency = "USD";

        if ("deepseek".equals(provider) || model.contains("deepseek")) {
            if (model.contains("reasoner") || model.contains("r1")) {
                pricePromptPer1M = BigDecimal.valueOf(0.55);
                priceCompletionPer1M = BigDecimal.valueOf(2.19);
            } else {
                pricePromptPer1M = BigDecimal.valueOf(0.14);
                priceCompletionPer1M = BigDecimal.valueOf(0.28);
            }
        } else if ("openai".equals(provider) || model.contains("gpt") || model.contains("o1") || model.contains("o3")) {
            if (model.contains("gpt-4o-mini")) {
                pricePromptPer1M = BigDecimal.valueOf(0.15);
                priceCompletionPer1M = BigDecimal.valueOf(0.60);
            } else if (model.contains("gpt-4o")) {
                pricePromptPer1M = BigDecimal.valueOf(2.50);
                priceCompletionPer1M = BigDecimal.valueOf(10.00);
            } else if (model.contains("o3-mini")) {
                pricePromptPer1M = BigDecimal.valueOf(1.10);
                priceCompletionPer1M = BigDecimal.valueOf(4.40);
            } else if (model.contains("o1")) {
                pricePromptPer1M = BigDecimal.valueOf(15.00);
                priceCompletionPer1M = BigDecimal.valueOf(60.00);
            } else if (model.contains("gpt-3.5")) {
                pricePromptPer1M = BigDecimal.valueOf(0.50);
                priceCompletionPer1M = BigDecimal.valueOf(1.50);
            } else {
                pricePromptPer1M = BigDecimal.valueOf(2.50);
                priceCompletionPer1M = BigDecimal.valueOf(10.00);
            }
        } else if ("anthropic".equals(provider) || model.contains("claude")) {
            if (model.contains("haiku")) {
                pricePromptPer1M = BigDecimal.valueOf(0.80);
                priceCompletionPer1M = BigDecimal.valueOf(4.00);
            } else if (model.contains("opus")) {
                pricePromptPer1M = BigDecimal.valueOf(15.00);
                priceCompletionPer1M = BigDecimal.valueOf(75.00);
            } else {
                // sonnet
                pricePromptPer1M = BigDecimal.valueOf(3.00);
                priceCompletionPer1M = BigDecimal.valueOf(15.00);
            }
        } else if ("gemini".equals(provider) || model.contains("gemini")) {
            if (model.contains("pro")) {
                pricePromptPer1M = BigDecimal.valueOf(1.25);
                priceCompletionPer1M = BigDecimal.valueOf(5.00);
            } else {
                pricePromptPer1M = BigDecimal.valueOf(0.10);
                priceCompletionPer1M = BigDecimal.valueOf(0.40);
            }
        } else if ("gigachat".equals(provider)) {
            currency = "RUB";
            if (model.contains("pro")) {
                pricePromptPer1M = BigDecimal.valueOf(1000.0);
                priceCompletionPer1M = BigDecimal.valueOf(1000.0);
            } else {
                pricePromptPer1M = BigDecimal.valueOf(200.0);
                priceCompletionPer1M = BigDecimal.valueOf(200.0);
            }
        } else if ("yandexgpt".equals(provider)) {
            currency = "RUB";
            pricePromptPer1M = BigDecimal.valueOf(400.0);
            priceCompletionPer1M = BigDecimal.valueOf(400.0);
        }

        BigDecimal promptCost = BigDecimal.valueOf(promptTokens)
                .multiply(pricePromptPer1M)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);

        BigDecimal completionCost = BigDecimal.valueOf(completionTokens)
                .multiply(priceCompletionPer1M)
                .divide(ONE_MILLION, 6, RoundingMode.HALF_UP);

        BigDecimal total = promptCost.add(completionCost).setScale(6, RoundingMode.HALF_UP);
        return new CostResult(total, currency);
    }

    private static String resolveCurrency(String providerCode) {
        if ("gigachat".equalsIgnoreCase(providerCode) || "yandexgpt".equalsIgnoreCase(providerCode)) {
            return "RUB";
        }
        return "USD";
    }
}

package com.company.hunttech.ai;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Единый UI-каталог AI-провайдеров HRM HuntTech.
 *
 * Каталог находится в global-модуле, потому что Web Client не должен зависеть от core
 * implementations AIProvider. Коды и модели синхронизируются контрактным тестом с
 * фактическим AIProviderRegistry/провайдерами core-модуля.
 */
public final class AiProviderCatalog {
    private static final Map<String, String> PROVIDER_OPTIONS;
    private static final Map<String, String> DEFAULT_MODELS;

    static {
        LinkedHashMap<String, String> providerOptions = new LinkedHashMap<>();
        providerOptions.put("YandexGPT", "yandex");
        providerOptions.put("GigaChat", "gigachat");
        providerOptions.put("OpenAI", "openai");
        providerOptions.put("Anthropic Claude", "anthropic");
        providerOptions.put("Google Gemini", "gemini");
        providerOptions.put("xAI Grok", "grok");
        providerOptions.put("DeepSeek", "deepseek");
        providerOptions.put("Alibaba Qwen", "qwen");
        providerOptions.put("Moonshot Kimi", "kimi");
        providerOptions.put("Z.AI GLM", "glm");
        PROVIDER_OPTIONS = Collections.unmodifiableMap(providerOptions);

        LinkedHashMap<String, String> defaultModels = new LinkedHashMap<>();
        defaultModels.put("yandex", "yandexgpt/latest");
        defaultModels.put("gigachat", "GigaChat");
        defaultModels.put("openai", "gpt-4o");
        defaultModels.put("anthropic", "claude-sonnet-4-6");
        defaultModels.put("gemini", "gemini-3.5-flash");
        defaultModels.put("grok", "grok-4.3");
        defaultModels.put("deepseek", "deepseek-v4-flash");
        defaultModels.put("qwen", "qwen-plus");
        defaultModels.put("kimi", "kimi-k2.5");
        defaultModels.put("glm", "glm-5.1");
        DEFAULT_MODELS = Collections.unmodifiableMap(defaultModels);
    }

    private AiProviderCatalog() {
    }

    /**
     * Возвращает неизменяемую карту caption -> providerCode для LookupField.
     */
    public static Map<String, String> getProviderOptions() {
        return PROVIDER_OPTIONS;
    }

    /**
     * Возвращает неизменяемую карту providerCode -> defaultModel.
     */
    public static Map<String, String> getDefaultModels() {
        return DEFAULT_MODELS;
    }

    public static String getDefaultModel(String providerCode) {
        return DEFAULT_MODELS.get(providerCode);
    }
}

package com.company.hunttech.core.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.util.Map;

/**
 * Подключение к OpenRouter через Chat Completions API.
 */
@Component
public class OpenRouterProvider extends AbstractOpenAiCompatibleProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterProvider.class);
    private static final String DEFAULT_NEMOTRON_FREE_MODEL = "nvidia/nemotron-3-ultra-550b-a55b:free";

    @Override
    public String getProviderCode() {
        return "openrouter";
    }

    @Override
    protected String getApiUrl() {
        return "https://openrouter.ai/api/v1/chat/completions";
    }

    @Override
    protected String getDefaultModel() {
        return DEFAULT_NEMOTRON_FREE_MODEL;
    }

    @Override
    protected String getProviderDisplayName() {
        return "OpenRouter";
    }

    @Override
    public AiProviderResponse executeTextWithTokens(String prompt, String systemContext, String apiKey,
                                                    String modelName, Map<String, Object> options) {
        String resolvedModel = resolveModelName(modelName);
        try {
            return super.executeTextWithTokens(prompt, systemContext, apiKey, resolvedModel, options);
        } catch (RuntimeException e) {
            if (shouldFallbackToPaid(resolvedModel, e)) {
                String paidModel = stripFreeSuffix(resolvedModel);
                log.warn("Бесплатный суточный лимит OpenRouter для модели {} исчерпан (429). Автоматический переход на платную модель {}",
                        resolvedModel, paidModel);
                return super.executeTextWithTokens(prompt, systemContext, apiKey, paidModel, options);
            }
            throw e;
        }
    }

    @Override
    public AiProviderResponse executeTextStreaming(String prompt, String systemContext, String apiKey,
                                                    String modelName, Map<String, Object> options,
                                                    com.company.hunttech.service.AiStreamListener listener) {
        String resolvedModel = resolveModelName(modelName);
        try {
            return super.executeTextStreaming(prompt, systemContext, apiKey, resolvedModel, options, listener);
        } catch (RuntimeException e) {
            if (shouldFallbackToPaid(resolvedModel, e)) {
                String paidModel = stripFreeSuffix(resolvedModel);
                log.warn("Бесплатный суточный лимит OpenRouter для стриминга модели {} исчерпан (429). Автоматический переход на платную модель {}",
                        resolvedModel, paidModel);
                return super.executeTextStreaming(prompt, systemContext, apiKey, paidModel, options, listener);
            }
            throw e;
        }
    }

    private boolean shouldFallbackToPaid(String modelName, Throwable e) {
        if (modelName == null || !modelName.endsWith(":free")) {
            return false;
        }
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        return msg.contains("429")
                || msg.contains("Rate limit exceeded")
                || msg.contains("free-models-per-day")
                || msg.contains("free_tier");
    }

    private String stripFreeSuffix(String modelName) {
        return modelName.substring(0, modelName.length() - ":free".length());
    }

    @Override
    protected String resolveModelName(String modelName) {
        String resolved = super.resolveModelName(modelName);
        if (resolved != null) {
            String trimmed = resolved.trim();
            if ("nemotron-3-ultra-550b-a55b".equalsIgnoreCase(trimmed)
                    || "nvidia/nemotron-3-ultra-550b-a55b".equalsIgnoreCase(trimmed)) {
                return "nvidia/nemotron-3-ultra-550b-a55b";
            }
            if ("nemotron-3-ultra-550b".equalsIgnoreCase(trimmed)
                    || "nemotron-3-ultra-550b:free".equalsIgnoreCase(trimmed)
                    || "nvidia/nemotron-3-ultra-550b".equalsIgnoreCase(trimmed)
                    || "nvidia/nemotron-3-ultra-550b:free".equalsIgnoreCase(trimmed)
                    || "nemotron-3-ultra-550b-a55b:free".equalsIgnoreCase(trimmed)
                    || "openrouter/openai".equalsIgnoreCase(trimmed)) {
                return DEFAULT_NEMOTRON_FREE_MODEL;
            }
        }
        return resolved;
    }

    @Override
    protected void customizeConnection(HttpURLConnection connection) {
        connection.setRequestProperty("HTTP-Referer", "https://hunttech.ru");
        connection.setRequestProperty("X-Title", "HuntTech HRM");
    }

    // Note: OpenRouter does not support image generation via the same endpoint as OpenAI.
    // If image generation is required, a different approach would be needed.
    // For now, we leave the method unimplemented as it is not used in the current context.
    @Override
    public byte[] generateImage(String prompt, String systemContext, String apiKey, String modelName,
                                Map<String, Object> options, byte[] sourceImage, String sourceMimeType) {
        throw new UnsupportedOperationException("OpenRouter image generation not implemented");
    }
}
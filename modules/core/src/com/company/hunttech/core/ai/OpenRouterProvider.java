package com.company.hunttech.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/**
 * Подключение к OpenRouter через Chat Completions API.
 */
@Component
public class OpenRouterProvider extends AbstractOpenAiCompatibleProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterProvider.class);

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
        return "openrouter/openai";
    }

    @Override
    protected String getProviderDisplayName() {
        return "OpenRouter";
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
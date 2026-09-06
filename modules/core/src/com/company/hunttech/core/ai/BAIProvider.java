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
 * Подключение к B.AI через Chat Completions API.
 */
@Component
public class BAIProvider extends AbstractOpenAiCompatibleProvider {

    private static final Logger log = LoggerFactory.getLogger(BAIProvider.class);

    @Override
    public String getProviderCode() {
        return "bai";
    }

    @Override
    protected String getApiUrl() {
        // Assuming B.AI uses an OpenAI-compatible API endpoint.
        // Replace with the actual endpoint if known.
        return "https://api.bai.chat/v1/chat/completions";
    }

    @Override
    protected String getDefaultModel() {
        return "bai/chat";
    }

    @Override
    protected String getProviderDisplayName() {
        return "B.AI";
    }

    // Note: B.AI may not support image generation via the same endpoint as OpenAI.
    // If image generation is required, a different approach would be needed.
    // For now, we leave the method unimplemented as it is not used in the current context.
    @Override
    public byte[] generateImage(String prompt, String systemContext, String apiKey, String modelName,
                                Map<String, Object> options, byte[] sourceImage, String sourceMimeType) {
        throw new UnsupportedOperationException("B.AI image generation not implemented");
    }
}
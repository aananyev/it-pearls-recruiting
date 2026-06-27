package com.company.itpearls.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class OpenAiProvider implements AIProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String DEFAULT_MODEL = "gpt-4o";
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderCode() {
        return "openai";
    }

    @Override
    public String generateText(String prompt, String systemContext, String apiKey, String modelName,
                               Map<String, Object> options) {
        try {
            String model = isConfigured(modelName) ? modelName.trim() : DEFAULT_MODEL;
            double temperature = resolveTemperature(options);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", model);
            requestBody.put("temperature", temperature);

            ArrayNode messages = requestBody.putArray("messages");
            if (isConfigured(systemContext)) {
                ObjectNode systemMessage = messages.addObject();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemContext);
            }
            ObjectNode userMessage = messages.addObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            HttpURLConnection connection = null;
            try {
                URL url = new URL(API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);

                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(requestJson.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                String responseBody = readResponseBody(connection, responseCode);

                if (responseCode < 200 || responseCode >= 300) {
                    throw new IOException("HTTP " + responseCode + ": " + responseBody);
                }

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode content = root.path("choices").path(0).path("message").path("content");
                if (content.isMissingNode() || content.isNull()) {
                    throw new IOException("пустой ответ: choices[0].message.content отсутствует");
                }
                return content.asText();
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        } catch (IOException e) {
            log.error("OpenAI API request failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка запроса к OpenAI API: " + e.getMessage(), e);
        }
    }

    private double resolveTemperature(Map<String, Object> options) {
        if (options == null) {
            return DEFAULT_TEMPERATURE;
        }
        Object temperature = options.get("temperature");
        if (temperature instanceof Number) {
            return ((Number) temperature).doubleValue();
        }
        if (temperature instanceof String) {
            try {
                return Double.parseDouble(((String) temperature).trim());
            } catch (NumberFormatException e) {
                log.warn("Invalid temperature value in options: {}", temperature);
            }
        }
        return DEFAULT_TEMPERATURE;
    }

    private String readResponseBody(HttpURLConnection connection, int responseCode) throws IOException {
        InputStream stream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

package com.company.hunttech.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Общая реализация провайдеров, поддерживающих протокол OpenAI Chat Completions.
 *
 * <p>Класс централизует формирование сообщений, авторизацию, таймауты и разбор
 * ответа. Благодаря этому отдельный провайдер задаёт только свой адрес, код и
 * модель по умолчанию, а пользовательский API-ключ всегда передаётся из
 * {@code UserAiConfiguration} и не хранится в исходном коде.</p>
 */
public abstract class AbstractOpenAiCompatibleProvider implements AIProvider {

    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 60_000;
    private static final double DEFAULT_TEMPERATURE = 0.7;

    protected final ObjectMapper objectMapper = new ObjectMapper();
    private final Logger log = LoggerFactory.getLogger(getClass());

    protected abstract String getApiUrl();

    protected abstract String getDefaultModel();

    protected abstract String getProviderDisplayName();

    @Override
    public String generateText(String prompt, String systemContext, String apiKey, String modelName,
                               Map<String, Object> options) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", isConfigured(modelName) ? modelName.trim() : getDefaultModel());
            requestBody.put("temperature", resolveTemperature(options));
            requestBody.put("stream", false);

            ArrayNode messages = requestBody.putArray("messages");
            addMessage(messages, "system", systemContext);
            addMessage(messages, "user", prompt);
            customizeRequestBody(requestBody, options);

            HttpURLConnection connection = openJsonPostConnection(getApiUrl());
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            customizeConnection(connection);
            String responseBody = execute(connection, objectMapper.writeValueAsString(requestBody));

            JsonNode content = objectMapper.readTree(responseBody)
                    .path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull() || !isConfigured(content.asText())) {
                throw new IOException("пустой ответ: choices[0].message.content отсутствует");
            }
            return content.asText();
        } catch (IOException e) {
            log.error("{} API request failed: {}", getProviderDisplayName(), e.getMessage(), e);
            throw new RuntimeException("Ошибка запроса к " + getProviderDisplayName() + " API: "
                    + e.getMessage(), e);
        }
    }

    /** Позволяет провайдеру добавить специфичные параметры запроса. */
    protected void customizeRequestBody(ObjectNode requestBody, Map<String, Object> options) {
        // Большинству OpenAI-совместимых API дополнительные поля не требуются.
    }

    /** Позволяет провайдеру добавить специфичные HTTP-заголовки. */
    protected void customizeConnection(HttpURLConnection connection) {
        // Большинству OpenAI-совместимых API достаточно Bearer-авторизации.
    }

    protected HttpURLConnection openJsonPostConnection(String apiUrl) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Accept", "application/json");
        return connection;
    }

    protected String execute(HttpURLConnection connection, String requestBody) throws IOException {
        try {
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(requestBody.getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = connection.getResponseCode();
            String responseBody = readResponseBody(connection, responseCode);
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("HTTP " + responseCode + ": " + responseBody);
            }
            return responseBody;
        } finally {
            connection.disconnect();
        }
    }

    protected void addMessage(ArrayNode messages, String role, String content) {
        if (!isConfigured(content)) {
            return;
        }
        ObjectNode message = messages.addObject();
        message.put("role", role);
        message.put("content", content);
    }

    protected boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /** Приводит общий параметр temperature к числу для любого дочернего адаптера. */
    protected double resolveTemperature(Map<String, Object> options) {
        if (options != null) {
            Object temperature = options.get("temperature");
            if (temperature instanceof Number) {
                return ((Number) temperature).doubleValue();
            }
            if (temperature instanceof String) {
                try {
                    return Double.parseDouble(((String) temperature).trim());
                } catch (NumberFormatException e) {
                    log.warn("Некорректное значение temperature: {}", temperature);
                }
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
}

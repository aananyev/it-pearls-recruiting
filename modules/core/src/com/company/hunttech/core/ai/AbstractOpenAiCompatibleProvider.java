package com.company.hunttech.core.ai;

import com.company.hunttech.service.AiStreamListener;
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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
    private final ConcurrentMap<String, HttpURLConnection> activeConnections = new ConcurrentHashMap<>();
    private final Set<String> cancelledRequests = ConcurrentHashMap.newKeySet();

    protected abstract String getApiUrl();

    protected abstract String getDefaultModel();

    protected abstract String getProviderDisplayName();

    @Override
    public String generateText(String prompt, String systemContext, String apiKey, String modelName,
                               Map<String, Object> options) {
        return executeTextWithTokens(prompt, systemContext, apiKey, modelName, options).getText();
    }

    @Override
    public AiProviderResponse executeTextWithTokens(String prompt, String systemContext, String apiKey,
                                                    String modelName, Map<String, Object> options) {
        String requestId = resolveRequestId(options);
        HttpURLConnection connection = null;
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", isConfigured(modelName) ? modelName.trim() : getDefaultModel());
            requestBody.put("temperature", resolveTemperature(options));
            requestBody.put("stream", false);

            ArrayNode messages = requestBody.putArray("messages");
            addMessage(messages, "system", systemContext);
            addMessage(messages, "user", prompt);
            customizeRequestBody(requestBody, options);

            connection = openJsonPostConnection(getApiUrl());
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            customizeConnection(connection);
            registerRequest(requestId, connection);
            String responseBody = execute(connection, objectMapper.writeValueAsString(requestBody));

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull() || !isConfigured(content.asText())) {
                throw new IOException("пустой ответ: choices[0].message.content отсутствует");
            }
            String text = content.asText();

            JsonNode usage = root.path("usage");
            int promptTokens = usage.path("prompt_tokens").asInt(0);
            int completionTokens = usage.path("completion_tokens").asInt(0);
            int totalTokens = usage.path("total_tokens").asInt(0);

            if (promptTokens == 0 && completionTokens == 0) {
                promptTokens = prompt != null ? (prompt.length() + (systemContext != null ? systemContext.length() : 0)) / 4 : 0;
                completionTokens = text.length() / 4;
                totalTokens = promptTokens + completionTokens;
            }

            String providerRequestId = root.path("id").isTextual() ? root.path("id").asText() : null;
            return AiProviderResponse.ofText(text, promptTokens, completionTokens, totalTokens, providerRequestId);
        } catch (IOException e) {
            if (isCancelled(requestId)) {
                throw new AiRequestCancelledException(requestId);
            }
            log.error("{} API request failed: {}", getProviderDisplayName(), e.getMessage(), e);
            throw new RuntimeException("Ошибка запроса к " + getProviderDisplayName() + " API: "
                    + e.getMessage(), e);
        } finally {
            unregisterRequest(requestId, connection);
        }
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public AiProviderResponse executeTextStreaming(String prompt, String systemContext, String apiKey,
                                                    String modelName, Map<String, Object> options,
                                                    AiStreamListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("Streaming listener не задан.");
        }
        String requestId = resolveRequestId(options);
        HttpURLConnection connection = null;
        StringBuilder text = new StringBuilder();
        String providerRequestId = null;
        int promptTokens = 0;
        int completionTokens = 0;
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", isConfigured(modelName) ? modelName.trim() : getDefaultModel());
            requestBody.put("temperature", resolveTemperature(options));
            requestBody.put("stream", true);
            requestBody.putObject("stream_options").put("include_usage", true);
            ArrayNode messages = requestBody.putArray("messages");
            addMessage(messages, "system", systemContext);
            addMessage(messages, "user", prompt);
            customizeRequestBody(requestBody, options);

            connection = openJsonPostConnection(getApiUrl());
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            connection.setRequestProperty("Accept", "text/event-stream");
            customizeConnection(connection);
            registerRequest(requestId, connection);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(objectMapper.writeValueAsString(requestBody).getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("HTTP " + responseCode + ": " + readResponseBody(connection, responseCode));
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.startsWith("data:")) {
                        continue;
                    }
                    String payload = line.substring("data:".length()).trim();
                    if ("[DONE]".equals(payload)) {
                        break;
                    }
                    JsonNode root = objectMapper.readTree(payload);
                    if (root.path("id").isTextual()) {
                        providerRequestId = root.path("id").asText();
                        listener.onProviderRequestId(providerRequestId);
                    }
                    JsonNode delta = root.path("choices").path(0).path("delta").path("content");
                    if (delta.isTextual() && isConfigured(delta.asText())) {
                        String part = delta.asText();
                        text.append(part);
                        listener.onDelta(part);
                    }
                    JsonNode usage = root.path("usage");
                    promptTokens = usage.path("prompt_tokens").asInt(promptTokens);
                    completionTokens = usage.path("completion_tokens").asInt(completionTokens);
                    int reportedTotalTokens = usage.path("total_tokens").asInt(-1);
                    if (reportedTotalTokens >= 0 || usage.has("prompt_tokens") || usage.has("completion_tokens")) {
                        listener.onUsage(promptTokens, completionTokens,
                                reportedTotalTokens >= 0 ? reportedTotalTokens : promptTokens + completionTokens);
                    }
                }
            }
            if (!isConfigured(text.toString())) {
                throw new IOException("пустой streaming-ответ");
            }
            if (promptTokens == 0 && completionTokens == 0) {
                promptTokens = prompt != null ? (prompt.length() + (systemContext != null ? systemContext.length() : 0)) / 4 : 0;
                completionTokens = text.length() / 4;
            }
            return AiProviderResponse.ofText(text.toString(), promptTokens, completionTokens,
                    promptTokens + completionTokens, providerRequestId);
        } catch (IOException e) {
            if (isCancelled(requestId)) {
                throw new AiRequestCancelledException(requestId);
            }
            throw new RuntimeException("Ошибка streaming-запроса к " + getProviderDisplayName()
                    + " API: " + e.getMessage(), e);
        } finally {
            unregisterRequest(requestId, connection);
        }
    }

    @Override
    public void cancelRequest(String requestId) {
        if (!isConfigured(requestId)) {
            return;
        }
        String normalized = requestId.trim();
        cancelledRequests.add(normalized);
        HttpURLConnection connection = activeConnections.get(normalized);
        if (connection != null) {
            connection.disconnect();
        }
    }

    private String resolveRequestId(Map<String, Object> options) {
        if (options == null || !(options.get("requestId") instanceof String)) {
            return null;
        }
        String requestId = ((String) options.get("requestId")).trim();
        return requestId.isEmpty() ? null : requestId;
    }

    private void registerRequest(String requestId, HttpURLConnection connection) {
        if (requestId == null) {
            return;
        }
        activeConnections.put(requestId, connection);
        if (cancelledRequests.contains(requestId)) {
            connection.disconnect();
        }
    }

    private void unregisterRequest(String requestId, HttpURLConnection connection) {
        if (requestId == null) {
            return;
        }
        if (connection != null) {
            activeConnections.remove(requestId, connection);
            connection.disconnect();
        }
        cancelledRequests.remove(requestId);
    }

    private boolean isCancelled(String requestId) {
        return requestId != null && cancelledRequests.contains(requestId);
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

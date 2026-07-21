package com.company.hunttech.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

/** Подключение к GigaChat с обязательным обменом ключа авторизации на OAuth-токен. */
@Component
public class GigaChatProvider extends AbstractOpenAiCompatibleProvider {

    private static final String OAUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";

    @Override public String getProviderCode() { return "gigachat"; }
    @Override protected String getApiUrl() { return "https://api.giga.chat/v1/chat/completions"; }
    @Override protected String getDefaultModel() { return "GigaChat"; }
    @Override protected String getProviderDisplayName() { return "GigaChat"; }

    @Override
    public String generateText(String prompt, String systemContext, String apiKey, String modelName,
                               Map<String, Object> options) {
        try {
            /* Необязательный префикс scope отделяется вертикальной чертой от ключа авторизации. */
            String[] credentials = apiKey.split("\\|", 2);
            String scope = credentials.length == 2 ? credentials[0].trim() : "GIGACHAT_API_PERS";
            String authorizationKey = credentials.length == 2 ? credentials[1].trim() : apiKey.trim();
            String accessToken = requestAccessToken(scope, authorizationKey);

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", isConfigured(modelName) ? modelName.trim() : getDefaultModel());
            requestBody.put("stream", false);
            requestBody.put("temperature", resolveTemperature(options));
            ArrayNode messages = requestBody.putArray("messages");
            addMessage(messages, "system", systemContext);
            addMessage(messages, "user", prompt);

            HttpURLConnection connection = openJsonPostConnection(getApiUrl());
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            connection.setRequestProperty("User-Agent", "HuntTech-HRM/1.0");
            String response = execute(connection, objectMapper.writeValueAsString(requestBody));
            JsonNode text = objectMapper.readTree(response)
                    .path("choices").path(0).path("message").path("content");
            if (text.isMissingNode() || !isConfigured(text.asText())) {
                throw new IOException("пустой ответ: choices[0].message.content отсутствует");
            }
            return text.asText();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка запроса к GigaChat API: " + e.getMessage(), e);
        }
    }

    private String requestAccessToken(String scope, String authorizationKey) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(OAUTH_URL).openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(15_000);
        connection.setReadTimeout(30_000);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("RqUID", UUID.randomUUID().toString());
        connection.setRequestProperty("Authorization", authorizationKey.startsWith("Basic ")
                ? authorizationKey : "Basic " + authorizationKey);
        String response = execute(connection, "scope=" + encodeFormValue(scope));
        JsonNode token = objectMapper.readTree(response).path("access_token");
        if (token.isMissingNode() || !isConfigured(token.asText())) {
            throw new IOException("OAuth не вернул access_token");
        }
        return token.asText();
    }

    private String encodeFormValue(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}

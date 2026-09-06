package com.company.hunttech.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

/** Подключение к YandexGPT через Yandex Cloud AI Studio Completion API. */
@Component
public class YandexGptProvider extends AbstractOpenAiCompatibleProvider {

    @Override public String getProviderCode() { return "yandex"; }
    @Override protected String getApiUrl() { return "https://llm.api.cloud.yandex.net/foundationModels/v1/completion"; }
    @Override protected String getDefaultModel() { return "yandexgpt/latest"; }
    @Override protected String getProviderDisplayName() { return "YandexGPT"; }
    @Override public boolean supportsStreaming() { return false; }

    @Override
    public String generateText(String prompt, String systemContext, String apiKey, String modelName,
                               Map<String, Object> options) {
        try {
            /* В одном персональном поле храним folderId и секрет: folderId|apiKey. */
            String[] credentials = apiKey.split("\\|", 2);
            if (credentials.length != 2 || !isConfigured(credentials[0]) || !isConfigured(credentials[1])) {
                throw new IOException("для YandexGPT укажите ключ в формате folderId|apiKey");
            }
            String configuredModel = isConfigured(modelName) ? modelName.trim() : getDefaultModel();
            String modelUri = configuredModel.startsWith("gpt://")
                    ? configuredModel : "gpt://" + credentials[0].trim() + "/" + configuredModel;

            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("modelUri", modelUri);
            ObjectNode completionOptions = requestBody.putObject("completionOptions");
            completionOptions.put("stream", false);
            completionOptions.put("temperature", resolveTemperature(options));
            ArrayNode messages = requestBody.putArray("messages");
            addMessage(messages, "system", systemContext);
            addMessage(messages, "user", prompt);

            HttpURLConnection connection = openJsonPostConnection(getApiUrl());
            connection.setRequestProperty("Authorization", "Api-Key " + credentials[1].trim());
            String response = execute(connection, objectMapper.writeValueAsString(requestBody));
            JsonNode text = objectMapper.readTree(response)
                    .path("result").path("alternatives").path(0).path("message").path("text");
            if (text.isMissingNode() || !isConfigured(text.asText())) {
                throw new IOException("пустой ответ: result.alternatives[0].message.text отсутствует");
            }
            return text.asText();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка запроса к YandexGPT API: " + e.getMessage(), e);
        }
    }
}

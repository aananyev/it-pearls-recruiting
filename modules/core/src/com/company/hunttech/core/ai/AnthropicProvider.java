package com.company.hunttech.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.Map;

/** Подключение к Claude через нативный Anthropic Messages API. */
@Component
public class AnthropicProvider extends AbstractOpenAiCompatibleProvider {

    @Override public String getProviderCode() { return "anthropic"; }
    @Override protected String getApiUrl() { return "https://api.anthropic.com/v1/messages"; }
    @Override protected String getDefaultModel() { return "claude-sonnet-4-6"; }
    @Override protected String getProviderDisplayName() { return "Anthropic Claude"; }

    @Override
    public String generateText(String prompt, String systemContext, String apiKey, String modelName,
                               Map<String, Object> options) {
        return executeTextWithTokens(prompt, systemContext, apiKey, modelName, options).getText();
    }

    @Override
    public AiProviderResponse executeTextWithTokens(String prompt, String systemContext, String apiKey,
                                                    String modelName, Map<String, Object> options) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", isConfigured(modelName) ? modelName.trim() : getDefaultModel());
            requestBody.put("max_tokens", 512);
            requestBody.put("temperature", resolveTemperature(options));
            if (isConfigured(systemContext)) {
                requestBody.put("system", systemContext);
            }
            ArrayNode messages = requestBody.putArray("messages");
            addMessage(messages, "user", prompt);

            HttpURLConnection connection = openJsonPostConnection(getApiUrl());
            connection.setRequestProperty("x-api-key", apiKey.trim());
            connection.setRequestProperty("anthropic-version", "2023-06-01");
            String response = execute(connection, objectMapper.writeValueAsString(requestBody));
            JsonNode root = objectMapper.readTree(response);
            JsonNode text = root.path("content").path(0).path("text");
            if (text.isMissingNode() || !isConfigured(text.asText())) {
                throw new IOException("пустой ответ: content[0].text отсутствует");
            }

            JsonNode usage = root.path("usage");
            int promptTokens = usage.path("input_tokens").asInt(0);
            int completionTokens = usage.path("output_tokens").asInt(0);
            if (promptTokens == 0 && completionTokens == 0) {
                promptTokens = prompt != null ? (prompt.length() + (systemContext != null ? systemContext.length() : 0)) / 4 : 0;
                completionTokens = text.asText().length() / 4;
            }

            return AiProviderResponse.ofText(text.asText(), promptTokens, completionTokens, promptTokens + completionTokens);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка запроса к Anthropic Claude API: " + e.getMessage(), e);
        }
    }
}

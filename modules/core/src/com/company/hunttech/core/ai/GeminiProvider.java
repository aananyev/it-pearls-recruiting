package com.company.hunttech.core.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Подключение к Google Gemini через нативный GenerateContent API. */
@Component
public class GeminiProvider extends AbstractOpenAiCompatibleProvider {

    private static final String API_BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/";

    @Override public String getProviderCode() { return "gemini"; }
    @Override protected String getApiUrl() { return API_BASE_URL; }
    @Override protected String getDefaultModel() { return "gemini-3.5-flash"; }
    @Override protected String getProviderDisplayName() { return "Google Gemini"; }

    @Override
    public String generateText(String prompt, String systemContext, String apiKey, String modelName,
                               Map<String, Object> options) {
        try {
            String model = isConfigured(modelName) ? modelName.trim() : getDefaultModel();
            String url = API_BASE_URL + URLEncoder.encode(model, StandardCharsets.UTF_8.name())
                    + ":generateContent";
            ObjectNode requestBody = objectMapper.createObjectNode();
            if (isConfigured(systemContext)) {
                requestBody.putObject("system_instruction").putArray("parts")
                        .addObject().put("text", systemContext);
            }
            ArrayNode contents = requestBody.putArray("contents");
            contents.addObject().put("role", "user").putArray("parts")
                    .addObject().put("text", prompt);
            requestBody.putObject("generationConfig")
                    .put("temperature", resolveTemperature(options));

            HttpURLConnection connection = openJsonPostConnection(url);
            connection.setRequestProperty("x-goog-api-key", apiKey.trim());
            String response = execute(connection, objectMapper.writeValueAsString(requestBody));
            JsonNode text = objectMapper.readTree(response)
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (text.isMissingNode() || !isConfigured(text.asText())) {
                throw new IOException("пустой ответ: candidates[0].content.parts[0].text отсутствует");
            }
            return text.asText();
        } catch (IOException e) {
            throw new RuntimeException("Ошибка запроса к Google Gemini API: " + e.getMessage(), e);
        }
    }
}

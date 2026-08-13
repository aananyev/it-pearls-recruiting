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

/** Подключение к OpenAI через Chat Completions API. */
@Component
public class OpenAiProvider extends AbstractOpenAiCompatibleProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiProvider.class);

    /** Endpoint редактирования изображений (capability IMAGE_GENERATION). */
    private static final String IMAGE_EDIT_URL = "https://api.openai.com/v1/images/edits";
    private static final String DEFAULT_IMAGE_MODEL = "gpt-image-2";
    private static final int IMAGE_READ_TIMEOUT_MS = 120_000;

    @Override public String getProviderCode() { return "openai"; }
    @Override protected String getApiUrl() { return "https://api.openai.com/v1/chat/completions"; }
    @Override protected String getDefaultModel() { return "gpt-4o"; }
    @Override protected String getProviderDisplayName() { return "OpenAI"; }

    @Override
    public byte[] generateImage(String prompt, String systemContext, String apiKey, String modelName,
                                Map<String, Object> options, byte[] sourceImage, String sourceMimeType) {
        if (sourceImage == null || sourceImage.length == 0) {
            throw new IllegalArgumentException("OpenAI image edit: исходное изображение пустое.");
        }
        String model = isConfigured(modelName) ? modelName.trim() : DEFAULT_IMAGE_MODEL;
        String mimeType = isConfigured(sourceMimeType) ? sourceMimeType.trim() : "image/png";
        String boundary = "----HuntTech" + UUID.randomUUID().toString().replace("-", "");
        try {
            byte[] body = buildMultipartBody(boundary, prompt, model, mimeType, sourceImage);
            HttpURLConnection connection = (HttpURLConnection) new URL(IMAGE_EDIT_URL).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(15_000);
            connection.setReadTimeout(IMAGE_READ_TIMEOUT_MS);
            connection.setRequestProperty("Authorization", "Bearer " + apiKey.trim());
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setRequestProperty("Accept", "application/json");
            String responseBody;
            try {
                try (OutputStream outputStream = connection.getOutputStream()) {
                    outputStream.write(body);
                }
                int responseCode = connection.getResponseCode();
                responseBody = readBody(connection, responseCode);
                if (responseCode < 200 || responseCode >= 300) {
                    throw new IOException("HTTP " + responseCode + ": " + responseBody);
                }
            } finally {
                connection.disconnect();
            }
            JsonNode data = objectMapper.readTree(responseBody).path("data").path(0).path("b64_json");
            if (data.isMissingNode() || data.isNull() || data.asText().trim().isEmpty()) {
                throw new IOException("пустой ответ: data[0].b64_json отсутствует");
            }
            return java.util.Base64.getDecoder().decode(data.asText().trim());
        } catch (IOException e) {
            log.error("OpenAI image edit API request failed: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка запроса к OpenAI image edit API: " + e.getMessage(), e);
        }
    }

    private byte[] buildMultipartBody(String boundary, String prompt, String model,
                                      String mimeType, byte[] sourceImage) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String crlf = "\r\n";
        writePart(out, boundary, crlf, "prompt", prompt);
        writePart(out, boundary, crlf, "model", model);
        out.write(("--" + boundary + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"image\"; filename=\"logo\"" + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + mimeType + crlf + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(sourceImage);
        out.write(crlf.getBytes(StandardCharsets.UTF_8));
        out.write(("--" + boundary + "--" + crlf).getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private void writePart(ByteArrayOutputStream out, String boundary, String crlf,
                           String name, String value) throws IOException {
        out.write(("--" + boundary + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"" + crlf + crlf).getBytes(StandardCharsets.UTF_8));
        out.write(value.getBytes(StandardCharsets.UTF_8));
        out.write(crlf.getBytes(StandardCharsets.UTF_8));
    }

    private String readBody(HttpURLConnection connection, int responseCode) throws IOException {
        InputStream stream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
        if (stream == null) {
            return "";
        }
        try (InputStream in = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int read;
            while ((read = in.read(chunk)) != -1) {
                buffer.write(chunk, 0, read);
            }
            return buffer.toString(StandardCharsets.UTF_8.name());
        }
    }
}

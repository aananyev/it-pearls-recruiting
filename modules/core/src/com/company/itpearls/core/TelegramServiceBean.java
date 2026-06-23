package com.company.itpearls.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service(TelegramService.NAME)
public class TelegramServiceBean implements TelegramService {

    private static final Logger log = LoggerFactory.getLogger(TelegramServiceBean.class);

    @Inject
    private ApplicationSetupService applicationSetupService;
    @Inject
    private TextManipulationService textManipulationService;

    @Override
    public boolean sendMessageToChat(String tgToken, int chatId, String txt) {
        return sendMessageToChatResult(tgToken, String.valueOf(chatId), txt).isSuccess();
    }

    @Override
    public boolean sendMessageToChat(int chatId, String txt) {
        return sendMessageToChatResult(applicationSetupService.getTelegramToken(), String.valueOf(chatId), txt).isSuccess();
    }

    @Override
    public boolean sendMessageToChat(String chatId, String txt) {
        return sendMessageToChatResult(applicationSetupService.getTelegramToken(), chatId, txt).isSuccess();
    }

    @Override
    public boolean sendMessageToChat(String tgToken, String chatId, String txt) {
        return sendMessageToChatResult(tgToken, chatId, txt).isSuccess();
    }

    @Override
    public TelegramSendResult sendMessageToChatResult(String chatId, String txt) {
        return sendMessageToChatResult(applicationSetupService.getTelegramToken(), chatId, txt);
    }

    @Override
    public TelegramSendResult sendMessageToChatResult(String tgToken, String chatId, String txt) {
        if (!isTelegramEnabled()) {
            log.debug("Telegram send skipped: bot disabled in ApplicationSetup");
            return TelegramSendResult.failure("бот Telegram отключён в настройках");
        }
        if (!isConfigured(tgToken)) {
            log.warn("Telegram send skipped: token not configured");
            return TelegramSendResult.failure("не задан токен бота");
        }
        if (!isConfigured(chatId)) {
            log.warn("Telegram send skipped: chat_id not configured");
            return TelegramSendResult.failure("не задан chat_id");
        }
        if (!isConfigured(txt)) {
            log.debug("Telegram send skipped: empty message");
            return TelegramSendResult.failure("пустое сообщение");
        }

        String normalizedChatId = chatId.trim();
        String messageText = textManipulationService.formattedHtml2text(txt);
        if (!isConfigured(messageText)) {
            log.debug("Telegram send skipped: empty message after formatting");
            return TelegramSendResult.failure("пустое сообщение после форматирования");
        }

        String urlParameters = "chat_id=" + normalizedChatId + "&text=" + messageText;
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
        HttpURLConnection connection = null;

        try {
            URL url = new URL("https://api.telegram.org/bot" + tgToken.trim() + "/sendMessage");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "Java upread.ru client");
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.write(postData);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                if (log.isDebugEnabled()) {
                    log.debug("Telegram message sent to chat_id={}", normalizedChatId);
                }
                return TelegramSendResult.success();
            }

            String apiError = readResponseBody(connection, responseCode);
            String failureReason = formatHttpFailureReason(responseCode, apiError);
            log.warn("Telegram send failed for chat_id={}: {}", normalizedChatId, failureReason);
            return TelegramSendResult.failure(failureReason);
        } catch (ProtocolException e) {
            String failureReason = "ошибка протокола HTTP: " + sanitizeErrorMessage(e.getMessage());
            log.warn("Telegram send failed for chat_id={}: {}", normalizedChatId, failureReason);
            return TelegramSendResult.failure(failureReason);
        } catch (MalformedURLException e) {
            log.warn("Telegram send failed: invalid API URL");
            return TelegramSendResult.failure("некорректный URL API Telegram");
        } catch (IOException e) {
            String failureReason = "ошибка сети: " + sanitizeErrorMessage(e.getMessage());
            log.warn("Telegram send failed for chat_id={}: {}", normalizedChatId, failureReason);
            return TelegramSendResult.failure(failureReason);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String formatHttpFailureReason(int responseCode, String apiError) {
        if (isConfigured(apiError)) {
            return "ошибка API Telegram (HTTP " + responseCode + "): " + apiError;
        }
        return "ошибка API Telegram (HTTP " + responseCode + ")";
    }

    private String readResponseBody(HttpURLConnection connection, int responseCode) {
        try {
            InputStream stream = responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (stream == null) {
                return null;
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String body = reader.lines().collect(Collectors.joining());
                return extractTelegramApiError(body);
            }
        } catch (IOException e) {
            log.debug("Unable to read Telegram API response body: {}", e.getMessage());
            return null;
        }
    }

    private String extractTelegramApiError(String responseBody) {
        if (!isConfigured(responseBody)) {
            return null;
        }
        String sanitized = sanitizeErrorMessage(responseBody);
        int descriptionIndex = sanitized.indexOf("\"description\"");
        if (descriptionIndex < 0) {
            return sanitized.length() > 200 ? sanitized.substring(0, 200) + "..." : sanitized;
        }
        int colonIndex = sanitized.indexOf(':', descriptionIndex);
        if (colonIndex < 0) {
            return sanitized;
        }
        int startQuote = sanitized.indexOf('"', colonIndex + 1);
        if (startQuote < 0) {
            return sanitized;
        }
        int endQuote = sanitized.indexOf('"', startQuote + 1);
        if (endQuote <= startQuote) {
            return sanitized;
        }
        return sanitized.substring(startQuote + 1, endQuote);
    }

    private String sanitizeErrorMessage(String message) {
        if (message == null) {
            return "неизвестная ошибка";
        }
        return message.replaceAll("bot\\d+:[A-Za-z0-9_-]+", "[скрыто]");
    }

    private boolean isTelegramEnabled() {
        Boolean start = applicationSetupService.getTelegramBotStart();
        return start != null && start;
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

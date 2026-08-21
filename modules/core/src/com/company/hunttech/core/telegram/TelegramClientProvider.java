package com.company.hunttech.core.telegram;

import com.company.hunttech.config.HunttechTelegramConfig;
import com.company.hunttech.core.ApplicationSetupService;
import com.haulmont.cuba.core.global.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMemberCount;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Провайдер и HTTP-клиент Telegram Bot API.
 * Инкапсулирует вызовы API Telegram без необходимости запуска long polling процесса.
 */
@Component("hunttech_TelegramClientProvider")
public class TelegramClientProvider {
    private static final Logger log = LoggerFactory.getLogger(TelegramClientProvider.class);

    @Inject
    private Configuration configuration;

    @Inject
    private ApplicationSetupService applicationSetupService;

    private volatile DefaultAbsSender cachedSender;
    private volatile String cachedToken;

    private static class TelegramSenderClient extends DefaultAbsSender {
        protected TelegramSenderClient(DefaultBotOptions options, String botToken) {
            super(options, botToken);
        }
    }

    /**
     * Проверяет, задан ли токен и включен ли Telegram в настройках.
     */
    public boolean isConfigured() {
        String token = resolveBotToken();
        return token != null && !token.trim().isEmpty() && isTelegramBotStartAllowed();
    }

    /**
     * Получает токен Telegram бота.
     */
    public String resolveBotToken() {
        HunttechTelegramConfig config = configuration.getConfig(HunttechTelegramConfig.class);
        String token = config.getBotToken();
        if (isNotBlank(token)) {
            return token.trim();
        }
        token = applicationSetupService.getTelegramToken();
        return isNotBlank(token) ? token.trim() : null;
    }

    /**
     * Получает имя Telegram бота.
     */
    public String resolveBotName() {
        HunttechTelegramConfig config = configuration.getConfig(HunttechTelegramConfig.class);
        String name = config.getBotName();
        if (isNotBlank(name)) {
            return name.trim();
        }
        name = applicationSetupService.getTelegramBotName();
        return isNotBlank(name) ? name.trim() : "HuntTechBot";
    }

    /**
     * Создает или возвращает закэшированный экземпляр sender клиента для текущего токена.
     */
    public synchronized DefaultAbsSender getSender() throws TelegramApiException {
        if (!isTelegramBotStartAllowed()) {
            throw new TelegramApiException("Telegram bot is disabled in application configuration (TELEGRAM_BOT_START=false)");
        }
        String token = resolveBotToken();
        if (!isNotBlank(token)) {
            throw new TelegramApiException("Telegram bot token is not configured");
        }
        if (cachedSender == null || !token.equals(cachedToken)) {
            DefaultBotOptions botOptions = new DefaultBotOptions();
            cachedSender = new TelegramSenderClient(botOptions, token);
            cachedToken = token;
        }
        return cachedSender;
    }

    /**
     * Универсальный вызов метода Telegram Bot API.
     */
    public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) throws TelegramApiException {
        DefaultAbsSender sender = getSender();
        return sender.execute(method);
    }

    /**
     * Отправка текстового сообщения.
     */
    public Message executeSendMessage(SendMessage sendMessage) throws TelegramApiException {
        DefaultAbsSender sender = getSender();
        return sender.execute(sendMessage);
    }

    /**
     * Отправка фото.
     */
    public Message executeSendPhoto(SendPhoto sendPhoto) throws TelegramApiException {
        DefaultAbsSender sender = getSender();
        return sender.execute(sendPhoto);
    }

    /**
     * Получение информации о чате, канале или супергруппе.
     */
    public Chat getChat(String chatId) throws TelegramApiException {
        GetChat getChat = new GetChat();
        getChat.setChatId(chatId);
        return execute(getChat);
    }

    /**
     * Получение количества участников в чате/канале.
     */
    public Integer getChatMemberCount(String chatId) {
        try {
            GetChatMemberCount getCount = new GetChatMemberCount();
            getCount.setChatId(chatId);
            return execute(getCount);
        } catch (Exception e) {
            log.debug("Could not get chat member count for chatId={}: {}", chatId, e.getMessage());
            return null;
        }
    }

    /**
     * Получение фотографий профиля пользователя.
     */
    public UserProfilePhotos getUserProfilePhotos(Long userId, Integer offset, Integer limit) throws TelegramApiException {
        GetUserProfilePhotos getPhotos = new GetUserProfilePhotos();
        getPhotos.setUserId(userId);
        if (offset != null) {
            getPhotos.setOffset(offset);
        }
        if (limit != null) {
            getPhotos.setLimit(limit);
        }
        return execute(getPhotos);
    }

    /**
     * Получение информации о файле (включая относительный file_path).
     */
    public File getFile(String fileId) throws TelegramApiException {
        GetFile getFile = new GetFile();
        getFile.setFileId(fileId);
        return execute(getFile);
    }

    private static final long MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024; // 20 MB max

    /**
     * Скачивание файла по его file_path в виде массива байтов.
     */
    public byte[] downloadFileBytes(String filePath) throws IOException {
        String token = resolveBotToken();
        if (!isNotBlank(token)) {
            throw new IllegalStateException("Telegram bot token is not configured");
        }
        if (!isNotBlank(filePath)) {
            throw new IllegalArgumentException("File path cannot be empty");
        }

        String fileUrlStr = "https://api.telegram.org/file/bot" + token + "/" + filePath;
        URL url = new URL(fileUrlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        try {
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new IOException("Telegram file download failed with HTTP " + responseCode);
            }

            long contentLength = conn.getContentLengthLong();
            if (contentLength > MAX_FILE_SIZE_BYTES) {
                throw new IOException("Telegram file too large: " + contentLength + " bytes (max allowed " + MAX_FILE_SIZE_BYTES + ")");
            }

            try (InputStream is = conn.getInputStream();
                 ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int n;
                long totalRead = 0;
                while ((n = is.read(buffer)) != -1) {
                    totalRead += n;
                    if (totalRead > MAX_FILE_SIZE_BYTES) {
                        throw new IOException("Telegram file stream exceeded max limit of " + MAX_FILE_SIZE_BYTES + " bytes");
                    }
                    os.write(buffer, 0, n);
                }
                return os.toByteArray();
            }
        } finally {
            conn.disconnect();
        }
    }

    private boolean isTelegramBotStartAllowed() {
        Boolean start = applicationSetupService.getTelegramBotStart();
        return start != null && start;
    }

    private boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }
}


package com.company.hunttech.core.telegrambot.telegram;

import com.company.hunttech.core.AccountingDocumentIngestResult;
import com.company.hunttech.core.AccountingDocumentIngestService;
import com.company.hunttech.core.telegrambot.telegram.commands.service.*;
import com.haulmont.cuba.core.global.AppBeans;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.company.hunttech.core.telegrambot.Utils;
import com.company.hunttech.core.telegrambot.telegram.nonCommand.NonCommand;
import com.company.hunttech.core.telegrambot.telegram.nonCommand.Settings;
import com.company.hunttech.core.telegrambot.telegram.commands.operations.*;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Собственно, бот
 */
public final class Bot extends TelegramLongPollingCommandBot {
    private Logger logger = LoggerFactory.getLogger(Bot.class);

    private final String BOT_NAME;
    private static final Settings defaultSettings = new Settings(3, true);
    private final NonCommand nonCommand;
    private Update update;

    public static Settings getDefaultSettings() {
        return new Settings(3, true);
    }

    /**
     * Настройки файла для разных пользователей. Ключ - уникальный id чата
     */
    private static Map<Long, Settings> userSettings;

    public Bot(String botName, String botToken) {
        super(botToken);
        // ОБНОВЛЕНИЕ: токен передаётся в современный constructor TelegramLongPollingCommandBot; override getBotToken() в 6.8 помечен deprecated.
        logger.debug("Конструктор суперкласса отработал");
        this.BOT_NAME = botName;
        logger.debug("Имя и токен присвоены");

        this.nonCommand = new NonCommand();
        logger.debug("Класс обработки сообщения, не являющегося командой, создан");

        register(new VacancyListCommand("allvacancy", "Все вакансии"));
        logger.debug("Команда allvacancy создана");

        register(new SubscribeCommand("subscribe", "Подписки на вакансии"));
        logger.debug("Команда subscribe создана");

        register(new UserSessionTg("usersession", "Пользователи в системе"));
        logger.debug("Команда usersession создана");

        register(new StartCommand("start", "Старт"));
        logger.debug("Команда start создана");

        register(new HelpCommand("help","Помощь"));
        logger.debug("Команда help создана");

        register(new SettingsCommand("settings", "Мои настройки"));
        logger.debug("Команда settings создана");

        userSettings = new HashMap<>();
        logger.info("Бот создан!");

/*        try {
            setAnswer(getMe().getId(), getBotUsername(), getHelloMessage());
        } catch (TelegramApiException e) {
            logger.debug(String.format("Ошибка создания бота **%s**.", getBotUsername()));
        } */
    }

    public static Map<Long, Settings> getUserSettings() {
        return userSettings;
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    /**
     * Ответ на запрос, не являющийся командой
     */
    @Override
    public void processNonCommandUpdate(Update update) {
        Message msg = update.getMessage();
        if (msg == null) {
            return;
        }
        Long chatId = msg.getChatId();
        String userName = Utils.getUserName(msg);

        if (isAccountingDocumentUpload(msg)) {
            String answer = processAccountingDocumentUpload(msg);
            setAnswer(chatId, userName, answer);
            return;
        }

        String answer = nonCommand.nonCommandExecute(chatId, userName, msg.getText());
        setAnswer(chatId, userName, answer);
    }

    /**
     * Получение настроек по id чата. Если ранее для этого чата в ходе сеанса работы бота настройки не были установлены,
     * используются настройки по умолчанию
     */
    public static Settings getUserSettings(Long chatId) {
        Map<Long, Settings> userSettings = Bot.getUserSettings();
        Settings settings = userSettings.get(chatId);
        if (settings == null) {
            return defaultSettings;
        }
        return settings;
    }

    /**
     * Отправка ответа
     * @param chatId id чата
     * @param userName имя пользователя
     * @param text текст ответа
     */
    private void setAnswer(Long chatId, String userName, String text) {
        // ОБНОВЛЕНИЕ: устаревшее ручное создание SendMessage через new/setters заменено на builder telegrambots 6.8.
        SendMessage answer = SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .parseMode(ParseMode.HTML)
                .build();

        Utils.setButtons(answer);

        try {
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error(String.format("*ОШИБКА* %s.\nСообщение %s, не являющееся командой.\nПользователь: **%s**",
                    e.getMessage(), userName));
            e.printStackTrace();
        }
    }

    private boolean isAccountingDocumentUpload(Message msg) {
        return msg.hasDocument() || msg.hasPhoto();
    }

    private String processAccountingDocumentUpload(Message msg) {
        try {
            TelegramIncomingFile incomingFile = resolveIncomingFile(msg);
            File downloadedFile = downloadFile(incomingFile.fileId);
            AccountingDocumentIngestService ingestService = AppBeans.get(AccountingDocumentIngestService.class);
            AccountingDocumentIngestResult result = ingestService.ingestTelegramFile(
                    downloadedFile.getAbsolutePath(),
                    incomingFile.originalFileName,
                    incomingFile.mimeType,
                    incomingFile.fileSize,
                    msg.getChatId().toString(),
                    msg.getMessageId().longValue(),
                    incomingFile.fileId,
                    resolveTelegramUserId(msg),
                    msg.getCaption()
            );
            return result.getMessage();
        } catch (TelegramApiException e) {
            logger.warn("Не удалось скачать бухгалтерский документ из Telegram: {}", e.getMessage(), e);
            return "Документ не принят: Telegram не отдал файл для загрузки.";
        } catch (Exception e) {
            logger.warn("Не удалось принять бухгалтерский документ из Telegram: {}", e.getMessage(), e);
            return "Документ не принят: внутренняя ошибка HRM HuntTech.";
        }
    }

    private TelegramIncomingFile resolveIncomingFile(Message msg) {
        if (msg.hasDocument()) {
            Document document = msg.getDocument();
            return new TelegramIncomingFile(document.getFileId(), document.getFileName(),
                    document.getMimeType(), document.getFileSize());
        }

        PhotoSize photo = findLargestPhoto(msg.getPhoto());
        Long fileSize = photo.getFileSize() == null ? null : photo.getFileSize().longValue();
        return new TelegramIncomingFile(photo.getFileId(), "telegram-photo-" + msg.getMessageId() + ".jpg",
                "image/jpeg", fileSize);
    }

    private PhotoSize findLargestPhoto(List<PhotoSize> photos) {
        return photos.stream()
                .max(Comparator.comparingInt(photo -> photo.getFileSize() == null ? 0 : photo.getFileSize()))
                .orElseThrow(() -> new IllegalArgumentException("Telegram не передал варианты фото"));
    }

    private String resolveTelegramUserId(Message msg) {
        return msg.getFrom() == null || msg.getFrom().getId() == null
                ? null
                : msg.getFrom().getId().toString();
    }

    private static class TelegramIncomingFile {
        private final String fileId;
        private final String originalFileName;
        private final String mimeType;
        private final Long fileSize;

        private TelegramIncomingFile(String fileId, String originalFileName, String mimeType, Long fileSize) {
            this.fileId = fileId;
            this.originalFileName = originalFileName;
            this.mimeType = mimeType;
            this.fileSize = fileSize;
        }
    }
}

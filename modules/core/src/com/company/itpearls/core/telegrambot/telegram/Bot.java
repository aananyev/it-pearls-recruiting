
package com.company.itpearls.core.telegrambot.telegram;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import com.company.itpearls.core.telegrambot.Utils;
import com.company.itpearls.core.telegrambot.telegram.nonCommand.NonCommand;
import com.company.itpearls.core.telegrambot.telegram.nonCommand.Settings;
import com.company.itpearls.core.telegrambot.telegram.commands.service.StartCommand;
import com.company.itpearls.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.company.itpearls.core.telegrambot.telegram.commands.service.HelpCommand;
//import com.company.itpearls.core.telegrambot.telegram.commands.operations.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Собственно, бот
 */
public final class Bot extends TelegramLongPollingCommandBot {
    private Logger logger = LoggerFactory.getLogger(Bot.class);

    private final String BOT_NAME;
    private final String BOT_TOKEN;

//    @Getter
//    private static final Settings defaultSettings = new Settings();
    private final NonCommand nonCommand;

    /**
     * Настройки файла для разных пользователей. Ключ - уникальный id чата
     */
    @Getter
    private static Map<Long, Settings> userSettings;

    public Bot(String botName, String botToken) {
        super();
        logger.debug("Конструктор суперкласса отработал");
        this.BOT_NAME = botName;
        this.BOT_TOKEN = botToken;
        logger.debug("Имя и токен присвоены");

        this.nonCommand = new NonCommand();
        logger.debug("Класс обработки сообщения, не являющегося командой, создан");

        register(new StartCommand("start", "Старт"));
        logger.debug("Команда start создана");

        register(new HelpCommand("help","Помощь"));
        logger.debug("Команда help создана");

        register(new SettingsCommand("settings", "Мои настройки"));
        logger.debug("Команда settings создана");

        userSettings = new HashMap<>();
        logger.info("Бот создан!");
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
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
        Long chatId = msg.getChatId();
        String userName = Utils.getUserName(msg);

        String answer = nonCommand.nonCommandExecute(chatId, userName, msg.getText());
        setAnswer(chatId, userName, answer);
    }

    /**
     * Получение настроек по id чата. Если ранее для этого чата в ходе сеанса работы бота настройки не были установлены,
     * используются настройки по умолчанию
     */
/*    public static Settings getUserSettings(Long chatId) {
        Map<Long, Settings> userSettings = Bot.getUserSettings();
        Settings settings = userSettings.get(chatId);
        if (settings == null) {
            return defaultSettings;
        }
        return settings;
    } */

    /**
     * Отправка ответа
     * @param chatId id чата
     * @param userName имя пользователя
     * @param text текст ответа
     */
    private void setAnswer(Long chatId, String userName, String text) {
        SendMessage answer = new SendMessage();
        answer.setText(text);
        answer.setChatId(chatId.toString());
        try {
            execute(answer);
        } catch (TelegramApiException e) {
            logger.error(String.format("Ошибка %s. Сообщение, не являющееся командой. Пользователь: %s", e.getMessage(),
                    userName));
            e.printStackTrace();
        }
    }
}
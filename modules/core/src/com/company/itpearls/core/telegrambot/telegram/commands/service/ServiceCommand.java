package com.company.itpearls.core.telegrambot.telegram.commands.service;


import com.company.itpearls.core.telegrambot.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Суперкласс для сервисных команд
 */
abstract class ServiceCommand extends BotCommand {
    private Logger logger = LoggerFactory.getLogger(ServiceCommand.class);

    ServiceCommand(String identifier, String description) {
        super(identifier, description);
    }

    /**
     * Отправка ответа пользователю
     */
    void sendAnswer(AbsSender absSender, Long chatId, String commandName, String userName, String text) {
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);
        message.enableHtml(true);
        message.setChatId(chatId.toString());
        message.setText(new StringBuilder()
                .append(text)
                .toString());
        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            logger.error(String.format("Ошибка %s. Команда %s. Пользователь: %s", e.getMessage(), commandName, userName));
            e.printStackTrace();
        }
    }

    void sendAnswer(AbsSender absSender, Long chatId, String commandName, String userName, String text, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.enableMarkdown(true);
        message.enableHtml(true);
        message.setChatId(chatId.toString());
        message.setText(new StringBuilder()
                .append(text)
                .toString());
        message.setReplyMarkup(keyboardMarkup);

        try {
            absSender.execute(message);
        } catch (TelegramApiException e) {
            logger.error(String.format("Ошибка %s. Команда %s. Пользователь: %s", e.getMessage(), commandName, userName));
            e.printStackTrace();
        }
    }
}
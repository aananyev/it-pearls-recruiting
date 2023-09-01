package com.company.itpearls.core;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

import java.io.File;

public class TelegramBot {
    private final org.telegram.telegrambots.meta.api.objects.Bot bot;

    public TelegramBot(String token) {
        bot = new org.telegram.telegrambots.meta.api.objects.Bot(token);
    }

    public void sendMessage(String chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        bot.execute(message);
    }

    public void sendPhoto(String chatId, File photo) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        InputFile inputFile = new InputFile(photo);
        message.setPhoto(inputFile);
        bot.execute(message);
    }

    public void sendReplyKeyboardMarkup(String chatId, String[] buttons) throws TelegramApiException {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        for (String button : buttons) {
            KeyboardButton keyboardButton = new KeyboardButton(button);
            replyKeyboardMarkup.addKeyboardButton(keyboardButton);
        }
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setReplyMarkup(replyKeyboardMarkup);
        bot.execute(message);
    }

    public void sendCallback(String chatId, String data) throws TelegramApiException {
        SentCallback sentCallback = new SentCallback();
        sentCallback.setCallbackQueryId(data);
        sentCallback.setChatId(chatId);
        bot.execute(sentCallback);
    }
}

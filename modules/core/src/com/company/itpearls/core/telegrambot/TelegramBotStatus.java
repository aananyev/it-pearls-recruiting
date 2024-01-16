package com.company.itpearls.core.telegrambot;

import com.company.itpearls.core.telegrambot.telegram.Bot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.generics.BotSession;

import java.io.Serializable;

public class TelegramBotStatus implements Serializable {
    private static TelegramBotsApi botsApi = null;
    private static Boolean botStarted = false;
    private static Bot bot = null;
    private final static String defaultBotToken = "6433663497:AAGvl9NRSddfNC78PQ0JUzXywWIuO5EsCd8";
    private final static String defaultBotName = "ITPearlsTestBot";
    private static BotSession botSession = null;

    public static TelegramBotsApi getBotsApi() {
        return TelegramBotStatus.botsApi;
    }

    public static void setBotsApi(TelegramBotsApi botsApi) {
        TelegramBotStatus.botsApi = botsApi;
    }

    public static Boolean getBotStarted() {
        return TelegramBotStatus.botStarted;
    }

    public static void setBotStarted(Boolean botStarted) {
        TelegramBotStatus.botStarted = botStarted;
    }

    public static String getDefaultBotName() {
        return TelegramBotStatus.defaultBotName;
    }

    public static String getDefaultBotToken() {
        return TelegramBotStatus.defaultBotToken;
    }

    public static Bot getBot() {
        return TelegramBotStatus.bot;
    }

    public static void setBot(Bot bot) {
        TelegramBotStatus.bot = bot;
    }

    public static void setBotSession(BotSession session) {
        botSession = session;
    }

    public static BotSession getBotSession() {
        return botSession;
    }
}

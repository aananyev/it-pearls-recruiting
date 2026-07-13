package com.company.hunttech.core;

import org.telegram.telegrambots.meta.TelegramBotsApi;

public interface TelegramBotService {
    String NAME = "hunttech_TelegramBotService";

    void saveTelegramBotApi(TelegramBotsApi botsApi);

    TelegramBotsApi restoreTelegramBotApi();

    void setBotStarted();

    void setBotStopped();

    Boolean isBotStarted();

    String telegramBotStart();

    String telegramBotRestart();

    String telegramBotStop();
}

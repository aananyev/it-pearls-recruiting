package com.company.itpearls.core;

import org.telegram.telegrambots.meta.TelegramBotsApi;

public interface TelegramBotService {
    String NAME = "itpearls_TelegramBotService";

    void saveTelegramBotApi(TelegramBotsApi botsApi);

    TelegramBotsApi restoreTelegramBotApi();

    void setBotStarted();

    void setBotStopped();

    Boolean isBotStarted();

    void telegramBotRestart();
}

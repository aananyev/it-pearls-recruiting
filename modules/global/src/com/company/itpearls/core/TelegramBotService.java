package com.company.itpearls.core;

import com.company.itpearls.entity.ApplicationSetup;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.generics.BotSession;

public interface TelegramBotService {
    String NAME = "itpearls_TelegramBotService";

    void saveTelegramBotApi(TelegramBotsApi botsApi);

    TelegramBotsApi restoreTelegramBotApi();

    void setBotStarted();

    void setBotStopped();

    Boolean isBotStarted();

    void telegramBotRestart();

    void telegramBotStop();

    void telegramBotStart(ApplicationSetup applicationSetup);

    ApplicationSetup restoreApplicationSetup();

    ApplicationSetup getApplicationSetup();

    void saveApplicationSetup(ApplicationSetup activeApplicationSetup);

    void telegramBotStart();

    void saveBotSession(BotSession session);

    BotSession restoreBotSession();

    void setApplicationSetup(ApplicationSetup activeApplicationSetup);

    String getBotName();
}

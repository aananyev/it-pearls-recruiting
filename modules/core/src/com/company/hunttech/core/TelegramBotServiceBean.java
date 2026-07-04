package com.company.hunttech.core;

import com.company.hunttech.core.telegrambot.TelegramBotStatus;
import com.company.hunttech.core.telegrambot.telegram.Bot;
import com.company.hunttech.config.HunttechTelegramConfig;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import javax.inject.Inject;
import java.io.Serializable;

@Service(TelegramBotService.NAME)
public class TelegramBotServiceBean implements TelegramBotService, Serializable {
    private Logger logger = LoggerFactory.getLogger(TelegramBotsApi.class);
    @Inject
    private ApplicationSetupService applicationSetupService;
    @Inject
    private Messages messages;
    @Inject
    private Configuration configuration;

    @Override
    public void saveTelegramBotApi(TelegramBotsApi botsApi) {
        TelegramBotStatus.setBotsApi(botsApi);
    }

    @Override
    public TelegramBotsApi restoreTelegramBotApi() {
        return TelegramBotStatus.getBotsApi();
    }

    @Override
    public void setBotStarted() {
        TelegramBotStatus.setBotStarted(true);
    }

    @Override
    public void setBotStopped() {
        TelegramBotStatus.setBotStarted(false);
    }

    @Override
    public Boolean isBotStarted() {
        return TelegramBotStatus.getBotStarted();
    }


    @Override
    public void telegramBotRestart() {
        // ОБНОВЛЕНИЕ: рестарт бота использует CUBA Config из app.properties, а не значения по умолчанию из статуса.
        String NAME = resolveTelegramBotName();
        String TOKEN = resolveTelegramBotToken();
        TelegramBotsApi botsApi = restoreTelegramBotApi();

        if (!isBotStarted()) {
            if (applicationSetupService.getTelegramBotStart() != null
                    ? applicationSetupService.getTelegramBotStart() : false) {
                try {
                    Bot bot = new Bot(NAME, TOKEN);
                    botsApi.registerBot(bot);

                    logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotInitialised"),
                            applicationSetupService.getTelegramBotName()));

                    setBotStarted();
                } catch (TelegramApiException e) {
                    logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotNotInitialised")));
                    setBotStopped();
                }
            }
        }
    }

    private String resolveTelegramBotName() {
        HunttechTelegramConfig telegramConfig = configuration.getConfig(HunttechTelegramConfig.class);
        return isConfigured(telegramConfig.getBotName())
                ? telegramConfig.getBotName()
                : applicationSetupService.getTelegramBotName();
    }

    private String resolveTelegramBotToken() {
        HunttechTelegramConfig telegramConfig = configuration.getConfig(HunttechTelegramConfig.class);
        return isConfigured(telegramConfig.getBotToken())
                ? telegramConfig.getBotToken()
                : applicationSetupService.getTelegramToken();
    }

    private boolean isConfigured(String value) {
        return value != null && !value.trim().isEmpty();
    }
}

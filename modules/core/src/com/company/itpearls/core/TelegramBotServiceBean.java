package com.company.itpearls.core;

import com.company.itpearls.core.telegrambot.TelegramBotStatus;
import com.company.itpearls.core.telegrambot.telegram.Bot;
import com.company.itpearls.entity.ApplicationSetup;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.security.app.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.BotSession;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

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
    private Authentication authentication;
    @Inject
    private TelegramBotService telegramBotService;

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
        String NAME = TelegramBotStatus.getDefaultBotName();
        String TOKEN = TelegramBotStatus.getDefaultBotToken();
        TelegramBotsApi botsApi = restoreTelegramBotApi();

        if (!isBotStarted()) {
            if (applicationSetupService.getTelegramBotStart() != null
                    ? applicationSetupService.getTelegramBotStart() : false) {
                if (applicationSetupService.getTelegramBotName() != null
                        && applicationSetupService.getTelegramToken() != null) {
                    NAME = applicationSetupService.getTelegramBotName();
                    TOKEN = applicationSetupService.getTelegramToken();
                }
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
        } else {
            telegramBotStart();
        }
    }

    @Override
    public void telegramBotStop() {
        if (telegramBotService.isBotStarted()) {
            telegramBotService.restoreBotSession().stop();
            telegramBotService.setBotStopped();
        }
    }

    @Override
    public void telegramBotStart(ApplicationSetup applicationSetup) {
        authentication.begin();

        String NAME = applicationSetup.getTelegramBotName();
        String TOKEN = applicationSetup.getTelegramToken();

        //инициализируйте конфигурацию здесь
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            Bot bot = new Bot(NAME, TOKEN);

            BotSession session = botsApi.registerBot(bot);
            logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotInitialised"),
                    applicationSetupService.getTelegramBotName()));

            telegramBotService.setBotStarted();

            telegramBotService.saveTelegramBotApi(botsApi);
            telegramBotService.saveBotSession(session);
            telegramBotService.saveApplicationSetup(applicationSetup);

            TelegramBotStatus.setBot(bot);
        } catch (TelegramApiException e) {
            logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotNotInitialised")));
            telegramBotService.setBotStopped();
            telegramBotService.saveBotSession(null);
            telegramBotService.saveApplicationSetup(null);
        } finally {
            authentication.end();
        }
    }

    @Override
    public ApplicationSetup restoreApplicationSetup() {
        return TelegramBotStatus.getApplicationSetup();
    }

    @Override
    public ApplicationSetup getApplicationSetup() {
        return TelegramBotStatus.getApplicationSetup();
    }

    @Override
    public void setApplicationSetup(ApplicationSetup applicationSetup) {
        TelegramBotStatus.setApplicationSetup(applicationSetup);
    }

    @Override
    public void saveApplicationSetup(ApplicationSetup applicationSetup) {
        TelegramBotStatus.setApplicationSetup(applicationSetup);
    }

    @Override
    public void telegramBotStart() {
        authentication.begin();

        String NAME = applicationSetupService.getTelegramBotName();
        String TOKEN = applicationSetupService.getTelegramToken();

        if (NAME == null || TOKEN == null) {
            NAME = new StringBuilder(TelegramBotStatus.getDefaultBotName()).toString();
            TOKEN = new StringBuilder(TelegramBotStatus.getDefaultBotToken()).toString();
        }
        //инициализируйте конфигурацию здесь
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            Bot bot = new Bot(NAME, TOKEN);

            BotSession session = botsApi.registerBot(bot);
            logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotInitialised"),
                    applicationSetupService.getTelegramBotName()));

            telegramBotService.setBotStarted();

            telegramBotService.saveTelegramBotApi(botsApi);
            telegramBotService.saveBotSession(session);
            telegramBotService.saveApplicationSetup(applicationSetupService.getActiveApplicationSetup());

            TelegramBotStatus.setBot(bot);
        } catch (TelegramApiException e) {
            logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotNotInitialised")));
            telegramBotService.setBotStopped();
            telegramBotService.saveBotSession(null);
            telegramBotService.saveApplicationSetup(null);
        } finally {
            authentication.end();
        }
    }

    @Override
    public void saveBotSession(BotSession session) {
        TelegramBotStatus.setBotSession(session);
    }

    @Override
    public BotSession restoreBotSession() {
        return TelegramBotStatus.getBotSession();
    }
}
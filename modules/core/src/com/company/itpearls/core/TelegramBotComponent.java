package com.company.itpearls.core;

import com.company.itpearls.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.security.app.Authentication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

@Component
public class TelegramBotComponent {
    //    private static final Map<String, String> getenv = System.getenv();
    private Logger logger = LoggerFactory.getLogger(SettingsCommand.class);
    @Inject
    private Messages messages;
    @Inject
    protected Authentication authentication;
    @Inject
    private TelegramBotService telegramBotService;

    @PostConstruct
    protected void init() throws IOException { /*
        authentication.begin();
        try {
//            telegramBotService.telegramBotRestart();
        } finally {
            authentication.end();
        } */
/*        String NAME = BOT_NAME;
        String TOKEN = BOT_TOKEN;
        authentication.begin();
        //инициализируйте конфигурацию здесь
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotService.saveTelegramBotApi(botsApi);

            if (applicationSetupService.getTelegramBotStart() != null
                    ? applicationSetupService.getTelegramBotStart() : false) {
                if (applicationSetupService.getTelegramBotName() != null
                        && applicationSetupService.getTelegramToken() != null) {
                    NAME = applicationSetupService.getTelegramBotName();
                    TOKEN = applicationSetupService.getTelegramToken();
                }

                Bot bot = new Bot(NAME, TOKEN);

                botsApi.registerBot(bot);
                logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotInitialised"),
                        applicationSetupService.getTelegramBotName()));

                telegramBotService.setBotStarted();
            } else {
                logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotNotStarted")));
                telegramBotService.setBotStopped();
            }
        } catch (TelegramApiException e) {
            logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotNotInitialised")));
            telegramBotService.setBotStopped();
        } finally {
            authentication.end();
        } */
    }

    @PreDestroy
    protected void closeSession() throws IOException {
        //де-инициализируйте конфигурацию здесь если есть такая необходимость
        //например закройте connection если таковой имеется
    }
}

package com.company.itpearls.core;

import com.company.itpearls.core.telegrambot.telegram.Bot;
import com.company.itpearls.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.haulmont.cuba.core.global.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

@Component
public class TelegramBotComponent {
    //    private static final Map<String, String> getenv = System.getenv();
    private final static String BOT_TOKEN = "6433663497:AAGvl9NRSddfNC78PQ0JUzXywWIuO5EsCd8";
    private final static String BOT_NAME = "ITPearlsTestBot";
    @Inject
    private ApplicationSetupService applicationSetupService;
    private Logger logger = LoggerFactory.getLogger(SettingsCommand.class);
    @Inject
    private Messages messages;


    @PostConstruct
    protected void init() throws IOException {
        String NAME = BOT_NAME;
        String TOKEN = BOT_TOKEN;
        //инициализируйте конфигурацию здесь
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);

            if (applicationSetupService.getTelegramBotName() != null
                    && applicationSetupService.getTelegramToken() != null) {
                NAME = applicationSetupService.getTelegramBotName();
                TOKEN = applicationSetupService.getTelegramToken();
            }

            botsApi.registerBot(new Bot(NAME, TOKEN));
            logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotInitialised"),
                    applicationSetupService.getTelegramBotName()));
        } catch (TelegramApiException e) {
            logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotNotInitialised")));
        }
    }

    @PreDestroy
    protected void closeSession() throws IOException {
        //де-инициализируйте конфигурацию здесь если есть такая необходимость
        //например закройте connection если таковой имеется
    }
}

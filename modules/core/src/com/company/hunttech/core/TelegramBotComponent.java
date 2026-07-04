package com.company.hunttech.core;

import com.company.hunttech.core.telegrambot.TelegramBotStatus;
import com.company.hunttech.core.telegrambot.telegram.Bot;
import com.company.hunttech.core.telegrambot.telegram.commands.service.SettingsCommand;
import com.company.hunttech.config.HunttechTelegramConfig;
import com.haulmont.cuba.core.global.Configuration;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.security.app.Authentication;
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
    @Inject
    private ApplicationSetupService applicationSetupService;
    @Inject
    private Configuration configuration;

    @PostConstruct
    protected void init() throws IOException {
        authentication.begin();

        // ОБНОВЛЕНИЕ: имя и токен берутся из app.properties через CUBA Config, ApplicationSetup остаётся fallback для существующих инсталляций.
        String NAME = resolveTelegramBotName();
        String TOKEN = resolveTelegramBotToken();
        //инициализируйте конфигурацию здесь
        try {
            // ОБНОВЛЕНИЕ: TelegramBotsApi создаётся с DefaultBotSession.class, как требует telegrambots 6.8+.
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotService.saveTelegramBotApi(botsApi);

            if (applicationSetupService.getTelegramBotStart() != null
                    ? applicationSetupService.getTelegramBotStart() : false) {
                Bot bot = new Bot(NAME, TOKEN);

                botsApi.registerBot(bot);
                logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotInitialised"),
                        applicationSetupService.getTelegramBotName()));

                telegramBotService.setBotStarted();
                TelegramBotStatus.setBot(bot);
            } else {
                logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotNotStarted")));
                telegramBotService.setBotStopped();
            }
        } catch (TelegramApiException e) {
            logger.debug(String.format(messages.getMainMessage("mainmsgTelegramBotNotInitialised")));
            telegramBotService.setBotStopped();
        } finally {
            authentication.end();
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

    @PreDestroy
    protected void closeSession() throws IOException {
        //де-инициализируйте конфигурацию здесь если есть такая необходимость
        //например закройте connection если таковой имеется
    }
}

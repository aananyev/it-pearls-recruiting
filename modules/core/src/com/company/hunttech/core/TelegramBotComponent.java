package com.company.hunttech.core;

import com.company.hunttech.core.telegrambot.telegram.commands.service.SettingsCommand;
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

    @PostConstruct
    protected void init() throws IOException {
        authentication.begin();

        //инициализируйте конфигурацию здесь
        try {
            // ОБНОВЛЕНИЕ: TelegramBotsApi создаётся с DefaultBotSession.class, как требует telegrambots 6.8+.
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotService.saveTelegramBotApi(botsApi);

            if (applicationSetupService.getTelegramBotStart() != null
                    ? applicationSetupService.getTelegramBotStart() : false) {
                // ОБНОВЛЕНИЕ: старт бота делегирован сервису, который сохраняет BotSession и возвращает статус выполнения.
                String startStatus = telegramBotService.telegramBotStart();
                logger.debug(startStatus);
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

    @PreDestroy
    protected void closeSession() throws IOException {
        //де-инициализируйте конфигурацию здесь если есть такая необходимость
        //например закройте connection если таковой имеется
    }
}

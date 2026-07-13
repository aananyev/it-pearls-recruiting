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
    public String telegramBotStart() {
        // ОБНОВЛЕНИЕ: запуск бота возвращает человекочитаемый статус для UI-уведомления.
        if (isBotStarted()) {
            return "Telegram-бот уже запущен";
        }
        if (!isTelegramBotStartAllowed()) {
            setBotStopped();
            return messages.getMainMessage("mainmsgTelegramBotNotStarted");
        }

        String NAME = resolveTelegramBotName();
        String TOKEN = resolveTelegramBotToken();
        if (!isConfigured(NAME) || !isConfigured(TOKEN)) {
            setBotStopped();
            return "Telegram-бот не запущен: не задано имя или токен";
        }

        try {
            TelegramBotsApi botsApi = getOrCreateBotsApi();
            Bot bot = new Bot(NAME, TOKEN);
            // ОБНОВЛЕНИЕ: сохраняем BotSession, чтобы команда остановки реально останавливала long polling.
            BotSession botSession = botsApi.registerBot(bot);

            TelegramBotStatus.setBot(bot);
            TelegramBotStatus.setBotSession(botSession);
            setBotStarted();

            String status = String.format(messages.getMainMessage("mainmsgTelegramBotInitialised"), NAME);
            logger.debug(status);
            return status;
        } catch (TelegramApiException e) {
            String status = messages.getMainMessage("mainmsgTelegramBotNotInitialised") + ": " + e.getMessage();
            logger.debug(status, e);
            setBotStopped();
            TelegramBotStatus.setBot(null);
            TelegramBotStatus.setBotSession(null);
            return status;
        }
    }

    @Override
    public String telegramBotRestart() {
        // ОБНОВЛЕНИЕ: перезапуск теперь выполняет реальный stop/start и возвращает статус для UI.
        String stopStatus = telegramBotStop();
        String startStatus = telegramBotStart();
        return stopStatus + ". " + startStatus;
    }

    @Override
    public String telegramBotStop() {
        BotSession botSession = TelegramBotStatus.getBotSession();
        if (botSession != null && botSession.isRunning()) {
            botSession.stop();
            setBotStopped();
            TelegramBotStatus.setBot(null);
            TelegramBotStatus.setBotSession(null);
            return "Telegram-бот остановлен";
        }

        setBotStopped();
        TelegramBotStatus.setBot(null);
        TelegramBotStatus.setBotSession(null);
        return "Telegram-бот уже остановлен";
    }

    private TelegramBotsApi getOrCreateBotsApi() throws TelegramApiException {
        TelegramBotsApi botsApi = restoreTelegramBotApi();
        if (botsApi == null) {
            // ОБНОВЛЕНИЕ: сервис сам создаёт TelegramBotsApi с DefaultBotSession.class, если UI-команда вызвана до PostConstruct.
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            saveTelegramBotApi(botsApi);
        }
        return botsApi;
    }

    private boolean isTelegramBotStartAllowed() {
        Boolean startAllowed = applicationSetupService.getTelegramBotStart();
        return startAllowed != null && startAllowed;
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

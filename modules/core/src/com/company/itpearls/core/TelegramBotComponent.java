package com.company.itpearls.core;

import com.company.itpearls.core.telegrambot.telegram.Bot;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.Map;

@Component
public class TelegramBotComponent {
    private static final Map<String, String> getenv = System.getenv();
    private final static String BOT_TOKEN = "6433663497:AAGvl9NRSddfNC78PQ0JUzXywWIuO5EsCd8";
    private final static String BOT_NAME = "ITPearlsTestBot";
    @PostConstruct
    protected void init() throws IOException {
        //инициализируйте конфигурацию здесь
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new Bot(BOT_NAME, BOT_TOKEN));
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    protected void closeSession() throws IOException {
        //де-инициализируйте конфигурацию здесь если есть такая необходимость
        //например закройте connection если таковой имеется
    }
}

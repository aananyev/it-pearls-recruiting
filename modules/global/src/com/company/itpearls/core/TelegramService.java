package com.company.itpearls.core;

public interface TelegramService {
    String NAME = "itpearls_TelegramService";

    void sendMessageToChat(String txt);

    void sendMessageToBot(String message);
    void sendMessageToBotWithSetting(String message);

    void sendMessageToBot(long chatId, String message);
    void sendMessageToBotWithSetting(long chatId, String message);

    void sendMessageToChat(String tgToken, int chatId, String txt);

    void sendMessageToChat(String chatId, String txt);

    void sendMessageToChat(String tgToken, String chatId, String txt);
}
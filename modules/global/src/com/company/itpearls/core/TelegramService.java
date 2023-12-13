package com.company.itpearls.core;

public interface TelegramService {
    String NAME = "itpearls_TelegramService";

    void sendMessageToChat(String tgToken, int chatId, String txt);

    void sendMessageToChat(String tgToken, String chatId, String txt);
}
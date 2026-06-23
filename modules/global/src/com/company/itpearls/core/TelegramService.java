package com.company.itpearls.core;

public interface TelegramService {
    String NAME = "itpearls_TelegramService";

    boolean sendMessageToChat(String tgToken, int chatId, String txt);

    boolean sendMessageToChat(int chatId, String txt);

    boolean sendMessageToChat(String chatId, String txt);

    boolean sendMessageToChat(String tgToken, String chatId, String txt);

    TelegramSendResult sendMessageToChatResult(String chatId, String txt);

    TelegramSendResult sendMessageToChatResult(String tgToken, String chatId, String txt);
}

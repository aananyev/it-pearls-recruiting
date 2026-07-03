package com.company.hunttech.core;

public interface TelegramService {
    String NAME = "hunttech_TelegramService";

    boolean sendMessageToChat(String tgToken, int chatId, String txt);

    boolean sendMessageToChat(int chatId, String txt);

    boolean sendMessageToChat(String chatId, String txt);

    boolean sendMessageToChat(String tgToken, String chatId, String txt);

    TelegramSendResult sendMessageToChatResult(String chatId, String txt);

    TelegramSendResult sendMessageToChatResult(String tgToken, String chatId, String txt);
}

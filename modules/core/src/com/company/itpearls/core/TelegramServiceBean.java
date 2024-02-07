package com.company.itpearls.core;

import com.company.itpearls.core.telegrambot.TelegramBotStatus;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.io.*;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;

@Service(TelegramService.NAME)
public class TelegramServiceBean implements TelegramService {

    private static HttpURLConnection con;
    @Inject
    private ApplicationSetupService applicationSetupService;
    @Inject
    private TextManipulationService textManipulationService;
    @Inject
    private TelegramBotService telegramBotService;

    @Override
    public void sendMessageToChat(String txt) {
        if (telegramBotService.isBotStarted()) {
            sendMessageToChat(applicationSetupService.getTelegramToken(),
                    applicationSetupService.getTelegramChatOpenPosition(),
                    txt);
        }

    }
    @Override
    public void sendMessageToChat(String tgToken, int chatId, String txt) {
        sendMessageToChat(tgToken, String.valueOf(chatId), txt);
    }

    @Override
    public void sendMessageToChat(String chatId, String txt) {
        sendMessageToChat(applicationSetupService.getTelegramToken(), chatId, txt);
    }
    @Override
    public void sendMessageToBot(String message) {
        if (TelegramBotStatus.getChatId() != null) {
            TelegramBotStatus.getBot().sendAnswer(TelegramBotStatus.getChatId(), message);
        }
    }

    @Override
    public void sendMessageToBotWithSetting(String message) {
        if (TelegramBotStatus.getChatId() != null) {
            TelegramBotStatus.getBot().sendAnswerWithSettings(TelegramBotStatus.getChatId(), message);
        }
    }

    @Override
    public void sendMessageToBot(long chatId, String message) {
        TelegramBotStatus.getBot().sendAnswer(chatId, message);
    }

    @Override
    public void sendMessageToBotWithSetting(long chatId, String message) {
        TelegramBotStatus.getBot().sendAnswerWithSettings(chatId, message);
    }

    @Override
    public void sendMessageToChat(String tgToken, String chatId, String txt) {
        if (telegramBotService.isBotStarted()) {
            String urlParameters = "chat_id=" + chatId + "&text="
                    + textManipulationService.formattedHtml2text(txt);
            byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
            String urlToken = "https://api.telegram.org/bot" + tgToken + "/sendMessage";

            try {
                URL url = new URL(urlToken);
                con = (HttpURLConnection) url.openConnection();

                con.setDoOutput(true);
                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", "Java upread.ru client");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
                    wr.write(postData);
                }

                StringBuilder content = new StringBuilder();

                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream()))) {
                    String line;
                    content = new StringBuilder();

                    while ((line = br.readLine()) != null) {
                        content.append(line);
                        content.append(System.lineSeparator());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.println(content.toString());
                }

            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (ProtocolException e) {
                throw new RuntimeException(e);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                con.disconnect();
            }
        }
    }
}
package com.company.itpearls.web.screens.openposition;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class GigaChatInvite {
    private static final String GIGA_CHAT_API_KEY = "YOUR_GIGA_CHAT_API_KEY";
    private static final String GIGA_CHAT_BASE_URL = "https://api.gigachat.io/v1/";

    public static String getGigaChaiInvite() {
        String jobDescription = "Java Developer"; // Описание вакансии
        String candidateName = "John Doe"; // Имя кандидата
        String candidateEmail = "john.doe@example.com"; // Адрес электронной почты кандидата
        String candidatePhone = "+1-555-555-5555"; // Номер телефона кандидата

        try {
            // Формируем URL для отправки запроса на создание собеседования
            String url = GIGA_CHAT_BASE_URL + "conversations";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // Устанавливаем заголовки запроса
            con.setRequestMethod("POST");
            con.setRequestProperty("Authorization", "Bearer " + GIGA_CHAT_API_KEY);
            con.setRequestProperty("Content-Type", "application/json");

            // Создаем тело запроса
            String body = "{\"title\": \"Interview with " + candidateName + "\", \"body\": \"" + jobDescription + "\", \"email\": \"" + candidateEmail + "\", \"phone\": \"" + candidatePhone + "\"}";
            con.setDoOutput(true);
            con.getOutputStream().write(body.getBytes());

            // Отправляем запрос и получаем ответ
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Выводим ответ на консоль
            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return jobDescription;
    }
}


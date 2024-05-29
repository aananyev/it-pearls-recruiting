package com.company.itpearls.service;

import com.google.api.client.http.HttpRequestInitializer;
import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@Service(GoogleCalendarService.NAME)
public class GoogleCalendarServiceBean implements GoogleCalendarService {
    private static final String APPLICATION_NAME = "Google Calendar API Java";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    private static Calendar calendarService;

    private static void authorize() throws GeneralSecurityException, IOException {
        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        // Load client secrets
        InputStream in = Calendar.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        List<String> scopes = Collections.singletonList(CalendarScopes.CALENDAR);

        // Initialize the authorization code flow
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, JSON_FACTORY, clientSecrets, scopes)
                .build();

        // Authorize
        String authorizationCode = "YOUR_AUTHORIZATION_CODE";
        GoogleAuthorizationCodeTokenRequest tokenRequest = flow.newTokenRequest(authorizationCode);
        GoogleTokenResponse tokenResponse = tokenRequest.execute();

        // Create the calendar service
        calendarService = new Calendar.Builder(httpTransport, JSON_FACTORY, (HttpRequestInitializer) tokenResponse)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static void addEvent(String summary, String description, String location, Date startDate, Date endDate) throws IOException, GeneralSecurityException {
        Event event = new Event()
                .setSummary(summary)
                .setDescription(description);

        EventDateTime startDateTime = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(startDate))
                .setTimeZone("UTC");
        event.setStart(startDateTime);

        EventDateTime endDateTime = new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(endDate))
                .setTimeZone("UTC");
        event.setEnd(endDateTime);

        Event createdEvent = calendarService.events().insert("primary", event).execute();
        System.out.println("Event created: " + createdEvent.getHtmlLink());
    }

    public static void start(String[] args) {
        try {
            authorize();
            addEvent("Test Event", "This is a test event", "New York", new Date(), new Date(new Date().getTime() + 3600000));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
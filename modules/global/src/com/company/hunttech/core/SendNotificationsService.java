package com.company.hunttech.core;

public interface SendNotificationsService {
    String NAME = "hunttech_SendNotificationsService";

    void SendEmail(String subject, String body);
}
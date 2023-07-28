package com.company.itpearls.core;

public interface SendNotificationsService {
    String NAME = "itpearls_SendNotificationsService";

    void SendEmail(String subject, String body);
}
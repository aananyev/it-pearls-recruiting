package com.company.itpearls.core;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.core.app.EmailService;
import com.haulmont.cuba.core.global.EmailInfo;
import com.haulmont.cuba.core.global.EmailInfoBuilder;
import com.haulmont.cuba.core.global.UserSessionSource;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Collections;

@Service(SendNotificationsService.NAME)
public class SendNotificationsServiceBean implements SendNotificationsService {

    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private EmailService emailService;

    @Override
    public void SendEmail(String subject, String body) {
        EmailInfo emailInfo = EmailInfoBuilder.create()
                .setAddresses(userSessionSource.getUserSession().getUser().getEmail())
                .setCaption(subject)
                .setFrom(null)
                .setTemplatePath("com/company/itpearls/templates/close_position.txt")
                .setTemplateParameters(Collections.singletonMap("body", body))
                .build();

        emailService.sendEmailAsync(emailInfo);
    }
}
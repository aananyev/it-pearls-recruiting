package com.company.hunttech.core;

import com.company.hunttech.entity.ApplicationSetup;
import com.haulmont.cuba.core.entity.FileDescriptor;

public interface ApplicationSetupService {
    String NAME = "hunttech_ApplicationSetupService";

    String getTelegramBotName();

    Boolean getTelegramBotStart();

    String getTelegramToken();

    String getTelegramChatOpenPosition();

    FileDescriptor getCompanyImage();

    FileDescriptor getActiveCompanyIcon();

    FileDescriptor getActiveCompanyLogo();

    FileDescriptor getCompanyIcon();

    ApplicationSetup getActiveApplicationSetup();

    void clearActiveApplicationSetup();

    void clearActiveApplicationSetup(ApplicationSetup current);

    String getActiveConfigName();
}
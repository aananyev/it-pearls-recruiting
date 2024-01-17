package com.company.itpearls.core;

import com.company.itpearls.entity.ApplicationSetup;
import com.haulmont.cuba.core.entity.FileDescriptor;

public interface ApplicationSetupService {
    String NAME = "itpearls_ApplicationSetupService";

    String getTelegramBotName();

    Boolean getTelegramBotStart();

    String getTelegramToken();

    String getTelegramChatOpenPosition();

    FileDescriptor getCompanyImage();

    ApplicationSetup getApplicationSetup();

    FileDescriptor getActiveCompanyIcon();

    FileDescriptor getActiveCompanyLogo();

    FileDescriptor getCompanyIcon();

    ApplicationSetup getActiveApplicationSetup();

    void clearActiveApplicationSetup();

    void clearActiveApplicationSetup(ApplicationSetup current);

    String getActiveConfigName();
}